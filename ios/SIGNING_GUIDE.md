# iOS 簽名與安裝指南

GitHub Actions 編譯出的 IPA 是**未簽名**版本，無法直接安裝到 iPhone。以下是幾種簽名和安裝方法。

## 方法 1: 使用 AltStore（推薦，免費）

AltStore 可以使用你的免費 Apple ID 簽名並安裝應用。

### 步驟

1. **安裝 AltStore**
   - 前往 [altstore.io](https://altstore.io/) 下載
   - 在電腦上安裝 AltServer（Windows/Mac）

2. **連接 iPhone**
   - 用 USB 連接 iPhone 到電腦
   - 確保 iPhone 和電腦在同一 Wi-Fi 網路

3. **安裝 AltStore 到 iPhone**
   - 執行 AltServer（系統托盤圖示）
   - 點擊圖示 > Install AltStore > 選擇你的 iPhone
   - 輸入 Apple ID 和密碼

4. **安裝 IPA**
   - 從 GitHub Actions 下載 `MosmeCheat.ipa`
   - 傳送到 iPhone（AirDrop/iCloud/其他方式）
   - 在 iPhone 上用 AltStore 開啟 IPA 檔案
   - 等待簽名和安裝完成

5. **信任開發者**
   - 設定 > 一般 > VPN與裝置管理
   - 找到你的 Apple ID
   - 點擊「信任」

### 注意事項

- 免費 Apple ID 簽名的應用**每 7 天過期**，需重新簽名
- AltStore 可以在背景自動重新簽名（需保持 AltServer 執行）
- 最多同時安裝 3 個應用

## 方法 2: 使用 Sideloadly（Windows/Mac）

Sideloadly 是另一個流行的簽名工具。

### 步驟

1. 下載 [Sideloadly](https://sideloadly.io/)
2. 連接 iPhone 到電腦
3. 開啟 Sideloadly
4. 拖曳 IPA 檔案到 Sideloadly
5. 輸入 Apple ID
6. 點擊 Start 開始簽名和安裝

## 方法 3: 使用 Xcode（Mac 專用）

如果你有 Mac，可以直接用 Xcode 簽名。

### 步驟

1. 解壓縮 IPA
   ```bash
   unzip MosmeCheat.ipa
   ```

2. 用 Xcode 開啟專案
   ```bash
   cd ios
   open MosmeCheat.xcodeproj
   ```

3. 連接 iPhone

4. 選擇 Target > Signing & Capabilities
   - Team: 選擇你的 Apple ID
   - Bundle Identifier: 改成唯一值（如 `com.yourname.mosmecheat`）

5. 點擊 Run (⌘R) 直接安裝到 iPhone

## 方法 4: 使用 iOS App Signer（Mac 專用）

### 步驟

1. 下載 [iOS App Signer](https://dantheman827.github.io/ios-app-signer/)
2. 開啟應用程式
3. 選擇 IPA 檔案
4. 選擇簽名證書（需要先在 Xcode 中登入 Apple ID）
5. 選擇 Provisioning Profile
6. 點擊 Start 生成已簽名的 IPA
7. 用 Xcode > Window > Devices and Simulators 安裝到 iPhone

## 方法 5: 使用付費開發者帳號

如果你有 Apple Developer Program 帳號（$99/年），可以：

1. 在 Xcode 中使用你的 Team ID
2. 簽名的應用有效期 1 年
3. 可以透過 TestFlight 分發給其他人

### 配置 GitHub Actions 自動簽名

在 GitHub Repository 設定 Secrets：

- `IOS_CERTIFICATE_BASE64`: 證書的 base64 編碼
- `CERTIFICATE_PASSWORD`: 證書密碼
- `PROVISIONING_PROFILE_BASE64`: Provisioning Profile 的 base64 編碼
- `TEAM_ID`: 你的 Team ID

然後在 Actions 頁面手動觸發 workflow，選擇 `sign_ipa: true`。

## 常見問題

### Q: 為什麼需要簽名？

A: iOS 的安全機制要求所有應用都必須經過簽名才能安裝。簽名證明應用來自可信來源。

### Q: 免費 Apple ID 和付費開發者帳號有什麼差別？

A:
- **免費**: 每 7 天過期，最多 3 個應用，只能安裝到自己的裝置
- **付費**: 1 年有效期，無應用數量限制，可以分發給其他人

### Q: 為什麼 GitHub Actions 不直接生成已簽名的 IPA？

A: 簽名需要你的 Apple ID 證書和 Provisioning Profile，這些是私密資訊。你可以選擇將它們加入 GitHub Secrets 來啟用自動簽名。

### Q: 安裝後顯示「無法驗證應用程式」

A: 前往 設定 > 一般 > VPN與裝置管理，找到開發者證書並點擊「信任」。

### Q: AltStore 顯示「Maximum number of apps reached」

A: 免費 Apple ID 最多同時安裝 3 個應用。刪除一些應用後再試。

## 推薦方案

| 使用情境 | 推薦方法 | 優點 |
|---------|---------|------|
| 個人使用（Windows） | AltStore 或 Sideloadly | 免費，簡單 |
| 個人使用（Mac） | Xcode | 最直接 |
| 長期使用 | 付費開發者帳號 | 1 年有效期 |
| 分發給他人 | TestFlight | 官方方案 |

## 安全提醒

- 只從可信來源下載 IPA 檔案
- 不要在簽名工具中輸入 Apple ID 密碼，除非是官方工具（Xcode、AltStore）
- 建議使用應用專用密碼（App-Specific Password）而非主密碼
