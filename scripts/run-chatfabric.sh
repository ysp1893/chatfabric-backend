#!/usr/bin/env bash

set -euo pipefail

APP_NAME="chatfabric-backend"
APP_HOME="${APP_HOME:-/opt/chatfabric}"
JAR_NAME="${JAR_NAME:-chatfabric-backend-0.0.1-SNAPSHOT.jar}"
JAR_PATH="${JAR_PATH:-$APP_HOME/$JAR_NAME}"
APP_PORT="${APP_PORT:-8080}"

export DB_HOST="${DB_HOST:-192.168.24.30}"
export DB_PORT="${DB_PORT:-3316}"
export DB_NAME="${DB_NAME:-chatfabric}"
export DB_USERNAME="${DB_USERNAME:-root}"
export DB_PASSWORD="${DB_PASSWORD:-root}"

export JWT_SECRET="${JWT_SECRET:-change-this-to-a-long-random-secret}"
export JWT_EXPIRATION_SECONDS="${JWT_EXPIRATION_SECONDS:-900}"

export PRESENCE_STORE="${PRESENCE_STORE:-in-memory}"
export REDIS_HOST="${REDIS_HOST:-localhost}"
export REDIS_PORT="${REDIS_PORT:-6379}"

export RATE_LIMIT_ENABLED="${RATE_LIMIT_ENABLED:-true}"
export RATE_LIMIT_REQUESTS_PER_MINUTE="${RATE_LIMIT_REQUESTS_PER_MINUTE:-120}"

export ALLOWED_ORIGIN_1="${ALLOWED_ORIGIN_1:-http://192.168.24.30:$APP_PORT}"
export ALLOWED_ORIGIN_2="${ALLOWED_ORIGIN_2:-http://localhost:$APP_PORT}"
export ALLOWED_ORIGIN_3="${ALLOWED_ORIGIN_3:-http://127.0.0.1:$APP_PORT}"
export ALLOWED_ORIGIN_4="${ALLOWED_ORIGIN_4:-http://192.168.24.30}"

export REQUIRE_SSL="${REQUIRE_SSL:-false}"

if ! command -v java >/dev/null 2>&1; then
  echo "java is not installed or not available on PATH"
  exit 1
fi

if [ ! -f "$JAR_PATH" ]; then
  echo "Jar file not found: $JAR_PATH"
  exit 1
fi

echo "Starting $APP_NAME"
echo "JAR_PATH=$JAR_PATH"
echo "APP_PORT=$APP_PORT"
echo "DB_HOST=$DB_HOST"
echo "DB_PORT=$DB_PORT"
echo "DB_NAME=$DB_NAME"
echo "PRESENCE_STORE=$PRESENCE_STORE"
echo "REQUIRE_SSL=$REQUIRE_SSL"

exec java -jar "$JAR_PATH" --server.port="$APP_PORT"
