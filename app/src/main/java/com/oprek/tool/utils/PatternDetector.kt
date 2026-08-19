package com.oprek.tool.utils

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

data class ObfuscatedString(
    val offset: Long,
    val raw: String,
    val type: String,
    val decoded: String,
    val confidence: Int // 0-100
)

data class PatchRecommendation(
    val offset: Long,
    val type: String,
    val description: String,
    val originalBytes: String,
    val suggestedPatch: String,
    val severity: String // "high", "medium", "low"
)

object PatternDetector {

    // ======== DEOBFUSCATION DETECTION ========

    fun detectObfuscatedStrings(data: ByteArray, minLength: Int = 8): List<ObfuscatedString> {
        val results = mutableListOf<ObfuscatedString>()
        val strings = extractStrings(data, minLength)

        for ((offset, str) in strings) {
            // Base64 detection
            if (str.length >= 10 && str.matches(Regex("^[A-Za-z0-9+/]+=*\$"))) {
                try {
                    val decoded = android.util.Base64.decode(str, android.util.Base64.DEFAULT).toString(Charsets.UTF_8)
                    if (decoded.any { it.code in 0x20..0x7E }) {
                        results.add(ObfuscatedString(offset, str, "Base64", decoded, 85))
                    }
                } catch (_: Exception) {}
            }

            // Hex string detection
            if (str.length >= 16 && str.matches(Regex("^[0-9A-Fa-f]+\$")) && str.length % 2 == 0) {
                val decoded = str.chunked(2).mapNotNull { it.toIntOrNull(16)?.toChar() }.joinToString("")
                if (decoded.any { it.code in 0x20..0x7E }) {
                    results.add(ObfuscatedString(offset, str, "Hex", decoded, 80))
                }
            }

            // Unicode escape detection
            if (str.contains("\\u") && str.matches(Regex(".*\\\\u[0-9a-fA-F]{4}.*"))) {
                val decoded = str.replace(Regex("\\\\u([0-9a-fA-F]{4})")) { match ->
                    match.groupValues[1].toInt(16).toChar().toString()
                }
                results.add(ObfuscatedString(offset, str, "Unicode", decoded, 90))
            }

            // URL encode detection
            if (str.contains("%") && str.matches(Regex(".*%[0-9A-Fa-f]{2}.*"))) {
                try {
                    val decoded = java.net.URLDecoder.decode(str, "UTF-8")
                    if (decoded != str) {
                        results.add(ObfuscatedString(offset, str, "URL Encode", decoded, 85))
                    }
                } catch (_: Exception) {}
            }

            // XOR detection (high entropy + try brute force)
            val entropy = calcEntropy(str.toByteArray())
            if (entropy > 5.0 && str.length >= 8) {
                val bestKey = bruteForceXOR(str.toByteArray())
                if (bestKey >= 0) {
                    val decoded = str.toByteArray().map { (it.toInt() xor bestKey).toChar() }.joinToString("")
                    if (decoded.any { it.code in 0x20..0x7E }) {
                        results.add(ObfuscatedString(offset, str, "XOR (key=0x${"%02X".format(bestKey)})", decoded, 70))
                    }
                }
            }

            // Shell escape detection
            if (str.contains("\\x") && str.matches(Regex(".*\\\\x[0-9a-fA-F]{2}.*"))) {
                val decoded = str.replace(Regex("\\\\x([0-9a-fA-F]{2})")) { match ->
                    match.groupValues[1].toInt(16).toChar().toString()
                }
                results.add(ObfuscatedString(offset, str, "Shell Escape", decoded, 80))
            }

            // Reverse detection (common reversed words)
            val reversed = str.reversed()
            if (reversed != str && isReadable(reversed)) {
                results.add(ObfuscatedString(offset, str, "Reversed", reversed, 60))
            }
        }

        return results.sortedByDescending { it.confidence }
    }

    // ======== AUTO DETECT PATCH PATTERNS ========

