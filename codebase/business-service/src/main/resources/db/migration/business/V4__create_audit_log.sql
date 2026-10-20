-- ----------------------------------------------------
-- 操作紀錄
--   - 記錄新增、修改、刪除、作廢、啟用／停用、重設密碼：誰、何時、哪一筆、哪些欄位從什麼改成什麼
--   - 由各 Service 在同一個交易內寫入（業務資料寫入失敗時，操作紀錄也不會留下）
--   - 只有總管理員可以查看；不能修改、不能刪除
-- ----------------------------------------------------

-- TABLE: mt_audit_log 操作紀錄
CREATE TABLE mt_audit_log
(
    audit_log_id VARCHAR(128) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    tenant_id    VARCHAR(128) REFERENCES mt_tenant (tenant_id),
    operator_id  VARCHAR(128) NOT NULL,
    audit_action VARCHAR(32)  NOT NULL,
    table_name   VARCHAR(64)  NOT NULL,
    record_id    VARCHAR(128) NOT NULL,
    record_label VARCHAR(256),
    changes      JSONB,
    create_time  BIGINT       NOT NULL,
    create_user  VARCHAR(128) NOT NULL,
    update_time  BIGINT,
    update_user  VARCHAR(128),
    CONSTRAINT ck_audit_log_action CHECK (audit_action IN ('CREATE', 'UPDATE', 'DELETE', 'VOID', 'ENABLE', 'DISABLE', 'RESET_PASSWORD'))
);
CREATE INDEX ix_audit_log_create_time ON mt_audit_log (create_time);
CREATE INDEX ix_audit_log_tenant_time ON mt_audit_log (tenant_id, create_time);
COMMENT ON TABLE mt_audit_log IS '操作紀錄：誰、何時、對哪一筆資料做了什麼（只有總管理員可以查看）';
COMMENT ON COLUMN mt_audit_log.audit_log_id IS '操作紀錄編號，資料庫自動產生 UUID';
COMMENT ON COLUMN mt_audit_log.tenant_id IS '被操作的資料所屬租戶；總管理員對租戶、帳號、全域設定的操作為空';
COMMENT ON COLUMN mt_audit_log.operator_id IS '操作者，即 mt_user.user_id';
COMMENT ON COLUMN mt_audit_log.audit_action IS '動作：CREATE 新增、UPDATE 修改、DELETE 刪除、VOID 作廢、ENABLE 啟用、DISABLE 停用、RESET_PASSWORD 重設密碼';
COMMENT ON COLUMN mt_audit_log.table_name IS '被操作的資料表，例如 mt_customer';
COMMENT ON COLUMN mt_audit_log.record_id IS '被操作的資料編號';
COMMENT ON COLUMN mt_audit_log.record_label IS '方便辨認的名稱（例如客戶名稱、訂單品項），刪除後也看得出是哪一筆';
COMMENT ON COLUMN mt_audit_log.changes IS '變更內容：{欄位: {from: 原本的值, to: 新的值}}；新增只有 to，刪除只有 from（刪除前的內容）';
COMMENT ON COLUMN mt_audit_log.create_time IS '操作時間，值為毫秒';
COMMENT ON COLUMN mt_audit_log.create_user IS '新建資料的使用者編號，即 mt_user.user_id（與 operator_id 相同）';
COMMENT ON COLUMN mt_audit_log.update_time IS '保留欄位（操作紀錄不會被修改）';
COMMENT ON COLUMN mt_audit_log.update_user IS '保留欄位（操作紀錄不會被修改）';

-- 只能新增與讀取（不能修改、刪除）
GRANT SELECT, INSERT ON mt_audit_log TO starter_app;

-- Row-Level Security
-- 讀取：只有總管理員；新增：總管理員（租戶 id 可能是空的，例如新增租戶），或屬於目前租戶
ALTER TABLE mt_audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE mt_audit_log FORCE ROW LEVEL SECURITY;
CREATE POLICY admin_read ON mt_audit_log FOR SELECT
    USING (current_setting('app.all_tenants', true) = 'on');
CREATE POLICY audit_insert ON mt_audit_log FOR INSERT
    WITH CHECK (current_setting('app.all_tenants', true) = 'on'
        OR tenant_id = current_setting('app.tenant_id', true));
