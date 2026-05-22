package com.ant.tunes.ui

import android.content.Context
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ant.tunes.player.CacheManager
import com.ant.tunes.player.PlayerManager
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.AntGlassBorder
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntSurface2
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3

val AccentColors = listOf(
    Color(0xFFDC143C), Color(0xFF8A2BE2), Color(0xFF0EA5E9), Color(0xFF10B981),
    Color(0xFFF59E0B), Color(0xFFA733FF), Color(0xFF14B8A6), Color(0xFF94A3B8),
    Color(0xFFF97316), Color(0xFFEC4899), Color(0xFFC67B33), Color(0xFF38BDF8)
)

@OptIn(UnstableApi::class)
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)

    // 🟢 FIXED: Default color changed to Crimson Red (#DC143C)
    var selectedColorInt by remember { mutableIntStateOf(prefs.getInt("accent_color", android.graphics.Color.parseColor("#DC143C"))) }
    val activeAccent = Color(selectedColorInt)

    var gapless by remember { mutableStateOf(prefs.getBoolean("gapless_playback", false)) }

    // 🟢 Audio States


    // 🟢 FIXED: Smart Cache default is now off (false)
    var cacheEnabled by remember { mutableStateOf(prefs.getBoolean("cache_enabled", false)) }
    var cacheLimitMB by remember { mutableFloatStateOf(prefs.getInt("cache_limit_mb", 500).toFloat()) }

    var stealthMode by remember { mutableStateOf(prefs.getBoolean("stealth", false)) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AntBlack)
            .statusBarsPadding()
    ) {
        // ── HEADER ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", style = MaterialTheme.typography.displayMedium, color = AntText)
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp).background(AntSurface2, CircleShape).border(1.dp, AntGlassBorder, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = AntText)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // ── 1. ACCENT COLOR PICKER ──
            Text("ACCENT COLOR", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AccentColors.chunked(6).forEach { rowColors ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        rowColors.forEach { color ->
                            val isSelected = color.toArgb() == selectedColorInt
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(color).border(width = if (isSelected) 3.dp else 0.dp, color = if (isSelected) AntText else Color.Transparent, shape = CircleShape)
                                    .clickable {
                                        selectedColorInt = color.toArgb()
                                        prefs.edit().putInt("accent_color", selectedColorInt).apply()
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 3. PLAYBACK & STORAGE ──
            Text("PLAYBACK & STORAGE", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(modifier = Modifier.height(16.dp))
            SettingsPanel {
                SettingsToggleRow("Gapless Playback", "Eliminate silence between tracks", gapless, activeAccent) {
                    gapless = it
                    prefs.edit().putBoolean("gapless_playback", it).apply()
                    PlayerManager.applyAudioTweaks(context) // Apply instantly to ExoPlayer
                }

                HorizontalDivider(color = AntGlassBorder, thickness = 1.dp)

                // 🟢 SMART CACHING TOGGLE
                SettingsToggleRow("Smart Caching", "Automatically cache songs for offline playback", cacheEnabled, activeAccent) {
                    cacheEnabled = it
                    prefs.edit().putBoolean("cache_enabled", it).apply()
                }

                // 🟢 CACHE LIMIT SLIDER (Appears only if caching is enabled)
                if (cacheEnabled) {
                    HorizontalDivider(color = AntGlassBorder, thickness = 1.dp)
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cache Size Limit", style = MaterialTheme.typography.titleMedium, color = AntText)
                            Text("${cacheLimitMB.toInt()} MB", style = MaterialTheme.typography.titleMedium, color = activeAccent)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = cacheLimitMB,
                            onValueChange = { cacheLimitMB = it },
                            onValueChangeFinished = {
                                prefs.edit().putInt("cache_limit_mb", cacheLimitMB.toInt()).apply()
                            },
                            valueRange = 100f..2048f, // 100 MB to ~2 GB
                            colors = SliderDefaults.colors(
                                thumbColor = activeAccent,
                                activeTrackColor = activeAccent,
                                inactiveTrackColor = AntSurface2
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 4. PRIVACY ──
            Text("PRIVACY", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(modifier = Modifier.height(16.dp))
            SettingsPanel {
                SettingsToggleRow("Stealth Mode", "Pause search history", stealthMode, activeAccent) {
                    stealthMode = it
                    prefs.edit().putBoolean("stealth", it).apply()
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 5. ABOUT ──
            Text("ABOUT", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(modifier = Modifier.height(16.dp))
            SettingsPanel {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("Version", style = MaterialTheme.typography.titleMedium, color = AntText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("v6.0 — AMOLED Edition", style = MaterialTheme.typography.bodyMedium, color = AntText2)
                }
                HorizontalDivider(color = AntGlassBorder, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        CacheManager.clearCache(context)
                        Toast.makeText(context, "Cache wiped successfully", Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clear Cache", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF4444))
                }

            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AntSurface1).border(1.dp, AntGlassBorder, RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, activeColor: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AntText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AntText2)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AntBlack, checkedTrackColor = activeColor,
                uncheckedThumbColor = AntText2, uncheckedTrackColor = AntSurface2,
                uncheckedBorderColor = AntGlassBorder
            )
        )
    }
}
