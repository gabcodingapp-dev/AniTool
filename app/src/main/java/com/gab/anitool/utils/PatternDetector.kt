package com.gab.anitool.utils

import kotlin.math.ln

data class ObfuscatedString(
    val offset: Long,
    val raw: String,
    val type: String,
    val decoded: String,
    val confidence: Int
)

data class PatchRecommendation(
    val offset: Long,
    val type: String,
    val description: String,
    val originalBytes: String,
    val suggestedPatch: String,
    val severity: String
)

object PatternDetector {

    private const val MAX_SCAN_SIZE = 5_000_000 // 5MB max
    private const val MAX_STRINGS = 10000
    private const val MAX_RESULTS = 200

    // ======== DEOBFUSCATION DETECTION ========

    fun detectObfuscatedStrings(data: ByteArray, minLength: Int = 8): List<ObfuscatedString> {
        val results = mutableListOf<ObfuscatedString>()
        try {
            val scanData = if (data.size > MAX_SCAN_SIZE) data.copyOf(MAX_SCAN_SIZE) else data
            val strings = extractStrings(scanData, minLength)

            for ((offset, str) in strings) {
                if (results.size >= MAX_RESULTS) break

                // Base64 detection
                if (str.length >= 10 && str.matches(Regex("^[A-Za-z0-9+/]+=*\$"))) {
                    try {
                        val decoded = android.util.Base64.decode(str, android.util.Base64.DEFAULT).toString(Charsets.UTF_8)
                        if (decoded.any { it.code in 0x20..0x7E }) {
                            results.add(ObfuscatedString(offset, str, "Base64", decoded, 85))
                            continue
                        }
                    } catch (_: Exception) {}
                }

                // Hex string detection
                if (str.length >= 16 && str.matches(Regex("^[0-9A-Fa-f]+\$")) && str.length % 2 == 0) {
                    try {
                        val decoded = str.chunked(2).mapNotNull { it.toIntOrNull(16)?.toChar() }.joinToString("")
                        if (decoded.any { it.code in 0x20..0x7E }) {
                            results.add(ObfuscatedString(offset, str, "Hex", decoded, 80))
                            continue
                        }
                    } catch (_: Exception) {}
                }

                // Unicode escape
                if (str.contains("\\u") && str.matches(Regex(".*\\\\u[0-9a-fA-F]{4}.*"))) {
                    try {
                        val decoded = str.replace(Regex("\\\\u([0-9a-fA-F]{4})")) { m ->
                            m.groupValues[1].toInt(16).toChar().toString()
                        }
                        results.add(ObfuscatedString(offset, str, "Unicode", decoded, 90))
                        continue
                    } catch (_: Exception) {}
                }

                // URL encode
                if (str.contains("%") && str.matches(Regex(".*%[0-9A-Fa-f]{2}.*"))) {
                    try {
                        val decoded = java.net.URLDecoder.decode(str, "UTF-8")
                        if (decoded != str) {
                            results.add(ObfuscatedString(offset, str, "URL Encode", decoded, 85))
                            continue
                        }
                    } catch (_: Exception) {}
                }

                // XOR detection
                val entropy = calcEntropy(str.toByteArray())
                if (entropy > 5.0 && str.length >= 8) {
                    val bestKey = bruteForceXOR(str.toByteArray())
                    if (bestKey >= 0) {
                        val decoded = str.toByteArray().map { (it.toInt() xor bestKey).toChar() }.joinToString("")
                        if (decoded.any { it.code in 0x20..0x7E }) {
                            results.add(ObfuscatedString(offset, str, "XOR (key=0x${"%02X".format(bestKey)})", decoded, 70))
                            continue
                        }
                    }
                }

                // Shell escape
                if (str.contains("\\x") && str.matches(Regex(".*\\\\x[0-9a-fA-F]{2}.*"))) {
                    try {
                        val decoded = str.replace(Regex("\\\\x([0-9a-fA-F]{2})")) { m ->
                            m.groupValues[1].toInt(16).toChar().toString()
                        }
                        results.add(ObfuscatedString(offset, str, "Shell Escape", decoded, 80))
                        continue
                    } catch (_: Exception) {}
                }

                // Reverse
                val reversed = str.reversed()
                if (reversed != str && isReadable(reversed)) {
                    results.add(ObfuscatedString(offset, str, "Reversed", reversed, 60))
                    continue
                }

                // Obfuscated URL/link
                val linkPatterns = listOf(
                    Regex("https?://[a-zA-Z0-9._/-]+"),
                    Regex("[a-zA-Z0-9]+\\.[a-zA-Z]{2,}/[a-zA-Z0-9._/-]+")
                )
                for (pat in linkPatterns) {
                    val match = pat.find(str)
                    if (match != null) {
                        results.add(ObfuscatedString(offset, str, "Link/URL", match.value, 75))
                        break
                    }
                }

                // Auth/login
                val authKw = listOf("login", "password", "username", "token", "session",
                    "verify", "auth", "credential", "otp", "api_key", "secret", "Bearer", "Basic")
                for (kw in authKw) {
                    if (str.contains(kw, ignoreCase = true)) {
                        results.add(ObfuscatedString(offset, str, "Auth/Login", str, 90))
                        break
                    }
                }
            }
        } catch (_: Exception) {}
        return results.sortedByDescending { it.confidence }
    }

    // ======== AUTO DETECT PATCH PATTERNS ========

