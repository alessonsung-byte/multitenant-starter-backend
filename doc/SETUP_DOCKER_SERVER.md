# 伺服器 Docker 部署（前端＋後端＋PostgreSQL＋Redis）

本文件說明用 `docker/docker-compose-server.yml` 在伺服器上啟動整套系統。**測試環境（SIT）與正式環境使用同一份設定與同樣的步驟**，
差別只在各自的 `.env`（每個環境的密碼、金鑰都要各自產生），這樣 SIT 測過的設定就是正式環境實際執行的設定。
本機開發見 [SETUP_DOCKER_LOCAL.md](SETUP_DOCKER_LOCAL.md)。

> **目前的範圍**：本文件適用於任何裝了 Docker 的 Linux 主機，尚未包含 HTTPS 與反向代理、資料庫備份。
> 這些完成前，**不要讓系統對外開放**。

## 架構

| 服務（容器名稱） | 用途 | 主機上連得到的位址 |
|---|---|---|
| `starter-frontend` | 前端網頁（Next.js） | `127.0.0.1:4200`（網址 `http://127.0.0.1:4200/starter`） |
| `starter-api-server` | 後端 API | `127.0.0.1:8085`（只供主機上測試、除錯） |
| `starter-postgres` | 資料庫 | 不發布到主機，只有後端連得到 |
| `starter-redis` | 登入用的暫存資料 | 不發布到主機，只有後端連得到 |

- 所有發布到主機的 port 都只綁 `127.0.0.1`：Docker 發布的 port 會繞過 ufw 防火牆，綁 `127.0.0.1` 才能確保外面的電腦連不進來；之後由同一台主機上的反向代理對外提供 HTTPS
- 前端透過 Docker 內部網路呼叫後端（`http://starter-api-server:8083`），不經過主機的 8085
- **SIT 與正式環境不能放在同一台主機**：容器名稱、port（`127.0.0.1:4200`、`127.0.0.1:8085`）、日誌目錄（`/opt/starter/logs/api`）都相同

## 第一次部署

以下假設兩個專案在部署主機上的位置是 `~/multitenant-starter-backend`、`~/multitenant-starter-frontend`，實際路徑依主機調整。

### 1. 前提

1. 主機已安裝 Docker（含 `docker compose` 指令），且 docker 服務已啟動
2. 主機可以連上網路（建置時要下載 Docker 映像檔、npm 套件與 Google Fonts 字型）
3. 建立後端日誌目錄：

   ```shell
   bash ~/multitenant-starter-backend/docker/create-working-dir.sh
   ```

### 2. 建置後端映像檔

```shell
cd ~/multitenant-starter-backend/codebase
./mvnw -pl api-server -am clean package    # 會自動把 api-server.jar 複製到 docker/

cd ~/multitenant-starter-backend/docker
docker build -t starter-api-server -f api-server.dockerfile .    # 最後有一個英文句點
```

### 3. 建置前端映像檔

```shell
cd ~/multitenant-starter-frontend
docker build -t starter-frontend --build-arg NEXT_PUBLIC_BASE_PATH=/starter .    # 最後有一個英文句點
```

- `NEXT_PUBLIC_BASE_PATH` 是網站的子路徑（網址 `/starter` 的部分），**在建置時就會寫入程式**，之後要改必須重新建置
- 前端的 `.env.development` 等環境變數檔不會被包進映像檔（`.dockerignore` 已排除）

### 4. 設定密鑰

```shell
cd ~/multitenant-starter-backend/docker
cp .env.server.example .env
chmod 600 .env      # 只有自己能讀
nano .env           # 依檔案內的說明填入每一項
```

