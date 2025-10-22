# Kibana Setup Guide for Delight Account Service Logs

## Step-by-Step Setup

### 1. Access Kibana
Open your browser and go to: http://localhost:5601

### 2. Create Index Pattern
1. Click on **"Stack Management"** in the left sidebar
2. Click on **"Index Patterns"**
3. Click **"Create index pattern"**
4. Enter the pattern: `delight-account-service-*`
5. Click **"Next step"**
6. Select **"@timestamp"** as the time field
7. Click **"Create index pattern"**

### 3. Access Log Explorer
1. Click on **"Discover"** in the left sidebar
2. Select your index pattern: `delight-account-service-*`
3. You should now see your logs!

### 4. Configure Time Range
1. In the top-right corner, click the time picker
2. Set to **"Last 7 days"** or **"Last 24 hours"**
3. Click **"Apply"**

### 5. View Your Logs
You should now see your application logs with fields like:
- `@timestamp`
- `level` (INFO, DEBUG, ERROR)
- `logger` (class names)
- `message` (log content)
- `requestId`
- `domain`
- `accountId`
- `method`
- `uri`

## Troubleshooting

### If you don't see any logs:
1. **Check time range**: Make sure you're looking at the right time period
2. **Refresh the page**: Sometimes Kibana needs a refresh
3. **Check index pattern**: Ensure it's `delight-account-service-*`
4. **Verify data exists**: Check Elasticsearch directly

### If you see "No results found":
1. **Expand time range**: Try "Last 30 days"
2. **Check filters**: Remove any active filters
3. **Verify index pattern**: Make sure it matches your data

## Quick Verification Commands

```bash
# Check if data exists in Elasticsearch
curl "http://localhost:9200/delight-account-service-*/_search?size=1"

# Check total document count
curl "http://localhost:9200/delight-account-service-*/_count"

# Check latest documents
curl "http://localhost:9200/delight-account-service-*/_search?size=3&sort=@timestamp:desc"
```
