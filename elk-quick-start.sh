#!/bin/bash

# Quick start script for ELK stack with Podman
# This script provides a simple way to get the ELK stack running

set -e

echo "🚀 Starting ELK Stack for Delight Account Service"
echo "================================================"

# Check if podman is available
if ! command -v podman &> /dev/null; then
    echo "❌ Podman is not installed. Please install Podman first."
    echo "   Visit: https://podman.io/getting-started/installation"
    exit 1
fi

# Check if podman-compose is available
if ! command -v podman-compose &> /dev/null; then
    echo "📦 Installing podman-compose..."
    if command -v pip3 &> /dev/null; then
        pip3 install podman-compose
    else
        echo "❌ pip3 not found. Please install podman-compose manually:"
        echo "   pip3 install podman-compose"
        exit 1
    fi
fi

# Create necessary directories
echo "📁 Creating directories..."
mkdir -p logs
mkdir -p elk-config/logstash/pipeline
mkdir -p elk-config/logstash/config
mkdir -p elk-config/filebeat

# Start the ELK stack
echo "🐳 Starting ELK stack containers..."
podman-compose -f elk-stack-compose.yaml up -d

echo "⏳ Waiting for services to start..."
echo "   This may take a few minutes on first run..."

# Wait for Elasticsearch
echo "🔍 Waiting for Elasticsearch..."
timeout=120
while [ $timeout -gt 0 ]; do
    if curl -s http://localhost:9200/_cluster/health > /dev/null 2>&1; then
        echo "✅ Elasticsearch is ready"
        break
    fi
    echo -n "."
    sleep 2
    timeout=$((timeout - 2))
done

if [ $timeout -le 0 ]; then
    echo "❌ Elasticsearch failed to start within 2 minutes"
    echo "   Check logs with: podman-compose -f elk-stack-compose.yaml logs elasticsearch"
    exit 1
fi

# Wait for Kibana
echo "📊 Waiting for Kibana..."
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

echo ""
echo "🎉 ELK Stack is running!"
echo "========================"
echo ""
echo "📊 Access your services:"
echo "   • Kibana Dashboard: http://localhost:5601"
echo "   • Elasticsearch:    http://localhost:9200"
echo "   • Logstash:         http://localhost:9600"
echo ""
echo "📝 Your application logs will be automatically collected from:"
echo "   • ./logs/application.log"
echo ""
echo "🔧 Management commands:"
echo "   • View logs:        ./elk-deploy.sh logs"
echo "   • Check status:     ./elk-deploy.sh status"
echo "   • Stop stack:       ./elk-deploy.sh stop"
echo "   • Restart stack:    ./elk-deploy.sh restart"
echo ""
echo "📚 Next steps:"
echo "   1. Start your application to generate logs"
echo "   2. Open Kibana at http://localhost:5601"
echo "   3. Create an index pattern for 'delight-account-service-*'"
echo "   4. Explore your logs in the Discover tab"
echo ""
echo "Happy logging! 🚀"
