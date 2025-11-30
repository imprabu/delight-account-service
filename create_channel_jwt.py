#!/usr/bin/env python3
"""
Script to create a valid Channel JWT token for testing purposes.
This script creates a JWT token that can be used with the Channel API endpoints.
"""

import jwt
import json
import sys
from datetime import datetime, timedelta

def create_channel_jwt(account_id="1", key_version="v1", secret_key=None):
    """
    Create a channel JWT token with the specified parameters.
    
    Args:
        account_id (str): The account ID to include in the token
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
        "account_id": str(account_id),  # Convert to string as expected by Java service
        "key_version": key_version,
        "iat": now,
        "exp": now + timedelta(hours=1)  # Token expires in 1 hour
    }
    
    # Create the JWT token
    token = jwt.encode(payload, secret_key, algorithm="HS256")
    
    return token

def main():
    """Main function to create and display the JWT token."""
    
    # Parse command line arguments
    account_id = sys.argv[1] if len(sys.argv) > 1 else "1"
    key_version = sys.argv[2] if len(sys.argv) > 2 else "v1"
    
    try:
        # Create the JWT token
        token = create_channel_jwt(account_id, key_version)
        
        print(f"Channel JWT Token created successfully!")
        print(f"Account ID: {account_id}")
        print(f"Key Version: {key_version}")
        print(f"Token: {token}")
        print()
        print("You can use this token in the Authorization header:")
        print(f"Authorization: Bearer {token}")
        print()
        print("Or in the request body for POST requests:")
        print(f'{{"jwt": "{token}"}}')
        
        return token
        
    except Exception as e:
        print(f"Error creating JWT token: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()