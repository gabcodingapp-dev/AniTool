package com.anitools.gab.data

data class ChangelogEntry(
    val version: String,
    val date: String,
    val title: String,
    val changes: List<String>,
    val isNew: Boolean = false
)

object ChangelogData {
    val entries = listOf(
        ChangelogEntry(
            version = "2.1.0",
            date = "2026-08-20",
            title = "AniTool by Gab — Premium Upgrade",
            isNew = true,
            changes = listOf(
                "✨ New package com.anitools.gab by Gab",
                "🎨 Premium theme — DarkBg #0F0F23 + Violet #7C3AED (MangananoX inspired)",
                "🖼️ New adaptive app icon (eye + wrench)",
                "📚 Changelog menu — see what's new",
                "🔍 Enhanced Search — real-time highlight",
                "🛡️ Adv. Patch — auto-scan vulnerable spots",
                "📊 Memory Analyzer — entropy + packer detection",
                "🔧 4 new tools: APK Diff, Visual Patch Diff, Hook Marketplace, AI Explainer (coming)"
            )
        ),
        ChangelogEntry(
            version = "2.0.0",
            date = "2026-08-19",
            title = "OprekTool Reborn",
            changes = listOf(
                "34 tools, fully offline, native C++ backend",
                "120fps smooth UI with Compose + Material 3",
                "ARM32/ARM64/x86 disassembler (Capstone)",
                "Frida Hook + LD_PRELOAD generator",
                "ELF/APK analysis, Deobfuscate, Patch Editor"
            )
        ),
        ChangelogEntry(
            version = "1.0.0",
            date = "2025-12-01",
            title = "Initial Release",
            changes = listOf(
                "Hex Viewer + Strings extractor",
                "Hash Calculator + Key Generator",
                "Terminal + Logcat"
            )
        )
    )
}
