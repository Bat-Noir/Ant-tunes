package com.ant.tunes.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ant.tunes.ui.theme.*

@Composable
fun VinylArt(
    imageUrl: String?,
    isPlaying: Boolean,
    onColorExtracted: (Color) -> Unit // 🟢 THE GLOW CALLBACK
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)

    // 🟢 PERMANENT MEMORY: Loads your last choice, defaults to false (vinyl)
    var isSquareFormat by remember { mutableStateOf(prefs.getBoolean("is_square_format", false)) }

    var rotation by remember { mutableFloatStateOf(0f) }
    var floatTime by remember { mutableFloatStateOf(0f) }

    // 🟢 DYNAMIC COLOR ENGINE: Scans the album art and extracts the color
    LaunchedEffect(imageUrl) {
        if (imageUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // 🟢 Critical: Allows pixel reading
                .size(400) // Small size for instant processing
                .build()

            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    // Get a rich, dark vibrant color (fallback to dark muted, then dark gray)
                    val vibrant = palette.getDarkVibrantColor(
                        palette.getDarkMutedColor(
                            palette.getDominantColor(android.graphics.Color.parseColor("#121212"))
                        )
                    )
                    onColorExtracted(Color(vibrant))
                }
            }
        } else {
            onColorExtracted(AntBlack)
        }
    }

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
            .height(320.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // 🟢 SAVES TO MEMORY INSTANTLY
                isSquareFormat = !isSquareFormat
                prefs.edit().putBoolean("is_square_format", isSquareFormat).apply()
            },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = isSquareFormat, animationSpec = tween(400), label = "fade") { isSquare ->
            if (isSquare) {
                Box(
                    modifier = Modifier
                        .size(280.dp)
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
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
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
                        Box(modifier = Modifier.size(12.dp).align(Alignment.Center).background(AntBlack, CircleShape).border(1.dp, Color.DarkGray, CircleShape))
                    }

                    Canvas(
                        modifier = Modifier
                            .size(160.dp)
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
