#!/bin/bash

# Script to automatically create Kibana index pattern
# This helps set up the index pattern programmatically

echo "🔧 Setting up Kibana Index Pattern"
echo "=================================="

# Wait for Kibana to be ready
echo "⏳ Waiting for Kibana to be ready..."
timeout=60
while [ $timeout -gt 0 ]; do
    if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
        echo "✅ Kibana is ready"
        break
    fi
    echo -n "."
    sleep 2
    timeout=$((timeout - 2))
done

if [ $timeout -le 0 ]; then
    echo "❌ Kibana is not ready after 60 seconds"
    exit 1
fi

# Create index pattern via API
echo "📊 Creating index pattern..."

INDEX_PATTERN_RESPONSE=$(curl -s -X POST "http://localhost:5601/api/saved_objects/index-pattern" \
  -H "kbn-xsrf: true" \
  -H "Content-Type: application/json" \
  -d '{
    "attributes": {
      "title": "delight-account-service-*",
      "timeFieldName": "@timestamp"
    }
  }')

if echo "$INDEX_PATTERN_RESPONSE" | jq -e '.id' > /dev/null 2>&1; then
    echo "✅ Index pattern created successfully"
    echo "📋 Pattern ID: $(echo "$INDEX_PATTERN_RESPONSE" | jq -r '.id')"
else
    echo "⚠️  Index pattern might already exist or there was an issue"
    echo "Response: $INDEX_PATTERN_RESPONSE"
fi

echo ""
echo "🌐 Next steps:"
echo "  1. Open http://localhost:5601"
echo "  2. Go to Discover"
echo "  3. Select 'delight-account-service-*' index pattern"
echo "  4. Set time range to 'Last 7 days'"
echo "  5. You should see your logs!"
echo ""
echo "📚 If you still don't see logs:"
echo "  - Check time range (top-right corner)"
echo "  - Remove any active filters"
echo "  - Refresh the page"