    fun detectPatchPatterns(data: ByteArray): List<PatchRecommendation> {
        val recommendations = mutableListOf<PatchRecommendation>()
        val strings = extractStrings(data, 4)

        // 1. Login bypass detection
        val loginStrings = listOf("wrong", "invalid", "login failed", "error", "unauthorized", "denied", "expired")
        for ((offset, str) in strings) {
            val lower = str.lowercase()
            if (loginStrings.any { lower.contains(it) }) {
                // Search for conditional branch nearby (within 200 bytes before/after)
                val searchStart = maxOf(0, offset.toInt() - 200)
                val searchEnd = minOf(data.size, offset.toInt() + str.length + 200)
                for (i in searchStart until searchEnd step 4) {
                    if (i + 4 <= data.size) {
                        val insn = data[i].toInt() and 0xFF or ((data[i + 1].toInt() and 0xFF) shl 8) or
                                ((data[i + 2].toInt() and 0xFF) shl 16) or ((data[i + 3].toInt() and 0xFF) shl 24)
                        // ARM64: B.cond = 0x54, CBZ/CBNZ = 0x34/0x35
                        val top = (insn shr 24) and 0xFF
                        if (top == 0x54 || top == 0x34 || top == 0x35) {
                            recommendations.add(PatchRecommendation(
                                i.toLong(), "Login Bypass",
                                "Conditional branch near \"$str\" — patch to unconditional B",
                                "%02X %02X %02X %02X".format(data[i], data[i+1], data[i+2], data[i+3]),
                                "NOP or unconditional B",
                                "high"
                            ))
                            break // One per string
                        }
                    }
                }
            }
        }

        // 2. License/strcmp detection
        for ((offset, str) in strings) {
            val lower = str.lowercase()
            if (lower.contains("license") || lower.contains("licence") || lower.contains("serial") || lower.contains("activation")) {
                // Look for BL (function call) nearby — could be strcmp/memcmp
                val searchStart = maxOf(0, offset.toInt() - 100)
                val searchEnd = minOf(data.size, offset.toInt() + str.length + 100)
                for (i in searchStart until searchEnd step 4) {
                    if (i + 4 <= data.size) {
                        val insn = data[i].toInt() and 0xFF or ((data[i + 1].toInt() and 0xFF) shl 8) or
                                ((data[i + 2].toInt() and 0xFF) shl 16) or ((data[i + 3].toInt() and 0xFF) shl 24)
                        val top = (insn shr 26) and 0x3F
                        if (top == 0x25) { // BL instruction
                            recommendations.add(PatchRecommendation(
                                i.toLong(), "License Check",
                                "Function call near \"$str\" — likely strcmp/memcmp",
                                "%02X %02X %02X %02X".format(data[i], data[i+1], data[i+2], data[i+3]),
                                "MOV W0, #0 + RET (return 0)",
                                "high"
                            ))
                            break
                        }
                    }
                }
            }
        }

        // 3. Time check detection
        for ((offset, str) in strings) {
            val lower = str.lowercase()
            if (lower.contains("time") || lower.contains("expire") || lower.contains("date")) {
                val searchStart = maxOf(0, offset.toInt() - 150)
                val searchEnd = minOf(data.size, offset.toInt() + str.length + 150)
                for (i in searchStart until searchEnd step 4) {
                    if (i + 4 <= data.size) {
                        val insn = data[i].toInt() and 0xFF or ((data[i + 1].toInt() and 0xFF) shl 8) or
                                ((data[i + 2].toInt() and 0xFF) shl 16) or ((data[i + 3].toInt() and 0xFF) shl 24)
                        val top = (insn shr 26) and 0x3F
                        if (top == 0x25) {
                            recommendations.add(PatchRecommendation(
                                i.toLong(), "Time Check",
                                "Function call near \"$str\" — likely time()/gettimeofday()",
                                "%02X %02X %02X %02X".format(data[i], data[i+1], data[i+2], data[i+3]),
                                "NOP (skip time check)",
                                "medium"
                            ))
                            break
                        }
                    }
                }
            }
        }

        // 4. Anti-debug detection
        for ((offset, str) in strings) {
            val lower = str.lowercase()
            if (lower.contains("tracerpid") || lower.contains("ptrace") || lower.contains("debug")) {
                val searchStart = maxOf(0, offset.toInt() - 100)
                val searchEnd = minOf(data.size, offset.toInt() + str.length + 100)
                for (i in searchStart until searchEnd step 4) {
                    if (i + 4 <= data.size) {
                        val insn = data[i].toInt() and 0xFF or ((data[i + 1].toInt() and 0xFF) shl 8) or
                                ((data[i + 2].toInt() and 0xFF) shl 16) or ((data[i + 3].toInt() and 0xFF) shl 24)
                        val top = (insn shr 26) and 0x3F
                        if (top == 0x25) {
                            recommendations.add(PatchRecommendation(
                                i.toLong(), "Anti-Debug",
                                "Function call near \"$str\" — likely ptrace()/TracerPid check",
                                "%02X %02X %02X %02X".format(data[i], data[i+1], data[i+2], data[i+3]),
                                "NOP (skip anti-debug)",
                                "medium"
                            ))
                            break
                        }
                    }
                }
            }
        }

        return recommendations.sortedByDescending { if (it.severity == "high") 3 else if (it.severity == "medium") 2 else 1 }
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
                if (sb.length >= minLen) results.add(start to sb.toString())
                sb.clear()
            }
        }
        if (sb.length >= minLen) results.add(start to sb.toString())
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
