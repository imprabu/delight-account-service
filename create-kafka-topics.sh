#!/bin/bash

# Script to create required Kafka topics for IAM Service
# Usage: ./create-kafka-topics.sh [bootstrap-servers]
# Example: ./create-kafka-topics.sh localhost:9092

BOOTSTRAP_SERVERS=${1:-localhost:9092}

echo "Creating Kafka topics for IAM Service..."
echo "Bootstrap servers: $BOOTSTRAP_SERVERS"
echo ""

# Topic: account_activities_v1
# Purpose: Account lifecycle events (account.created, account.updated, account.deleted)
echo "Creating topic: account_activities_v1"
kafka-topics.sh --create \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --topic account_activities_v1 \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# Topic: user_activities_v1
# Purpose: User table changes only (user.created, user.updated, user.deleted)
echo "Creating topic: user_activities_v1"
kafka-topics.sh --create \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --topic user_activities_v1 \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# Topic: user_role_activities_v1
# Purpose: Membership and role changes (user.membership.added, user.membership.updated, user.membership.removed, user.role.changed)
echo "Creating topic: user_role_activities_v1"
kafka-topics.sh --create \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --topic user_role_activities_v1 \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

echo ""
echo "✅ All topics created successfully!"
echo ""
echo "Verifying topics..."
kafka-topics.sh --list --bootstrap-server $BOOTSTRAP_SERVERS | grep -E "(account_activities_v1|user_activities_v1|user_role_activities_v1)"

