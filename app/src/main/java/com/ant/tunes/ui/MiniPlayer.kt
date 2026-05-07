package com.ant.tunes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.viewmodel.PlayerViewModel

@Composable
fun MiniPlayer(vm: PlayerViewModel, modifier: Modifier = Modifier) {
    val isPlaying by PlayerManager.isPlayingFlow.collectAsState()
    MiniPlayerBar(
        modifier = modifier,
        onClick = {},
        isPlaying = isPlaying
    )
}