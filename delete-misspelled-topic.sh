#!/bin/bash

# Script to delete the misspelled Kafka topic
# Usage: ./delete-misspelled-topic.sh [bootstrap-servers]
# Example: ./delete-misspelled-topic.sh localhost:9092

BOOTSTRAP_SERVERS=${1:-localhost:9092}

echo "⚠️  WARNING: This will delete the misspelled topic: account_actvities_v1"
echo "Bootstrap servers: $BOOTSTRAP_SERVERS"
echo ""
read -p "Are you sure you want to delete this topic? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Cancelled."
    exit 0
fi

echo ""
echo "Deleting topic: account_actvities_v1"
kafka-topics.sh --delete \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --topic account_actvities_v1

echo ""
echo "✅ Topic deleted successfully!"
echo ""
echo "Verifying remaining topics..."
kafka-topics.sh --list --bootstrap-server $BOOTSTRAP_SERVERS | grep account

