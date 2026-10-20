# Maven 常用指令

> 本專案是 Maven 多模組專案，`mvnw` 與根 `pom.xml` 位於 `multitenant-starter-backend/codebase`，以下指令都在該目錄執行。

## 打包整個專案（所有模組）

```shell
./mvnw clean package
```

## 執行專案（api-server）

Maven 的 reactor 根目錄是 `pom` packaging，不能直接 `spring-boot:run`，需要用 `-pl` 指定模組、`-am` 讓 Maven 先建置它依賴的其他模組：

```shell
./mvnw -pl api-server -am spring-boot:run
```

`-am` 會讓每個被一起建置的模組都執行 `spring-boot:run`，但只有 api-server 有啟動程式，所以根目錄 `pom.xml` 把 `spring-boot-maven-plugin` 預設設為略過（`skip`），只在 `api-server/pom.xml` 改回不略過。新增模組時不需要另外設定。

也可以打包後直接執行：`./mvnw clean package` 後 `java -jar api-server/target/api-server.jar`。

## 清理建置目錄

```shell
./mvnw clean
```

## 查看專案結構（有哪些模組）

```shell
cat pom.xml | grep -A10 "<modules>"
```

## 查看專案依賴（依賴樹）

```shell
./mvnw dependency:tree
```

只看 compile scope：

```shell
./mvnw dependency:tree -Dscope=compile
```

## 查看 Maven 版本

```shell
./mvnw -v
```

## 查看合併後的有效設定

```shell
./mvnw help:effective-pom
```

## 依賴設定被修改後，強制重新檢查最新版本

```shell
./mvnw -U clean package
```

## 以精簡模式顯示執行詳情（減少互動/顏色輸出，適合 CI）

```shell
./mvnw -B clean package
```

## 僅針對特定模組執行（避免建置其他模組）

`-am`（also make）會先建置它相依的模組；若確定依賴的模組已經建置過，可以拿掉 `-am`：

```shell
./mvnw -pl <模組名稱> -am <goal>

# 例如：
./mvnw -pl user-service -am compile
```

## 顯示更多除錯資訊

```shell
./mvnw clean package -X
```

若只需要完整的錯誤堆疊（不需要完整 debug log），可以用：

```shell
./mvnw clean package -e
```

## 啟用平行構建模式

```shell
# 依實體 CPU 核心數平行處理
./mvnw -T 1C clean package

# 或指定固定執行緒數
./mvnw -T 4 clean package
```

## 啟用離線模式（適用於斷網或節省下載）

```shell
./mvnw -o clean package
```

## 執行單元測試

```shell
./mvnw test
```

## 產生測試報告（HTML）

Maven 內建 Surefire 已經會在 `target/surefire-reports` 產出測試結果；若要 HTML 格式的報告：

```shell
./mvnw surefire-report:report
```

## 進行完整驗證（測試＋checkstyle 等所有檢查）

`checkstyle` 已經設定綁定在 `validate` phase，會在一般建置流程中自動執行；若要連同其他檢查一起手動跑一次：

```shell
./mvnw verify
```

## 產生 Javadoc 文件

```shell
./mvnw javadoc:javadoc
```

## 只打包成 Jar 檔（不含測試）

```shell
./mvnw package -DskipTests

# 或針對單一模組
./mvnw -pl api-server -am package -DskipTests
```
