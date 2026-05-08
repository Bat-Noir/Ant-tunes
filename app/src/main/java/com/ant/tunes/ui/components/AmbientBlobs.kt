package com.ant.tunes.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ant.tunes.ui.theme.AntBlobRed
import com.ant.tunes.ui.theme.AntBlue
import com.ant.tunes.ui.theme.AntBlueBright

@Composable
fun AmbientBlobs() {
    val infiniteTransition = rememberInfiniteTransition(label = "blobs")

    val blob1Y by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = -70f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "blob1y"
    )
    val blob2Y by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, 4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "blob2y"
    )
    val blob3Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, 8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "blob3y"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Blob 1 — blue top left
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset((-80).dp, blob1Y.dp)
                .blur(80.dp)
                .clip(CircleShape)
                .background(AntBlue.copy(alpha = 0.10f))
        )
        // Blob 2 — red bottom right
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(250.dp, blob2Y.dp)
                .blur(80.dp)
                .clip(CircleShape)
                .background(AntBlobRed.copy(alpha = 0.10f))
        )
        // Blob 3 — bright blue center
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(150.dp, blob3Y.dp)
                .blur(60.dp)
                .clip(CircleShape)
                .background(AntBlueBright.copy(alpha = 0.05f))
        )
    }
}