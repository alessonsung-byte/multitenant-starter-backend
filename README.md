# Multi-tenant Starter — Backend

多租戶（multi-tenant）SaaS 起始專案的後端 API。多個組織共用同一套系統與資料庫，**資料隔離做兩層**：程式層由 Hibernate 自動加上租戶條件，資料庫層再由 PostgreSQL Row-Level Security 擋一次——就算某支 API 漏寫條件、或有人手寫 SQL，資料庫也不會回傳其他租戶的資料。

| Repo | 內容 |
|---|---|
| [multitenant-starter-docs](https://github.com/alessonsung-byte/multitenant-starter-docs) | 需求、決策紀錄、資料庫設計、系統架構、AI 協作開發方法 |
| **multitenant-starter-backend**（本 repo） | Java 21 · Spring Boot 3.4 · PostgreSQL 16 · Redis 7 · Flyway |
| [multitenant-starter-frontend](https://github.com/alessonsung-byte/multitenant-starter-frontend) | Next.js 14 · TypeScript · Tailwind CSS · shadcn/ui |

## 特色

- **兩層多租戶隔離**：Hibernate 6 `@TenantId`＋PostgreSQL Row-Level Security；每個交易開始時以 `set_config(..., true)` 告訴資料庫目前租戶（只在這個交易有效，不會殘留在連線池）。故障時是「看不到」，不是「看到別人的」
- **建表帳號與執行帳號分開**：後端以非擁有者、非 superuser 的 `starter_app` 連線，RLS 一定生效；權限逐表授予（只能作廢的資料表沒有 `DELETE`）
- **總管理員只能檢視**：可以跨租戶檢視與報表，但資料庫本身就不允許總管理員寫入任何租戶的業務資料
- **登入安全**：一次性 RSA 公鑰＋AES 加密傳送密碼、JWT access／refresh token、強制首次改密碼、連續失敗鎖定（Redis）、停用帳號或租戶下一次請求立即失效
- **操作紀錄**：在業務交易內寫入「誰、何時、哪一筆、哪些欄位從什麼改成什麼」，只有總管理員能讀
- **範例業務模組**：客戶 → 訂單（只能作廢、不能修改，資料庫 trigger 再擋一次）→ 月報表；新增功能照這個模組的寫法即可
- **測試**：Testcontainers 啟動真的 PostgreSQL 與 Redis；`TenantIsolationIT` 證明兩個租戶透過每支 API 都拿不到對方的資料，`RlsFailClosedIT` 直接以 `starter_app` 連線證明資料庫層擋得住

## 架構

```
Next.js 伺服器（前端）
   │  Authorization: Bearer <access token>（Docker 內部網路，不對外）
   ▼
api-server（Spring Boot）
   │  1. JwtAuthenticationFilter：驗證 token → 帳號、角色、所屬租戶 → TenantContext
   │  2. Controller：@PreAuthorize 角色檢查
   │  3. Service：業務規則與操作紀錄（@Transactional）
   │     └─ TenantAwareJpaTransactionManager：交易開始時 set_config('app.tenant_id', …)
   │  4. Repository：Hibernate @TenantId 自動加上 tenant_id = 目前租戶
   ▼
PostgreSQL（Row-Level Security：每張租戶資料表都 ENABLE＋FORCE，讀寫分開的 policy）
Redis（登入用一次性金鑰、refresh token、登入失敗次數）
```

模組與依賴方向：

```
api-server ──► business-service ──► user-service ──► common-service
     └───────► logging-service ─────────────────────► common-service
```

| 模組 | 內容 |
|---|---|
| `common-service` | 共用例外、錯誤回應格式、RSA／AES 加解密、多語系訊息 |
| `logging-service` | API 請求日誌；`@NoLogToParam` 等註解避免個人資料寫進日誌 |
| `user-service` | 帳號、租戶、角色、JWT、`tenant/`（多租戶的兩層保護）、操作紀錄介面 |
| `business-service` | 範例業務：`customer/`、`order/`、`report/`、`audit/` |
| `api-server` | 啟動程式、Controller、安全設定、整合測試 |

完整的設計說明見 docs repo 的 `13-系統架構設計.md`，技術選型的理由見 `03-決策紀錄.md`。

## 目錄結構

```
multitenant-starter-backend/
├── codebase/                Maven 專案（mvnw、根 pom.xml）
│   ├── common-service/
│   ├── logging-service/
│   ├── user-service/        db/migration/user/（V1～V3：帳號、租戶、授權）
│   ├── business-service/    db/migration/business/（V4 起：操作紀錄、客戶、訂單）
│   └── api-server/          db/migration/local/（只在 local profile：示範資料）
├── docker/                  本機與伺服器的 docker 設定；postgres-init/ 建立 starter_app
├── scripts/dev-env.sh       本機開發用的環境變數
└── doc/                     說明文件
```

## 本機啟動

### 1. 需要安裝

- JDK 21
- Docker Desktop（執行 PostgreSQL 與 Redis，以及整合測試）

Maven 不需要另外安裝，使用專案內的 `./mvnw`。

### 2. 啟動 PostgreSQL 與 Redis

```shell
cd docker
docker compose -f docker-compose-local.yml up -d
```

第一次啟動時會自動建立後端執行用的資料庫帳號 `starter_app`（`docker/postgres-init/`）。

### 3. 設定環境變數並啟動

```shell
. scripts/dev-env.sh          # 本機開發用的帳密、隨機 JWT 金鑰、啟用 local profile
cd codebase
./mvnw -pl api-server -am spring-boot:run
```

```shell
curl http://localhost:8080/ping          # 回應 pong
```

第一次啟動時 Flyway 會自動建立資料表與預設帳號 `admin`／`admin`（第一次登入必須修改密碼）。

### 4. 本機示範資料

`scripts/dev-env.sh` 啟用了 Spring profile `local`，Flyway 會多執行 `db/migration/local/`，建立兩個示範租戶、三組帳號與幾筆客戶、訂單：

| 帳號 | 密碼 | 身分 |
|---|---|---|
| `admin` | `Starter-Admin-2026` | 總管理員（取代初始密碼 `admin`） |
| `tenant_a_user` | `Starter-TenantA-2026` | 示範租戶 A |
| `tenant_b_user` | `Starter-TenantB-2026` | 示範租戶 B |

分別用兩個租戶帳號登入，就能看到彼此看不到對方的客戶與訂單。**這些密碼寫在 git 裡，正式環境絕對不可以啟用 local profile。**

不要示範資料時，啟動前執行 `unset SPRING_PROFILES_ACTIVE`。

## 資料庫帳號

| 帳號 | 用途 | 本機開發的值 | 環境變數 |
|---|---|---|---|
| 建表帳號 | Flyway 建立、修改資料表，是資料表擁有者 | `root`／`starter_owner_local` | `APP_FLYWAY_USERNAME`／`APP_FLYWAY_PASSWORD` |
| `starter_app` | 後端執行時連線 | `starter_app`／`starter_app_local` | `APP_DB_USERNAME`／`APP_DB_PASSWORD` |

- 為什麼要分開：PostgreSQL 的 Row-Level Security 對資料表擁有者不生效；以非擁有者連線，資料庫才擋得住「讀到其他租戶的資料」與「總管理員寫入業務資料」
- **新增資料表時**：在同一個 migration 授權給 `starter_app`（只給需要的權限），屬於租戶的資料表啟用並強制 RLS，寫法見 `business-service` 的 `V5__create_customer_and_order_tables.sql`（[doc/HOW_TO_USE_FLYWAY.md](doc/HOW_TO_USE_FLYWAY.md)）

## 測試

```shell
cd codebase
./mvnw clean verify
```

- 單元測試（`*Test`）與整合測試（`*IT`）都會執行；整合測試以 Testcontainers 自動啟動 PostgreSQL 與 Redis，**電腦上必須有正在執行的 Docker**，不會動到本機開發用的資料庫
- 多租戶隔離：`TenantIsolationIT`（透過 API）、`RlsFailClosedIT`（直接以 `starter_app` 連線）；**每新增一張屬於租戶的資料表或 API，都要在這兩個測試加上對應的項目**
- 其他：`AccountManagementIT`、`LoginLockoutIT`、`AuditLogIT`、`CustomerOrderApiIT`、`RequestRejectedIT`

## API

| 方法與路徑 | 誰 | 說明 |
|---|---|---|
| `GET /api/public/publickey/{username}`、`POST /api/public/login`、`/refreshToken`、`/logout` | 不需登入 | 登入流程（[doc/HOW_TO_LOGIN.md](doc/HOW_TO_LOGIN.md)） |
| `GET /api/me`、`POST /api/me/password` | 所有登入者 | 目前登入者、修改密碼 |
| `GET/POST /api/customers`、`GET/PUT/DELETE /api/customers/{id}` | 租戶帳號（總管理員只能 GET） | 客戶 |
| `GET/POST /api/orders`、`POST /api/orders/{id}/void` | 租戶帳號（總管理員只能 GET） | 訂單、作廢 |
| `GET /api/reports/monthly?month=yyyy-MM` | 租戶帳號；總管理員帶 `tenantId` | 月報表 |
| `/api/admin/tenants`、`/api/admin/accounts`、`/api/admin/audit-logs`、`/api/admin/reports/tenants` | 總管理員 | 租戶、帳號、操作紀錄、跨租戶報表 |

錯誤回應統一為 `{ "code": "...", "message": "可以直接顯示的文字" }`；存取其他租戶的資料一律回 404。

## 環境變數

**必填**：

| 變數 | 說明 |
|---|---|
| `APP_JWT_KEY` | JWT 簽章金鑰，Base64、至少 32 bytes：`openssl rand -base64 32` |
| `APP_JASYPT_PASSWORD` | 設定值寫成 `ENC(...)` 時的解密主密碼（[doc/HOW_TO_ENCRYPT_STRING_IN_PROPERTIES.md](doc/HOW_TO_ENCRYPT_STRING_IN_PROPERTIES.md)） |
| `APP_FLYWAY_USERNAME`、`APP_FLYWAY_PASSWORD` | 建表帳號 |
| `APP_DB_PASSWORD` | `starter_app` 的密碼 |

**選填**（括號內為預設值）：

| 變數 | 說明 |
|---|---|
| `API_SERVER_PORT` (`8080`) | 服務監聽的 port |
| `APP_DB_IP` (`127.0.0.1`)、`APP_DB_PORT` (`5432`)、`APP_DB_NAME` (`starter`)、`APP_DB_USERNAME` (`starter_app`) | PostgreSQL 連線 |
| `APP_REDIS_HOST` (`127.0.0.1`)、`APP_REDIS_PORT` (`6379`)、`APP_REDIS_PASSWORD` (空) | Redis 連線 |
| `APP_TOKEN_EXPIRED` (`1800000`)、`APP_REFRESH_TOKEN_EXPIRED` (`86400000`) | access／refresh token 有效期間（毫秒） |
| `APP_LOGIN_LOCK_SECONDS` (`900`) | 連續登入失敗 5 次後鎖定的秒數 |
| `APP_PW_ENC_STRENGTH` (`12`) | BCrypt 強度 |
| `APP_SHOW_SQL` (`false`) | 日誌印出 SQL（只在除錯時使用） |
| `SPRING_PROFILES_ACTIVE` (空) | `local`：建立示範資料（正式環境必須留空） |

## 部署

測試環境與正式環境使用同一份 `docker/docker-compose-server.yml`（前端＋後端＋PostgreSQL＋Redis），差別只在各自的 `.env`（範本 `docker/.env.server.example`）。步驟見 [doc/SETUP_DOCKER_SERVER.md](doc/SETUP_DOCKER_SERVER.md)。

## 文件

| 文件 | 內容 |
|---|---|
| [HOW_TO_LOGIN.md](doc/HOW_TO_LOGIN.md) | 登入流程、token 設計、角色與權限、強制修改密碼、登入鎖定 |
| [HOW_TO_USE_FLYWAY.md](doc/HOW_TO_USE_FLYWAY.md) | 資料表異動的方式、新增租戶資料表的規則 |
| [SETUP_DOCKER_LOCAL.md](doc/SETUP_DOCKER_LOCAL.md) | 本機的 PostgreSQL 與 Redis |
| [SETUP_DOCKER_SERVER.md](doc/SETUP_DOCKER_SERVER.md) | 部署到伺服器 |
| [MAVEN_GET_STARTED.md](doc/MAVEN_GET_STARTED.md) | 常用 Maven 指令 |
| [HOW_TO_ENCRYPT_STRING_IN_PROPERTIES.md](doc/HOW_TO_ENCRYPT_STRING_IN_PROPERTIES.md) | 設定值加密（Jasypt） |
| [HOW_TO_USE_INTERNATIONALIZATION.md](doc/HOW_TO_USE_INTERNATIONALIZATION.md) | 多語系訊息 |
| [HOW_TO_CHECK_DEPENDENCIES.md](doc/HOW_TO_CHECK_DEPENDENCIES.md) | 第三方函式庫弱點掃描 |

## 授權

[MIT License](LICENSE)
