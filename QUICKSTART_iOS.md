# 快速開始：從 Android 到 iOS

本指南幫助你快速將 MosmeCheat 從 Android 轉換到 iOS 並編譯成 IPA。

## 📁 專案結構

```
mosme-android/
├── android/              # 原 Android 專案
│   └── app/
│       └── src/main/java/com/mosme/cheat/MainActivity.kt
├── ios/                  # 新建的 iOS 專案 ✨
│   ├── MosmeCheat.xcodeproj/
│   ├── MosmeCheat/
│   │   ├── AppDelegate.swift
│   │   ├── SceneDelegate.swift
│   │   ├── ViewController.swift      # 主要邏輯（對應 MainActivity.kt）
│   │   ├── Info.plist
│   │   └── Assets.xcassets/
│   ├── README.md
│   └── SIGNING_GUIDE.md
└── .github/workflows/
    └── build-ios.yml     # GitHub Actions 自動編譯
```

## 🚀 三種使用方式

### 方式 1: GitHub Actions 自動編譯（最簡單）

1. **推送到 GitHub**
   ```bash
   git add .
   git commit -m "Add iOS version"
   git push
   ```

2. **查看編譯進度**
   - 前往你的 GitHub Repository
   - 點擊 "Actions" 標籤
   - 等待編譯完成（約 5-10 分鐘）

3. **下載 IPA**
   - 點擊完成的 workflow
   - 在 "Artifacts" 區域下載 `MosmeCheat-iOS-unsigned`
   - 解壓縮得到 `MosmeCheat.ipa`

4. **簽名並安裝**
   - 參考 [SIGNING_GUIDE.md](ios/SIGNING_GUIDE.md)
   - 推薦使用 AltStore（免費）

### 方式 2: 在 Mac 上用 Xcode 編譯

1. **開啟專案**
   ```bash
   cd ios
   open MosmeCheat.xcodeproj
   ```

2. **配置簽名**
   - 選擇 Target "MosmeCheat"
   - 前往 "Signing & Capabilities"
   - Team: 選擇你的 Apple ID
   - Bundle Identifier: 改成唯一值（如 `com.yourname.mosmecheat`）

3. **連接 iPhone 並執行**
   - 用 USB 連接 iPhone
   - 在 Xcode 頂部選擇你的 iPhone
   - 點擊 Run (⌘R)

4. **信任開發者**
   - iPhone: 設定 > 一般 > VPN與裝置管理
   - 找到你的開發者證書並點擊「信任」

### 方式 3: 命令列編譯（進階）

```bash
cd ios

# 編譯（未簽名）
xcodebuild archive \
  -project MosmeCheat.xcodeproj \
  -scheme MosmeCheat \
  -configuration Release \
  -sdk iphoneos \
  -archivePath build/MosmeCheat.xcarchive \
  CODE_SIGN_IDENTITY="" \
  CODE_SIGNING_REQUIRED=NO

# 打包成 IPA
mkdir -p build/Payload
cp -r build/MosmeCheat.xcarchive/Products/Applications/MosmeCheat.app build/Payload/
cd build
zip -r MosmeCheat.ipa Payload
```

## 📱 功能對照表

| 功能 | Android | iOS | 說明 |
|------|---------|-----|------|
| WebView | ✅ | ✅ | Android WebView → WKWebView |
| 三連點顯示按鈕 | ✅ | ✅ | 完全相同 |
| 自動答題（Knockout.js） | ✅ | ✅ | JavaScript 邏輯相同 |
| 自動答題（isanswer） | ✅ | ✅ | JavaScript 邏輯相同 |
| PDF 答案對照 | ✅ | ❌ | iOS 版未實現 |
| 返回鍵 | ✅ | ✅ | iOS 使用手勢 |

## 🔧 常見問題

### Q: 為什麼 GitHub Actions 編譯的 IPA 無法直接安裝？

A: iOS 要求所有應用都必須簽名。GitHub Actions 生成的是未簽名版本，需要用 AltStore 等工具簽名後才能安裝。詳見 [SIGNING_GUIDE.md](ios/SIGNING_GUIDE.md)。

### Q: 我沒有 Mac，可以編譯 iOS 應用嗎？

A: 可以！使用 GitHub Actions 在雲端編譯，然後用 Windows 上的 Sideloadly 或 AltStore 簽名安裝。

### Q: 免費 Apple ID 可以用嗎？

A: 可以！但簽名的應用每 7 天過期，需要重新簽名。AltStore 可以自動處理。

### Q: 如何修改應用功能？

A: 編輯 `ios/MosmeCheat/ViewController.swift`，主要邏輯都在這個檔案中。

### Q: 編譯失敗怎麼辦？

A:
1. 檢查 GitHub Actions 日誌
2. 確認 Xcode 版本（需要 15.0+）
3. 在 Issues 中回報問題

## 📚 相關文件

- [iOS README](ios/README.md) - 詳細的 iOS 專案說明
- [SIGNING_GUIDE](ios/SIGNING_GUIDE.md) - 簽名與安裝指南
- [Android README](README.md) - 原 Android 專案說明

## 🎯 下一步

1. ✅ 已完成：iOS 專案創建
2. ✅ 已完成：GitHub Actions 配置
3. 🔄 待完成：推送到 GitHub 並測試編譯
4. 🔄 待完成：下載 IPA 並簽名安裝
5. 🔄 待完成：在 iPhone 上測試功能

## 💡 提示

- 第一次編譯可能需要 10 分鐘，後續會更快
- 建議使用 AltStore，它可以自動重新簽名
- 如果有 Mac，直接用 Xcode 最方便
- iOS 版本的 JavaScript 邏輯與 Android 完全相同

## ⚠️ 注意事項

- 本工具僅供個人學習使用
- 請遵守相關服務條款
- 不要分享已簽名的 IPA 給他人（會包含你的證書）
