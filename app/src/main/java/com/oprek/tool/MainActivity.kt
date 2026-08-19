package com.oprek.tool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.oprek.tool.ui.theme.DarkBg
import com.oprek.tool.ui.theme.OprekToolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OprekToolTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBg) {
                    val navController = rememberNavController()
                    AppNavigation(navController)
                }
            }
        }
    }
}
