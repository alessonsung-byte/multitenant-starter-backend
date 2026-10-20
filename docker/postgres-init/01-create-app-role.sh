#!/bin/sh
# =====================================================================
# 建立後端執行時使用的資料庫帳號 starter_app
#
# PostgreSQL 容器「第一次啟動、資料庫還是空的」時會自動執行這個資料夾內的腳本；
# 之後重新啟動不會再執行。已經有資料的 volume 要另外手動建立，步驟見 doc/SETUP_DOCKER_LOCAL.md。
#
# - starter_app 不是 superuser、不是資料表擁有者，所以 Row-Level Security 對它一定生效
# - 資料表由建表帳號（POSTGRES_USER）透過 Flyway 建立，權限也由 Flyway migration 授予
# - 密碼由環境變數 APP_DB_ROLE_PASSWORD 提供，不寫在這個檔案裡
# =====================================================================
set -eu

if [ -z "${APP_DB_ROLE_PASSWORD:-}" ]; then
  echo "請設定環境變數 APP_DB_ROLE_PASSWORD（starter_app 的密碼）" >&2
  exit 1
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set app_password="$APP_DB_ROLE_PASSWORD" <<'SQL'
CREATE ROLE starter_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT PASSWORD :'app_password';
SQL
