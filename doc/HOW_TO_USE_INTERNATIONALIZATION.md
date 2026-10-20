# 多語系訊息（i18n）

API 回傳的部分訊息（目前是 request 欄位驗證的錯誤訊息）依語系顯示對應文字。

## 語系怎麼決定

- 依前端送來的 HTTP header `Accept-Language` 決定（`common-service` 的 `LocaleConfig`，使用 `AcceptHeaderLocaleResolver`）
- 沒有帶此 header 時，使用預設語系：環境變數 `APP_LOCALE_LANGUAGE`（預設 `zh`）、`APP_LOCALE_COUNTRY`（預設 `TW`）

## 訊息檔

位於 `user-service/src/main/resources/`：

| 檔案 | 語系 |
|---|---|
| `messages.properties` | 預設（英文），找不到對應語系時使用 |
| `messages_zh_TW.properties` | 繁體中文 |

- 檔名格式：`messages_{語言}_{地區}.properties`
- `.properties` 檔以 ISO-8859-1 編碼儲存，中文必須寫成 `\uXXXX` 格式（IntelliJ 可開啟 Transparent native-to-ascii conversion 直接編輯中文）
- 文字中若有英文單引號 `'`，要寫成兩個 `''`
- `{0}`、`{1}` 是參數位置

## 使用方式

request 欄位驗證：

```java
@NotBlank(message = "{login.username.empty}")
private String username;
```

程式中取得訊息（`common-service` 的 `MessageHelper`）：

```java
messageHelper.localizedMessage("user.not.found.by.name", new String[]{username});
```
