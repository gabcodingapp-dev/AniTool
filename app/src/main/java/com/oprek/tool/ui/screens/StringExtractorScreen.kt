package com.oprek.tool.ui.screens

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.oprek.tool.MainViewModel
import com.oprek.tool.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StringExtractorScreen(navController: NavController, vm: MainViewModel) {
    val strings by vm.strings.collectAsState()
    val statusMessage by vm.statusMessage.collectAsState()
    var minLength by remember { mutableStateOf("4") }
    var filter by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.extractStrings() }

    val filtered = remember(strings, filter) {
        if (filter.isEmpty()) strings
        else strings.filter { it.value.contains(filter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Strings (${filtered.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilter = !showFilter }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                    IconButton(onClick = { vm.extractStrings(minLength.toIntOrNull() ?: 4) }) {
                        Icon(Icons.Default.Refresh, "Reload")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Filter bar
            if (showFilter) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        placeholder = { Text("Filter strings...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = minLength,
                        onValueChange = { minLength = it },
                        label = { Text("Min") },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { vm.extractStrings(minLength.toIntOrNull() ?: 4) }) {
                        Icon(Icons.Default.Check, "Apply", tint = AccentBlue)
                    }
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, fontSize = 11.sp, color = AccentGreen,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }

            // String list
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(filtered) { idx, sp ->
                    StringRow(idx, sp)
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("No strings found\nExtract strings from a loaded file", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StringRow(idx: Int, sp: com.oprek.tool.core.StringPair) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .background(if (idx % 2 == 0) DarkBg else DarkSurface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "0x${"%08X".format(sp.offset)}",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = AccentPurple,
            modifier = Modifier.width(90.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            sp.value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = AccentGreen,
            maxLines = 1,
            modifier = Modifier.horizontalScroll(rememberScrollState())
        )
    }
}
