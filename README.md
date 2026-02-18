# Gatling 專案說明（精簡）

可直接執行的 Gatling 壓測專案骨架（Maven + Java DSL）。

## 必要條件
- JDK 17+
- Maven 3.8+ 或使用本專案的 Maven Wrapper（免安裝）

## 快速開始
```
.\mvnw.cmd test-compile
.\mvnw.cmd gatling:test "-Dgatling.simulationClass=simulations.BasicSimulation"
```

## 基本操作（彙整）
- 編譯：`.\mvnw.cmd test-compile`
- 執行：`.\mvnw.cmd gatling:test "-Dgatling.simulationClass=simulations.BasicSimulation"`
- 加描述（顯示在報告）：`.\mvnw.cmd gatling:test "-Dgatling.simulationClass=simulations.BasicSimulation" "-Dgatling.runDescription=你的描述"`
- 看報告：`target\gatling\<時間戳>\index.html`
- 看完整 URL：`target\requests.txt`
- 產生摘要：`.\summarize-report.ps1`（輸出到 `target\summary.txt`）
- 強制更新依賴：`.\mvnw.cmd -U test-compile`
- 清理輸出：`.\mvnw.cmd clean`

## 自動摘要（結果解讀）
執行後產生可讀摘要與 `target\summary.txt`：
```
.\summarize-report.ps1
```

## CDN 判斷與執行方式
`BasicSimulation` 已改成「先 warm，再連續兩次 probe」的 CDN 驗證模式。

可用參數（可不填）：
- `-Dtest.baseUrl`：目標站台（預設 `https://httpbin.org`）
- `-Dtest.cachePath`：可快取路徑（預設 `/cache/60`）
- `-Dtest.warmRequests`：warm 次數（預設 `2`）

### 指令要改的地方
請改這 3 個值：
- `-Dtest.baseUrl=...`：你的網站主網域（例：`https://www.starlux-airlines.com`）
- `-Dtest.cachePath=...`：最終頁路徑（建議用不會再 301 的路徑，例：`/flights/zh-tw/`）
- `-Dtest.warmRequests=...`：預熱次數（建議 `2` 或 `3`）

通用模板：
```powershell
.\mvnw.cmd gatling:test "-Dgatling.simulationClass=simulations.BasicSimulation" "-Dtest.baseUrl=<你的網域>" "-Dtest.cachePath=<可快取路徑>" "-Dtest.warmRequests=2"
```

Starlux 範例：
```powershell
.\mvnw.cmd gatling:test "-Dgatling.simulationClass=simulations.BasicSimulation" "-Dtest.baseUrl=https://www.starlux-airlines.com" "-Dtest.cachePath=/flights/zh-tw/" "-Dtest.warmRequests=2"
```

啟動後 console 會輸出：
- `[CDN][Config] baseUrl=..., cachePath=..., warmRequests=...`
- `[CDN][Probe-1] ...`
- `[CDN][Probe-2] ...`
- `[CDN][Final] strict_hit_probe1=..., strict_hit_probe2=..., strict_hit_any=...`

判斷原理（實務慣例）：
- `cdn_in_path`：是否經過 CDN/WAF
- `cdn_cache_hit`：是否命中快取
- `strictHit`：`cdn_in_path && cdn_cache_hit`

判斷條件：
- `cdn_in_path = true`：任一 header 出現供應商特徵（`x-cdn` / `x-iinfo` / `cf-ray`）
- `cdn_cache_hit = true`：符合任一條件
- `age > 0`
- `x-cache` 包含 `HIT`
- `x-cache-hits > 0`
- `cf-cache-status = HIT`
- `strict_hit_any = true`：`Probe-1` 或 `Probe-2` 任一個 `strictHit=true`

結果判讀：
- `cdn_in_path=true, cdn_cache_hit=false`：有經過 CDN，但本次未命中快取
- `cdn_in_path=true, cdn_cache_hit=true`：有經過 CDN 且命中快取
- `strict_hit_any=true`：本輪測試至少一次同時滿足「經過 CDN + 命中快取」

判斷流程圖（文字版）：
```text
Warm_1 ... Warm_N -> CDN_Probe_1 -> CDN_Probe_2
                            |
                            |-- 讀取回應 header:
                            |   x-cdn / x-iinfo / cf-ray / age / x-cache / x-cache-hits / cf-cache-status
                            |
                            |-- cdn_in_path  = (x-cdn or x-iinfo or cf-ray present)
                            |-- cdn_cache_hit = (age>0 or x-cache contains HIT or x-cache-hits>0 or cf-cache-status==HIT)
                            |-- strictHit = cdn_in_path && cdn_cache_hit
                            |
                            `-- strict_hit_any = strictHit(Probe-1) OR strictHit(Probe-2)
```

附圖：`flow_cdn_zh.svg`

## 報告
- 位置：`target\gatling\<時間戳>\index.html`
- 本次成功：OK=10、KO=0（全部成功）
- 完整 URL 紀錄：`target\requests.txt`（每次執行覆蓋）

## 你需要改的地方
- 執行參數：`-Dtest.baseUrl` / `-Dtest.cachePath`（建議用參數指定，不用改程式碼）
- `data/users.csv`：改成你的測試帳號/參數（Feeder 來源）
- `bodies/login.json`：改成你要送的 request body（需要 POST/PUT 時）
- 執行指定腳本：`-Dgatling.simulationClass=你的完整類名`

## 資料與模板
- `src/test/resources/data`：Feeder 資料
- `src/test/resources/bodies`：請求 Body 模板
- `src/test/resources/logback.xml`：日誌

## 專案結構（精簡）
```
.
├─ pom.xml
├─ README.md
├─ .mvn/wrapper/...
├─ mvnw / mvnw.cmd
├─ src
│  └─ test
│     ├─ java
│     │  └─ simulations
│     │     └─ BasicSimulation.java
│     └─ resources
│        ├─ data
│        ├─ bodies
│        └─ logback.xml
└─ target/（執行後產生的報表與編譯輸出）
```

## 各檔案用途（精簡）
- `pom.xml`：依賴與 Gatling 插件設定
- `mvnw` / `mvnw.cmd` / `.mvn/wrapper`：Maven Wrapper（免安裝 Maven）
- `src/test/java/...`：壓測腳本
- `src/test/resources/...`：測試資料與模板
- `target/gatling/...`：報表輸出

## 常見問題（精簡）
- `mvn` 無法辨識：改用 `.\mvnw.cmd`
- 依賴卡住：`.\mvnw.cmd -U test-compile`
