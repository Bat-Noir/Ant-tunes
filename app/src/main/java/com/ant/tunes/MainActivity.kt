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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.*
import com.ant.tunes.ui.components.AmbientBlobs
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.AntTunesTheme
import com.ant.tunes.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PlayerManager.init(this)
        PlayerManager.restorePlayback(this)
        setContent {
            AntTunesTheme {
                AntTunesApp()
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
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPlayerExpanded) 0.dp else 80.dp)
        ) {
            when (currentTab) {
                NavTab.HOME    -> PlayerScreen(onOpenProfile = { showProfile = true })
                NavTab.SEARCH  -> SearchScreen(vm)
                NavTab.LIBRARY -> LibraryScreen(vm)
            }
        }

        // 🟢 FIXED: Hides BottomNav when Player is expanded, Search UI is active, OR a Library sub-screen is open
        val hideBottomNav = isPlayerExpanded ||
                (currentTab == NavTab.SEARCH && IsSearchUIActive) ||
                (currentTab == NavTab.LIBRARY && IsLibrarySubScreenActive)

        if (!hideBottomNav) {
            BottomNav(
                selected = currentTab,
                onSelect = { currentTab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .navigationBarsPadding()
            )
        }
    }
}
