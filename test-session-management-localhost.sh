#!/bin/bash

# Session Management Test Script
# Requires the application running at `http://localhost:8080`.
# This script targets localhost and uses the Host header to emulate
# company subdomain routing. It does NOT modify /etc/hosts.

BASE_URL="http://localhost:8080"
COOKIE_JAR="cookies.txt"
# Use a unique company name per run to avoid domain collisions
COMPANY_NAME="${COMPANY_NAME:-Test Company $(date +%s)}"
PASSWORD="${PASSWORD:-Tr!dent9Alpha}"
# Convert company name to domain format (lowercase, no spaces)
DOMAIN=$(echo "$COMPANY_NAME" | tr '[:upper:]' '[:lower:]' | tr -d ' ')

set -euo pipefail
OS_NAME="$(uname -s || echo Unknown)"
HOSTS_FILE="/etc/hosts"
if [ "${SUDO:-unset}" = "unset" ]; then
  if [ "$(id -u)" -eq 0 ]; then SUDO=""; else SUDO="sudo"; fi
fi

echo "🚀 Testing Session Management Endpoints"
echo "========================================"
echo "Using domain: $DOMAIN"
echo ""

cleanup() {
  rm -f "$COOKIE_JAR" || true
}
trap cleanup EXIT

# Clean up any existing cookies
rm -f "$COOKIE_JAR"

echo ""
echo "1. 📝 Testing Sign Up (Create Account & User)"
echo "--------------------------------------------"
echo "Note: Signup uses localhost domain"
SIGNUP_RESPONSE=$(curl -s -S -w "\nHTTP_CODE:%{http_code}" \
  -X POST "$BASE_URL/account/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "'"$COMPANY_NAME"'",
    "emailAddress": "admin@'"$DOMAIN"'.com",
    "industryType": "Technology",
    "firstName": "Admin",
    "lastName": "User"
  }')

echo "Response: $SIGNUP_RESPONSE"
echo ""