- 每一項都要重新產生，**每個環境各自一組，不可沿用本機開發或其他環境的值**；產生指令寫在 `.env.server.example` 各項上方
- `APP_SPRING_PROFILES`：SIT 可以填 `local`（建立示範租戶與三組固定密碼帳號，見 README「本機示範資料」）；**正式環境必須留空**
- `APP_QUICK_LOGIN_*`：SIT 可填三組測試帳號的密碼，登入頁會出現快速登入按鈕；**正式環境必須留空**；SIT 開放給其他人使用之前也要清空，改完執行 `docker compose -f docker-compose-server.yml up -d starter-frontend` 讓前端重新讀取
- 資料庫有兩個帳號：建表帳號（`APP_FLYWAY_USERNAME`／`APP_FLYWAY_PASSWORD`）與後端執行用的 `starter_app`（只填密碼 `APP_DB_PASSWORD`），原因見 [README.md](../README.md)「資料庫帳號」。`starter_app` 由資料庫第一次啟動時的 `docker/postgres-init/` 自動建立
- 必填的值沒有填時，下一步會直接顯示「請在 .env 設定 …」並拒絕啟動

### 5. 啟動

```shell
cd ~/multitenant-starter-backend/docker
docker compose -f docker-compose-server.yml up -d
```

### 6. 確認

```shell
cd ~/multitenant-starter-backend/docker
docker compose -f docker-compose-server.yml ps        # 四個服務都是 running（資料庫與 Redis 顯示 healthy）
docker compose -f docker-compose-server.yml logs starter-api-server --tail 50

# 前端：應回應 307（導向登入頁）
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:4200/starter

# 確認資料庫與 Redis 沒有發布到主機：下面兩行都不應該有任何輸出
sudo ss -tlnp | grep ':5432'
sudo ss -tlnp | grep ':6379'
```

在主機上（或用 SSH 通道）以瀏覽器開啟 `http://127.0.0.1:4200/starter`，用預設帳號 `admin`／`admin` 登入。

> 登入 cookie 預設只能透過 HTTPS 傳送（`APP_COOKIE_SECURE=true`）。瀏覽器通常把 `localhost`／`127.0.0.1` 視為安全連線，所以用 SSH 通道測試可以登入；若透過其他 http 位址無法登入，是這個設定造成的，請等 HTTPS 設定完成，不要為了測試長期關閉。

**第一次登入後必須立即更換 `admin` 密碼**。

## 日常操作

所有指令都在 `~/multitenant-starter-backend/docker` 執行。

| 要做的事 | 指令 |
|---|---|
| 查看狀態 | `docker compose -f docker-compose-server.yml ps` |
| 停止全部（資料保留） | `docker compose -f docker-compose-server.yml stop` |
| 重新啟動全部 | `docker compose -f docker-compose-server.yml up -d` |
| 查看後端日誌 | `tail -f /opt/starter/logs/api/api-server.log` |
| 查看前端日誌 | `docker compose -f docker-compose-server.yml logs -f starter-frontend` |

- 每個服務都設定了 `restart: unless-stopped`：主機重開機或程式當掉會自動重啟；用 `stop` 手動停掉的服務不會自動啟動
- 資料庫資料存在 Docker volume `starter_pgdata`。**不要執行 `docker compose down -v`**，`-v` 會刪除資料庫資料

## 更新程式

```shell
# 後端：重做「2. 建置後端映像檔」，然後
docker compose -f docker-compose-server.yml up -d starter-api-server

# 前端：重做「3. 建置前端映像檔」，然後
docker compose -f docker-compose-server.yml up -d starter-frontend
```

資料庫結構的變更由後端啟動時的 Flyway 自動套用；更新前請先備份資料庫。

## 回到上一版

建置新映像檔前，先把目前的映像檔加上備份標籤：

```shell
docker tag starter-api-server starter-api-server:previous
docker tag starter-frontend starter-frontend:previous
```

新版有問題時，把備份標籤改回來再啟動：

```shell
docker tag starter-api-server:previous starter-api-server
docker tag starter-frontend:previous starter-frontend
docker compose -f docker-compose-server.yml up -d
```

> 若新版後端已經執行了新的資料庫 migration（Flyway），只換回舊映像檔可能無法啟動，需要從備份還原資料庫。
