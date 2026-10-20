# 第三方函式庫弱點掃描

使用 [OWASP Dependency-Check](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/index.html)，比對專案使用的第三方函式庫與已知弱點資料庫（NVD），找出需要升級的函式庫。

## 設定

已在 `codebase/pom.xml` 設定好 `dependency-check-maven` plugin：

- 產出 HTML、CSV 報告，CVSS 分數 ≥ 8 時掃描結果為失敗
- 排除名單：`codebase/dependency-check-suppression.xml`
- **刻意沒有綁定在一般建置流程**，`./mvnw clean package` 不會自動掃描，需要時手動執行

NVD API Key 可以大幅加快弱點資料庫的下載速度，由環境變數提供，不寫在檔案裡。
免費申請：<https://nvd.nist.gov/developers/request-an-api-key>

```shell
export NVD_API_KEY=<你的 API Key>
```

## 執行掃描

第一次執行需要下載弱點資料庫，約 10 分鐘以上；之後只做增量更新。

```shell
cd multitenant-starter-backend/codebase
./mvnw org.owasp:dependency-check-maven:check

# 只掃描指定模組
./mvnw -pl user-service org.owasp:dependency-check-maven:check
```

報告位於各模組的 `target/dependency-check-report.html`，用瀏覽器開啟。

## 處理弱點

1. 在報告中找到有弱點的函式庫與 CVE 編號
2. 到 <https://mvnrepository.com> 找出沒有該弱點的版本，通常選同一個次版號的最新版
3. 調整 `codebase/pom.xml` `<properties>` 中對應的版本號（Spring Boot 管理的函式庫則升級 `spring-boot-starter-parent`）
4. 若確認是誤判或暫時無法升級，在報告中點選 `suppress` 取得 XML，貼到 `dependency-check-suppression.xml`，並在 `<notes>` 寫明原因
5. 重新執行掃描確認

## 最近一次掃描

- 沒有 NVD API Key 時，第一次下載弱點資料庫約 2～3 小時；之後加上 `-DautoUpdate=false` 可以直接使用已下載的資料。Sonatype OSS Index 需要帳號，沒有帳號時加上 `-DossindexAnalyzerEnabled=false`，否則每個 jar 都會等待逾時
- 處理：Spring Boot 3.4.2 → 3.4.13；`pom.xml` 以屬性指定 netty、Tomcat、Spring Framework、log4j、Jackson、PostgreSQL JDBC 的修補版（升級 Spring Boot 時，這些屬性要重新檢查是否還需要）
- 高風險／嚴重從 159 項降到 53 項；剩下的在 Spring Boot 3.4、Spring Security 6.4、Spring Framework 6.2 的最後一版，需要升級到 Spring Boot 3.5 以上

