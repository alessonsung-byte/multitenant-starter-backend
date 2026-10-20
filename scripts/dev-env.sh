#!/bin/sh
# =====================================================================
# 本機開發用的環境變數（只給自己電腦上的開發環境使用）
#
# 使用方式（在 repo 根目錄）：
#   . scripts/dev-env.sh        # 或 source scripts/dev-env.sh
#
# 這些值與 docker/docker-compose-local.yml 相同，都是公開的開發用預設值；
# 測試環境、正式環境一律使用 docker/.env.server.example 重新產生的隨機值，不可以沿用這裡的值。
# =====================================================================

# Flyway 建表帳號（資料表擁有者）
export APP_FLYWAY_USERNAME=root
export APP_FLYWAY_PASSWORD=starter_owner_local
# 後端執行用帳號 starter_app 的密碼（不是資料表擁有者，Row-Level Security 才會生效）
export APP_DB_PASSWORD=starter_app_local
# Jasypt 主密碼：設定值寫成 ENC(...) 時用來解密（本機沒有加密的值，任意字串即可）
export APP_JASYPT_PASSWORD=local-dev-only
# JWT 簽章金鑰：每次開新的終端機都重新產生（重新產生後需要重新登入）
if [ -z "${APP_JWT_KEY:-}" ]; then
  APP_JWT_KEY=$(openssl rand -base64 32)
  export APP_JWT_KEY
fi
# 建立兩個示範租戶與三組固定密碼的帳號（見 README「本機示範資料」）
export SPRING_PROFILES_ACTIVE=local

echo "已設定本機開發用環境變數（profile: ${SPRING_PROFILES_ACTIVE}）"