    fun detectPatchPatterns(data: ByteArray): List<PatchRecommendation> {
        val recommendations = mutableListOf<PatchRecommendation>()
        try {
            val scanData = if (data.size > MAX_SCAN_SIZE) data.copyOf(MAX_SCAN_SIZE) else data
            val strings = extractStrings(scanData, 4).take(MAX_STRINGS)

            val loginStrings = listOf("wrong", "invalid", "login failed", "error", "unauthorized", "denied", "expired",
                "password", "username", "credential", "auth", "token", "session",
                "verify", "verification", "confirm", "otp", "code", "captcha",
                "block", "banned", "restricted", "not found", "failed", "incorrect")

            for ((offset, str) in strings) {
                if (recommendations.size >= MAX_RESULTS) break
                val lower = str.lowercase()

                // Login bypass
                if (loginStrings.any { lower.contains(it) }) {
                    findBranchNearby(scanData, offset.toInt(), str.length, "Login Bypass",
                        "Conditional branch near \"$str\"", recommendations)
                }

                // License/strcmp
                if (lower.contains("license") || lower.contains("licence") || lower.contains("serial") || lower.contains("activation")) {
                    findCallNearby(scanData, offset.toInt(), str.length, "License Check",
                        "Function call near \"$str\" — likely strcmp/memcmp", recommendations)
                }

                // Time check
                if (lower.contains("time") || lower.contains("expire") || lower.contains("date") || lower.contains("clock")) {
                    findCallNearby(scanData, offset.toInt(), str.length, "Time Check",
                        "Function call near \"$str\" — likely time()/gettimeofday()", recommendations)
                }

                // Anti-debug
                if (lower.contains("tracerpid") || lower.contains("ptrace") || lower.contains("debug") || lower.contains("frida")) {
                    findCallNearby(scanData, offset.toInt(), str.length, "Anti-Debug",
                        "Function call near \"$str\" — likely ptrace()/TracerPid check", recommendations)
                }
            }
        } catch (_: Exception) {}
        return recommendations.sortedByDescending { if (it.severity == "high") 3 else if (it.severity == "medium") 2 else 1 }
    }

    private fun findBranchNearby(data: ByteArray, offset: Int, strLen: Int, type: String, desc: String, out: MutableList<PatchRecommendation>) {
        val searchStart = maxOf(0, offset - 200)
        val searchEnd = minOf(data.size, offset + strLen + 200)
        for (i in searchStart until searchEnd step 4) {
            if (i + 4 > data.size) return
            try {
                val insn = (data[i].toInt() and 0xFF) or ((data[i + 1].toInt() and 0xFF) shl 8) or
                        ((data[i + 2].toInt() and 0xFF) shl 16) or ((data[i + 3].toInt() and 0xFF) shl 24)
                val top = (insn shr 24) and 0xFF
                if (top == 0x54 || top == 0x34 || top == 0x35) {
                    out.add(PatchRecommendation(i.toLong(), type, desc, "%02X %02X %02X %02X".format(data[i], data[i+1], data[i+2], data[i+3]), "NOP or unconditional B", "high"))
                    return
                }
            } catch (_: Exception) { return }
        }
    }

    private fun findCallNearby(data: ByteArray, offset: Int, strLen: Int, type: String, desc: String, out: MutableList<PatchRecommendation>) {
        val searchStart = maxOf(0, offset - 100)
        val searchEnd = minOf(data.size, offset + strLen + 100)
        for (i in searchStart until searchEnd step 4) {
            if (i + 4 > data.size) return
            try {
                val insn = (data[i].toInt() and 0xFF) or ((data[i + 1].toInt() and 0xFF) shl 8) or
                        ((data[i + 2].toInt() and 0xFF) shl 16) or ((data[i + 3].toInt() and 0xFF) shl 24)
                val top = (insn shr 26) and 0x3F
                if (top == 0x25) {
                    val sev = when(type) { "License Check", "Anti-Debug" -> "high"; else -> "medium" }
                    out.add(PatchRecommendation(i.toLong(), type, desc, "%02X %02X %02X %02X".format(data[i], data[i+1], data[i+2], data[i+3]),
                        if (type == "Time Check") "NOP (skip time check)" else "MOV W0, #0 + RET (return 0)", sev))
                    return
                }
            } catch (_: Exception) { return }
        }
    }

    // ======== HELPERS ========

    private fun extractStrings(data: ByteArray, minLen: Int): List<Pair<Long, String>> {
        val results = mutableListOf<Pair<Long, String>>()
        val sb = StringBuilder()
        var start = 0L
        for (i in data.indices) {
            val b = data[i].toInt() and 0xFF
            if (b in 0x20..0x7E) {
                if (sb.isEmpty()) start = i.toLong()
                sb.append(b.toChar())
            } else {
                if (sb.length >= minLen && results.size < MAX_STRINGS) results.add(start to sb.toString())
                sb.clear()
            }
        }
        if (sb.length >= minLen && results.size < MAX_STRINGS) results.add(start to sb.toString())
        return results
    }

    private fun calcEntropy(data: ByteArray): Double {
        if (data.isEmpty()) return 0.0
        val freq = IntArray(256)
        for (b in data) freq[b.toInt() and 0xFF]++
        var entropy = 0.0
        for (f in freq) {
            if (f > 0) {
                val p = f.toDouble() / data.size
                entropy -= p * ln(p) / ln(2.0)
            }
        }
        return entropy
    }

    private fun bruteForceXOR(data: ByteArray): Int {
        var bestKey = -1
        var bestScore = 0
        for (k in 0..255) {
            var score = 0
            for (b in data) {
                val c = (b.toInt() xor k) and 0xFF
                if (c in 0x20..0x7E || c == 0x0A || c == 0x0D) score++
            }
            if (score > bestScore && score > data.size * 0.7) {
                bestScore = score
                bestKey = k
            }
        }
        return bestKey
    }

    private fun isReadable(s: String) = s.count { it.code in 0x20..0x7E } > s.length * 0.7
}
