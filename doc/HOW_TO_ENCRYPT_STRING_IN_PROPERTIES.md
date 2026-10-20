# 設定檔中的機敏資料加密（Jasypt）

設定檔（`application.yml` 等）中的帳號密碼可以用 `ENC(...)` 加密後再寫入，服務啟動時自動解密。
使用 [jasypt-spring-boot](https://github.com/ulisesbocchio/jasypt-spring-boot)。

## 目前的設定

- 函式庫與 Maven plugin：`codebase/pom.xml`（`jasypt-spring-boot-starter`、`jasypt-maven-plugin`）
- 啟用：`ApiServerApplication` 使用 `StandardEncryptableEnvironment`，讓啟動一開始就能解密
- 演算法等參數：`api-server/src/main/resources/application.yml` 的 `jasypt.encryptor.*`
- **主密碼**：一律由環境變數 `APP_JASYPT_PASSWORD` 提供，沒有設定時服務無法啟動

`application.yml` 中的資料庫帳密沒有寫死任何預設值，一律由環境變數提供（本機開發用的值在 `scripts/dev-env.sh`）。
環境變數的值本身也可以是 `ENC(...)`：例如部署主機的 `.env` 不想放明碼時，填 `APP_DB_PASSWORD=ENC(....)`，服務啟動時以 `APP_JASYPT_PASSWORD` 解密。

## 加密單一字串

以下指令都在 `codebase` 目錄執行，`-Djasypt.encryptor.password` 必須與執行服務時的 `APP_JASYPT_PASSWORD` 相同。

```shell
cd multitenant-starter-backend/codebase

# 加密，印出 ENC(...) 格式的結果
./mvnw -pl api-server jasypt:encrypt-value -Djasypt.encryptor.password="<主密碼>" -Djasypt.plugin.value="<要加密的字串>"

# 解密檢查
./mvnw -pl api-server jasypt:decrypt-value -Djasypt.encryptor.password="<主密碼>" -Djasypt.plugin.value="<ENC(...) 括號內的字串>"
```

把印出的 `ENC(...)` 貼到設定檔，例如：

```yaml
spring:
  datasource:
    password: "${APP_DB_PASSWORD:ENC(....)}"
```

## 一次加密整個模組的設定檔

1. 把要加密的明碼用 `DEC(...)` 包起來，例如 `password: "${APP_DB_PASSWORD:DEC(新密碼)}"`
2. 執行：

   ```shell
   ./mvnw -pl api-server jasypt:encrypt -Djasypt.encryptor.password="<主密碼>"
   ```

   該模組 `src/main/resources` 下的 `yml`／`yaml`／`properties` 中，所有 `DEC(...)` 會被改寫成 `ENC(...)`

3. 反向檢查（只印在畫面上，不會改檔案）：

   ```shell
   ./mvnw -pl api-server jasypt:decrypt -Djasypt.encryptor.password="<主密碼>"
   ```

## 確認

以新的 `APP_JASYPT_PASSWORD` 啟動服務，確認資料庫連線成功、可以正常登入。
