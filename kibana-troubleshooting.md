# Kibana Log Explorer Troubleshooting

## ✅ Your ELK Stack Status
- **Elasticsearch**: ✅ Running (7 documents)
- **Kibana**: ✅ Running  
- **Logstash**: ✅ Running
- **Data**: ✅ Present and accessible

## 🔧 Step-by-Step Fix

### 1. Open Kibana
```
http://localhost:5601
```

### 2. Create Index Pattern (CRITICAL STEP)
1. **Click "Stack Management"** (gear icon) in left sidebar
2. **Click "Index Patterns"**
3. **Click "Create index pattern"**
4. **Enter**: `delight-account-service-*`
5. **Click "Next step"**
6. **Select**: `@timestamp` as time field
7. **Click "Create index pattern"**

### 3. Access Log Explorer
1. **Click "Discover"** in left sidebar
2. **Select your index pattern**: `delight-account-service-*`
3. **Set time range**: Click time picker (top-right) → "Last 7 days"

### 4. If Still No Results

#### Check Time Range
- Click the time picker (top-right corner)
- Select "Last 7 days" or "Last 30 days"
- Click "Apply"

#### Check Filters
- Look for any active filters (top of page)
- Remove any filters that might be hiding results

#### Refresh Data
- Click the refresh button (circular arrow)
- Or press F5 to refresh the page

## 🚨 Common Issues & Solutions

### Issue: "No results found"
**Solution**: 
1. Check time range (set to "Last 7 days")
2. Remove any active filters
3. Verify index pattern is `delight-account-service-*`

### Issue: "No index patterns match"
**Solution**: 
1. Create index pattern: `delight-account-service-*`
2. Select `@timestamp` as time field

### Issue: "Index pattern not found"
**Solution**: 
1. Go to Stack Management → Index Patterns
2. Create new pattern: `delight-account-service-*`

## 📊 Verify Your Data

Run this command to see your data:
```bash
./verify-elk.sh
```

## 🔍 Manual Verification

Check if data exists:
```bash
# Check total documents
curl "http://localhost:9200/delight-account-service-*/_count"

# Check latest documents  
curl "http://localhost:9200/delight-account-service-*/_search?size=3&sort=@timestamp:desc"
```

## 📝 Expected Log Fields

Your logs should show these fields:
- `@timestamp` - Log time
- `level` - INFO, DEBUG, ERROR
- `logger` - Class name
- `message` - Log content
- `requestId` - Request correlation ID
- `domain` - Tenant domain
- `accountId` - Account ID
- `method` - HTTP method
- `uri` - Request URI

## 🆘 Still Not Working?

1. **Clear browser cache** and refresh Kibana
2. **Restart Kibana**: `podman restart delight-kibana`
3. **Check container logs**: `podman logs delight-kibana`
4. **Verify network**: Ensure all containers are on same network
