package com.ant.tunes.ui

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ant.tunes.ui.theme.AntBlack
import com.ant.tunes.ui.theme.AntBlobRed
import com.ant.tunes.ui.theme.AntGlassBorder
import com.ant.tunes.ui.theme.AntGlassBorderHot
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3
import com.ant.tunes.ui.theme.LocalAccentColor

@Composable
fun OnboardingScreen(onFinish: (String) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // 🟢 Grab the dynamic accent color
    val accent = LocalAccentColor.current

    val dotAlpha by rememberInfiniteTransition(label = "dot")
        .animateFloat(
            initialValue = 1f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "da"
        )
    val blob1Y by rememberInfiniteTransition(label = "b1")
        .animateFloat(
            initialValue = -100f, targetValue = -70f,
            animationSpec = infiniteRepeatable(
                tween(12000, easing = EaseInOutSine), RepeatMode.Reverse
            ), label = "b1y"
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AntBlack)
    ) {
        // blobs
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset((-80).dp, blob1Y.dp)
                .blur(80.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)) // 🟢 Replaced AntBlue
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(250.dp, (-80).dp)
                .blur(80.dp)
                .clip(CircleShape)
                .background(AntBlobRed.copy(alpha = 0.10f)) // Kept red blob as requested
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── TOP ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .alpha(dotAlpha)
                            .background(accent, CircleShape) // 🟢 Replaced AntBlue
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("ANT TUNES",
                        style = MaterialTheme.typography.labelLarge,
                        color = AntText3)
                }
                if (step < 3) {
                    TextButton(onClick = { onFinish(name.ifEmpty { "Listener" }) }) {
                        Text("SKIP",
                            style = MaterialTheme.typography.labelMedium,
                            color = AntText3)
                    }
                }
            }

            // ── CENTER ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // vinyl with step number
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.15f), AntBlack))) // 🟢 Replaced AntBlueDim
                            .border(1.dp, AntGlassBorderHot, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(AntSurface1)
                            .border(1.dp, AntGlassBorder, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.15f), AntBlack))), // 🟢 Replaced AntBlueDim
                        contentAlignment = Alignment.Center
                    ) {
                        Text("0${step + 1}",
                            style = MaterialTheme.typography.displayLarge,
                            color = accent) // 🟢 Replaced AntBlueBright
                    }
                    Box(Modifier.size(16.dp).background(AntBlack, CircleShape))
                }

                Spacer(Modifier.height(40.dp))

                // step dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(if (i == step) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i == step) accent else AntGlassBorder) // 🟢 Replaced AntBlue
                        )
                    }
                }
            }

            // ── BOTTOM ──
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                when (step) {
                    0 -> StepContent(
                        tag = "WELCOME",
                        title = "Music,\nUnfiltered.",
                        subtitle = "Stream from JioSaavn, Gaana and YouTube. All in one place. No ads. No limits.",
                        accent = accent
                    )
                    1 -> StepContent(
                        tag = "DISCOVER",
                        title = "Search\nAnything.",
                        subtitle = "From Arijit Singh to JVKE — find any song from any source instantly.",
                        accent = accent
                    )
                    2 -> {
                        // name input step
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(20.dp).height(1.dp).background(accent)) // 🟢 Replaced AntBlueBright
                            Spacer(Modifier.width(8.dp))
                            Text("PERSONALIZE",
                                style = MaterialTheme.typography.labelLarge,
                                color = accent) // 🟢 Replaced AntBlueBright
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("What should\nwe call you?",
                            style = MaterialTheme.typography.displayLarge,
                            color = AntText)
                        Spacer(Modifier.height(12.dp))
                        Text("Just your first name. That's all we need.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AntText2)
                        Spacer(Modifier.height(20.dp))

                        // name input field
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = {
                                Text("Your name...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AntText3)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                if (name.isNotEmpty()) step++
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent, // 🟢 Replaced AntBlue
                                unfocusedBorderColor = AntGlassBorder,
                                focusedTextColor = AntText,
                                unfocusedTextColor = AntText,
                                cursorColor = accent, // 🟢 Replaced AntBlue
                                focusedContainerColor = AntSurface1,
                                unfocusedContainerColor = AntSurface1,
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    }
                    3 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(20.dp).height(1.dp).background(accent)) // 🟢 Replaced AntBlueBright
                            Spacer(Modifier.width(8.dp))
                            Text("READY",
                                style = MaterialTheme.typography.labelLarge,
                                color = accent) // 🟢 Replaced AntBlueBright
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (name.isNotEmpty()) "Hey,\n${name}." else "You're\nAll Set.",
                            style = MaterialTheme.typography.displayLarge,
                            color = AntText
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Your queue is waiting. Let's go.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AntText2)
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (step == 2 && name.isEmpty()) {
                            // skip name
                            step++
                        } else if (step < 3) {
                            step++
                        } else {
                            onFinish(name.ifEmpty { "Listener" })
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent) // 🟢 Replaced AntBlue
                ) {
                    Text(
                        text = when (step) {
                            3 -> "START LISTENING"
                            2 -> if (name.isNotEmpty()) "CONTINUE" else "SKIP FOR NOW"
                            else -> "NEXT"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }

                if (step == 3) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onFinish(name.ifEmpty { "Listener" }) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, AntGlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AntText2)
                    ) {
                        Text("EXPLORE FIRST",
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepContent(tag: String, title: String, subtitle: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(20.dp).height(1.dp).background(accent)) // 🟢 Replaced AntBlueBright
        Spacer(Modifier.width(8.dp))
        Text(tag, style = MaterialTheme.typography.labelLarge, color = accent) // 🟢 Replaced AntBlueBright
    }
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.displayLarge, color = AntText)
    Spacer(Modifier.height(12.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AntText2)
}
