package com.oprek.tool

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.oprek.tool.ui.screens.*

@Composable
fun AppNavigation(navController: NavHostController, vm: MainViewModel = viewModel()) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController, vm) }
        composable("hex") { HexViewerScreen(navController, vm) }
        composable("strings") { StringExtractorScreen(navController, vm) }
        composable("elf") { ElfAnalyzerScreen(navController, vm) }
        composable("apk") { ApkAnalyzerScreen(navController, vm) }
        composable("patch") { PatchEditorScreen(navController, vm) }
        composable("info") { FileInfoScreen(navController, vm) }
        composable("terminal") { TerminalScreen(navController) }
        composable(
            "search?query={query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            SearchScreen(navController, vm, backStackEntry.arguments?.getString("query") ?: "")
        }
    }
}
