-- ----------------------------------------------------
-- 本機示範資料：兩個租戶、三組固定密碼的帳號、幾筆客戶與訂單
-- 只在 Spring profile「local」時執行（application-local.yml）；正式環境、整合測試都不會執行。
-- 這裡的密碼寫在 git 裡，所以絕對不可以在正式環境啟用 local profile。
--
--   帳號            密碼                    身分
--   admin           Starter-Admin-2026      總管理員（覆蓋 V1 的初始密碼 admin，不需要第一次登入改密碼）
--   tenant_a_user   Starter-TenantA-2026    「示範租戶 A」的租戶帳號
--   tenant_b_user   Starter-TenantB-2026    「示範租戶 B」的租戶帳號
--
-- 用兩個租戶的帳號分別登入，就可以直接看到「彼此看不到對方的客戶與訂單」。
--
-- 這是 Flyway 的 repeatable migration（R__）：在所有版本（V）之後執行，檔案內容改變才會重新執行；
-- 內容都可以重複執行（已經存在的資料不會重複新增）。
-- ----------------------------------------------------

-- 總管理員身分：可以新增租戶（Row-Level Security 只讓總管理員的交易寫入 mt_tenant）
SELECT set_config('app.all_tenants', 'on', true), set_config('app.tenant_id', '', true);

