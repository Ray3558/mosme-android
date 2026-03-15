$ErrorActionPreference = "Stop"

$SDK_DIR = "$env:USERPROFILE\android-sdk"
$CMDTOOLS_DIR = "$SDK_DIR\cmdline-tools\latest"
$CMDTOOLS_ZIP = "$env:TEMP\cmdline-tools.zip"
$CMDTOOLS_URL = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"

Write-Host "=== MOSME Android Build Script ===" -ForegroundColor Cyan

# Step 1: Download cmdline-tools
if (-not (Test-Path "$CMDTOOLS_DIR\bin\sdkmanager.bat")) {
    Write-Host "[1/5] Downloading Android SDK Command-line Tools..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path "$SDK_DIR\cmdline-tools" | Out-Null
    Invoke-WebRequest -Uri $CMDTOOLS_URL -OutFile $CMDTOOLS_ZIP -UseBasicParsing
    Write-Host "      Extracting..."
    Expand-Archive -Path $CMDTOOLS_ZIP -DestinationPath "$SDK_DIR\cmdline-tools" -Force
    $extracted = "$SDK_DIR\cmdline-tools\cmdline-tools"
    if (Test-Path $extracted) {
        Rename-Item -Path $extracted -NewName "latest" -Force
    }
    Remove-Item $CMDTOOLS_ZIP -Force
    Write-Host "      Done." -ForegroundColor Green
} else {
    Write-Host "[1/5] cmdline-tools already exists, skipping." -ForegroundColor Green
}

# Step 2: Set env vars
Write-Host "[2/5] Setting ANDROID_HOME..." -ForegroundColor Yellow
$env:ANDROID_HOME = $SDK_DIR
$env:PATH = "$CMDTOOLS_DIR\bin;" + $env:PATH
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", $SDK_DIR, "User")
Write-Host "      ANDROID_HOME = $SDK_DIR" -ForegroundColor Green

# Step 3: Accept licenses & install packages
Write-Host "[3/5] Installing SDK packages..." -ForegroundColor Yellow
$sdkmanager = "$CMDTOOLS_DIR\bin\sdkmanager.bat"
$sdkRoot = "--sdk_root=$SDK_DIR"

"y`ny`ny`ny`ny`ny`n" | & $sdkmanager --licenses $sdkRoot 2>&1 | Out-Null

$pkg1 = "platforms;android-34"
$pkg2 = "build-tools;34.0.0"
$pkg3 = "platform-tools"

Write-Host "      Installing $pkg1 ..."
& $sdkmanager $pkg1 $sdkRoot 2>&1 | Where-Object { $_ -notmatch "^\[" } | Select-Object -Last 3

Write-Host "      Installing $pkg2 ..."
& $sdkmanager $pkg2 $sdkRoot 2>&1 | Where-Object { $_ -notmatch "^\[" } | Select-Object -Last 3

Write-Host "      Installing $pkg3 ..."
& $sdkmanager $pkg3 $sdkRoot 2>&1 | Where-Object { $_ -notmatch "^\[" } | Select-Object -Last 3

Write-Host "      SDK packages installed." -ForegroundColor Green

# Step 4: Write local.properties
Write-Host "[4/5] Writing local.properties..." -ForegroundColor Yellow
$localProps = "$PSScriptRoot\local.properties"
$sdkPath = $SDK_DIR -replace "\\", "\\"
Set-Content -Path $localProps -Value "sdk.dir=$sdkPath" -Encoding UTF8
Write-Host "      Done." -ForegroundColor Green

# Step 5: Build APK
Write-Host "[5/5] Building Debug APK..." -ForegroundColor Yellow
Set-Location $PSScriptRoot
& "$PSScriptRoot\gradlew.bat" assembleDebug

if ($LASTEXITCODE -eq 0) {
    $apk = "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apk) {
        $size = [math]::Round((Get-Item $apk).Length / 1MB, 2)
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  BUILD SUCCESS!" -ForegroundColor Green
        Write-Host "  APK: $apk" -ForegroundColor Green
        Write-Host "  Size: $size MB" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        explorer.exe /select,"$apk"
    }
} else {
    Write-Host "BUILD FAILED. Check errors above." -ForegroundColor Red
    exit 1
}
