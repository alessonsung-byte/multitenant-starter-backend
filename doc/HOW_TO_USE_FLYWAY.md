# 資料庫版本控管（Flyway）

資料表的建立與異動一律寫成 SQL 檔，由 Flyway 在服務啟動時自動執行，不手動修改資料庫結構。

## 運作方式

- SQL 檔放在各模組的 `src/main/resources/db/migration/<模組>/`：帳號與租戶在 `user-service/.../db/migration/user/`（V1～V3），業務資料表在 `business-service/.../db/migration/business/`（V4 起）。位置設定於 `api-server` 的 `application.yml`：`spring.flyway.locations`
- 服務啟動時，Flyway 依版本號順序執行尚未執行過的 SQL 檔，並記錄在資料表 `flyway_schema_history`
- 已執行過的 SQL 檔內容若被修改，服務會啟動失敗（checksum 不符）

## 執行的資料庫帳號

Flyway 以建表帳號（`APP_FLYWAY_USERNAME`，資料表擁有者）執行；後端執行時則以 `starter_app` 連線（原因見 [README.md](../README.md)「資料庫帳號」）。所以：

- 每新增一張資料表，要在同一個 SQL 檔 `GRANT` 需要的權限給 `starter_app`（只給需要的；只能停用、作廢的資料不給 `DELETE`）
- 屬於租戶的資料表（有 `tenant_id`）要啟用並強制 Row-Level Security，寫法見 `business-service` 的 `V5__create_customer_and_order_tables.sql`（授權 → `ENABLE`／`FORCE ROW LEVEL SECURITY` → 讀取、新增、修改、刪除四個 policy），並在 `TenantIsolationIT`、`RlsFailClosedIT` 加上對應的測試

## 新增資料庫異動

1. 新增一個版本號更大的 SQL 檔，檔名格式 `V{版本號}__{說明}.sql`（兩個底線），例如 `V6__create_invoice_tables.sql`
2. **不要修改已經執行過的 SQL 檔**，任何異動都用新的版本檔處理
3. 重新啟動服務，確認 `flyway_schema_history` 多了一筆紀錄

新模組若有自己的 SQL 檔，放在該模組的 `src/main/resources/db/migration/<模組名稱>/`，並把路徑加到 `spring.flyway.locations`。
所有模組共用同一組版本號，不可重複。

## 本機開發時重建資料庫

開發階段若需要從頭重建（例如修改了尚未上線的 SQL 檔），刪除本機 postgres 的資料後重新啟動即可，見 [SETUP_DOCKER_LOCAL.md](SETUP_DOCKER_LOCAL.md)「清除資料重新開始」。