INSERT INTO mt_tenant (tenant_id, tenant_name, tenant_enabled, create_time, create_user)
VALUES ('a3c1e7f0-5b2d-4e8a-9c6f-0d1b2e3f4a5b', '示範租戶 A', TRUE,
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e'),
       ('b4d2f8a1-6c3e-4f9b-8d7a-1e2c3f4a5b6c', '示範租戶 B', TRUE,
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e')
ON CONFLICT (tenant_id) DO NOTHING;

-- 總管理員 admin：改成固定密碼、不需要改密碼
UPDATE mt_signin
SET signin_pw   = '$2a$12$9lq1tkXaLupgJctxyUb39OxoyTcv6mzAx7zkec5FZ8a9lXLXESWMC',
    update_time = CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT),
    update_user = 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e'
WHERE signin_account = 'admin';
UPDATE mt_user
SET must_change_password = FALSE
WHERE user_account = 'admin';

-- 兩個租戶帳號（角色 TENANT_USER 由 V2 建立）
INSERT INTO mt_user (user_id, user_account, user_email, user_enabled, tenant_id, must_change_password, create_time, create_user)
VALUES ('e7a5c1d4-9f6b-4c2e-8a0d-4b5c6d7e8f90', 'tenant_a_user', NULL, TRUE, 'a3c1e7f0-5b2d-4e8a-9c6f-0d1b2e3f4a5b', FALSE,
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e'),
       ('f8b6d2e5-0a7c-4d3f-9b1e-5c6d7e8f9a01', 'tenant_b_user', NULL, TRUE, 'b4d2f8a1-6c3e-4f9b-8d7a-1e2c3f4a5b6c', FALSE,
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e')
ON CONFLICT DO NOTHING;

INSERT INTO mt_signin (signin_account, signin_pw, user_id, create_time, create_user)
VALUES ('tenant_a_user', '$2a$12$Dm5zBkbA1kQu7hnw2B0m7.Xq7IUACd7X6tm67prM58.eKFXrb.B2K', 'e7a5c1d4-9f6b-4c2e-8a0d-4b5c6d7e8f90',
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e'),
       ('tenant_b_user', '$2a$12$4zRnBYX1NUZgmkPfZiCvQOf.J9QLKjkJLVQX24XizyAcDYl4NBZGe', 'f8b6d2e5-0a7c-4d3f-9b1e-5c6d7e8f9a01',
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e')
ON CONFLICT DO NOTHING;

INSERT INTO mt_role_user (user_id, role_id, create_time, create_user)
VALUES ('e7a5c1d4-9f6b-4c2e-8a0d-4b5c6d7e8f90', '5b0d7a52-8a4e-4f3c-9f0e-3c2f1d6e8a11',
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e'),
       ('f8b6d2e5-0a7c-4d3f-9b1e-5c6d7e8f9a01', '5b0d7a52-8a4e-4f3c-9f0e-3c2f1d6e8a11',
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e')
ON CONFLICT DO NOTHING;

-- 示範租戶 A 的客戶與訂單（業務資料只能以「該租戶」的身分寫入）
SELECT set_config('app.all_tenants', 'off', true), set_config('app.tenant_id', 'a3c1e7f0-5b2d-4e8a-9c6f-0d1b2e3f4a5b', true);

INSERT INTO mt_customer (customer_id, tenant_id, customer_name, customer_code, customer_email, create_time, create_user)
VALUES ('0a1b2c3d-0000-4000-8000-00000000a001', 'a3c1e7f0-5b2d-4e8a-9c6f-0d1b2e3f4a5b', '晨光咖啡', 'A-001', 'hello@example.com',
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'e7a5c1d4-9f6b-4c2e-8a0d-4b5c6d7e8f90'),
       ('0a1b2c3d-0000-4000-8000-00000000a002', 'a3c1e7f0-5b2d-4e8a-9c6f-0d1b2e3f4a5b', '山嵐設計工作室', 'A-002', NULL,
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'e7a5c1d4-9f6b-4c2e-8a0d-4b5c6d7e8f90')
ON CONFLICT DO NOTHING;

INSERT INTO mt_sales_order (order_id, tenant_id, customer_id, order_date, item_name, unit_price, quantity, subtotal, create_time, create_user)
VALUES ('0a1b2c3d-0000-4000-8000-0000000a0001', 'a3c1e7f0-5b2d-4e8a-9c6f-0d1b2e3f4a5b', '0a1b2c3d-0000-4000-8000-00000000a001',
        DATE '2026-09-03', '咖啡豆 1kg', 850.00, 3, 2550.00,
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'e7a5c1d4-9f6b-4c2e-8a0d-4b5c6d7e8f90'),
       ('0a1b2c3d-0000-4000-8000-0000000a0002', 'a3c1e7f0-5b2d-4e8a-9c6f-0d1b2e3f4a5b', '0a1b2c3d-0000-4000-8000-00000000a002',
        DATE '2026-09-15', '品牌識別設計', 30000.00, 1, 30000.00,
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'e7a5c1d4-9f6b-4c2e-8a0d-4b5c6d7e8f90')
ON CONFLICT DO NOTHING;

-- 示範租戶 B 的客戶與訂單
SELECT set_config('app.tenant_id', 'b4d2f8a1-6c3e-4f9b-8d7a-1e2c3f4a5b6c', true);

INSERT INTO mt_customer (customer_id, tenant_id, customer_name, customer_code, customer_email, create_time, create_user)
VALUES ('0a1b2c3d-0000-4000-8000-00000000b001', 'b4d2f8a1-6c3e-4f9b-8d7a-1e2c3f4a5b6c', '海風民宿', 'B-001', NULL,
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'f8b6d2e5-0a7c-4d3f-9b1e-5c6d7e8f9a01')
ON CONFLICT DO NOTHING;

INSERT INTO mt_sales_order (order_id, tenant_id, customer_id, order_date, item_name, unit_price, quantity, subtotal, create_time, create_user)
VALUES ('0a1b2c3d-0000-4000-8000-0000000b0001', 'b4d2f8a1-6c3e-4f9b-8d7a-1e2c3f4a5b6c', '0a1b2c3d-0000-4000-8000-00000000b001',
        DATE '2026-09-20', '床單清洗', 120.00, 40, 4800.00,
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'f8b6d2e5-0a7c-4d3f-9b1e-5c6d7e8f9a01')
ON CONFLICT DO NOTHING;
