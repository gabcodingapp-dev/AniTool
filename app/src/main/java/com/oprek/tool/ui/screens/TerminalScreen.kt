package com.oprek.tool.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
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
import java.io.BufferedReader
import java.io.InputStreamReader

data class TerminalLine(val text: String, val isCommand: Boolean, val isError: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var command by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<TerminalLine>() }
    var isRunning by remember { mutableStateOf(false) }

    fun runCommand(cmd: String) {
        if (cmd.isBlank()) return
        lines.add(TerminalLine("$ $cmd", isCommand = true))
        isRunning = true
        scope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", cmd))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errReader = BufferedReader(InputStreamReader(process.errorStream))

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    withContext(Dispatchers.Main) {
                        lines.add(TerminalLine(line!!, isCommand = false))
                    }
                }
                while (errReader.readLine().also { line = it } != null) {
                    withContext(Dispatchers.Main) {
                        lines.add(TerminalLine(line!!, isCommand = false, isError = true))
                    }
                }
                val exitCode = process.waitFor()
                withContext(Dispatchers.Main) {
                    lines.add(TerminalLine("[exit: $exitCode]", isCommand = false, isError = exitCode != 0))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lines.add(TerminalLine("Error: ${e.message}", isCommand = false, isError = true))
                }
            }
            isRunning = false
        }
    }

    LaunchedEffect(lines.size) {
        listState.animateScrollToItem(lines.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { lines.clear() }) {
                        Icon(Icons.Default.DeleteSweep, "Clear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = Color(0xFF0A0E14)
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Terminal output
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0A0E14))
                    .padding(8.dp)
            ) {
                // Welcome message
                if (lines.isEmpty()) {
                    item {
                        Text("OprekTool Terminal v1.0\nType a command below.\n", fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace, color = AccentGreen)
                    }
                }
                items(lines) { line ->
                    Text(
                        line.text,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            line.isCommand -> AccentCyan
                            line.isError -> AccentRed
                            else -> AccentGreen
                        },
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }

            // Divider
            HorizontalDivider(color = AccentGreen.copy(alpha = 0.3f))

            // Input
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$ ", fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = AccentGreen)
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter command...", color = TextMuted) },
                    singleLine = true,
                    enabled = !isRunning,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        cursorColor = AccentGreen,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                IconButton(onClick = {
                    runCommand(command)
                    command = ""
                }, enabled = command.isNotEmpty() && !isRunning) {
                    Icon(Icons.Default.Send, "Run", tint = if (command.isNotEmpty()) AccentGreen else TextMuted)
                }
            }
        }
    }
}
