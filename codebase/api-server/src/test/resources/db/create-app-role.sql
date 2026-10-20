-- 整合測試用：建立後端執行用的資料庫帳號 starter_app（等同 docker/postgres-init/01-create-app-role.sh）
-- 密碼只用於測試時自動啟動、測試結束就刪除的資料庫
CREATE ROLE starter_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT PASSWORD 'starter_app_test';
