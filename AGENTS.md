# AGENTS.md — 後端 repo 入口

> **本 repo 不放 AI 開發規則。** 開始任何工作前，先讀文件 repo 的 `AGENTS.md`（三個 repo 預設放在同一層：`../multitenant-starter-docs/AGENTS.md`），並依它第 1 節的順序讀完文件。
> 本檔只補充這個 repo 自己的指令與位置。本檔與文件 repo 的內容有出入時，依文件 repo `AGENTS.md` 第 6 節當作矛盾處理。

## 指令

| 要做什麼 | 指令 |
|---|---|
| 全部測試（單元＋整合，需要正在執行的 Docker） | `cd codebase && ./mvnw clean verify` |
| 啟動本機 PostgreSQL 與 Redis | `cd docker && docker compose -f docker-compose-local.yml up -d` |
| 本機啟動後端（port 8080） | `. scripts/dev-env.sh && cd codebase && ./mvnw -pl api-server -am spring-boot:run` |
| 三個 repo 一起驗收 | 在文件 repo 執行 `bash goal/verify-local.sh` |

細節（環境變數、資料庫帳號、示範資料）見本 repo 的 `README.md`。

## 位置

- Maven 專案根目錄：`codebase/`（模組：`api-server`、`business-service`、`common-service`、`logging-service`、`user-service`）
- 業務資料表的 Flyway migration：`codebase/business-service/src/main/resources/db/migration/business/`
- 新增業務模組的步驟：文件 repo 的 `11-開發計畫.md` 第二節