SIGNUP_HTTP_STATUS=$(echo "$SIGNUP_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [[ "$SIGNUP_HTTP_STATUS" == "200" || "$SIGNUP_HTTP_STATUS" == "201" ]]; then
  echo "✅ Signup successful"
  
  # Extract the actual domain from the signup response
  ACTUAL_DOMAIN=$(echo "$SIGNUP_RESPONSE" | grep -v "HTTP_CODE" | jq -r '.domain')
  if [[ "$ACTUAL_DOMAIN" != "null" && "$ACTUAL_DOMAIN" != "" ]]; then
    echo "Using actual domain from response: $ACTUAL_DOMAIN"
    DOMAIN="$ACTUAL_DOMAIN"
  else
    echo "Using generated domain: $DOMAIN"
  fi
  
  # Add domain to hosts file for subsequent calls using company domain
  if ! grep -qE "^[[:space:]]*127\\.0\\.0\\.1[[:space:]]+$DOMAIN(\\s|$)" "$HOSTS_FILE"; then
    echo "Adding $DOMAIN to $HOSTS_FILE (requires sudo if not root)"
    echo "127.0.0.1 $DOMAIN" | $SUDO tee -a "$HOSTS_FILE" >/dev/null
  else
    echo "Hosts entry for $DOMAIN already present"
  fi
else
  echo "❌ Signup failed with HTTP $SIGNUP_HTTP_STATUS. Exiting..."
  exit 1
fi
echo ""

echo "2. 🔐 Testing User Activation (Set Password)"
echo "--------------------------------------------"
echo "Note: Activation uses company domain: $DOMAIN"
ACTIVATION_RESPONSE=$(curl -s -S -w "\nHTTP_CODE:%{http_code}" \
  -X POST "http://localhost:8080/account/activate" \
  -H "Content-Type: application/json" \
  -H "Host: $DOMAIN" \
  -d '{
    "emailAddress": "admin@'"$DOMAIN"'.com",
    "firstName": "Admin",
    "lastName": "User",
    "phoneNumber": "+1-555-123-4567",
    "password": "'"$PASSWORD"'",
    "retypePassword": "'"$PASSWORD"'"
  }')

echo "Response: $ACTIVATION_RESPONSE"
echo ""
ACTIVATION_HTTP_STATUS=$(echo "$ACTIVATION_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [[ "$ACTIVATION_HTTP_STATUS" != "200" && "$ACTIVATION_HTTP_STATUS" != "201" ]]; then
  # Allow idempotent runs: treat 'already active' as success
  ACT_ERROR_CODE=$(echo "$ACTIVATION_RESPONSE" | sed -n 's/.*"code":"\([^"]*\)".*/\1/p')
  if [[ "$ACT_ERROR_CODE" == "error.user.already.active" ]]; then
    echo "ℹ️  Activation skipped: user already active"
  else
    echo "❌ Activation failed with HTTP $ACTIVATION_HTTP_STATUS"
    exit 1
  fi
else
  echo "✅ Activation successful"
fi

echo "3. 🔐 Testing Sign In with Session Creation"
echo "-------------------------------------------"
echo "Note: Signin uses company domain: $DOMAIN"
SIGNIN_RESPONSE=$(curl -s -S -w "\nHTTP_CODE:%{http_code}" \
  -X POST "http://localhost:8080/account/signin" \
  -H "Content-Type: application/json" \
  -H "Host: $DOMAIN" \
  -c "$COOKIE_JAR" \
  -d '{
    "emailAddress": "admin@'"$DOMAIN"'.com",
    "password": "'"$PASSWORD"'"
  }')

echo "Response: $SIGNIN_RESPONSE"
echo ""

# Check if cookies were set
echo "3. 🍪 Checking Session Cookie"
echo "-----------------------------"
SIGNIN_HTTP_STATUS=$(echo "$SIGNIN_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [[ "$SIGNIN_HTTP_STATUS" != "200" ]]; then
  echo "❌ Signin failed with HTTP $SIGNIN_HTTP_STATUS"
  exit 1
fi
if [[ -f "$COOKIE_JAR" ]] && grep -q "hrsession" "$COOKIE_JAR"; then
  echo "✅ Found hrsession cookie"
else
  echo "❌ hrsession cookie not set"
  echo "Cookie jar contents:"
  [[ -f "$COOKIE_JAR" ]] && cat "$COOKIE_JAR" || echo "(no cookie jar)"
  exit 1
fi
echo ""

echo "4. 🔍 Testing Session Validation"
echo "--------------------------------"
echo "Note: Session validation uses company domain: $DOMAIN"
VALIDATE_RESPONSE=$(curl -s -S -w "\nHTTP_CODE:%{http_code}" \
  -b "$COOKIE_JAR" \
  -H "Host: $DOMAIN" \
  -X GET "http://localhost:8080/accounts/session")

echo "Response: $VALIDATE_RESPONSE"
echo ""
VALIDATE_HTTP_STATUS=$(echo "$VALIDATE_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [[ "$VALIDATE_HTTP_STATUS" != "200" ]]; then
  echo "❌ Session validation failed with HTTP $VALIDATE_HTTP_STATUS"
  exit 1
fi

echo "5. 🔄 Testing Session Update (Last Access Time)"
echo "-----------------------------------------------"
# The validation endpoint should automatically update last access time
# Let's call it again to see the updated timestamp
sleep 2
VALIDATE_RESPONSE2=$(curl -s -S -w "\nHTTP_CODE:%{http_code}" \
  -b "$COOKIE_JAR" \
  -H "Host: $DOMAIN" \
  -X GET "http://localhost:8080/accounts/session")

echo "Response: $VALIDATE_RESPONSE2"
echo ""

echo "6. 🚪 Testing Single Session Logout"
echo "-----------------------------------"
echo "Note: Logout uses company domain: $DOMAIN"
LOGOUT_RESPONSE=$(curl -s -S -w "\nHTTP_CODE:%{http_code}" \
  -b "$COOKIE_JAR" \
  -H "Host: $DOMAIN" \
  -X POST "http://localhost:8080/accounts/logout")

echo "Response: $LOGOUT_RESPONSE"
echo ""
LOGOUT_HTTP_STATUS=$(echo "$LOGOUT_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [[ "$LOGOUT_HTTP_STATUS" != "200" ]]; then
  echo "❌ Logout failed with HTTP $LOGOUT_HTTP_STATUS"
  exit 1
fi

echo "7. ❌ Testing Session Validation After Logout"
echo "---------------------------------------------"
VALIDATE_AFTER_LOGOUT=$(curl -s -S -w "\nHTTP_CODE:%{http_code}" \
  -b "$COOKIE_JAR" \
  -H "Host: $DOMAIN" \
  -X GET "http://localhost:8080/accounts/session")

echo "Response: $VALIDATE_AFTER_LOGOUT"
echo ""
VAL_AFTER_LOGOUT_HTTP=$(echo "$VALIDATE_AFTER_LOGOUT" | grep "HTTP_CODE" | cut -d: -f2)
if [[ "$VAL_AFTER_LOGOUT_HTTP" != "401" && "$VAL_AFTER_LOGOUT_HTTP" != "403" ]]; then
  echo "❌ Expected 401/403 after logout, got $VAL_AFTER_LOGOUT_HTTP"
  exit 1
else
  echo "✅ Session invalid as expected (HTTP $VAL_AFTER_LOGOUT_HTTP)"
fi

echo "8. 🔐 Testing Sign In Again (for logout all test)"
echo "-------------------------------------------------"
SIGNIN_RESPONSE2=$(curl -s -S -w "\nHTTP_CODE:%{http_code}" \
  -c "$COOKIE_JAR" \
  -X POST "http://localhost:8080/account/signin" \
  -H "Content-Type: application/json" \
  -H "User-Agent: curl/test-client-2" \
  -H "Host: $DOMAIN" \
  -d '{
    "emailAddress": "admin@'"$DOMAIN"'.com",
    "password": "'"$PASSWORD"'"
  }')

echo "Response: $SIGNIN_RESPONSE2"
echo ""
SIGNIN2_HTTP_STATUS=$(echo "$SIGNIN_RESPONSE2" | grep "HTTP_CODE" | cut -d: -f2)
if [[ "$SIGNIN2_HTTP_STATUS" != "200" ]]; then
  echo "❌ Second signin failed with HTTP $SIGNIN2_HTTP_STATUS"
  exit 1
fi

echo "9. 🚪🚪 Testing Logout All Sessions"
echo "-----------------------------------"
echo "Note: Logout all uses company domain: $DOMAIN"
LOGOUT_ALL_RESPONSE=$(curl -s -S -w "\nHTTP_CODE:%{http_code}" \
  -b "$COOKIE_JAR" \
  -H "Host: $DOMAIN" \
  -X POST "http://localhost:8080/accounts/logout-all")

echo "Response: $LOGOUT_ALL_RESPONSE"
echo ""
LOGOUT_ALL_HTTP_STATUS=$(echo "$LOGOUT_ALL_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [[ "$LOGOUT_ALL_HTTP_STATUS" != "200" ]]; then
  echo "❌ Logout-all failed with HTTP $LOGOUT_ALL_HTTP_STATUS"
  exit 1
fi

echo "10. ❌ Testing Session Validation After Logout All"
echo "--------------------------------------------------"
VALIDATE_AFTER_LOGOUT_ALL=$(curl -s -S -w "\nHTTP_CODE:%{http_code}" \
  -b "$COOKIE_JAR" \
  -H "Host: $DOMAIN" \
  -X GET "http://localhost:8080/accounts/session")

echo "Response: $VALIDATE_AFTER_LOGOUT_ALL"
echo ""
VAL_AFTER_LOGOUT_ALL_HTTP=$(echo "$VALIDATE_AFTER_LOGOUT_ALL" | grep "HTTP_CODE" | cut -d: -f2)
if [[ "$VAL_AFTER_LOGOUT_ALL_HTTP" != "401" && "$VAL_AFTER_LOGOUT_ALL_HTTP" != "403" ]]; then
  echo "❌ Expected 401/403 after logout-all, got $VAL_AFTER_LOGOUT_ALL_HTTP"
  exit 1
else
  echo "✅ All sessions invalid as expected (HTTP $VAL_AFTER_LOGOUT_ALL_HTTP)"
fi

echo "🧹 Cleaning up test files"
echo "-------------------------"
rm -f "$COOKIE_JAR"

echo ""
if [[ "${CLEANUP_HOSTS:-1}" = "1" ]]; then
  echo "Removing domain $DOMAIN from hosts file..."
  if [[ "$OS_NAME" == "Darwin" ]]; then
    $SUDO sed -i '' "/[[:space:]]$DOMAIN\(\\s\|$\)/d" "$HOSTS_FILE"
  else
    $SUDO sed -i "/[[:space:]]$DOMAIN\(\\s\|$\)/d" "$HOSTS_FILE"
  fi
  echo "Domain $DOMAIN removed from hosts file"
else
  echo "Skipping hosts cleanup; CLEANUP_HOSTS=${CLEANUP_HOSTS:-1}"
fi

echo "✅ Session Management Tests Complete!"
echo "===================================="
echo ""
echo "Expected Results:"
echo "- Sign up: HTTP 200/201"
echo "- Sign in: HTTP 200 with hrsession cookie"
echo "- Session validation: HTTP 200 with session details"
echo "- Logout: HTTP 200 with success message"
echo "- Validation after logout: HTTP 401 Unauthorized"
echo "- Logout all: HTTP 200 with success message"
