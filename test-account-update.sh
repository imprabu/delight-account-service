#!/bin/bash

# Script to test account update and verify message posting
# This script will:
# 1. Find an existing account in the database
# 2. Update it to trigger the account update event
# 3. Check logs to verify message was posted

echo "=== Testing Account Update Message Posting ==="

# Check if service is running
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "❌ Service is not running on port 8080"
    exit 1
fi

echo "✅ Service is running"

# Get database connection details from application.properties
DB_URL="${DB_URL:-jdbc:mysql://127.0.0.1:4000/delight-accounts}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"

# Extract host, port, and database from JDBC URL
# Format: jdbc:mysql://host:port/database
HOST_PORT_DB=$(echo "$DB_URL" | sed -n 's/.*:\/\/\([^\/]*\)\/\(.*\)/\1|\2/p')
HOST_PORT=$(echo "$HOST_PORT_DB" | cut -d'|' -f1)
DB_NAME=$(echo "$HOST_PORT_DB" | cut -d'|' -f2)
HOST=$(echo "$HOST_PORT" | cut -d':' -f1)
PORT=$(echo "$HOST_PORT" | cut -d':' -f2)

echo "Connecting to database: $HOST:$PORT/$DB_NAME"

# Check if mysql client is available
if ! command -v mysql &> /dev/null; then
    echo "⚠️  mysql client not found. Please install MySQL client or use a different method to update the account."
    echo "You can manually update an account using:"
    echo "  UPDATE accounts SET company_name = CONCAT(company_name, '_updated') WHERE id = <account_id>;"
    exit 1
fi

# Find an existing account
echo "Finding an existing account..."
ACCOUNT_ID=$(mysql -h"$HOST" -P"$PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -N -e "SELECT id FROM accounts LIMIT 1;" 2>/dev/null)

if [ -z "$ACCOUNT_ID" ]; then
    echo "❌ No accounts found in database. Please create an account first."
    exit 1
fi

echo "✅ Found account with ID: $ACCOUNT_ID"

# Get current company name
CURRENT_NAME=$(mysql -h"$HOST" -P"$PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -N -e "SELECT company_name FROM accounts WHERE id = $ACCOUNT_ID;" 2>/dev/null)
echo "Current company name: $CURRENT_NAME"

# Update the account
NEW_NAME="${CURRENT_NAME}_test_$(date +%s)"
echo "Updating account company_name to: $NEW_NAME"

mysql -h"$HOST" -P"$PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "UPDATE accounts SET company_name = '$NEW_NAME' WHERE id = $ACCOUNT_ID;" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ Account updated successfully"
    echo ""
    echo "Waiting 5 seconds for async job processing..."
    sleep 5
    echo ""
    echo "=== Checking logs for message posting ==="
    tail -n 100 logs/application.log | grep -E "(ACCOUNT-EVENT|MESSAGING|ACCOUNT-EVENT-WORKER)" | tail -n 20
else
    echo "❌ Failed to update account"
    exit 1
fi

