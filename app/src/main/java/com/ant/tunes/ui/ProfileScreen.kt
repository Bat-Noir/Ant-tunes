package com.ant.tunes.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.ant.tunes.R
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ant.tunes.lastfm.LastFmAuthManager
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.AntBlue
import com.ant.tunes.ui.theme.AntGlassBorder
import com.ant.tunes.ui.theme.AntGlassBorderHot
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntSurface2
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor
import com.ant.tunes.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    userName: String,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var showYoutubeLogin by remember { mutableStateOf(false) }
    val isYtLoggedIn by com.ant.tunes.ytmusic.YoutubeAuthManager.isLoggedIn.collectAsState()
    val context = LocalContext.current

    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)

    val recentSongs by PlayerManager.recentlyPlayed.collectAsState()
    val topTracks by PlayerManager.topTracks.collectAsState()
    val downloadedSongs by PlayerManager.downloadedSongs.collectAsState()
    val currentSong by PlayerManager.currentSong.collectAsState()

    val isLastFmConnected by LastFmAuthManager.isLoggedIn.collectAsState()
    val lastFmUsername by LastFmAuthManager.username.collectAsState()
    val vm: PlayerViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    val accent = LocalAccentColor.current
    val accentDim = accent.copy(alpha = 0.15f)
    val accentHot = accent.copy(alpha = 0.3f)
    var showAvatarPicker by remember { mutableStateOf(false) }
    var avatarRefreshKey by remember { mutableStateOf(0) }

    var currentName by remember { mutableStateOf(prefs.getString("user_name", userName)?.ifEmpty { "Listener" } ?: "Listener") }
    var profileImageUri by remember { mutableStateOf<Uri?>(prefs.getString("profile_image", null)?.let { Uri.parse(it) }) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(currentName) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            prefs.edit().putString("profile_image", it.toString()).apply()
        }
    }

    val totalPlays = recentSongs.size
    val offline = downloadedSongs.size
    val likedSongsCount = globalLikedSongs.size

    // ═══════════════════════════════════════
    // 🟢 REAL-TIME DIAGNOSTIC PING
    // ═══════════════════════════════════════
    var saavnStatus by remember { mutableStateOf("CHECKING...") }
    var saavnColor by remember { mutableStateOf(Color.Gray) }

    var newPipeStatus by remember { mutableStateOf("CHECKING...") }
    var newPipeColor by remember { mutableStateOf(Color.Gray) }

    LaunchedEffect(Unit) {
        // Ping JioSaavn
        launch(Dispatchers.IO) {
            try {
                val response = com.ant.tunes.network.RetrofitClient.api.searchSongs("test", 1, 1)
                if (response.isSuccessful) {
                    saavnStatus = "ACTIVE"
                    saavnColor = Color(0xFF1DB954)
                } else {
                    saavnStatus = "OFFLINE"
                    saavnColor = Color(0xFFFF4444)
                }
            } catch(e: Exception) {
                saavnStatus = "OFFLINE"
                saavnColor = Color(0xFFFF4444)
            }
        }

        // Ping YouTube/NewPipe
        launch(Dispatchers.IO) {
            try {
                val results = com.ant.tunes.NewPipeHelper.search("test")
                if (results.isNotEmpty()) {
                    newPipeStatus = "ACTIVE"
                    newPipeColor = Color(0xFFFF0000)
                } else {
                    newPipeStatus = "UPDATE APP"
                    newPipeColor = Color(0xFFFF4444)
                }
            } catch(e: Exception) {
                newPipeStatus = "UPDATE APP"
                newPipeColor = Color(0xFFFF4444)
            }
        }
    }

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
        modifier = Modifier.fillMaxSize().background(AntBlack)
    ) {
        Box(
            modifier = Modifier.size(300.dp).offset(x = 100.dp, y = (-50).dp).blur(80.dp)
                .background(Brush.radialGradient(listOf(accentDim, Color.Transparent)), CircleShape)
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 100.dp)
        ) {
            Spacer(Modifier.height(16.dp))

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
                Spacer(modifier = Modifier.size(40.dp))
            }

            Box(modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally)) {
                key(avatarRefreshKey) {
                    AvatarDisplay(userName = currentName, size = 100.dp, accent = accent)
                }
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(accent)
                        .align(Alignment.BottomEnd).clickable { showAvatarPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, null, tint = AntBlack, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().clickable {
                    tempName = currentName
                    showEditNameDialog = true
                }
            ) {
                Text(text = currentName, style = MaterialTheme.typography.titleLarge, color = AntText)
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("LIKED", "$likedSongsCount", "SONGS", accent, Modifier.weight(1f))
                StatCard("OFFLINE", "$offline", "SAVED", accent, Modifier.weight(1f))
                StatCard("HISTORY", "$totalPlays", "PLAYED", accent, Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))

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

            // ═══════════════════════════════════════
            // 🟢 DYNAMIC ACTIVE SOURCES UI
            // ═══════════════════════════════════════
            Text("ACTIVE SOURCES", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(Modifier.height(12.dp))

            // JioSaavn (Dynamic)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(saavnColor, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("JioSaavn", style = MaterialTheme.typography.titleSmall, color = AntText)
                    Text("STREAMING", style = MaterialTheme.typography.labelSmall, color = AntText3)
                }
                Text(saavnStatus, style = MaterialTheme.typography.labelSmall, color = saavnColor, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)

            // Gaana (Static fallback)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF6B35), CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Gaana", style = MaterialTheme.typography.titleSmall, color = AntText)
                    Text("STREAMING", style = MaterialTheme.typography.labelSmall, color = AntText3)
                }
                Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF6B35), fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)

            // YouTube / NewPipe (Dynamic)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(newPipeColor, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("YouTube", style = MaterialTheme.typography.titleSmall, color = AntText)
                    Text("VIA NEWPIPE", style = MaterialTheme.typography.labelSmall, color = AntText3)
                }
                Text(newPipeStatus, style = MaterialTheme.typography.labelSmall, color = newPipeColor, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)

            Spacer(Modifier.height(28.dp))
            Text("OTHER ACTIVE SOURCES", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(Modifier.height(12.dp))

            listOf(
                Triple("iTunes", "METADATA ENGINE", Color(0xFF00B0FF)),
                Triple("LRCLIB", "LYRICS ENGINE", Color(0xFFAA00FF))
            ).forEach { (source, type, color) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(source, style = MaterialTheme.typography.titleSmall, color = AntText)
                        Text(type, style = MaterialTheme.typography.labelSmall, color = AntText3)
                    }
                    Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = AntGlassBorder, thickness = 0.5.dp)
            }

            Spacer(Modifier.height(28.dp))
            Text("INTEGRATIONS", style = MaterialTheme.typography.labelLarge, color = AntBlue)
            Spacer(Modifier.height(12.dp))

            // YouTube Integration Card
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AntSurface1)
                    .border(1.dp, if (isYtLoggedIn) AntGlassBorderHot else AntGlassBorder, RoundedCornerShape(20.dp)).padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFF0000)), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_youtube),
                            contentDescription = "YouTube",
                            modifier = Modifier.size(28.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("YouTube Music", style = MaterialTheme.typography.titleSmall, color = AntText)
                        Text(if (isYtLoggedIn) "Connected & ready" else "Import library & all other data", style = MaterialTheme.typography.labelSmall, color = if (isYtLoggedIn) AntBlue else AntText3, maxLines = 1)
                    }
                    Button(
                        onClick = {
                            if (isYtLoggedIn) com.ant.tunes.ytmusic.YoutubeAuthManager.logout(context) else showYoutubeLogin = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isYtLoggedIn) AntSurface2 else Color(0xFFFF0000)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(if (isYtLoggedIn) "DISCONNECT" else "CONNECT", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

// Last.fm Integration Card
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AntSurface1)
                    .border(1.dp, if (isLastFmConnected) AntGlassBorderHot else AntGlassBorder, RoundedCornerShape(20.dp)).padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFD51007)), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_lastfm),
                            contentDescription = "Last.fm",
                            modifier = Modifier.size(28.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Last.fm", style = MaterialTheme.typography.titleSmall, color = AntText)
                        Text(if (isLastFmConnected) "Connected as $lastFmUsername" else "Connect for smart recommendations", style = MaterialTheme.typography.labelSmall, color = if (isLastFmConnected) AntBlue else AntText3)
                    }
                    Button(
                        onClick = {
                            if (isLastFmConnected) LastFmAuthManager.logout(context) else coroutineScope.launch { LastFmAuthManager.startAuth(context) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isLastFmConnected) AntSurface2 else Color(0xFFD51007)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(if (isLastFmConnected) "DISCONNECT" else "CONNECT", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
        }
    }

    if (showAvatarPicker) {
        AvatarPickerSheet(
            onDismiss = { showAvatarPicker = false },
            onAvatarChanged = { avatarRefreshKey++ }
        )
    }
    if (showYoutubeLogin) {
        com.ant.tunes.ytmusic.YoutubeLoginWebView(
            onDismiss = { showYoutubeLogin = false },
            onSuccess = { showYoutubeLogin = false }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, accent: Color, modifier: Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(AntSurface1).border(1.dp, AntGlassBorder, RoundedCornerShape(16.dp)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = AntText2)
            Text(label, style = MaterialTheme.typography.labelSmall, color = AntText3)
        }
    }
}
