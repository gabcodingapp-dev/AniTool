package com.gab.anitool

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.gab.anitool.ui.theme.DarkBg
import com.gab.anitool.ui.theme.AniToolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 120fps support — set preferred display refresh rate
        window.attributes.preferredDisplayModeId.let { current ->
            val modes = display?.supportedModes ?: emptyArray()
            val highRefresh = modes.maxByOrNull { it.refreshRate }
            if (highRefresh != null) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = highRefresh.modeId
                }
            }
        }

        // Keep screen on during analysis
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        setContent {
            AniToolTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBg) {
                    val navController = rememberNavController()
                    AppNavigation(navController)
                }
            }
        }
    }
}
