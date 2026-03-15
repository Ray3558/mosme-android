# MosmeCheat iOS

iOS 版本的 MOSME 自動答題工具，使用 WKWebView 實現與 Android 版本相同的功能。

## 功能

- 使用 WKWebView 載入 MOSME 網站
- 三連點螢幕顯示/隱藏「自動答題」按鈕
- 兩層自動作答策略：
  1. 從頁面 Knockout.js ViewModel 取得答案
  2. 讀取 HTML `isanswer` 屬性

## 專案結構

```
ios/
├── MosmeCheat.xcodeproj/     # Xcode 專案檔
│   └── project.pbxproj
└── MosmeCheat/               # 源碼目錄
    ├── AppDelegate.swift     # 應用程式入口
    ├── SceneDelegate.swift   # 場景管理
    ├── ViewController.swift  # 主要邏輯（WebView + 自動答題）
    ├── Info.plist           # 應用程式配置
    ├── Assets.xcassets/     # 資源檔案
    └── Base.lproj/
        └── LaunchScreen.storyboard  # 啟動畫面
```

## 編譯方式

### 方法 1: 使用 Xcode（推薦）

1. 在 macOS 上安裝 Xcode
2. 開啟 `ios/MosmeCheat.xcodeproj`
3. 選擇目標裝置或模擬器
4. 點擊 Run (⌘R) 或 Archive (Product > Archive)

### 方法 2: 使用 GitHub Actions（自動化）

專案已配置 GitHub Actions，推送到 GitHub 後會自動編譯：

1. 將專案推送到 GitHub
2. 前往 Actions 頁面查看編譯進度
3. 編譯完成後下載 Artifacts 中的 IPA 檔案

**注意**: GitHub Actions 編譯的 IPA 是未簽名版本，需要自行簽名才能安裝到實體裝置。

### 方法 3: 命令列編譯

```bash
cd ios

# 編譯（不簽名）
xcodebuild clean build \
  -project MosmeCheat.xcodeproj \
  -scheme MosmeCheat \
  -configuration Release \
  -sdk iphoneos \
  CODE_SIGN_IDENTITY="" \
  CODE_SIGNING_REQUIRED=NO

# 建立 Archive
xcodebuild archive \
  -project MosmeCheat.xcodeproj \
  -scheme MosmeCheat \
  -configuration Release \
  -sdk iphoneos \
  -archivePath build/MosmeCheat.xcarchive \
  CODE_SIGN_IDENTITY="" \
  CODE_SIGNING_REQUIRED=NO
```

## 簽名與安裝

### 使用 Xcode 簽名（最簡單）

1. 在 Xcode 中開啟專案
2. 選擇 Target > Signing & Capabilities
3. 選擇你的 Team（需要 Apple Developer 帳號）
4. Xcode 會自動處理簽名

### 使用免費 Apple ID

1. 在 Xcode 中登入你的 Apple ID（Preferences > Accounts）
2. 選擇 Team 為你的個人帳號
3. 修改 Bundle Identifier 為唯一值（例如：`com.yourname.mosmecheat`）
4. 連接 iPhone 並信任開發者證書（設定 > 一般 > VPN與裝置管理）

### 使用第三方簽名工具

- **AltStore**: 免費，每 7 天需重新簽名
- **Sideloadly**: 支援 Windows/Mac
- **iOS App Signer**: Mac 專用

## 使用方法

1. 啟動應用程式
2. 自動載入 MOSME 登入頁面
3. 登入並進入測驗頁面
4. **三連點螢幕**顯示「自動答題」按鈕
5. 點擊按鈕自動完成答題

## 與 Android 版本的差異

| 功能 | Android | iOS |
|------|---------|-----|
| WebView | Android WebView | WKWebView |
| 按鈕觸發 | 三連點 | 三連點 |
| JavaScript 注入 | ✅ | ✅ |
| PDF 答案對照 | ✅ | ❌ (未實現) |
| 返回鍵支援 | ✅ | ✅ (手勢) |

## 系統需求

- iOS 13.0 或更高版本
- iPhone/iPad

## 注意事項

- 本工具僅供個人學習使用
- 使用前請確認符合相關服務條款
- 未簽名的 IPA 無法直接安裝到實體裝置
- 免費 Apple ID 簽名的應用每 7 天需重新安裝

## 技術細節

- **語言**: Swift 5.0
- **最低部署版本**: iOS 13.0
- **架構**: UIKit + WKWebView
- **自動答題邏輯**: 與 Android 版本相同的 JavaScript 代碼

## 疑難排解

### 編譯錯誤：Signing for "MosmeCheat" requires a development team

**解決方法**:
1. 在 Xcode 中選擇 Target > Signing & Capabilities
2. 勾選 "Automatically manage signing"
3. 選擇你的 Team

### 無法安裝到裝置：Untrusted Developer

**解決方法**:
1. 前往 設定 > 一般 > VPN與裝置管理
2. 找到你的開發者證書
3. 點擊「信任」

### GitHub Actions 編譯失敗

**可能原因**:
- Xcode 版本不相容
- 專案配置錯誤

**解決方法**: 查看 Actions 日誌，根據錯誤訊息調整配置

## 授權

本專案僅供學習交流使用。
