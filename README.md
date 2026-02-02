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

## 報告
- 位置：`target\gatling\<時間戳>\index.html`
- 本次成功：OK=10、KO=0（全部成功）
- 完整 URL 紀錄：`target\requests.txt`（每次執行覆蓋）

## 你需要改的地方
- `src/test/java/simulations/BasicSimulation.java`：改 `baseUrl`（已先改成 `https://your-api.example.com`，之後換成你的真實 URL）
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
