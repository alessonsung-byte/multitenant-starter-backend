-- ----------------------------------------------------
-- 授權給後端執行時使用的資料庫帳號 starter_app
-- ----------------------------------------------------
-- 資料庫帳號分成兩個：
--   建表帳號（本機為 root）：執行 Flyway、擁有所有資料表
--   starter_app：後端執行時連線用，不是資料表擁有者，所以 Row-Level Security 對它一定生效
-- starter_app 由 PostgreSQL 容器第一次啟動時的初始化腳本建立（docker/postgres-init/），這裡只授權。
-- 一律只授予需要的權限：帳號相關資料表都是停用而非刪除，所以不授予 DELETE。
-- 之後每新增一張資料表，都要在同一個 migration 補上授權。

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'starter_app') THEN
            RAISE EXCEPTION '找不到資料庫帳號 starter_app，請先執行 docker/postgres-init/ 的初始化腳本建立';
        END IF;
    END
$$;

GRANT USAGE ON SCHEMA public TO starter_app;

GRANT SELECT, INSERT, UPDATE ON mt_tenant TO starter_app;
GRANT SELECT, INSERT, UPDATE ON mt_user TO starter_app;
GRANT SELECT, INSERT, UPDATE ON mt_signin TO starter_app;
GRANT SELECT, INSERT, UPDATE ON mt_role_user TO starter_app;
-- 角色與權限只由 migration 維護，後端只需要讀取
GRANT SELECT ON mt_role TO starter_app;
GRANT SELECT ON mt_permission TO starter_app;
GRANT SELECT ON mt_role_permission TO starter_app;
