-- ----------------------------------------------------
-- 範例業務資料表：客戶、訂單（全部屬於租戶）
--   - 每張表都啟用並強制 Row-Level Security：後端執行用帳號 starter_app 只讀寫得到「目前租戶」的資料
--   - 下層資料（訂單）的租戶必須與上層（客戶）相同，以複合外鍵由資料庫保證
--   - 訂單「不能修改，只能作廢」：除了程式不提供修改的 API，資料庫的 trigger 也會再擋一次
-- 新增其他業務資料表時，照這個檔案的寫法：授權 → ENABLE/FORCE RLS → 四個 policy
-- ----------------------------------------------------

-- TABLE: mt_customer 客戶
CREATE TABLE mt_customer
(
    customer_id     VARCHAR(128) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    tenant_id       VARCHAR(128) NOT NULL REFERENCES mt_tenant (tenant_id),
    customer_name   VARCHAR(128) NOT NULL,
    customer_code   VARCHAR(64),
    customer_email  VARCHAR(128),
    customer_active BOOLEAN      NOT NULL DEFAULT TRUE,
    create_time     BIGINT       NOT NULL,
    create_user     VARCHAR(128) NOT NULL,
    update_time     BIGINT,
    update_user     VARCHAR(128),
    -- 讓下層資料表可以用 (tenant_id, customer_id) 當外鍵，確保「訂單的客戶一定是同一個租戶的」
    CONSTRAINT uq_customer_tenant UNIQUE (tenant_id, customer_id)
);
CREATE INDEX ix_customer_tenant_name ON mt_customer (tenant_id, customer_name);
-- 客戶編號選填；有填時同一個租戶內不可重複（不同租戶可以重複）
CREATE UNIQUE INDEX ux_customer_tenant_code ON mt_customer (tenant_id, customer_code) WHERE customer_code IS NOT NULL;
COMMENT ON TABLE mt_customer IS '客戶（範例業務資料，屬於租戶）';
COMMENT ON COLUMN mt_customer.customer_id IS '客戶編號，資料庫自動產生 UUID';
COMMENT ON COLUMN mt_customer.tenant_id IS '所屬租戶，即 mt_tenant.tenant_id';
COMMENT ON COLUMN mt_customer.customer_name IS '客戶名稱';
COMMENT ON COLUMN mt_customer.customer_code IS '客戶代碼，選填；同一租戶內不可重複';
COMMENT ON COLUMN mt_customer.customer_email IS '聯絡信箱，選填';
COMMENT ON COLUMN mt_customer.customer_active IS '是否往來中；false 為已停止往來（列表預設不顯示）';
COMMENT ON COLUMN mt_customer.create_time IS '新建資料的時間，值為毫秒';
COMMENT ON COLUMN mt_customer.create_user IS '新建資料的使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_customer.update_time IS '更新資料的時間，值為毫秒';
COMMENT ON COLUMN mt_customer.update_user IS '更新資料的使用者編號，即 mt_user.user_id';

-- 客戶可以刪除（只限沒有訂單的客戶，由程式檢查；有訂單時外鍵也會擋下）
GRANT SELECT, INSERT, UPDATE, DELETE ON mt_customer TO starter_app;

ALTER TABLE mt_customer ENABLE ROW LEVEL SECURITY;
-- FORCE：連資料表擁有者也受限制（後端本來就不是擁有者，這是多一層保險）
ALTER TABLE mt_customer FORCE ROW LEVEL SECURITY;
-- 讀取：只看得到目前租戶的資料；總管理員看得到全部
CREATE POLICY tenant_read ON mt_customer FOR SELECT
    USING (current_setting('app.all_tenants', true) = 'on'
        OR tenant_id = current_setting('app.tenant_id', true));
-- 新增、修改、刪除：只能是目前租戶的資料。總管理員沒有設定 app.tenant_id，所以一律被擋下
CREATE POLICY tenant_insert ON mt_customer FOR INSERT
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true));
CREATE POLICY tenant_update ON mt_customer FOR UPDATE
    USING (tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true));
CREATE POLICY tenant_delete ON mt_customer FOR DELETE
    USING (tenant_id = current_setting('app.tenant_id', true));

