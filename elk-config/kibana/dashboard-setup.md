# Kibana Dashboard Setup for Delight Account Service

This guide will help you set up Kibana dashboards to visualize your application logs.

## Initial Setup

1. **Access Kibana**: Open http://localhost:5601 in your browser
2. **Create Index Pattern**: 
   - Go to Stack Management > Index Patterns
   - Click "Create index pattern"
   - Enter pattern: `delight-account-service-*`
   - Select `@timestamp` as the time field
   - Click "Create index pattern"

## Pre-configured Dashboards

### 1. Application Overview Dashboard

**Purpose**: High-level view of application health and activity

**Visualizations**:
- Total requests over time (line chart)
- Response time distribution (histogram)
- Error rate percentage (metric)
- Top endpoints by request count (data table)
- Active users over time (line chart)

### 2. Error Analysis Dashboard

**Purpose**: Deep dive into application errors and exceptions

**Visualizations**:
- Error count by level (pie chart)
- Exception types (data table)
- Error trends over time (line chart)
- Failed requests by endpoint (bar chart)
- Stack traces (data table)

### 3. Performance Dashboard

**Purpose**: Monitor application performance metrics

**Visualizations**:
- Average response time (line chart)
- Response time percentiles (line chart)
- Slowest endpoints (data table)
- Request duration distribution (histogram)
- Database query performance (if SQL logs are enabled)

### 4. User Activity Dashboard

**Purpose**: Track user behavior and account activity

**Visualizations**:
- Active users by domain (pie chart)
- User activity over time (line chart)
- Top accounts by activity (data table)
- Request patterns by user (heatmap)
- Account creation/updates (timeline)

## Saved Searches

### 1. High Error Rate Detection
```
level:ERROR AND @timestamp:[now-1h TO now]
```

### 2. Slow Requests
```
response_time_ms:>1000 AND @timestamp:[now-1h TO now]
```

### 3. Database Issues
```
logger:org.hibernate* AND level:ERROR
```

### 4. Authentication Issues
```
message:*authentication* OR message:*login* OR message:*auth*
```

## Alerts and Monitoring

### 1. Error Rate Alert
- **Condition**: Error rate > 5% in last 5 minutes
- **Action**: Send notification

### 2. High Response Time Alert
- **Condition**: Average response time > 2 seconds in last 5 minutes
- **Action**: Send notification

### 3. Database Connection Issues
- **Condition**: Hibernate errors > 10 in last 5 minutes
- **Action**: Send notification

## Custom Visualizations

### Request Flow Analysis
Create a visualization showing the flow of requests through your application:
- Filter by specific requestId
- Show timeline of events
- Include request/response details

### User Journey Tracking
Track individual user sessions:
- Filter by userId
- Show chronological activity
- Include domain and account context

## Index Patterns and Fields

### Key Fields to Monitor
- `@timestamp`: Event timestamp
- `level`: Log level (INFO, ERROR, WARN, DEBUG)
- `service`: Service name
- `requestId`: Request correlation ID
- `userId`: User identifier
- `domain`: Tenant domain
- `accountId`: Account identifier
- `http_method`: HTTP method
- `http_uri`: Request URI
- `http_status`: Response status code
- `response_time_ms`: Request duration
- `message`: Log message
- `exception`: Exception class name
- `stackTrace`: Full stack trace

## Best Practices

1. **Regular Index Rotation**: Configure index lifecycle management
2. **Field Mapping**: Ensure proper field types for aggregations
3. **Dashboard Refresh**: Set appropriate refresh intervals
4. **Access Control**: Configure user roles and permissions
5. **Backup**: Export dashboard configurations regularly

## Troubleshooting

### Common Issues
1. **No Data**: Check if Filebeat is running and collecting logs
2. **Missing Fields**: Verify Logstash parsing configuration
3. **Performance**: Adjust Elasticsearch memory settings
4. **Index Errors**: Check Elasticsearch cluster health

### Useful Queries
```json
# Check if logs are being ingested
GET /_cat/indices/delight-account-service-*

# Check Elasticsearch health
GET /_cluster/health

# View recent logs
GET /delight-account-service-*/_search
{
  "query": {
    "match_all": {}
  },
  "sort": [
    {
      "@timestamp": {
        "order": "desc"
      }
    }
  ],
  "size": 10
}
```
