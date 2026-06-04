package dev.patryk.score66

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.patryk.score66.ui.ScoreKeeperApp
import dev.patryk.score66.ui.theme.Score66Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Score66Theme {
                ScoreKeeperApp()
            }
        }
    }
}
