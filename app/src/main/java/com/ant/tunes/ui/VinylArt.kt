package com.ant.tunes.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ant.tunes.ui.theme.*

@Composable
fun VinylArt(imageUrl: String?, isPlaying: Boolean) {
    var isSquareFormat by remember { mutableStateOf(false) }

    var rotation by remember { mutableFloatStateOf(0f) }
    var floatTime by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var lastFrameTime = withFrameNanos { it }
            while (true) {
                val currentFrameTime = withFrameNanos { it }
                val deltaMs = (currentFrameTime - lastFrameTime) / 1_000_000f
                lastFrameTime = currentFrameTime

                rotation = (rotation + (deltaMs * 360f / 6000f)) % 360f
                floatTime += deltaMs
            }
        }
    }

    val floatY = kotlin.math.sin(floatTime * 2.0 * Math.PI / 3000.0).toFloat() * 15f

    val armRotation by animateFloatAsState(
        targetValue = if (isPlaying) 32f else 10f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tonearm"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 🟢 REDUCED: Scaled down from 380dp to 320dp
            .height(320.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                isSquareFormat = !isSquareFormat
            },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = isSquareFormat, animationSpec = tween(400), label = "fade") { isSquare ->
            if (isSquare) {
                Box(
                    modifier = Modifier
                        .size(280.dp) // 🟢 REDUCED: Scaled down from 330dp
                        .graphicsLayer { translationY = floatY }
                        .clip(RoundedCornerShape(24.dp))
                        .background(AntSurface2)
                        .border(1.dp, AntGlassBorderHot, RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(320.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(280.dp) // 🟢 REDUCED: Scaled down from 330dp
                            .graphicsLayer { rotationZ = rotation }
                            .clip(CircleShape)
                            .background(AntBlack)
                            .border(3.dp, AntSurface2, CircleShape)
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Vinyl Label",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(130.dp).align(Alignment.Center).clip(CircleShape)
                        )
                        Box(
                            modifier = Modifier.size(12.dp).align(Alignment.Center).background(AntBlack, CircleShape).border(1.dp, Color.DarkGray, CircleShape)
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .size(160.dp) // 🟢 REDUCED: Scaled down from 190dp
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-20).dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.8f, 0.2f)
                                rotationZ = armRotation
                            }
                    ) {
                        val pivot = Offset(size.width * 0.8f, size.height * 0.2f)
                        val endPoint = Offset(size.width * 0.3f, size.height * 0.8f)

                        drawCircle(color = Color(0xFF222222), radius = 28f, center = pivot)
                        drawCircle(color = Color(0xFF4FC3F7), radius = 6f, center = pivot)

                        drawLine(color = Color(0xFFCCCCCC), start = pivot, end = endPoint, strokeWidth = 8f, cap = StrokeCap.Round)

                        withTransform({
                            translate(left = endPoint.x, top = endPoint.y)
                            rotate(degrees = 15f)
                        }) {
                            drawRoundRect(color = Color(0xFF333333), topLeft = Offset(-12f, -10f), size = Size(24f, 44f), cornerRadius = CornerRadius(6f))
                            drawCircle(color = Color.Red, radius = 4f, center = Offset(0f, 30f))
                        }
                    }
                }
            }
        }
    }
}
