package com.ant.tunes.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ant.tunes.ui.theme.*
import java.io.File

// Built-in chibi avatars using emoji (no image files needed)
val CHIBI_AVATARS = listOf(
    "🕷️" to "Spider",
    "🛡️" to "Shield",
    "🦇" to "Bat",
    "🦸‍♂️" to "Superman",
    "⚡" to "Thunder",
    "💚" to "Green Heart",
    "🐱" to "Cat",
    "💫" to "Shooting Star",
    "🕸️" to "Web",
    "🔴" to "Red Dot",
    "🐉" to "Dragon",
    "🍥" to "Naruto",
    "🏴‍☠️" to "Pirate Flag",
    "⚔️" to "Swords",
    "🌸" to "Flower1",
    "💜" to "Purple Heart",
    "🌺" to "Flower2",
    "👑" to "Crown"
).distinctBy { it.second } // 🟢 Kills any duplicate names instantly!

object AvatarManager {
    private const val KEY_AVATAR_TYPE = "avatar_type" // "custom" | "chibi" | "initials"
    private const val KEY_AVATAR_CHIBI = "avatar_chibi_index"
    private const val KEY_AVATAR_CUSTOM_PATH = "avatar_custom_path"

    fun saveCustomAvatar(context: Context, uri: Uri) {
        // Copy to app files for persistence
        val dest = File(context.filesDir, "avatar.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_AVATAR_TYPE, "custom")
            .putString(KEY_AVATAR_CUSTOM_PATH, dest.absolutePath)
            .apply()
    }

    fun saveChibiAvatar(context: Context, index: Int) {
        context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AVATAR_TYPE, "chibi")
            .putInt(KEY_AVATAR_CHIBI, index)
            .apply()
    }

    fun removeAvatar(context: Context) {
        File(context.filesDir, "avatar.jpg").delete()
        context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AVATAR_TYPE, "initials")
            .apply()
    }

    fun getAvatarType(context: Context): String =
        context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
            .getString("avatar_type", "initials") ?: "initials"

    fun getCustomPath(context: Context): String? {
        val path = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
            .getString(KEY_AVATAR_CUSTOM_PATH, null)
        return if (path != null && File(path).exists()) path else null
    }

    fun getChibiIndex(context: Context): Int =
        context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
            .getInt(KEY_AVATAR_CHIBI, 0)
}

@Composable
fun AvatarDisplay(
    userName: String,
    size: androidx.compose.ui.unit.Dp = 100.dp,
    accent: Color
) {
    val context = LocalContext.current
    val avatarType = remember { AvatarManager.getAvatarType(context) }
    val initials = userName.take(2).uppercase().ifEmpty { "AT" }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(accent.copy(0.3f), AntBlack)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when (avatarType) {
            "custom" -> {
                val path = AvatarManager.getCustomPath(context)
                if (path != null) {
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback to initials if file deleted
                    Text(initials,
                        style = MaterialTheme.typography.titleLarge,
                        color = accent)
                }
            }
            "chibi" -> {
                val idx = AvatarManager.getChibiIndex(context)
                Text(
                    CHIBI_AVATARS.getOrNull(idx)?.first ?: initials,
                    style = MaterialTheme.typography.displayMedium
                )
            }
            else -> {
                Text(initials,
                    style = MaterialTheme.typography.titleLarge,
                    color = accent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerSheet(
    onDismiss: () -> Unit,
    onAvatarChanged: () -> Unit
) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    var showChibiGrid by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            AvatarManager.saveCustomAvatar(context, it)
            onAvatarChanged()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0E0E0E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Text("CHANGE AVATAR",
                style = MaterialTheme.typography.labelLarge,
                color = accent)
            Spacer(Modifier.height(20.dp))

            if (!showChibiGrid) {
                // Options
                listOf(
                    Triple(Icons.Default.PhotoCamera, "Upload Photo", "From gallery") to {
                        photoPicker.launch("image/*")
                    },
                    // 🟢 FIXED: "Chibi Avatars" renamed to "Emojis", and subtitle removed
                    Triple(Icons.Default.EmojiEmotions, "Emojis", "") to {
                        showChibiGrid = true
                    },
                    // 🟢 FIXED: "Use Initials" completely removed
                    Triple(Icons.Default.Delete, "Remove Photo", "Reset to initials") to {
                        AvatarManager.removeAvatar(context)
                        onAvatarChanged()
                        onDismiss()
                    }
                ).forEach { (info, action) ->
                    val (icon, title, subtitle) = info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { action() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AntSurface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = AntText)
                            // 🟢 FIXED: Dynamically hides the subtitle text completely if it's blank
                            if (subtitle.isNotEmpty()) {
                                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AntText3)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            } else {
                // Chibi grid
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    IconButton(
                        onClick = { showChibiGrid = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = AntText2)
                    }
                    // 🟢 FIXED: Removed the secondary "PICK AVATAR" heading next to the back button!
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(CHIBI_AVATARS) { index, (emoji, name) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                AvatarManager.saveChibiAvatar(context, index)
                                onAvatarChanged()
                                onDismiss()
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(AntSurface1)
                                    .border(1.dp, AntGlassBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(name,
                                style = MaterialTheme.typography.labelSmall,
                                color = AntText3,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
