# 登入機制

本系統使用自己的帳號體系，由 api-server 直接驗證帳號密碼並簽發 JWT，不使用外部的 OAuth2／OIDC 服務。

## 整體流程

```
前端                                         api-server                         Redis
 │ 1. GET /api/public/publickey/{帳號}          │                                  │
 │ ───────────────────────────────────────────▶ │ 產生 RSA 金鑰對＋nonce            │
 │                                              │ 私鑰 ──────────────────────────▶ │（5 分鐘後自動刪除）
 │ ◀─────────────── 公鑰、nonce ─────────────── │                                  │
 │ 2. 產生隨機 AES 密鑰                          │                                  │
 │    AES(密鑰, IV=nonce) 加密密碼               │                                  │
 │    RSA(公鑰) 加密 AES 密鑰                    │                                  │
 │ 3. POST /api/public/login                    │                                  │
 │ ───────────────────────────────────────────▶ │ 取出並刪除私鑰 ◀──────────────── │（只能用一次）
 │                                              │ 解密 → 比對 BCrypt 密碼雜湊       │
 │                                              │ refresh token 編號 ────────────▶ │（24 小時後自動刪除）
 │ ◀──── access token、refresh token ───────── │                                  │
 │ 4. 之後呼叫 API：                             │                                  │
 │    Authorization: Bearer <access token>      │                                  │
```

密碼在網路上一律以加密後的形式傳送，而且每次登入用的金鑰都不同、只能用一次，截取到的封包無法重送登入。
資料庫只存密碼的 BCrypt 雜湊值。

## API

所有錯誤回應都是相同格式：

```json
{ "code": "ERROR_USER_LOGIN", "message": "帳號或密碼錯誤" }
```

`message` 可以直接顯示給使用者。帳號不存在與密碼錯誤回應相同的訊息，避免外人猜出哪些帳號存在。

### 1. 取得公鑰與 nonce

`GET /api/public/publickey/{username}`

```json
{
  "username": "admin",
  "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
  "nonce": "q3Jv0mX1n9s8b0rX0y6f4w=="
}
```

- `publicKey`：RSA 2048 公鑰，X.509（SubjectPublicKeyInfo）DER 格式再 Base64 編碼，也就是 PEM 檔去掉頭尾兩行的內容
- `nonce`：16 bytes 隨機值，Base64 編碼；必須在 5 分鐘內（`APP_NONCE_VALID_PERIOD_IN_MIN`）完成登入

### 2. 前端加密密碼

| 步驟 | 演算法 |
|---|---|
| 產生 AES 密鑰 | 隨機 16 bytes（AES-128） |
| 加密密碼 | AES-CBC、PKCS#5/PKCS#7 padding，IV 為 nonce Base64 解碼後的 16 bytes；密碼先以 **UTF-8** 轉成 bytes |
| 加密 AES 密鑰 | RSA、PKCS#1 v1.5 padding，使用第 1 步的公鑰 |

兩個加密結果都以 Base64 編碼後送出。前端實作見 multitenant-starter-frontend 的 `apps/starter/lib/login-cipher.ts`。

### 3. 登入

`POST /api/public/login`

```json
{
  "username": "admin",
  "password": "<AES 加密後的密碼，Base64>",
  "nonce": "<第 1 步的 nonce>",
  "encryption": "<RSA 加密後的 AES 密鑰，Base64>"
}
```

成功（200）：

