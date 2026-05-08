package com.ant.tunes.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsMenu(
    onDismiss: () -> Unit
) {
    val accent = LocalAccentColor.current
    val context = LocalContext.current
    val currentSong by PlayerManager.currentSong.collectAsState()

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    // 🟢 SOLID SLEEP TIMER DIALOG
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            containerColor = Color(0xFF1A1A1A), // 🔥 Solid AMOLED Dark Grey (No transparency)
            title = { Text("Sleep Timer", color = AntText) },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        Text(
                            text = "$mins Minutes",
                            color = AntText2,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    PlayerManager.setSleepTimer(mins)
                                    Toast.makeText(context, "Timer set for $mins minutes", Toast.LENGTH_SHORT).show()
                                    showSleepTimerDialog = false
                                    onDismiss()
                                }
                                .padding(vertical = 16.dp)
                        )
                    }
                    Text(
                        text = "Turn Off Timer",
                        color = accent,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PlayerManager.setSleepTimer(0)
                                Toast.makeText(context, "Timer cancelled", Toast.LENGTH_SHORT).show()
                                showSleepTimerDialog = false
                                onDismiss()
                            }
                            .padding(vertical = 16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("CANCEL", color = accent)
                }
            }
        )
    }

    // 🟢 ADD TO PLAYLIST DIALOG
    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            containerColor = Color(0xFF1A1A1A), // 🔥 Solid AMOLED Dark Grey
            title = { Text("Add to Playlist", color = AntText) },
            text = {
                if (globalPlaylists.isEmpty()) {
                    Text("No playlists yet. Create one in the Library tab!", color = AntText3)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp) // Makes it scrollable if there are many playlists
                    ) {
                        items(globalPlaylists) { playlist ->
                            val isAdded = playlist.tracks.any { it.id == currentSong?.id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAdded && currentSong != null) {
                                        currentSong?.let { song ->
                                            playlist.tracks.add(song)
                                            Toast.makeText(context, "Added to ${playlist.name.value}", Toast.LENGTH_SHORT).show()
                                        }
                                        showPlaylistDialog = false
                                        onDismiss()
                                    }
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = if (isAdded) AntText3 else accent)
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = playlist.name.value,
                                    color = if (isAdded) AntText3 else AntText,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isAdded) {
                                    Icon(Icons.Default.Check, contentDescription = "Already Added", tint = AntText3)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("CLOSE", color = accent)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AntText3) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = "OPTIONS",
                style = MaterialTheme.typography.labelLarge,
                color = AntText3,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            // 🟢 UPDATED OPTIONS
            OptionRow(Icons.Default.Timer, "Sleep Timer", accent) {
                showSleepTimerDialog = true
            }
            OptionRow(Icons.Default.PlaylistAdd, "Add to Playlist", accent) {
                showPlaylistDialog = true
            }
        }
    }
}

@Composable
private fun OptionRow(icon: ImageVector, text: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = accent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = AntText)
    }
}
