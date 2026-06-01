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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures

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

    // 🟢 FIXED: No longer forces the Home tab! Just expands the player globally.
    LaunchedEffect(RequestFullScreenPlayer) {
        if (RequestFullScreenPlayer) {
            vm.isPlayerExpanded.value = true
            RequestFullScreenPlayer = false
        }
    }

    val isPlayerExpanded by vm.isPlayerExpanded

    // 🟢 NEW: Global animation state for the player overlay
    val expandAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPlayerExpanded) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ), label = "expand"
    )

    // 🟢 Add this state
    var showUpdateDialog by remember { mutableStateOf(false) }

    // 🟢 Add this logic right below your other LaunchedEffects
    LaunchedEffect(Unit) {
        // Simple delay to let app load first
        kotlinx.coroutines.delay(2000)

        // PSEUDO-CODE: Implement your actual network call here
        // val latestVersion = githubApi.getLatestVersion()
        // if (latestVersion > BuildConfig.VERSION_NAME) { showUpdateDialog = true }

        // For testing, you can uncomment the line below to see it:
        // showUpdateDialog = true
    }

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
            onClose = { showProfile = false }
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
            modifier = Modifier.fillMaxSize()
        ) {
            when (currentTab) {
                NavTab.HOME -> PlayerScreen(
                    onOpenProfile = { showProfile = true },
                    onOpenSettings = { showSettings = true }
                )

                NavTab.SEARCH -> SearchScreen(vm)
                NavTab.LIBRARY -> LibraryScreen(vm)
            }
        }

        // 🟢 Hides Navs when Player is expanded, Search UI is active, OR a Library/Home sub-screen is open
        val hideBottomNav = isPlayerExpanded ||
                (currentTab == NavTab.SEARCH && com.ant.tunes.ui.IsSearchUIActive) ||
                (currentTab == NavTab.LIBRARY && com.ant.tunes.ui.IsLibrarySubScreenActive) ||
                (currentTab == NavTab.HOME && com.ant.tunes.ui.IsHomeSubScreenActive)

        // 🟢 THE MINI PLAYER AND BOTTOM NAV
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
                        // 🟢 FIXED: Just expand the player globally!
                        vm.isPlayerExpanded.value = true
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

        // ═══════════════════════════════════════
        // 🟢 THE GLOBAL FULL PLAYER OVERLAY
        // ═══════════════════════════════════════
        // This sits above EVERYTHING and slides up seamlessly from any tab!
        if (expandAnim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(expandAnim) // 🟢 Cleaned up!
                    .graphicsLayer { translationY = (1f - expandAnim) * 800f } // 🟢 Cleaned up!
                    .background(AntBlack)
                    .pointerInput(Unit) { // 🟢 Cleaned up!
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 30f) vm.isPlayerExpanded.value = false
                        }
                    }
            ) {
                com.ant.tunes.ui.FullPlayer(
                    isPlaying = isPlaying,
                    onCollapse = { vm.isPlayerExpanded.value = false }
                )
            }
            // 🟢 Place this right before the last closing brace '}' of AntTunesApp
            if (showUpdateDialog) {
                com.ant.tunes.ui.components.UpdateDialog(onDismiss = { showUpdateDialog = false })
            }
        } // End of main Box
    } // End of AntTunesApp
}
