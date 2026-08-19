# ⚡ OprekTool — Reverse Engineering Toolkit for Android

A full-featured Android app for binary reverse engineering. Analyze `.sh`, `.apk`, `.so`, `.elf`, `.bin` files on your phone.

## 🛠️ Features

| Feature | Description |
|---------|-------------|
| 🔍 **Hex Viewer** | View & edit raw bytes with ASCII sidebar |
| 📝 **String Extractor** | Extract readable strings with offset + filtering |
| 📦 **ELF Analyzer** | Parse ELF headers, sections, architecture info |
| 📱 **APK Analyzer** | Inspect APK entries, DEX detection, native libs |
| 🔧 **Patch Editor** | Single + bulk binary patching with export |
| 🔐 **File Info** | MD5, SHA-256, magic bytes, metadata |
| 💻 **Terminal** | Built-in shell command executor |
| 🔎 **Byte Search** | Find hex patterns in binary files |

## 🏗️ Build

### Auto Build (GitHub Actions)
Push to `main` → APK auto-builds → Download from Actions artifacts.

### Manual Build
```bash
git clone https://github.com/YOUR_USER/oprek-tool.git
cd oprek-tool
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## 📦 Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Architecture:** MVVM + StateFlow
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Build:** Gradle 8.11 + AGP 8.7.3

## 📂 Project Structure

```
app/src/main/java/com/oprek/tool/
├── MainActivity.kt          # Entry point
├── MainViewModel.kt         # State management + analysis logic
├── Navigation.kt            # Navigation graph
├── core/
│   ├── FileAnalyzer.kt      # Core analysis engine (ELF, APK, strings, hex)
│   └── FileUtils.kt         # File I/O utilities
└── ui/
    ├── theme/Theme.kt       # Dark hacker theme
    └── screens/
        ├── HomeScreen.kt         # Main menu + file picker
        ├── HexViewerScreen.kt    # Hex dump + edit
        ├── StringExtractorScreen.kt
        ├── ElfAnalyzerScreen.kt
        ├── ApkAnalyzerScreen.kk
        ├── PatchEditorScreen.kt  # Single + bulk patch
        ├── FileInfoScreen.kt     # Hashes + metadata
        ├── TerminalScreen.kt     # Shell executor
        └── SearchScreen.kt       # Pattern search
```

## 🎨 Theme

Dark hacker theme with green/cyan/purple accents — designed for extended reverse engineering sessions.

## ⚠️ Disclaimer

For educational and security research purposes only. Use responsibly.
