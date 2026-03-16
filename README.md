# mosme-cheat

自動完成 [MOSME](https://www.mosme.net) 即時測評的工具。

## 📱 多平台支援

本專案提供三個版本：

| 平台 | 說明 | 快速開始 |
|------|------|----------|
| 🐍 **Python** | 使用 Playwright 自動化瀏覽器 | 見下方說明 |
| 🤖 **Android** | WebView 應用 + 自動答題按鈕 | [android/](android/) |
| 🍎 **iOS** | WKWebView 應用 + 自動答題按鈕 | [QUICKSTART_iOS.md](QUICKSTART_iOS.md) |

## 展示

<video src="demo.mp4" controls width="100%"></video>

---

## 🐍 Python 版本

### 功能

- 自動登入 MOSME（透過 IPOE 帳號）
- 展開指定課程的題庫清單
- **互動選擇試卷**：列出所有可用試卷，讓使用者選擇後再開始
- 三層自動作答策略：
  1. 從頁面 Knockout.js ViewModel 取得答案
  2. 讀取 HTML `isanswer` 屬性
  3. 對照 PDF 答案卷（依題號點選正確選項）

### 環境需求

- Python 3.13+
- [uv](https://github.com/astral-sh/uv) 套件管理器

### 安裝

```bash
uv sync
uv run playwright install chromium
```

### 設定

複製 `.env.example` 並填入帳號密碼：

```bash
cp .env.example .env
```

`.env` 內容：

```env
MOSME_ACCOUNT=你的帳號
MOSME_PASSWORD=你的密碼
```

### 使用

```bash
uv run main.py
# 或
.venv/Scripts/python main.py
```

執行後：
1. 自動登入並前往目標課程頁面
2. 展開題庫後列出所有可用試卷
3. 輸入編號選擇試卷（直接 Enter 選第一個）
4. 自動開始測驗並作答

### PDF 答案卷

將官方答案 PDF 放置於專案根目錄，預設讀取 `028003A11.pdf`。
路徑可在 `main.py` 頂部的 `PDF_PATH` 修改。

---

## 🤖 Android 版本

簡單的 WebView 應用，內建自動答題功能。

- **三連點螢幕**顯示/隱藏「自動答題」按鈕
- 支援 Knockout.js 和 isanswer 屬性兩種答題策略
- 詳見 [android/](android/) 目錄

---

## 🍎 iOS 版本

與 Android 版本功能相同的 iOS 應用。

### 快速開始

1. **使用 GitHub Actions 自動編譯**（無需 Mac）
   ```bash
   git push
   # 前往 GitHub Actions 下載 IPA
   ```

2. **或在 Mac 上用 Xcode 編譯**
   ```bash
   cd ios
   open MosmeCheat.xcodeproj
   ```

3. **簽名並安裝**
   - 詳見 [ios/SIGNING_GUIDE.md](ios/SIGNING_GUIDE.md)
   - 推薦使用 iLoader（免費）

完整說明：[QUICKSTART_iOS.md](QUICKSTART_iOS.md)

---

## 注意事項

- `.env` 含有帳號密碼，**請勿上傳至公開版本庫**（已加入 `.gitignore`）
- 本工具僅供個人練習使用
- 請遵守相關服務條款
