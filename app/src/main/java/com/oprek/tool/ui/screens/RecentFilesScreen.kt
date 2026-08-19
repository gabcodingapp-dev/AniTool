package com.oprek.tool.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.oprek.tool.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class RecentFile(val name: String, val path: String, val timestamp: Long, val size: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentFilesScreen(navController: NavController, onFileSelected: (String) -> Unit = {}) {
    val context = LocalContext.current
    val recents = remember { mutableStateListOf<RecentFile>() }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("oprek_recent", Context.MODE_PRIVATE)
        val entries = prefs.getStringSet("files", emptySet()) ?: emptySet()
        entries.forEach { entry ->
            val parts = entry.split("|")
            if (parts.size >= 3) {
                val f = File(parts[1])
                if (f.exists()) {
                    recents.add(RecentFile(parts[0], parts[1], parts[2].toLongOrNull() ?: 0, f.length()))
                }
            }
        }
        recents.sortByDescending { it.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("🕐 Recent Files", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = {
                        context.getSharedPreferences("oprek_recent", Context.MODE_PRIVATE).edit().remove("files").apply()
                        recents.clear()
                    }) { Icon(Icons.Default.DeleteSweep, "Clear All") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg))
        },
        containerColor = DarkBg
    ) { padding ->
        if (recents.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recent files", color = TextSecondary)
            }
        } else {
            LazyColumn(Modifier.padding(padding).padding(12.dp)) {
                itemsIndexed(recents) { idx, rf ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onFileSelected(rf.path) },
                        colors = CardDefaults.cardColors(containerColor = DarkCard), shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(rf.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text(rf.path, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextMuted, maxLines = 1)
                                Text("${formatSize(rf.size)} • ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(rf.timestamp))}",
                                    fontSize = 10.sp, color = TextSecondary)
                            }
                            IconButton(onClick = {
                                recents.removeAt(idx)
                                val prefs = context.getSharedPreferences("oprek_recent", Context.MODE_PRIVATE)
                                val set = prefs.getStringSet("files", emptySet())?.toMutableSet() ?: mutableSetOf()
                                set.remove("${rf.name}|${rf.path}|${rf.timestamp}")
                                prefs.edit().putStringSet("files", set).apply()
                            }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, "Remove", Modifier.size(14.dp), tint = AccentRed) }
                        }
                    }
                }
            }
        }
    }
}

fun addRecentFile(context: Context, name: String, path: String) {
    val prefs = context.getSharedPreferences("oprek_recent", Context.MODE_PRIVATE)
    val set = prefs.getStringSet("files", emptySet())?.toMutableSet() ?: mutableSetOf()
    set.add("$name|$path|${System.currentTimeMillis()}")
    if (set.size > 50) { val list = set.toList().takeLast(50); set.clear(); set.addAll(list) }
    prefs.edit().putStringSet("files", set).apply()
}

private fun formatSize(bytes: Long) = when { bytes < 1024 -> "${bytes}B"; bytes < 1048576 -> "${bytes/1024}KB"; else -> "${"%.1f".format(bytes/1048576.0)}MB" }
