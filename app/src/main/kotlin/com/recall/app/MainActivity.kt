package com.recall.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.recall.app.core.designsystem.theme.RecallTheme
import com.recall.app.ui.RecallApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val darkScrim = Color.parseColor("#121212")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(darkScrim),
            navigationBarStyle = SystemBarStyle.dark(darkScrim),
        )
        setContent {
            RecallTheme {
                RecallApp()
            }
        }
    }
}
