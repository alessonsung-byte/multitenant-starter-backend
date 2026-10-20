# 本機開發用的 PostgreSQL 與 Redis

本機開發時，api-server 直接在自己的電腦上執行（見 [README.md](../README.md)「本機啟動」；需要的環境變數由 `scripts/dev-env.sh` 設定），
只有它需要的 PostgreSQL 與 Redis 用 docker 啟動。部署到伺服器（測試環境、正式環境）請看 [SETUP_DOCKER_SERVER.md](SETUP_DOCKER_SERVER.md)。

## 前提

- 已安裝並啟動 Docker Desktop
- 本機的 5432（PostgreSQL）、6379（Redis）連接埠沒有被其他程式佔用

## 啟動

```shell
cd multitenant-starter-backend/docker
docker compose -f docker-compose-local.yml up -d
```

會啟動兩個容器：

| 容器 | 連接埠 | 說明 |
|---|---|---|
| `starter-postgres-local` | 5432 | 自動建立資料庫 `starter`；建表帳號 `root`／`starter_owner_local`，後端執行用帳號 `starter_app`／`starter_app_local` |
| `starter-redis-local` | 6379 | 無密碼 |

資料表不用手動建立，api-server 第一次啟動時會由 Flyway 自動建立（見 [HOW_TO_USE_FLYWAY.md](HOW_TO_USE_FLYWAY.md)）。

後端執行用帳號 `starter_app` 由 `docker/postgres-init/01-create-app-role.sh` 在**資料庫第一次建立時**自動建立（原因見 [README.md](../README.md)「資料庫帳號」）。

## 確認狀態

```shell
docker compose -f docker-compose-local.yml ps     # STATUS 應顯示 healthy
docker compose -f docker-compose-local.yml logs starter-postgres-local
```

## 停止（保留資料）

```shell
docker compose -f docker-compose-local.yml stop
```

## 清除資料重新開始

會刪除本機資料庫與 Redis 的所有資料，下次啟動 api-server 時重新建立資料表與預設帳號：

```shell
docker compose -f docker-compose-local.yml down -v
```
