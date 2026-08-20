package com.anitools.gab.utils

data class ShellScriptInfo(
    val interpreter: String,
    val lineCount: Int,
    val size: Long,
    val hasShebang: Boolean,
    val commands: List<String>,
    val urls: List<String>,
    val variables: List<String>,
    val functions: List<String>,
    val obfuscatedStrings: List<String>,
    val binaryOffsets: List<BinaryPayload>
)

data class BinaryPayload(
    val offset: Long,
    val type: String, // base64, hex, gzip, uuencode, dd
    val description: String,
    val decodedSize: Long
)

object ShellScriptParser {

    fun parse(content: String): ShellScriptInfo {
        val lines = content.lines()
        val interpreter = if (lines.isNotEmpty() && lines[0].startsWith("#!")) {
            lines[0].substringAfter("#!").trim()
        } else "unknown"

        val hasShebang = lines.isNotEmpty() && lines[0].startsWith("#!")
        val commands = extractCommands(content)
        val urls = extractUrls(content)
        val variables = extractVariables(content)
        val functions = extractFunctions(content)
        val obfuscatedStrings = detectObfuscatedStrings(content)
        val binaryOffsets = detectBinaryPayloads(content)

        return ShellScriptInfo(
            interpreter = interpreter,
            lineCount = lines.size,
            size = content.length.toLong(),
            hasShebang = hasShebang,
            commands = commands.distinct(),
            urls = urls.distinct(),
            variables = variables.distinct(),
            functions = functions.distinct(),
            obfuscatedStrings = obfuscatedStrings.distinct(),
            binaryOffsets = binaryOffsets
        )
    }

    fun extractBinary(content: String, payload: BinaryPayload): ByteArray? {
        return try {
            when (payload.type) {
                "base64" -> {
                    val b64Regex = Regex("""(?:echo\s+)?['"]?([A-Za-z0-9+/]{20,}={0,2})['"]?\s*\|\s*base64\s*-d""")
                    val match = b64Regex.find(content)
                    if (match != null) android.util.Base64.decode(match.groupValues[1], android.util.Base64.DEFAULT)
                    else null
                }
                "hex" -> {
                    val hexRegex = Regex("""(?:echo\s+-e\s+)?['"]([0-9A-Fa-f\\x\s]+)['"]""")
                    val match = hexRegex.find(content)
                    if (match != null) {
                        val hex = match.groupValues[1].replace("\\\\x".toRegex(), "").replace("\\s".toRegex(), "")
                        hex.chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
                    } else null
                }
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun extractCommands(content: String): List<String> {
        val cmdRegex = Regex("""\b(curl|wget|chmod|chown|mkdir|rm|cp|mv|dd|tar|gzip|gunzip|base64|xxd|sed|awk|grep|eval|exec|su|mount|umount|iptables|systemctl|service|kill|pkill|nohup|screen|tmux|nc|ncat|socat|python|perl|ruby|php|java|bash|sh|zsh)\b""")
        return cmdRegex.findAll(content).map { it.value }.toList()
    }

    private fun extractUrls(content: String): List<String> {
        val urlRegex = Regex("""https?://[a-zA-Z0-9._/~:@!$&'()*+,;=%?-]+""")
        val domainRegex = Regex("""(?:https?://)?([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(?:/[a-zA-Z0-9._/~:@!$&'()*+,;=%?-]*)?""")
        val urls = urlRegex.findAll(content).map { it.value }.toMutableList()
        urls.addAll(domainRegex.findAll(content).map { it.value }.filter { it.contains(".") && it.length > 5 })
        return urls
    }

    private fun extractVariables(content: String): List<String> {
        val varRegex = Regex("""(?:export\s+)?([A-Z_][A-Z0-9_]*)\s*=""")
        return varRegex.findAll(content).map { it.groupValues[1] }.toList()
    }

    private fun extractFunctions(content: String): List<String> {
        val funcRegex = Regex("""(?:function\s+)?([a-zA-Z_][a-zA-Z0-9_]*)\s*\(\s*\)\s*\{""")
        return funcRegex.findAll(content).map { it.groupValues[1] }.toList()
    }

    private fun detectObfuscatedStrings(content: String): List<String> {
        val results = mutableListOf<String>()
        // base64 in echo/printf
        val b64 = Regex("""echo\s+['"]?([A-Za-z0-9+/]{40,}={0,2})['"]?\s*\|\s*base64""")
        b64.findAll(content).forEach { results.add("Base64: ${it.groupValues[1].take(40)}...") }

        // hex in echo -e
        val hex = Regex("""echo\s+-e\s+['"]([\\x0-9a-fA-F]{20,})['"]""")
        hex.findAll(content).forEach { results.add("Hex: ${it.groupValues[1].take(40)}...") }

        // eval with encoded
        val eval = Regex("""eval\s+["'](.{20,})["']""")
        eval.findAll(content).forEach { results.add("Eval: ${it.groupValues[1].take(40)}...") }

        // shell escape \xXX
        val shellEsc = Regex("""\\x[0-9a-fA-F]{2}""")
        if (shellEsc.containsMatchIn(content)) results.add("Shell escape sequences (\\xXX)")

        return results
    }

    private fun detectBinaryPayloads(content: String): List<BinaryPayload> {
        val payloads = mutableListOf<BinaryPayload>()

        // base64 payload
        val b64Pattern = Regex("""(?:echo\s+)?['"]([A-Za-z0-9+/]{100,}={0,2})['"]\s*\|\s*base64\s*-d""")
        b64Pattern.findAll(content).forEach { m ->
            val decoded = try { android.util.Base64.decode(m.groupValues[1], android.util.Base64.DEFAULT).size.toLong() } catch (_: Exception) { 0L }
            payloads.add(BinaryPayload(content.indexOf(m.value).toLong(), "base64", "Base64 encoded binary (${m.groupValues[1].length} chars → ${decoded} bytes)", decoded))
        }

        // dd payload
        val ddPattern = Regex("""dd\s+if=([^\s]+)""")
        ddPattern.findAll(content).forEach { m ->
            payloads.add(BinaryPayload(content.indexOf(m.value).toLong(), "dd", "dd dump from ${m.groupValues[1]}", 0))
        }

        // gzip/tar
        val gzipPattern = Regex("""(?:gunzip|gzip\s+-d|tar\s+-[xf])\s+([^\s]+)""")
        gzipPattern.findAll(content).forEach { m ->
            payloads.add(BinaryPayload(content.indexOf(m.value).toLong(), "gzip", "Compressed payload: ${m.groupValues[1]}", 0))
        }

        // hex dump
        val hexPattern = Regex("""(?:echo\s+-e\s+|printf\s+['"])([\\x0-9a-fA-F]{50,})""")
        hexPattern.findAll(content).forEach { m ->
            payloads.add(BinaryPayload(content.indexOf(m.value).toLong(), "hex", "Hex encoded payload (${m.groupValues[1].length} chars)", 0))
        }

        return payloads
    }
}
