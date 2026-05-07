package com.ant.tunes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ant.tunes.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsMenu(
    onDismiss: () -> Unit
) {
    val accent = LocalAccentColor.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212), // Solid AMOLED dark background
        dragHandle = { BottomSheetDefaults.DragHandle(color = AntText3) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = "OPTIONS",
                style = MaterialTheme.typography.labelLarge,
                color = AntText3,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            OptionRow(Icons.Default.Timer, "Sleep Timer", accent) { /* TODO: Implement later */ }
            OptionRow(Icons.Default.Tune, "Equalizer", accent) { /* TODO: Implement later */ }
            OptionRow(Icons.Default.Info, "Song Info", accent) { /* TODO: Implement later */ }
            OptionRow(Icons.Default.Album, "Go to Album", accent) { /* TODO: Implement later */ }
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
