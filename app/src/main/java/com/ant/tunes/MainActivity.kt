package com.ant.tunes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.PlayerScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 init player once
        PlayerManager.init(this)
        PlayerManager.restorePlayback(this)
        setContent {
            AntTunesApp()
        }
    }
}

@Composable
fun AntTunesApp() {
    Surface {
        PlayerScreen()
    }
}