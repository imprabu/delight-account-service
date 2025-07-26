#!/bin/sh
set -e
LOG_DIR="${LOG_DIR:-/app/logs}"
mkdir -p "$LOG_DIR"
exec java -jar /app/app.jar
