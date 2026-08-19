# ⚡ OprekTool v2.0 — Android Reverse Engineering Toolkit

<p align="center">
  <img src="https://img.shields.io/badge/Android-26%2B-brightgreen" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/Version-2.0-blue" alt="Version"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License"/>
  <img src="https://img.shields.io/badge/Tools-34-orange" alt="Tools"/>
  <img src="https://img.shields.io/badge/FPS-120-red" alt="120fps"/>
</p>

<p align="center">
  <b>34 tools, fully offline, native C++ backend, 120fps smooth UI</b>
</p>

---

## 📥 Download

**[⬇️ Download APK (Latest)](https://github.com/opanx/oprek-tool/actions)**

> Klik action terbaru → download **OprekTool-debug** atau **OprekTool-release**

---

## 📱 Owner & Contact

| | |
|---|---|
| 👤 **Owner** | **@Gk_Gene** |
| 📢 **Channel 1** | **[t.me/kembungjir](https://t.me/kembungjir)** (non-official) |
| 📢 **Channel 2** | **[t.me/lazy_fat_catt](https://t.me/lazy_fat_catt)** |
| 💬 **Telegram** | **[t.me/Gk_Gene](https://t.me/Gk_Gene)** |

---

## 🛠️ Features (34 Tools)

### Binary Analysis
| Tool | Description | Auto |
|------|-------------|------|
| 🔍 **Hex Viewer** | View & edit raw bytes, hex+ASCII | ✅ |
| 📝 **Strings** | Extract text, **real-time search + highlight** | ✅ |
| 📦 **Disassembler** | ARM32/ARM64/x86 (Capstone bridge) | ✅ Auto-detect arch |
| 📋 **ELF Info** | Parse headers, sections, symbols | ✅ |
| 📱 **APK Info** | Analyze APK structure, DEX, native libs | ✅ |
| 🤖 **Android Tools** | DEX header, class dump | ✅ |

### Patching & Deobfuscation
| Tool | Description | Auto |
|------|-------------|------|
| 🔧 **Patch Editor** | Single + bulk binary patch | ✅ |
| 🔧 **Adv. Patch** | NOP/RET/String patch + **auto-detect** | ✅ Auto-scan |
| 🔓 **Deobfuscate** | Decode Base64/Hex/XOR/URL/Unicode | ✅ Auto-scan |
| 🔒 **Obfuscate** | Encode Base64/Hex/XOR/ROT13 | ✅ |
| 📜 **Shell Script** | Parse .sh, extract commands/URLs/functions | ✅ |
| 🔧 **Shell Patcher** | Edit URLs/keys/commands in .sh | ✅ |

### Hooking & Anti-Debug
| Tool | Description | Auto |
|------|-------------|------|
| 🪝 **Frida Hook** | Generate Frida hook scripts | ✅ |
| 🪝 **Hook Generator** | LD_PRELOAD + ARM64 trampoline | ✅ |
| 🛡️ **Anti-Debug** | Detect TracerPid, ptrace, debugger apps | ✅ |

### Tools & Utilities
| Tool | Description | Auto |
|------|-------------|------|
| 🔐 **Hash Calculator** | MD5/SHA-1/SHA-256/SHA-512/CRC32 | ✅ Auto-calc |
| 🔑 **Key Generator** | Random keys, custom charset | ✅ |
| 🔄 **Base64/Hex** | Encode/decode strings | ✅ Auto-detect |
| ⚖️ **Diff Tool** | Binary file comparison | ✅ Auto-compare |
| 📋 **Manifest Reader** | APK permissions & entries | ✅ |
| 📌 **Bookmarks** | Save important offsets | ✅ Auto-scan |
| 📤 **Export Report** | Save analysis as TXT | ✅ |
| 🕐 **Recent Files** | History with SharedPrefs | ✅ |
| 📊 **Memory Analyzer** | Entropy, packer detection | ✅ |
| 📋 **Logcat** | Capture & filter Android logs | ✅ Auto-refresh |
| 📦 **ELF Full Header** | Complete ELF header display | ✅ |
| 🧩 **Packer Detection** | UPX/Themida/OLLMV detection | ✅ |
| 🧠 **Memory Dump** | Analyze raw memory dumps | ✅ |
| 🌙 **Lua Analyzer** | Parse .lua scripts, detect obfuscation | ✅ |
| 📦 **Pak Archive** | Analyze .pak/.paks/.unity3d | ✅ |
| 💻 **Terminal** | Shell emulator + built-in commands | ✅ |
| 📋 **Hex Copy** | Export bytes as C/Python/hex | ✅ |
| 🔍 **ELF Symbols** | Symbol table + dynamic sections | ✅ |
| 🔎 **Search** | Find byte patterns in binary | ✅ |

---

## 🎮 Performance

- **120fps** — smooth scrolling on high refresh rate displays
- **Native C++** — ELF/PE/DEX parsers, XOR, patching via JNI
- **Coroutines** — all heavy processing on background threads
- **5MB scan limit** — prevents OOM on large files

---

## 🔧 Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + StateFlow |
| Native | C++ (ELF/PE/DEX parsers, XOR, patching) |
| Build | Gradle 8.11 + AGP 8.7.3 |
| CI/CD | GitHub Actions |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## 📂 Project Structure

```
oprek-tool/
├── app/src/main/
│   ├── java/com/oprek/tool/
│   │   ├── MainActivity.kt          # Entry point (120fps)
│   │   ├── MainViewModel.kt         # State management
│   │   ├── Navigation.kt            # 34 screen routes
│   │   ├── core/
│   │   │   ├── FileAnalyzer.kt      # Core analysis engine
│   │   │   ├── NativeLib.kt         # JNI bridge
│   │   │   └── FileUtils.kt         # File I/O
│   │   ├── utils/
│   │   │   ├── PatternDetector.kt   # Auto-detect patterns
│   │   │   └── ShellScriptParser.kt # Shell script analysis
│   │   └── ui/screens/              # 27+ screen files
│   ├── cpp/                         # Native C/C++
│   │   ├── CMakeLists.txt
│   │   ├── jni_bridge.cpp
│   │   ├── elf_parser.c
│   │   ├── pe_parser.c
│   │   ├── dex_parser.c
│   │   ├── obfuscate.c
│   │   └── patch_utils.c
│   └── res/
├── .github/workflows/build.yml      # CI/CD
└── README.md
```

---

## 🏗️ Build

```bash
git clone https://github.com/opanx/oprek-tool.git
cd oprek-tool
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

---

## ©️ Copyright

**© Panxcz & Freebuff**

Built with ❤️ by [opanx](https://github.com/opanx)

---

## ⚠️ Disclaimer

For educational and security research purposes only. Use responsibly.
