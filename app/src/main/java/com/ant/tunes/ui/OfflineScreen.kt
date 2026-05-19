package com.ant.tunes.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.ant.tunes.data.Song
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.*

@Composable
fun OfflineScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val songs by PlayerManager.downloadedSongs.collectAsState()
    val currentSong by PlayerManager.currentSong.collectAsState()

    // 🟢 Grab the live accent color!
    val accent = LocalAccentColor.current

    // local file picker
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            addLocalFile(context, uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AntBlack)
    ) {
        // ambient glow top
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-50).dp, y = (-80).dp)
                .blur(80.dp)
                .background(
                    // 🟢 Replaced AntBlue with accent
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.15f), Color.Transparent)),
                    CircleShape
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 200.dp)
        ) {
            // ── HEADER ──
            item {
                Spacer(Modifier.height(80.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            // 🟢 Replaced AntBlueDim and AntGlassBorderHot with accent variations
                            .background(accent.copy(alpha = 0.15f))
                            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Download, null,
                            tint = accent, // 🟢 Replaced AntBlueBright
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    "MY OFFLINE MUSIC",
                    style = MaterialTheme.typography.displayLarge,
                    color = AntText,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (songs.isEmpty()) "NO TRACKS YET"
                    else "${songs.size} TRACKS AVAILABLE OFFLINE",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent, // 🟢 Replaced AntBlueBright
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(24.dp))
            }


            // ── ACTION BUTTONS ──
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // PLAY ALL
                    Button(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                PlayerManager.play(context, songs, 0)
                                com.ant.tunes.ui.RequestFullScreenPlayer = true // 🟢 ADDED
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent // 🟢 Replaced AntBlue
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PLAY ALL",
                            style = MaterialTheme.typography.labelLarge)
                    }

                    // SHUFFLE
                    OutlinedButton(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                val shuffled = songs.shuffled()
                                PlayerManager.play(context, shuffled, 0)
                                com.ant.tunes.ui.RequestFullScreenPlayer = true // 🟢 ADDED
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, AntGlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AntText
                        )
                    ) {
                        Icon(Icons.Default.Shuffle, null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("SHUFFLE",
                            style = MaterialTheme.typography.labelLarge)
                    }

                    // ADD FILES
                    IconButton(
                        onClick = { filePicker.launch("audio/*") },
                        modifier = Modifier
                            .size(48.dp)
                            .background(AntSurface2, CircleShape)
                            .border(1.dp, AntGlassBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, null,
                            tint = AntText, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            // ── EMPTY STATE ──
            if (songs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(AntSurface1)
                                .border(1.dp, AntGlassBorder, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderOpen, null,
                                tint = AntText3, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("YOUR LOCAL FILES LIVE HERE",
                            style = MaterialTheme.typography.labelLarge,
                            color = AntText3)
                        Spacer(Modifier.height(8.dp))
                        Text("TAP + TO ADD MUSIC FROM YOUR DEVICE",
                            style = MaterialTheme.typography.labelSmall,
                            color = AntText3)
                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = { filePicker.launch("audio/*") },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)), // 🟢 Replaced AntGlassBorderHot
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = accent // 🟢 Replaced AntBlueBright
                            )
                        ) {
                            Icon(Icons.Default.Add, null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("+ ADD FILES",
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // ── SONG LIST ──
            itemsIndexed(songs) { index, song ->
                val isCurrent = currentSong?.id == song.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrent) accent.copy(alpha = 0.08f) else Color.Transparent
                        )
                        .clickable {
                            PlayerManager.play(context, songs, index)
                            com.ant.tunes.ui.RequestFullScreenPlayer = true // 🟢 ADDED
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                // track number or eq bars
                    Box(
                        modifier = Modifier.width(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrent) {
                            // eq animation bars
                            EqBars()
                        } else {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AntText3
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))

                    // album art
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AntSurface2)
                    ) {
                        AsyncImage(
                            model = song.albumArt,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isCurrent) accent else AntText, // 🟢 Replaced AntBlueBright
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = AntText2, maxLines = 1
                        )
                    }

                    // delete
                    IconButton(
                        onClick = { PlayerManager.deleteSong(context, song) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, null,
                            tint = AntText3.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(
                    color = AntGlassBorder,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        // back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .statusBarsPadding()
                .size(40.dp)
                .background(AntSurface2, CircleShape)
                .border(1.dp, AntGlassBorder, CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, null,
                tint = AntText, modifier = Modifier.size(20.dp))
        }
    }
}

// ── EQ BARS animation ──
@Composable
fun EqBars() {
    // 🟢 Grab the live accent color!
    val accent = LocalAccentColor.current

    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse),
        label = "b3"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(14.dp)
    ) {
        listOf(bar1, bar2, bar3).forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .background(accent, RoundedCornerShape(2.dp)) // 🟢 Replaced AntBlue
            )
        }
    }
}

// ── add local audio file ──
fun addLocalFile(context: Context, uri: Uri) {
    try {
        val cursor = context.contentResolver.query(
            uri, null, null, null, null
        )
        cursor?.use { c ->
            val nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            c.moveToFirst()
            val name = if (nameIdx >= 0) c.getString(nameIdx) else "Unknown"
            val title = name.substringBeforeLast(".")
            val song = Song(
                id = uri.toString(),
                title = title,
                artist = "Local File",
                albumArt = "",
                streamUrl = uri.toString(),
                album = "Local",
                source = "local"
            )
            PlayerManager.addToDownloaded(context, song)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
