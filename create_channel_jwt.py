#!/usr/bin/env python3
"""
Script to create a valid Channel JWT token for testing purposes.
This script creates a JWT token that can be used with the Channel API endpoints.
"""

import jwt
import json
import sys
from datetime import datetime, timedelta

def create_channel_jwt(account_id=None, user_id=None, key_version="v1", secret_key=None):
    """
    Create a channel JWT token with the specified parameters.
    
    Args:
        account_id (str, optional): The account ID to include in the token. If None, account will be resolved from domain.
        user_id (str, optional): The user ID to include in the token. If None, user will be resolved from request body JWT if available.
        key_version (str): The key version to use (v1 or v2)
        secret_key (str): The secret key for signing. If None, uses default.
    
    Returns:
        str: The JWT token
    """
    
    # Keys from envVariables_staging (base64 encoded)
    default_keys = {
        "v1": "zcPXq33YkT6zxK950bMR+I5bmEEJ/YZ/IFeR/ogeVVM=",
        "v2": "sQ35GIHp/YqoIxNtuutTHzWN0xgWLkAWZG0z2UArMe0="
    }
    
    if secret_key is None:
        secret_key = default_keys.get(key_version)
        if secret_key is None:
            raise ValueError(f"Unknown key version: {key_version}")
    
    # Create the payload
    now = datetime.utcnow()
    payload = {
        "key_version": key_version,
        "iat": now,
        "exp": now + timedelta(hours=1)  # Token expires in 1 hour
    }
    
    # Add optional account_id
    if account_id is not None:
        payload["account_id"] = str(account_id)  # Convert to string as expected by Java service
    
    # Add optional user_id
    if user_id is not None:
        payload["user_id"] = str(user_id)  # Convert to string as expected by Java service
    
    # Create the JWT token
    token = jwt.encode(payload, secret_key, algorithm="HS256")
    
    return token

def main():
    """Main function to create and display the JWT token."""
    
    # Parse command line arguments
    # Usage: python3 create_channel_jwt.py [account_id] [user_id] [key_version]
    account_id = sys.argv[1] if len(sys.argv) > 1 and sys.argv[1] != "None" else None
    user_id = sys.argv[2] if len(sys.argv) > 2 and sys.argv[2] != "None" else None
    key_version = sys.argv[3] if len(sys.argv) > 3 else "v1"
    
    try:
        # Create the JWT token
        token = create_channel_jwt(account_id, user_id, key_version)
        
        print(f"Channel JWT Token created successfully!")
        if account_id:
            print(f"Account ID: {account_id}")
        else:
            print(f"Account ID: (will be resolved from domain)")
        if user_id:
            print(f"User ID: {user_id}")
        else:
            print(f"User ID: (will be resolved from request body JWT if available)")
        print(f"Key Version: {key_version}")
        print(f"Token: {token}")
        print()
        print("You can use this token in the Authorization header:")
        print(f"Authorization: Bearer {token}")
        print()
        print("Note: If account_id is not in the token, the system will look up the account")
        print("      using the domain from the Host header.")
        print("      If user_id is not in the token, the system will extract it from the")
        print("      request body JWT token (for POST requests).")
        
        return token
        
    except Exception as e:
        print(f"Error creating JWT token: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()