#!/bin/bash

# ELK Stack Verification Script
# This script verifies that your ELK stack is working correctly

echo "🔍 ELK Stack Verification"
echo "========================"

# Check Elasticsearch health
echo "📊 Checking Elasticsearch..."
if curl -s http://localhost:9200/_cluster/health > /dev/null; then
    echo "✅ Elasticsearch is running"
else
    echo "❌ Elasticsearch is not accessible"
    exit 1
fi

# Check Kibana
echo "📊 Checking Kibana..."
if curl -s http://localhost:5601/api/status > /dev/null; then
    echo "✅ Kibana is running"
else
    echo "❌ Kibana is not accessible"
    exit 1
fi

# Check Logstash
echo "📊 Checking Logstash..."
if curl -s http://localhost:9600/_node/stats > /dev/null; then
    echo "✅ Logstash is running"
else
    echo "❌ Logstash is not accessible"
fi

# Check data in Elasticsearch
echo "📊 Checking data in Elasticsearch..."
TOTAL_DOCS=$(curl -s "http://localhost:9200/delight-account-service-*/_count" | jq '.count')
echo "📝 Total documents: $TOTAL_DOCS"

if [ "$TOTAL_DOCS" -gt 0 ]; then
    echo "✅ Data is present in Elasticsearch"
    
    # Show latest documents
    echo "📋 Latest log entries:"
    curl -s "http://localhost:9200/delight-account-service-*/_search?size=3&sort=@timestamp:desc" | jq -r '.hits.hits[] | "  • \(._source.timestamp) [\(._source.level)] \(._source.logger): \(._source.message)"'
else
    echo "❌ No data found in Elasticsearch"
fi

echo ""
echo "🌐 Access your logs:"
echo "  • Kibana: http://localhost:5601"
echo "  • Elasticsearch: http://localhost:9200"
echo ""
echo "📚 Setup instructions:"
echo "  1. Open http://localhost:5601"
echo "  2. Go to Stack Management > Index Patterns"
echo "  3. Create index pattern: delight-account-service-*"
echo "  4. Select @timestamp as time field"
echo "  5. Go to Discover to view your logs"
