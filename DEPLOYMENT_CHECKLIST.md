# 部署檢查清單

## ✅ 已完成

- [x] 創建 iOS 專案結構
- [x] 實現 ViewController（WebView + 自動答題）
- [x] 配置 Info.plist（允許 HTTP、Portrait 方向）
- [x] 創建 Xcode 專案檔（project.pbxproj）
- [x] 配置 GitHub Actions workflow
- [x] 創建啟動畫面（LaunchScreen.storyboard）
- [x] 創建 Assets.xcassets
- [x] 撰寫 README 文件
- [x] 撰寫簽名指南
- [x] 撰寫快速開始指南
- [x] 更新主 README

## 🔄 下一步（用戶需要做的）

### 1. 推送到 GitHub

```bash
git add .
git commit -m "Add iOS version with GitHub Actions"
git push origin main
```

### 2. 觸發 GitHub Actions

- 推送後會自動觸發
- 或前往 GitHub > Actions > Build iOS IPA > Run workflow

### 3. 等待編譯完成

- 編譯時間：約 5-10 分鐘
- 查看進度：GitHub > Actions 頁面

### 4. 下載 IPA

- 點擊完成的 workflow
- 在 Artifacts 區域下載 `MosmeCheat-iOS-unsigned`
- 解壓縮得到 `MosmeCheat.ipa`

### 5. 簽名並安裝

選擇以下任一方法：

#### 方法 A: AltStore（推薦，免費）
1. 安裝 AltStore 到電腦和 iPhone
2. 用 AltStore 開啟 IPA 檔案
3. 等待簽名和安裝完成

#### 方法 B: Xcode（需要 Mac）
1. 在 Mac 上開啟 `ios/MosmeCheat.xcodeproj`
2. 配置簽名（選擇 Team）
3. 連接 iPhone 並點擊 Run

#### 方法 C: Sideloadly（Windows/Mac）
1. 下載 Sideloadly
2. 連接 iPhone
3. 拖曳 IPA 到 Sideloadly
4. 輸入 Apple ID 並簽名

詳細步驟見 [ios/SIGNING_GUIDE.md](ios/SIGNING_GUIDE.md)

### 6. 信任開發者證書

在 iPhone 上：
1. 設定 > 一般 > VPN與裝置管理
2. 找到開發者證書
3. 點擊「信任」

### 7. 測試應用

1. 開啟 MosmeCheat 應用
2. 登入 MOSME
3. 進入測驗頁面
4. 三連點螢幕顯示「自動答題」按鈕
5. 點擊按鈕測試自動答題功能

## 🐛 疑難排解

### GitHub Actions 編譯失敗

**檢查項目**：
- [ ] 確認 `ios/` 目錄結構完整
- [ ] 查看 Actions 日誌中的錯誤訊息
- [ ] 確認 Xcode 版本相容性

**常見錯誤**：
- `xcodebuild: error: Unable to find a destination matching the provided destination specifier`
  - 解決：檢查 workflow 中的 `-destination` 參數

- `Code signing is required`
  - 解決：確認 workflow 中有 `CODE_SIGNING_REQUIRED=NO`

### IPA 無法安裝

**可能原因**：
- [ ] IPA 未簽名
- [ ] 簽名證書過期
- [ ] Bundle Identifier 衝突

**解決方法**：
- 使用 AltStore 或 Sideloadly 重新簽名
- 在 Xcode 中修改 Bundle Identifier

### 應用閃退

**檢查項目**：
- [ ] 確認 iOS 版本 >= 13.0
- [ ] 查看 Xcode Console 日誌
- [ ] 檢查 Info.plist 配置

## 📝 技術細節

### 核心檔案說明

| 檔案 | 說明 | 行數 |
|------|------|------|
| ViewController.swift | 主要邏輯（WebView + 自動答題） | 177 |
| AppDelegate.swift | 應用程式入口 | 13 |
| SceneDelegate.swift | 場景管理 | 12 |
| Info.plist | 應用程式配置 | 47 |

### 與 Android 版本的對應

| Android | iOS | 說明 |
|---------|-----|------|
| MainActivity.kt | ViewController.swift | 主要邏輯 |
| WebView | WKWebView | 網頁容器 |
| GestureDetector | UITapGestureRecognizer | 手勢識別 |
| evaluateJavascript | evaluateJavaScript | JS 執行 |

### JavaScript 邏輯

iOS 和 Android 版本使用**完全相同**的 JavaScript 代碼：
- Knockout.js ViewModel 答案提取
- isanswer 屬性識別
- 選項點擊邏輯

## 🎯 成功標準

- [ ] GitHub Actions 編譯成功
- [ ] 下載到 IPA 檔案
- [ ] 成功簽名並安裝到 iPhone
- [ ] 應用可以正常啟動
- [ ] WebView 可以載入網頁
- [ ] 三連點可以顯示按鈕
- [ ] 自動答題功能正常運作

## 📚 相關資源

- [iOS README](ios/README.md)
- [簽名指南](ios/SIGNING_GUIDE.md)
- [快速開始](QUICKSTART_iOS.md)
- [GitHub Actions Workflow](.github/workflows/build-ios.yml)

## 💡 提示

- 第一次編譯可能需要較長時間
- 建議使用 AltStore，可以自動重新簽名
- 如果有 Mac，直接用 Xcode 最方便
- 免費 Apple ID 簽名的應用每 7 天過期

## ⚠️ 重要提醒

- 本工具僅供個人學習使用
- 請遵守相關服務條款
- 不要分享已簽名的 IPA（包含個人證書）
- 定期檢查應用簽名有效期