```json
{
  "userId": "d9d3d4e2-ec90-4cf0-b984-c131d901e38e",
  "username": "admin",
  "roleNames": ["SYS_ADMIN"],
  "permissionNames": [],
  "tenantId": null,
  "tenantName": null,
  "mustChangePassword": true,
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiredAt": 1790155112776,
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

- `tenantId`／`tenantName`：所屬租戶；總管理員為 `null`
- `mustChangePassword`：為 `true` 時要先修改密碼（見下方「強制修改密碼」）

失敗：401（帳號密碼錯誤、帳號被鎖定、nonce 逾時或已使用過、帳號或所屬租戶停用）、400（欄位空白）。

**登入失敗次數限制**：同一個帳號連續失敗 5 次，鎖定 15 分鐘（`APP_LOGIN_LOCK_SECONDS`）。鎖定期間即使密碼正確也回覆與密碼錯誤相同的「帳號或密碼錯誤」，不透露帳號是否存在或被鎖定；不存在的帳號一樣計算。登入成功、或總管理員重設密碼時，失敗次數歸零。

### 4. 呼叫需要登入的 API

HTTP header 加上 `Authorization: Bearer <accessToken>`。
access token 缺少、無效或過期時回 401（`code` 為 `ERROR_UNAUTHORIZED`），前端應改用 refresh token 換發；
已登入但沒有權限時回 403（`ERROR_AUTHORIZATION_DENIED`）。

### 5. 換發 access token

`POST /api/public/refreshToken`

```json
{ "refreshToken": "<refresh token>" }
```

成功時回應格式與登入相同，其中 `accessToken` 是新的，`refreshToken` 原樣帶回（不會換新）。
refresh token 在有效期間內可以重複使用，直到過期或登出；這樣前端同時有多個請求各自換發時，不會互相讓對方失效。
refresh token 無效、過期或已登出時回 401，前端應導回登入頁。

### 6. 登出

`POST /api/public/logout`

```json
{ "refreshToken": "<refresh token>" }
```

作廢該 refresh token，回 204。token 無效時一樣回 204。
access token 無法作廢，會在逾期時間（預設 30 分鐘）後自然失效。

### 7. 目前登入者與修改密碼

| 方法 | 路徑 | 說明 |
|---|---|---|
| GET | `/api/me` | 目前登入者：帳號、角色、租戶 id 與名稱、是否需要改密碼 |
| POST | `/api/me/password` | 修改密碼，成功回 204 |

修改密碼與登入一樣不以明碼傳送：先以自己的帳號呼叫第 1 步取得公鑰與 nonce，用**同一把**隨機 AES 密鑰分別加密目前密碼與新密碼：

```json
{
  "nonce": "<第 1 步的 nonce>",
  "encryption": "<RSA 加密後的 AES 密鑰，Base64>",
  "currentPassword": "<AES 加密後的目前密碼，Base64>",
  "newPassword": "<AES 加密後的新密碼，Base64>"
}
```

密碼規則：至少 8 碼、不可與帳號相同（不分大小寫）、不可與舊密碼相同；不符合時回 400，`message` 可直接顯示。

### 強制修改密碼

新帳號、被總管理員重設密碼的帳號（`mt_user.must_change_password = true`），除了 `POST /api/me/password` 以外的 API 一律回 403（`code` 為 `ERROR_PASSWORD_CHANGE_REQUIRED`）；登入、登出等 `/api/public/**` 不受影響。

## Token 設計

| | access token | refresh token |
|---|---|---|
| 用途 | 呼叫 API | 只能用來換發 |
| 有效期間 | 30 分鐘（`APP_TOKEN_EXPIRED`） | 24 小時（`APP_REFRESH_TOKEN_EXPIRED`） |
| 伺服器端紀錄 | 無 | Redis，以 token 編號（jti）為 key |

- 兩者都是以 `APP_JWT_KEY` 簽章的 JWT（HMAC-SHA256），以自訂欄位 `typ`（`access`／`refresh`）區分，不能互相冒用
- 每次登入各自產生一個 refresh token，同一個帳號可以同時在電腦與手機登入
- 帳號或所屬租戶被停用後，已發出的 access token 也會立即失效（每次請求都會檢查帳號與租戶是否啟用），refresh token 也無法再換發
- 總管理員重設密碼或停用帳號時，該帳號所有 refresh token 立即作廢

## 角色與權限

- 資料表：`mt_role`（角色）、`mt_permission`（權限）、`mt_role_user`、`mt_role_permission`（多對多關聯）
- 登入時，使用者的角色會以 `ROLE_<角色名稱>`、權限會以權限名稱，放進 Spring Security
- controller 可以用 `@PreAuthorize("hasRole('SYS_ADMIN')")` 或 `@PreAuthorize("hasAuthority('<權限名稱>')")` 限制存取
- 角色有兩個：`SYS_ADMIN`（總管理員，不屬於任何租戶）、`TENANT_USER`（租戶帳號，`mt_user.tenant_id` 必填）；一個帳號只有一個角色。權限表目前是空的
- `/api/admin/**` 整組限制 `SYS_ADMIN`
- 登入後依角色決定可以存取哪個租戶的資料（`user-service` 的 `tenant/`）

## 預設帳號

資料表建立時（`V1__create_user_tables.sql`）會建立一個總管理員帳號：

| 帳號 | 初始密碼 | 角色 |
|---|---|---|
| `admin` | `admin` | `SYS_ADMIN` |

第一次登入時必須修改密碼（`V2__create_tenant_and_account_columns.sql` 設定 `must_change_password = true`）。

## 相關程式

| 項目 | 位置 |
|---|---|
| 登入 API | `api-server`：`PublicController`、`LoginService` |
| 每次請求驗證 access token | `user-service`：`JwtAuthenticationFilter` |
| 簽發／解析 JWT | `user-service`：`JwtHelper` |
| 一次性私鑰、refresh token 的 Redis 存取 | `user-service`：`RedisLoginKeyRepository`、`RedisRefreshTokenRepository` |
| RSA／AES 加解密 | `common-service`：`EncryptHelper` |
| 未登入（401）／權限不足（403）的回應 | `api-server`：`CustomAuthenticationEntryPoint`、`CustomAccessDeniedHandler` |
| 哪些 API 不需要登入 | `api-server`：`ProjectConfig` |