-- TABLE: mt_sales_order 訂單（ORDER 是 SQL 保留字，所以表名用 sales_order）
CREATE TABLE mt_sales_order
(
    order_id     VARCHAR(128) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    tenant_id    VARCHAR(128)   NOT NULL REFERENCES mt_tenant (tenant_id),
    customer_id  VARCHAR(128)   NOT NULL,
    order_date   DATE           NOT NULL,
    item_name    VARCHAR(128)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    quantity     INTEGER        NOT NULL CHECK (quantity > 0),
    subtotal     NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    order_status VARCHAR(16)    NOT NULL DEFAULT 'NORMAL',
    void_reason  VARCHAR(500),
    void_time    BIGINT,
    void_user    VARCHAR(128),
    create_time  BIGINT         NOT NULL,
    create_user  VARCHAR(128)   NOT NULL,
    update_time  BIGINT,
    update_user  VARCHAR(128),
    CONSTRAINT ck_sales_order_status CHECK (order_status IN ('NORMAL', 'VOIDED')),
    -- 作廢一定有原因、作廢者、作廢時間
    CONSTRAINT ck_sales_order_void CHECK (order_status <> 'VOIDED'
        OR (void_reason IS NOT NULL AND void_time IS NOT NULL AND void_user IS NOT NULL)),
    -- 小計＝單價×數量（後端計算，資料庫再確認一次）
    CONSTRAINT ck_sales_order_subtotal CHECK (subtotal = unit_price * quantity),
    -- 客戶必須是同一個租戶的
    CONSTRAINT fk_sales_order_customer FOREIGN KEY (tenant_id, customer_id) REFERENCES mt_customer (tenant_id, customer_id)
);
CREATE INDEX ix_sales_order_tenant_date ON mt_sales_order (tenant_id, order_date);
CREATE INDEX ix_sales_order_tenant_customer ON mt_sales_order (tenant_id, customer_id);
COMMENT ON TABLE mt_sales_order IS '訂單（範例業務資料）：不能修改，只能作廢';
COMMENT ON COLUMN mt_sales_order.order_id IS '訂單編號，資料庫自動產生 UUID';
COMMENT ON COLUMN mt_sales_order.tenant_id IS '所屬租戶，即 mt_tenant.tenant_id';
COMMENT ON COLUMN mt_sales_order.customer_id IS '客戶，必須是同一個租戶的客戶';
COMMENT ON COLUMN mt_sales_order.order_date IS '訂單日期（業務時區）';
COMMENT ON COLUMN mt_sales_order.item_name IS '品項名稱';
COMMENT ON COLUMN mt_sales_order.unit_price IS '單價';
COMMENT ON COLUMN mt_sales_order.quantity IS '數量，1 以上';
COMMENT ON COLUMN mt_sales_order.subtotal IS '小計＝單價×數量，由後端計算';
COMMENT ON COLUMN mt_sales_order.order_status IS '狀態：NORMAL 正常、VOIDED 作廢（作廢的不計入任何金額與筆數）';
COMMENT ON COLUMN mt_sales_order.void_reason IS '作廢原因（作廢時必填）';
COMMENT ON COLUMN mt_sales_order.void_time IS '作廢時間，值為毫秒';
COMMENT ON COLUMN mt_sales_order.void_user IS '作廢者，即 mt_user.user_id';
COMMENT ON COLUMN mt_sales_order.create_time IS '新建資料的時間，值為毫秒';
COMMENT ON COLUMN mt_sales_order.create_user IS '新建資料的使用者編號，即 mt_user.user_id';
COMMENT ON COLUMN mt_sales_order.update_time IS '更新資料的時間，值為毫秒';
COMMENT ON COLUMN mt_sales_order.update_user IS '更新資料的使用者編號，即 mt_user.user_id';

-- 「不能修改，只能作廢」由資料庫再擋一次：唯一允許的修改是「正常 → 作廢」，而且訂單內容不能跟著改
CREATE FUNCTION mt_sales_order_only_void() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF OLD.order_status <> 'NORMAL' OR NEW.order_status <> 'VOIDED'
        OR NEW.tenant_id IS DISTINCT FROM OLD.tenant_id
        OR NEW.customer_id IS DISTINCT FROM OLD.customer_id
        OR NEW.order_date IS DISTINCT FROM OLD.order_date
        OR NEW.item_name IS DISTINCT FROM OLD.item_name
        OR NEW.unit_price IS DISTINCT FROM OLD.unit_price
        OR NEW.quantity IS DISTINCT FROM OLD.quantity
        OR NEW.subtotal IS DISTINCT FROM OLD.subtotal
        OR NEW.create_time IS DISTINCT FROM OLD.create_time
        OR NEW.create_user IS DISTINCT FROM OLD.create_user THEN
        RAISE EXCEPTION '訂單不能修改，只能作廢' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER tg_sales_order_only_void
    BEFORE UPDATE ON mt_sales_order
    FOR EACH ROW
EXECUTE FUNCTION mt_sales_order_only_void();

-- 權限：新增、讀取、作廢（UPDATE）；不能刪除
GRANT SELECT, INSERT, UPDATE ON mt_sales_order TO starter_app;

ALTER TABLE mt_sales_order ENABLE ROW LEVEL SECURITY;
ALTER TABLE mt_sales_order FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_read ON mt_sales_order FOR SELECT
    USING (current_setting('app.all_tenants', true) = 'on'
        OR tenant_id = current_setting('app.tenant_id', true));
CREATE POLICY tenant_insert ON mt_sales_order FOR INSERT
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true));
CREATE POLICY tenant_update ON mt_sales_order FOR UPDATE
    USING (tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true));
