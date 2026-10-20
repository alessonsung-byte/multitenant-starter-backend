-- ----------------------------------------------------
-- 租戶資料表、帳號加上所屬租戶與「需要修改密碼」、新增租戶帳號角色
-- ----------------------------------------------------

-- TABLE: mt_tenant 租戶（多租戶的租戶單位，不屬於任何一個租戶）
CREATE TABLE mt_tenant
(
    tenant_id      VARCHAR(128) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    tenant_name    VARCHAR(128) NOT NULL,
    tenant_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    create_time    BIGINT       NOT NULL,
    create_user    VARCHAR(128) NOT NULL,
    update_time    BIGINT,
    update_user    VARCHAR(128)
);
CREATE UNIQUE INDEX ix_tenant_tenant_name ON mt_tenant (tenant_name);
COMMENT ON TABLE mt_tenant IS '租戶（多租戶的租戶單位）';
COMMENT ON COLUMN mt_tenant.tenant_id IS '租戶編號，資料庫自動產生 UUID';
COMMENT ON COLUMN mt_tenant.tenant_name IS '租戶名稱，不可重複';
COMMENT ON COLUMN mt_tenant.tenant_enabled IS '是否啟用。停用後該租戶所有帳號立即無法使用（停用而非刪除）';
COMMENT ON COLUMN mt_tenant.create_time IS '新建資料的時間，值為毫秒';
COMMENT ON COLUMN mt_tenant.create_user IS '新建資料的使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_tenant.update_time IS '更新資料的時間，值為毫秒';
COMMENT ON COLUMN mt_tenant.update_user IS '更新資料的使用者編號，即 mt_user.user_id';

-- mt_tenant 的 Row-Level Security
-- 租戶表本身不屬於任何一個租戶，所以「讀取」不限制：登入時還不知道是哪個租戶，就要讀取帳號所屬租戶是否啟用。
-- 「新增、修改」只允許總管理員的交易（app.all_tenants = on），即使程式有錯，租戶帳號也改不了租戶資料；
-- 不提供刪除（租戶只能停用）。
ALTER TABLE mt_tenant ENABLE ROW LEVEL SECURITY;
ALTER TABLE mt_tenant FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_read ON mt_tenant FOR SELECT
    USING (true);
CREATE POLICY admin_insert ON mt_tenant FOR INSERT
    WITH CHECK (current_setting('app.all_tenants', true) = 'on');
CREATE POLICY admin_update ON mt_tenant FOR UPDATE
    USING (current_setting('app.all_tenants', true) = 'on')
    WITH CHECK (current_setting('app.all_tenants', true) = 'on');

-- mt_user：所屬租戶（總管理員為空）、是否需要修改密碼
ALTER TABLE mt_user ADD COLUMN tenant_id VARCHAR(128) NULL REFERENCES mt_tenant (tenant_id);
ALTER TABLE mt_user ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX ix_user_tenant_id ON mt_user (tenant_id);
COMMENT ON COLUMN mt_user.tenant_id IS '所屬租戶，即 mt_tenant.tenant_id。租戶帳號必填；總管理員為空';
COMMENT ON COLUMN mt_user.must_change_password IS '是否需要修改密碼。新帳號、被總管理員重設密碼後為 true，使用者改完密碼後為 false';

-- 預設總管理員 admin 第一次登入就要修改密碼
UPDATE mt_user SET must_change_password = TRUE WHERE user_account = 'admin';

-- 角色 TENANT_USER：租戶帳號
INSERT INTO mt_role (role_id, role_name, create_time, create_user)
VALUES ('5b0d7a52-8a4e-4f3c-9f0e-3c2f1d6e8a11', 'TENANT_USER',
        CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT), 'd9d3d4e2-ec90-4cf0-b984-c131d901e38e');
