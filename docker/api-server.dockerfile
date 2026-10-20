FROM eclipse-temurin:21-jre

# 應用程式日誌目錄（對應 application.yml 的 logging.file.name，實際路徑由環境變數 API_SERVER_LOG_FILE_NAME 指定）
RUN mkdir -p /opt/starter/logs && chmod -R 770 /opt/starter

WORKDIR /app

# api-server.jar 由 codebase 目錄執行 `./mvnw -pl api-server -am clean package` 建置，
# 並由 api-server/pom.xml 的 maven-resources-plugin 自動複製到這個資料夾（docker/）
COPY api-server.jar /app/api-server.jar
COPY run-api-server.sh /app/run-api-server.sh

RUN chmod +x /app/run-api-server.sh

ENTRYPOINT ["/app/run-api-server.sh"]
