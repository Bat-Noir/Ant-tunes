package com.ant.tunes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ant.tunes.lastfm.LastFmAuthManager
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.*
import com.ant.tunes.ui.components.AmbientBlobs
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.AntTunesTheme
import com.ant.tunes.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🟢 RUN PLAYLIST MIGRATION ON BOOT
        com.ant.tunes.player.PlaylistMigration.runIfNeeded(this)
        enableEdgeToEdge()
        // 🟢 FIXED: Check the public 'currentSong.value' instead of the private 'player'
        // This perfectly protects your background session without crashing the compiler!
        if (com.ant.tunes.player.PlayerManager.currentSong.value == null) {
            com.ant.tunes.player.PlayerManager.init(this)
            com.ant.tunes.player.PlayerManager.restorePlayback(this)
            // Add this inside onCreate() in MainActivity.kt
            com.ant.tunes.player.LocalHistoryManager.loadHistory(this)
        }

        // ✅ restore liked songs + playlists from disk
        com.ant.tunes.ui.initGlobalData(this)
        LastFmAuthManager.init(this)
        setContent {
            AntTunesTheme {
                AntTunesApp()
            }
        }
    }

    // 🟢 CATCHES THE DEEP LINK WHEN BROWSER CLOSES
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleLastFmIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        handleLastFmIntent(intent)
    }

    private fun handleLastFmIntent(intent: android.content.Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "anttunes" && uri.host == "lastfm") {
            val token = uri.getQueryParameter("token")
            if (token != null) {
                intent.data = null // Clear the intent so it doesn't loop
                lifecycleScope.launch {
                    com.ant.tunes.lastfm.LastFmAuthManager.completeAuth(this@MainActivity, token)
                }
            }
        }
    }
}

@Composable
fun AntTunesApp() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
    val vm: PlayerViewModel = viewModel()
    var currentTab by remember { mutableStateOf(NavTab.HOME) }

    LaunchedEffect(RequestTabSwitch) {
        RequestTabSwitch?.let { targetTab ->
            currentTab = targetTab
            RequestTabSwitch = null
        }
    }

    // 🟢 Jumps to HOME Tab immediately so the Player can expand
    LaunchedEffect(RequestFullScreenPlayer) {
        if (RequestFullScreenPlayer) {
            currentTab = NavTab.HOME
        }
    }

    val isPlayerExpanded by vm.isPlayerExpanded
    // 🟢 ADDED: We need isPlaying for the global MiniPlayer
    val isPlaying by PlayerManager.isPlayingFlow.collectAsState()

    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarded", false)) }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var showProfile by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    if (showOnboarding) {
        OnboardingScreen(onFinish = { name ->
            prefs.edit().putBoolean("onboarded", true).putString("user_name", name).apply()
            userName = name
            showOnboarding = false
        })
        return
    }

    if (showSettings) {
        SettingsScreen(onClose = { showSettings = false })
        return
    }

    if (showProfile) {
        ProfileScreen(
            userName = userName.ifEmpty { "Listener" },
            onClose = { showProfile = false },
            onOpenSettings = { showProfile = false; showSettings = true }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AntBlack)
    ) {
        AmbientBlobs()

        Box(
            modifier = Modifier.fillMaxSize() // 🟢 FIXED: Let it fill the whole screen!
        ) {
            when (currentTab) {
                NavTab.HOME    -> PlayerScreen(onOpenProfile = { showProfile = true })
                NavTab.SEARCH  -> SearchScreen(vm)
                NavTab.LIBRARY -> LibraryScreen(vm)
            }
        }


        // 🟢 Hides Navs when Player is expanded, Search UI is active, OR a Library/Home sub-screen is open
        val hideBottomNav = isPlayerExpanded ||
                (currentTab == NavTab.SEARCH && com.ant.tunes.ui.IsSearchUIActive) ||
                (currentTab == NavTab.LIBRARY && com.ant.tunes.ui.IsLibrarySubScreenActive) ||
                (currentTab == NavTab.HOME && com.ant.tunes.ui.IsHomeSubScreenActive)

        // 🟢 FIXED: ONE single AnimatedVisibility block controls BOTH the Mini Player and the Bottom Nav.
        // They are glued together and will slide up and down flawlessly as a single unit.
        androidx.compose.animation.AnimatedVisibility(
            visible = !hideBottomNav,
            enter = androidx.compose.animation.slideInVertically { it },
            exit = androidx.compose.animation.slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── GLOBAL MINI PLAYER ──
                MiniPlayerBar(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    onClick = {
                        currentTab = NavTab.HOME
                        RequestFullScreenPlayer = true
                    },
                    isPlaying = isPlaying
                )

                // ── BOTTOM NAV ──
                BottomNav(
                    selected = currentTab,
                    onSelect = { currentTab = it },
                    modifier = Modifier.padding(bottom = 7.dp)
                )
            }
        }
    } // <-- End of main Box
} // <-- End of AntTunesApp
