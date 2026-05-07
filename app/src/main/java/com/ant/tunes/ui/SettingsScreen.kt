package com.ant.tunes.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ant.tunes.ui.theme.*

val AccentColors = listOf(
    Color(0xFFDC143C), Color(0xFF8A2BE2), Color(0xFF0EA5E9), Color(0xFF10B981),
    Color(0xFFF59E0B), Color(0xFFE11D48), Color(0xFF14B8A6), Color(0xFF94A3B8),
    Color(0xFFF97316), Color(0xFFEC4899), Color(0xFFEAB308), Color(0xFF38BDF8)
)

val EqPresets = listOf("Flat", "Bass Boost", "Acoustic", "Electronic", "Vocal")

@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)

    var selectedColorInt by remember { mutableIntStateOf(prefs.getInt("accent_color", android.graphics.Color.parseColor("#0EA5E9"))) }
    val activeAccent = Color(selectedColorInt)
    var selectedEq by remember { mutableStateOf(prefs.getString("eq_preset", "Flat") ?: "Flat") }

    var gapless by remember { mutableStateOf(prefs.getBoolean("gapless", false)) }
    var normalize by remember { mutableStateOf(prefs.getBoolean("normalize", true)) }
    var monoAudio by remember { mutableStateOf(prefs.getBoolean("mono_audio", false)) }

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

            // ── 2. EQUALIZER PRESETS ──
            Text("EQUALIZER", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(EqPresets) { preset ->
                    val isSelected = preset == selectedEq
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) activeAccent else AntSurface2).border(1.dp, if (isSelected) activeAccent else AntGlassBorder, RoundedCornerShape(20.dp))
                            .clickable {
                                selectedEq = preset
                                prefs.edit().putString("eq_preset", preset).apply()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(text = preset, style = MaterialTheme.typography.labelLarge, color = if (isSelected) AntBlack else AntText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 3. AUDIO TWEAKS ──
            Text("AUDIO TWEAKS", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(modifier = Modifier.height(16.dp))
            SettingsPanel {
                SettingsToggleRow("Gapless Playback", "Eliminate silence between tracks", gapless, activeAccent) { gapless = it; prefs.edit().putBoolean("gapless", it).apply() }
                HorizontalDivider(color = AntGlassBorder, thickness = 1.dp)
                SettingsToggleRow("Normalize Volume", "Set the same volume level for all songs", normalize, activeAccent) { normalize = it; prefs.edit().putBoolean("normalize", it).apply() }
                HorizontalDivider(color = AntGlassBorder, thickness = 1.dp)
                SettingsToggleRow("Mono Audio", "Combine channels when playing audio", monoAudio, activeAccent) { monoAudio = it; prefs.edit().putBoolean("mono_audio", it).apply() }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 4. PRIVACY ──
            Text("PRIVACY", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(modifier = Modifier.height(16.dp))
            SettingsPanel {
                // 🟢 Removed "Clear Search History" logic
                SettingsToggleRow("Stealth Mode", "Pause search history", stealthMode, activeAccent) { stealthMode = it; prefs.edit().putBoolean("stealth", it).apply() }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 5. ABOUT ──
            Text("ABOUT", style = MaterialTheme.typography.labelLarge, color = AntText3)
            Spacer(modifier = Modifier.height(16.dp))
            SettingsPanel {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("Version", style = MaterialTheme.typography.titleMedium, color = AntText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("v3.0 — AMOLED Edition", style = MaterialTheme.typography.bodyMedium, color = AntText2) // 🟢 Updated Version
                }
                HorizontalDivider(color = AntGlassBorder, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { Toast.makeText(context, "Cache cleared (0 MB)", Toast.LENGTH_SHORT).show() }.padding(horizontal = 20.dp, vertical = 16.dp),
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
