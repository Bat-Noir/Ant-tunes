package com.ant.tunes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
// 🟢 ADD THESE THEME IMPORTS
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor


@Composable
fun LyricsTab(
    lyrics: String?,
    isLoading: Boolean
) {
    val accent = LocalAccentColor.current

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        if (isLoading) {
            CircularProgressIndicator(
                color = accent,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (lyrics.isNullOrBlank() || lyrics.contains("No lyrics found") || lyrics.contains("Error fetching")) {
            // Empty / Error State
            Text(
                text = lyrics ?: "No lyrics available for this track.",
                style = MaterialTheme.typography.bodyLarge,
                color = AntText3,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Loaded Lyrics State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp)
            ) {
                Text(
                    text = lyrics,
                    style = MaterialTheme.typography.titleLarge.copy(
                        lineHeight = MaterialTheme.typography.titleLarge.lineHeight * 1.5f
                    ),
                    color = AntText,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(120.dp)) // Padding for bottom player controls
            }
        }
    }
}
