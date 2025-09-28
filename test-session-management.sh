#!/bin/bash

# Session Management Test Script
# Make sure the application is running on http://localhost:8080

BASE_URL="http://localhost:8080"
COOKIE_JAR="cookies.txt"
COMPANY_NAME="Test Company"
# Convert company name to domain format (lowercase, no spaces)
DOMAIN=$(echo "$COMPANY_NAME" | tr '[:upper:]' '[:lower:]' | tr -d ' ')

echo "🚀 Testing Session Management Endpoints"
echo "========================================"
echo "Using domain: $DOMAIN"
echo ""

# Clean up any existing cookies
rm -f $COOKIE_JAR

echo ""
echo "1. 📝 Testing Sign Up (Create Account & User)"
echo "--------------------------------------------"
echo "Note: Signup uses localhost domain"
SIGNUP_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
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

# Check if signup was successful (HTTP 200 or 201)
SIGNUP_HTTP_STATUS=$(echo "$SIGNUP_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [[ "$SIGNUP_HTTP_STATUS" == "200" || "$SIGNUP_HTTP_STATUS" == "201" ]]; then
    echo "✅ Signup successful! Adding domain to hosts file..."
    # Add domain to hosts file (requires sudo)
    echo "127.0.0.1 $DOMAIN" | sudo tee -a /etc/hosts
    echo "Domain $DOMAIN added to hosts file"
else
    echo "❌ Signup failed with HTTP $SIGNUP_HTTP_STATUS. Exiting..."
    exit 1
fi
echo ""

echo "2. 🔐 Testing User Activation (Set Password)"
echo "--------------------------------------------"
echo "Note: Activation uses company domain: $DOMAIN"
ACTIVATION_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST "http://$DOMAIN:8080/account/activate" \
  -H "Content-Type: application/json" \
  -H "Host: $DOMAIN" \
  -d '{
    "emailAddress": "admin@'"$DOMAIN"'.com",
    "firstName": "Admin",
    "lastName": "User",
    "phoneNumber": "+1-555-123-4567",
    "password": "SecurePassword123!",
    "retypePassword": "SecurePassword123!"
  }')

echo "Response: $ACTIVATION_RESPONSE"
echo ""

echo "3. 🔐 Testing Sign In with Session Creation"
echo "-------------------------------------------"
echo "Note: Signin uses company domain: $DOMAIN"
SIGNIN_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST "http://$DOMAIN:8080/account/signin" \
  -H "Content-Type: application/json" \
  -H "Host: $DOMAIN" \
  -c "$COOKIE_JAR" \
  -d '{
    "emailAddress": "admin@'"$DOMAIN"'.com",
    "password": "SecurePassword123!"
  }')

echo "Response: $SIGNIN_RESPONSE"
echo ""

# Check if cookies were set
echo "3. 🍪 Checking Session Cookie"
echo "-----------------------------"
if [ -f $COOKIE_JAR ]; then
    echo "Cookies saved:"
    cat $COOKIE_JAR
else
    echo "No cookies found!"
fi
echo ""

echo "4. 🔍 Testing Session Validation"
echo "--------------------------------"
echo "Note: Session validation uses company domain: $DOMAIN"
VALIDATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -b $COOKIE_JAR \
  -H "Host: $DOMAIN" \
  -X GET "http://$DOMAIN:8080/accounts/session")

echo "Response: $VALIDATE_RESPONSE"
echo ""

echo "5. 🔄 Testing Session Update (Last Access Time)"
echo "-----------------------------------------------"
# The validation endpoint should automatically update last access time
# Let's call it again to see the updated timestamp
sleep 2
VALIDATE_RESPONSE2=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -b $COOKIE_JAR \
  -H "Host: $DOMAIN" \
  -X GET "http://$DOMAIN:8080/accounts/session")

echo "Response: $VALIDATE_RESPONSE2"
echo ""

echo "6. 🚪 Testing Single Session Logout"
echo "-----------------------------------"
echo "Note: Logout uses company domain: $DOMAIN"
LOGOUT_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -b $COOKIE_JAR \
  -H "Host: $DOMAIN" \
  -X POST "http://$DOMAIN:8080/accounts/logout")

echo "Response: $LOGOUT_RESPONSE"
echo ""

echo "7. ❌ Testing Session Validation After Logout"
echo "---------------------------------------------"
VALIDATE_AFTER_LOGOUT=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -b $COOKIE_JAR \
  -H "Host: $DOMAIN" \
  -X GET "http://$DOMAIN:8080/accounts/session")

echo "Response: $VALIDATE_AFTER_LOGOUT"
echo ""

echo "8. 🔐 Testing Sign In Again (for logout all test)"
echo "-------------------------------------------------"
SIGNIN_RESPONSE2=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -c $COOKIE_JAR \
  -X POST "http://$DOMAIN:8080/account/signin" \
  -H "Content-Type: application/json" \
  -H "User-Agent: curl/test-client-2" \
  -H "Host: $DOMAIN" \
  -d '{
    "emailAddress": "admin@'"$DOMAIN"'.com",
    "password": "SecurePassword123!"
  }')

echo "Response: $SIGNIN_RESPONSE2"
echo ""

echo "9. 🚪🚪 Testing Logout All Sessions"
echo "-----------------------------------"
echo "Note: Logout all uses company domain: $DOMAIN"
LOGOUT_ALL_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -b $COOKIE_JAR \
  -H "Host: $DOMAIN" \
  -X POST "http://$DOMAIN:8080/accounts/logout-all")

echo "Response: $LOGOUT_ALL_RESPONSE"
echo ""

echo "10. ❌ Testing Session Validation After Logout All"
echo "--------------------------------------------------"
VALIDATE_AFTER_LOGOUT_ALL=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -b $COOKIE_JAR \
  -H "Host: $DOMAIN" \
  -X GET "http://$DOMAIN:8080/accounts/session")

echo "Response: $VALIDATE_AFTER_LOGOUT_ALL"
echo ""

echo "🧹 Cleaning up test files"
echo "-------------------------"
rm -f $COOKIE_JAR

# Remove domain from hosts file
echo "Removing domain $DOMAIN from hosts file..."
sudo sed -i '' "/127.0.0.1 $DOMAIN/d" /etc/hosts
echo "Domain $DOMAIN removed from hosts file"

echo ""
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