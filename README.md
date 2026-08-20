<div align="center">

  <img src="assets/images/logo.png" width="120" height="120" style="border-radius: 24px;" onerror="this.style.display='none'">

  # AniTool
  ### Premium Toolkit for Creators — by Gab

  **34+ Tools • 120fps • Offline • Native C++ • For Android 8.0+**

  <p>
    <a href="https://github.com/gabcodingapp-dev/AniTool/releases/latest"><img src="https://img.shields.io/github/v/release/gabcodingapp-dev/AniTool?label=Latest&color=7C3AED&style=for-the-badge"></a>
    <a href="https://github.com/gabcodingapp-dev/AniTool/releases"><img src="https://img.shields.io/github/downloads/gabcodingapp-dev/AniTool/total?color=00D1B2&style=for-the-badge&label=Downloads"></a>
    <a href="https://github.com/gabcodingapp-dev/AniTool/stargazers"><img src="https://img.shields.io/github/stars/gabcodingapp-dev/AniTool?color=FFB800&style=for-the-badge"></a>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white">
    <img src="https://img.shields.io/badge/Compose-Material3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white">
    <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white">
    <img src="https://img.shields.io/badge/License-MIT-00C853?style=flat-square">
  </p>

  <p>
    <a href="#download"><b>⬇️ Download</b></a> •
    <a href="#features"><b>✨ Features</b></a> •
    <a href="#brainstorm"><b>💡 New Ideas</b></a> •
    <a href="#build"><b>🛠️ Build</b></a>
  </p>

</div>

---

> [!TIP]
> **Fully offline. No ads. No tracking. Your files never leave your device.**

---

## ✨ Why AniTool?

| **Before** | **With AniTool** |
|---|---|
| 7 different APKs for hex / patch / deobfuscate | **One app, 34 tools** — all offline |
| Laggy viewers, crashes on large files | **120fps** + native C++ + 5MB safe limit |
| Manual search for strings/patterns | **Auto-scan** + real-time highlight |
| Copy-paste Frida scripts | **One-tap Hook Generator** (Frida + LD_PRELOAD + ARM64 trampoline) |

---

## 🎯 <a id="features"></a>Features — 34 Tools, Organized

### 🔍 Binary Analysis (Auto-detect)
| Tool | What it does |
|---|---|
| **Hex Viewer** | Hex + ASCII, edit bytes live |
| **Strings** | Real-time search + highlight |
| **Disassembler** | ARM32/ARM64/x86 (Capstone) — auto arch |
| **ELF Info** | Headers, sections, symbols |
| **APK Info** | DEX, libs, manifest |
| **ELF Full Header** | Complete header dump |

### 🛡️ Patching & Deobfuscation (Auto-scan)
| Tool | What it does |
|---|---|
| **Patch Editor** | Single + bulk binary patch |
| **Adv. Patch** | NOP/RET/String — auto-scan vulnerable spots |
| **Deobfuscate** | Base64 / Hex / XOR / URL / Unicode — auto-scan |
| **Obfuscate** | Encode for testing |
| **Shell Script** | Parse `.sh` → commands / URLs / functions |
| **Shell Patcher** | Edit URLs/keys in `.sh` |

### 🪝 Hooking & Anti-Debug
| Tool | What it does |
|---|---|
| **Frida Hook** | Generate Frida JS |
| **Hook Generator** | LD_PRELOAD + ARM64 trampoline |
| **Anti-Debug** | TracerPid, ptrace, debugger apps — auto-detect |

### 🧰 Utils (Auto)
| Tool |
|---|
| Hash (MD5/SHA-*/CRC32) • Key Generator • Base64/Hex • Diff • Manifest Reader • Bookmarks • Export Report • Recent Files • Memory Analyzer (entropy) • Logcat (auto-refresh) • ELF Symbols • Search • Memory Dump • Lua Analyzer • Pak Archive (.pak/.unity3d) • Terminal (shell + built-ins) • Hex Copy (C/Python) |

---

## 💡 <a id="brainstorm"></a>Brainstorm — What's Next for AniTool

> **Inspired by MangananoX's polish — now for RE creators**

### 🚀 High-Value (Build Next)
1. **AI Script Explainer** — paste obfuscated `.sh`/`.lua` → AI explains what it does, line-by-line (offline LLM via `llama.cpp` on-device)
2. **Visual Patch Diff** — before/after hex with colors, like Git diff but for binaries
3. **APK Diff Pro** — compare two APKs → show added/removed permissions, native libs, DEX diff, resources
4. **Frida Script Marketplace (Local)** — save, tag, and share your hooks — no internet needed

### 🔥 Power User
- **Collaborative Patch Share** — export patch as `.anipatch` (JSON + base64) → import on another device via QR
- **Cloud Backup (Optional)** — encrypt bookmarks/patches → backup to your own GitHub Gist
- **Hook Lab** — live test Frida hooks on a connected `adb` device from the app (over Wi-Fi)
- **Entropy Visualizer Pro** — interactive graph + packer confidence score (UPX/Themida/OLLVM)

### ✨ Quality of Life
- **Recent Files Timeline** — Today / Yesterday / This Week, with preview thumbnails
- **Bookmarks 2.0** — tags + search + export to CSV
- **Terminal Pro** — history, autocomplete for `hexdump`, `strings`, `patch`
- **Dark Hacker → Premium Themes** — switch between `OLED Black`, `Hacker Green`, `Manga Purple` (like MangananoX)

> **Want a feature?** [Open an issue](https://github.com/gabcodingapp-dev/AniTool/issues) — Gab builds what the community asks.

---

## ⬇️ <a id="download"></a>Download

| Variant | For | Link |
|---|---|---|
| **Universal APK** | All devices | [⬇️ Latest Release](https://github.com/gabcodingapp-dev/AniTool/releases/latest) |
| **ARM64** | Modern phones (2016+) | Same release → `*-arm64-v8a.apk` |
| **Debug APK** | Testers | Every push → `Actions` → `Artifacts` → `AniTool-debug` |

> **Install:** Allow “Install unknown apps” → open APK → grant `MANAGE_EXTERNAL_STORAGE` for `/sdcard` analysis.

---

## 🏗️ <a id="build"></a>Build From Source

**Need:** JDK 17 + Android SDK + NDK

```bash
git clone https://github.com/gabcodingapp-dev/AniTool.git
cd AniTool
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

**Native C++:** `app/src/main/cpp/` auto-builds via CMake `3.22.1` (downloaded by GitHub Actions if missing).

**GitHub Actions:** Every push → `Build APK` → `AniTool-debug` + `AniTool-release` in Artifacts (30 days). Tag `v*` → auto Release.

---

## 🎨 Design — Like MangananoX, For Hackers

- **120fps** — `preferredDisplayModeId` max refresh
- **Material 3 + Dark Hacker** — `DarkBg #0D1117`, `AccentGreen #3FB950`, `AccentPurple #BC8CFF` — premium, OLED, MangananoX-inspired polish
- **Native C++** — ELF/PE/DEX parsers via JNI — zero lag
- **Compose + StateFlow + MVVM** — smooth, testable

---

## 📄 License & Credits

MIT © **Gab** (`gabcodingapp-dev`)

- Original: `opanx/oprek-tool` by **Panxcz & Freebuff** — reimagined as **AniTool** with MangananoX design language
- Forked with ❤️, rebuilt for creators

> **Disclaimer:** For educational & security research only. Use responsibly.

---

<div align="center">

**Made with 💚 for the community — by Gab**

[⬆️ Back to Top](#anitool)

</div>
