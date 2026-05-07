package com.ant.tunes.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.*

@Composable
fun ProfileScreen(
    userName: String, // Fallback if not set in prefs
    onClose: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)

    // ── DYNAMIC STATES ──
    val playlist by PlayerManager.playlistFlow.collectAsState()
    val downloadedSongs by PlayerManager.downloadedSongs.collectAsState()
    val currentSong by PlayerManager.currentSong.collectAsState()

    // 🟢 Grab the dynamic accent color
    val accent = LocalAccentColor.current
    val accentDim = accent.copy(alpha = 0.15f)
    val accentHot = accent.copy(alpha = 0.3f)

    // 🟢 Profile Details from SharedPreferences
    var currentName by remember { mutableStateOf(prefs.getString("user_name", userName)?.ifEmpty { "Listener" } ?: "Listener") }
    var profileImageUri by remember { mutableStateOf<Uri?>(prefs.getString("profile_image", null)?.let { Uri.parse(it) }) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(currentName) }

    // 🟢 Native Photo Picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            prefs.edit().putString("profile_image", it.toString()).apply()
        }
    }

    // Dynamic Stats
    val totalPlays = playlist.size
    val offline = downloadedSongs.size
    val likedSongsCount = globalLikedSongs.size // Pulling from global states wired in LibraryScreen
    val initials = currentName.take(2).uppercase()

    // ── EDIT NAME DIALOG ──
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = AntSurface2,
            title = { Text("Edit Profile Name", color = AntText) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = AntGlassBorder,
                        focusedTextColor = AntText, unfocusedTextColor = AntText,
                        cursorColor = accent,
                        focusedContainerColor = AntSurface1, unfocusedContainerColor = AntSurface1
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempName.isNotBlank()) {
                        currentName = tempName
                        prefs.edit().putString("user_name", tempName).apply()
                    }
                    showEditNameDialog = false
                }) { Text("Save", color = accent) }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel", color = AntText2) }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AntBlack)
    ) {
        // Background Glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 100.dp, y = (-50).dp)
                .blur(80.dp)
                .background(Brush.radialGradient(listOf(accentDim, Color.Transparent)), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // ── TOP BAR ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp).background(AntSurface1, CircleShape).border(1.dp, AntGlassBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = AntText, modifier = Modifier.size(18.dp))
                }
                Text("PROFILE", style = MaterialTheme.typography.labelLarge, color = AntText3)
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(40.dp).background(AntSurface1, CircleShape).border(1.dp, AntGlassBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Settings, null, tint = AntText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── CUSTOM AVATAR ──
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).size(100.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(accentDim, AntBlack)))
                        .border(2.dp, accentHot, CircleShape)
                        .clickable { launcher.launch("image/*") }, // Triggers photo picker
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(initials, style = MaterialTheme.typography.displayMedium, color = accent)
                    }
                }

                // Edit Pencil overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(28.dp)
                        .background(accent, CircleShape)
                        .border(2.5.dp, AntBlack, CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, "Change Photo", tint = AntBlack, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── EDITABLE NAME ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().clickable {
                    tempName = currentName
                    showEditNameDialog = true
                }
            ) {
                Text(
                    text = currentName,
                    style = MaterialTheme.typography.titleLarge,
                    color = AntText
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Edit, "Edit Name", tint = AntText3, modifier = Modifier.size(14.dp))
            }

            Text(
                text = "ANT TUNES LISTENER",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
            )

            Spacer(Modifier.height(28.dp))

            // ── STATS ROW ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("LIKED", "$likedSongsCount", "SONGS", accent, Modifier.weight(1f))
                StatCard("OFFLINE", "$offline", "SAVED", accent, Modifier.weight(1f))
                StatCard("HISTORY", "$totalPlays", "PLAYED", accent, Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))

            // ── NOW PLAYING SECTION ──
            if (currentSong != null) {
                Text("NOW PLAYING", style = MaterialTheme.typography.labelLarge, color = AntText3)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AntSurface1).border(1.dp, accentHot, RoundedCornerShape(20.dp)).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(accent, CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(currentSong?.title ?: "", style = MaterialTheme.typography.titleSmall, color = AntText, maxLines = 1)
                            Text(currentSong?.artist ?: "", style = MaterialTheme.typography.labelSmall, color = AntText2, maxLines = 1)
                        }
                        Box(modifier = Modifier.background(accentDim, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(currentSong?.source?.uppercase() ?: "", style = MaterialTheme.typography.labelSmall, color = accent)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            // ── SOURCES ──
            Text("ACTIVE SOURCES", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(Modifier.height(12.dp))

            listOf(
                Triple("JioSaavn", "STREAMING", Color(0xFF1DB954)),
                Triple("Gaana", "STREAMING", Color(0xFFFF6B35)),
                Triple("YouTube", "VIA NEWPIPE", Color(0xFFFF0000))
            ).forEach { (source, type, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(source, style = MaterialTheme.typography.titleSmall, color = AntText)
                        Text(type, style = MaterialTheme.typography.labelSmall, color = AntText3)
                    }
                    Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = color)
                }
                HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, accent: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AntSurface1)
            .border(1.dp, AntGlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = AntText2)
            Text(label, style = MaterialTheme.typography.labelSmall, color = AntText3)
        }
    }
}
