#!/bin/bash

# Script to bypass Kibana setup and go directly to logs
# This helps skip the integration setup and access your logs directly

echo "🚀 Bypassing Kibana Setup to Access Your Logs"
echo "============================================="

# Wait for Kibana to be ready
echo "⏳ Waiting for Kibana..."
timeout=30
while [ $timeout -gt 0 ]; do
    if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
        echo "✅ Kibana is ready"
        break
    fi
    echo -n "."
    sleep 2
    timeout=$((timeout - 2))
done

# Disable the setup guide
echo "🔧 Disabling Kibana setup guide..."

# Set up the default route to go directly to Discover
curl -s -X POST "http://localhost:5601/api/saved_objects/_bulk_create" \
  -H "kbn-xsrf: true" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "type": "config",
      "id": "8.11.0",
      "attributes": {
        "defaultRoute": "/app/discover",
        "telemetry:optIn": false,
        "xpack:reporting:enabled": false,
        "xpack:security:enabled": false
      }
    }
  ]' > /dev/null

echo "✅ Kibana configuration updated"

echo ""
echo "🌐 Direct Access URLs:"
echo "  • Discover (Logs): http://localhost:5601/app/discover"
echo "  • Stack Management: http://localhost:5601/app/management"
echo "  • Index Patterns: http://localhost:5601/app/management/kibana/indexPatterns"
echo ""
echo "📊 Your Data Status:"
curl -s "http://localhost:9200/delight-account-service-*/_count" | jq -r '"  • Total documents: " + (.count | tostring)'
echo ""
echo "🎯 Next Steps:"
echo "  1. Open: http://localhost:5601/app/discover"
echo "  2. Select index pattern: delight-account-service-*"
echo "  3. Set time range to 'Last 7 days'"
echo "  4. View your logs!"
