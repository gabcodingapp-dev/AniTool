package com.oprek.tool.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.oprek.tool.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeobfuscateScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("strings") }
    var isProcessing by remember { mutableStateOf(false) }

    val modes = listOf(
        "strings" to "Extract Strings",
        "unicode" to "Decode Unicode",
        "hex" to "Decode Hex",
        "base64" to "Decode Base64",
        "url" to "Decode URL",
        "xor" to "XOR Decrypt",
        "reverse" to "Reverse Strings",
        "unescape" to "Unescape Shell",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔓 Deobfuscate", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Mode selector
            Card(
                Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentPurple)
                    Spacer(Modifier.height(8.dp))
                    // Chips row 1
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        modes.take(4).forEach { (key, label) ->
                            DeobChip(label, selectedMode == key) { selectedMode = key }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Chips row 2
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        modes.drop(4).forEach { (key, label) ->
                            DeobChip(label, selectedMode == key) { selectedMode = key }
                        }
                    }
                }
            }

            // Input
            Card(
                Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Input", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentCyan)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("Paste obfuscated text here...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentCyan)
                    )
                }
            }

            // Process button
            Button(
                onClick = {
                    isProcessing = true
                    scope.launch(Dispatchers.Default) {
                        val result = withContext(Dispatchers.Default) {
                            processDeobfuscate(inputText, selectedMode)
                        }
                        outputText = result
                        isProcessing = false
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = inputText.isNotEmpty() && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Deobfuscate", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            // Output
            if (outputText.isNotEmpty()) {
                Card(
                    Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Output", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentGreen, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val clipboard = navController.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("output", outputText))
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ContentCopy, "Copy", Modifier.size(16.dp), tint = AccentGreen)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            outputText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentGreen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun DeobChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AccentPurple.copy(alpha = 0.3f),
            selectedLabelColor = AccentPurple
        ),
        modifier = Modifier.height(32.dp)
    )
}

private fun processDeobfuscate(input: String, mode: String): String {
    if (input.isBlank()) return ""
    return try {
        when (mode) {
            "strings" -> {
                // Extract printable strings (min 3 chars)
                val sb = StringBuilder()
                val cur = StringBuilder()
                for (c in input) {
                    if (c.code in 0x20..0x7E) {
                        cur.append(c)
                    } else {
                        if (cur.length >= 3) {
                            if (sb.isNotEmpty()) sb.append("\n")
                            sb.append(cur)
                        }
                        cur.clear()
                    }
                }
                if (cur.length >= 3) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(cur)
                }
                sb.toString()
            }
            "unicode" -> {
                // Decode \\uXXXX sequences
                val regex = Regex("\\\\u([0-9a-fA-F]{4})")
                regex.replace(input) { match ->
                    match.groupValues[1].toInt(16).toChar().toString()
                }
            }
            "hex" -> {
                // Decode hex string (spaces or no spaces)
                val hex = input.replace("\\s".toRegex(), "")
                hex.chunked(2).map { it.toInt(16).toChar() }.joinToString("")
            }
            "base64" -> {
                String(android.util.Base64.decode(input.trim(), android.util.Base64.DEFAULT))
            }
            "url" -> {
                java.net.URLDecoder.decode(input, "UTF-8")
            }
            "xor" -> {
                // Auto-detect XOR key by trying single-byte keys
                val bytes = input.toByteArray()
                val results = mutableListOf<String>()
                for (key in 0..255) {
                    val decoded = bytes.map { (it.toInt() xor key).toChar() }.joinToString("")
                    val score = decoded.count { it.code in 0x20..0x7E || it == '\n' || it == '\r' }
                    if (score > bytes.size * 0.7) {
                        results.add("Key 0x${"%02X".format(key)}:\n$decoded")
                    }
                }
                if (results.isNotEmpty()) results.joinToString("\n\n") else "No likely XOR key found (tried 0x00-0xFF)"
            }
            "reverse" -> {
                input.reversed()
            }
            "unescape" -> {
                // Shell unescape: \\n -> newline, \\t -> tab, \\\\ -> \\
                input.replace("\\\\n", "\n")
                    .replace("\\\\t", "\t")
                    .replace("\\\\\\\\", "\\")
                    .replace("\\\\r", "\r")
                    .replace("\\\\0", "\u0000")
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
            }
            else -> input
        }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
