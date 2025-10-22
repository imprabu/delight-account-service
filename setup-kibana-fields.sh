#!/bin/bash

# Script to help configure Kibana for proper field display
# This addresses the issue where fields appear nested instead of as individual columns

echo "🔧 Setting up Kibana for Clean Field Display"
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

echo ""
echo "📊 Current Field Structure:"
curl -s "http://localhost:9200/delight-account-service-*/_search?size=1" | jq -r '.hits.hits[0]._source | keys[]' | sort

echo ""
echo "🎯 Clean Log Structure (what you want to see):"
curl -s "http://localhost:9200/delight-account-service-*/_search?size=1" | jq -r '.hits.hits[0]._source | {timestamp, level, logger, message, requestId, domain, accountId, method, uri, app_service}'

echo ""
echo "🌐 Next Steps in Kibana:"
echo "1. Open: http://localhost:5601/app/discover"
echo "2. Select index pattern: delight-account-service-*"
echo "3. Click 'Add field' and add these columns:"
echo "   • @timestamp"
echo "   • level"
echo "   • logger" 
echo "   • message"
echo "   • requestId"
echo "   • domain"
echo "   • accountId"
echo "   • method"
echo "   • uri"
echo "   • app_service"
echo ""
echo "4. Remove unwanted fields (agent, ecs, host, input, log)"
echo "5. Save the search as 'Application Logs Clean View'"
echo ""
echo "📚 The fields ARE at root level - you just need to configure Kibana to display them as columns!"
