#!/bin/sh
set -e

# 啟動 api-server。
# 資料庫與 Redis 是否已經可以連線，由 docker-compose-server.yml 的 depends_on（condition: service_healthy）確保，
# 這裡不需要再另外等待
exec java ${JAVA_OPTS} -jar /app/api-server.jar
