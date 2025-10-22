# Kibana Field Display Setup

## Issue: Fields Not Showing as Individual Columns

Your logs are being indexed correctly, but Kibana is showing them in a nested structure. Here's how to fix this:

## Solution 1: Configure Kibana Field Display

### 1. Open Kibana
- Go to: http://localhost:5601/app/discover
- Select index pattern: `delight-account-service-*`

### 2. Configure Field Display
1. **Click the gear icon** (Settings) in the top-right
2. **Go to "Index Patterns"**
3. **Select your pattern**: `delight-account-service-*`
4. **Click "Refresh fields"**

### 3. Customize Column Display
1. **In Discover view**, click the **"Add field"** button
2. **Add these fields as columns**:
   - `@timestamp`
   - `level`
   - `logger`
   - `message`
   - `requestId`
   - `domain`
   - `accountId`
   - `method`
   - `uri`
   - `app_service`

### 4. Remove Unwanted Fields
1. **Click the field name** in the left sidebar
2. **Click "Remove"** for unwanted fields like:
   - `agent`
   - `ecs`
   - `host`
   - `input`
   - `log`

## Solution 2: Create a Custom View

### 1. Save a Search
1. **Configure your desired columns**
2. **Click "Save"** in the top menu
3. **Name it**: "Application Logs Clean View"

### 2. Set Default Columns
1. **Go to Settings** → **Advanced Settings**
2. **Search for**: `discover:sampleSize`
3. **Set to**: `100` (or your preferred number)

## Solution 3: Create a Dashboard

### 1. Create Visualizations
1. **Go to "Visualize"**
2. **Create visualizations for**:
   - Log levels over time
   - Top loggers
   - Request patterns
   - Error analysis

### 2. Build Dashboard
1. **Go to "Dashboard"**
2. **Add your visualizations**
3. **Save as**: "Application Logs Dashboard"

## Expected Result

After setup, you should see:
- **Clean columns**: timestamp, level, logger, message, requestId, domain, accountId, method, uri
- **No nested objects**: All fields at root level
- **Searchable fields**: Each field individually searchable and filterable

## Quick Fix Commands

```bash
# Check current field structure
curl -s "http://localhost:9200/delight-account-service-*/_search?size=1" | jq '.hits.hits[0]._source | keys'

# View clean log structure
curl -s "http://localhost:9200/delight-account-service-*/_search?size=1" | jq '.hits.hits[0]._source | {timestamp, level, logger, message, requestId, domain, accountId, method, uri, app_service}'
```
