package com.anitools.gab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.anitools.gab.data.ChangelogData
import com.anitools.gab.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changelog — What's New", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        var showContent by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { showContent = true }
        if (!showContent) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentViolet)
            }
        }
        AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NewReleases, contentDescription = null, tint = AccentViolet, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("AniTool by Gab", color = AccentViolet, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("com.anitools.gab • Premium Toolkit", color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap any version to see details. New features are highlighted.", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            items(ChangelogData.entries) { entry ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (entry.isNew) DarkSurface else DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(entry.version, color = AccentViolet, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            if (entry.isNew) {
                                Surface(color = AccentViolet, shape = RoundedCornerShape(6.dp)) {
                                    Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("NEW", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Text(entry.date, color = TextMuted, fontSize = 12.sp)
                        }
                        Text(entry.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                        Spacer(Modifier.height(8.dp))
                        entry.changes.forEach { change ->
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Text("•", color = AccentCyan, modifier = Modifier.padding(end = 8.dp))
                                Text(change, color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💡 Have an idea?", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Open an issue at github.com/gabcodingapp-dev/AniTool", color = AccentCyan, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Made with 💜 by Gab for creators", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
            }
        }
    }
}
