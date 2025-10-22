# ELK Stack Setup for Delight Account Service

This document describes how to set up and use the ELK (Elasticsearch, Logstash, Kibana) stack with Podman for log aggregation and analysis of the Delight Account Service.

## Overview

The ELK stack provides:
- **Elasticsearch**: Search and analytics engine for log data
- **Logstash**: Data processing pipeline for parsing and enriching logs
- **Kibana**: Visualization and dashboard interface
- **Filebeat**: Lightweight log shipper for collecting logs

## Quick Start

### Prerequisites

1. **Podman**: Container runtime
2. **podman-compose**: Container orchestration
3. **curl**: For health checks

### Installation

1. **Install Podman** (if not already installed):
   ```bash
   # macOS
   brew install podman
   
   # Linux
   sudo dnf install podman  # or apt-get install podman
   ```

2. **Install podman-compose**:
   ```bash
   pip3 install podman-compose
   ```

### Starting the ELK Stack

1. **Quick Start** (recommended):
   ```bash
   ./elk-quick-start.sh
   ```

2. **Manual Start**:
   ```bash
   ./elk-deploy.sh start
   ```

3. **Check Status**:
   ```bash
   ./elk-deploy.sh status
   ```

### Access Points

Once started, you can access:
- **Kibana Dashboard**: http://localhost:5601
- **Elasticsearch**: http://localhost:9200
- **Logstash**: http://localhost:9600

## Configuration

### Application Logging

The application is already configured with structured JSON logging that works well with ELK:

- **Log Format**: JSON with structured fields
- **Log Location**: `./logs/application.log`
- **Fields Included**: timestamp, level, logger, message, requestId, userId, domain, accountId, method, uri, statusCode, duration

### ELK Components

#### Elasticsearch
- **Port**: 9200
- **Memory**: 1GB heap
- **Security**: Disabled for development
- **Data**: Persistent volume for data retention

#### Logstash
- **Port**: 5044 (Beats input), 9600 (HTTP)
- **Configuration**: Parses JSON logs and enriches with metadata
- **Output**: Sends processed logs to Elasticsearch

#### Kibana
- **Port**: 5601
- **Configuration**: Connected to Elasticsearch
- **Dashboards**: Pre-configured for application monitoring

#### Filebeat
- **Input**: Monitors `./logs/application.log`
- **Output**: Sends logs to Logstash
- **Fields**: Adds service and environment metadata

## Usage

### 1. Start Your Application

Start your Delight Account Service to generate logs:

```bash
# Your application will write logs to ./logs/application.log
./gradlew bootRun
```

### 2. View Logs in Kibana

1. Open http://localhost:5601
2. Create an index pattern: `delight-account-service-*`
3. Go to Discover to view your logs
4. Use the pre-configured dashboards for analysis

### 3. Monitor Application Health

The ELK stack automatically collects and processes:
- Application logs
- Error messages
- Performance metrics
- User activity
- Database queries (if enabled)

## Management Commands

### Start/Stop/Status
```bash
# Start ELK stack
./elk-deploy.sh start

# Stop ELK stack
./elk-deploy.sh stop

# Restart ELK stack
./elk-deploy.sh restart

# Check status
./elk-deploy.sh status

# View logs
./elk-deploy.sh logs
```

### Cleanup
```bash
# Stop and remove all containers and volumes
./elk-deploy.sh cleanup
```

## Dashboards and Visualizations

### Pre-configured Dashboards

1. **Application Overview**: High-level metrics and trends
2. **Error Analysis**: Error rates, exceptions, and stack traces
3. **Performance**: Response times, slow queries, bottlenecks
4. **User Activity**: User behavior and account activity

### Key Metrics to Monitor

- **Request Volume**: Total requests over time
- **Response Times**: Average and percentile response times
- **Error Rates**: Error percentage and trends
- **User Activity**: Active users and account activity
- **Database Performance**: Query execution times

## Troubleshooting

### Common Issues

1. **ELK Stack Won't Start**:
   ```bash
   # Check Podman status
   podman ps -a
   
   # Check logs
   ./elk-deploy.sh logs
   ```

2. **No Logs in Kibana**:
   - Verify application is writing to `./logs/application.log`
   - Check Filebeat is running: `podman logs delight-filebeat`
   - Check Logstash is processing: `podman logs delight-logstash`

3. **Performance Issues**:
   - Increase Elasticsearch memory in `elk-stack-compose.yaml`
   - Check disk space for log files
   - Monitor container resource usage

### Health Checks

```bash
# Check Elasticsearch health
curl http://localhost:9200/_cluster/health

# Check Kibana status
curl http://localhost:5601/api/status

# Check Logstash status
curl http://localhost:9600/_node/stats
```

### Log Analysis

```bash
# View application logs
tail -f logs/application.log

# View ELK stack logs
./elk-deploy.sh logs

# Check specific container logs
podman logs delight-elasticsearch
podman logs delight-logstash
podman logs delight-kibana
podman logs delight-filebeat
```

## Advanced Configuration

### Custom Logstash Filters

Edit `elk-config/logstash/pipeline/logstash.conf` to add custom parsing rules.

### Custom Kibana Dashboards

1. Create visualizations in Kibana
2. Export dashboard configurations
3. Import into other environments

### Index Lifecycle Management

Configure automatic index rotation and cleanup in Elasticsearch.

### Security

For production use:
1. Enable Elasticsearch security
2. Configure authentication
3. Set up SSL/TLS
4. Implement access controls

## File Structure

```
delight-account-service/
├── elk-stack-compose.yaml          # ELK stack container configuration
├── elk-deploy.sh                   # Management script
├── elk-quick-start.sh              # Quick start script
├── elk-config/
│   ├── logstash/
│   │   ├── pipeline/
│   │   │   └── logstash.conf       # Logstash parsing configuration
│   │   └── config/
│   │       └── logstash.yml        # Logstash settings
│   ├── filebeat/
│   │   └── filebeat.yml            # Filebeat configuration
│   └── kibana/
│       ├── dashboard-setup.md      # Dashboard setup guide
│       └── index-pattern.json      # Kibana index pattern
├── logs/
│   └── application.log             # Application log file
└── README-ELK.md                   # This documentation
```

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review container logs
3. Verify configuration files
4. Check Elasticsearch cluster health

## Next Steps

1. **Custom Dashboards**: Create specific dashboards for your use cases
2. **Alerts**: Set up monitoring alerts for critical issues
3. **Log Retention**: Configure log rotation and retention policies
4. **Scaling**: Consider multi-node Elasticsearch for production
5. **Security**: Implement proper security measures for production use
