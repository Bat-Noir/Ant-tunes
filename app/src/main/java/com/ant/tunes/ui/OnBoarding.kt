package com.ant.tunes.ui

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.ant.tunes.ui.theme.*

@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun OnboardingScreen(onFinish: (String) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val accent = LocalAccentColor.current

    // Animations
    val dotAlpha by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "da"
    )
    val blob1Y by rememberInfiniteTransition(label = "b1").animateFloat(
        initialValue = -100f, targetValue = -70f,
        animationSpec = infiniteRepeatable(tween(12000, easing = EaseInOutSine), RepeatMode.Reverse), label = "b1y"
    )

    // Button Logic: Only disable if on the name step and name is empty
    val isNextEnabled = (step != 3) || name.isNotBlank()

    Box(modifier = Modifier.fillMaxSize().background(AntBlack)) {
        // Blobs
        Box(modifier = Modifier.size(350.dp).offset((-80).dp, blob1Y.dp).blur(80.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)))
        Box(modifier = Modifier.size(280.dp).offset(250.dp, (-80).dp).blur(80.dp).clip(CircleShape).background(AntBlobRed.copy(alpha = 0.10f)))

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).alpha(dotAlpha).background(accent, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("ANT TUNES", style = MaterialTheme.typography.labelLarge, color = AntText3)
            }

            // Center Content
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(200.dp).clip(CircleShape).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.15f), AntBlack))).border(1.dp, AntGlassBorderHot, CircleShape))
                    Box(modifier = Modifier.size(150.dp).clip(CircleShape).background(AntSurface1).border(1.dp, AntGlassBorder, CircleShape))
                    Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.15f), AntBlack))), contentAlignment = Alignment.Center) {
                        Text("0${step + 1}", style = MaterialTheme.typography.displayLarge, color = accent)
                    }
                }
                Spacer(Modifier.height(40.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { i ->
                        Box(modifier = Modifier.height(4.dp).width(if (i == step) 24.dp else 8.dp).clip(RoundedCornerShape(2.dp)).background(if (i == step) accent else AntGlassBorder))
                    }
                }
            }

            // Bottom Content
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                when (step) {
                    0 -> StepContent("THE REBELLION", "Zero Ads.\nZero Limits.", "Tired of paying just to skip a song? We unified YouTube, Gaana, and JioSaavn into one seamless, ad-free engine.", accent)
                    1 -> StepContent("PRO CONTROLS", "Built For\nSpeed.", "Swipe left on any searched track to 'Play Next'. Your heavy rotation auto-caches for offline play. Swipe, tap, vibe.", accent)
                    2 -> StepContent("BRING YOUR DATA", "Built For\nCustomization.", "Get all your data from just logging in via profile. Supported - Last fm, YT Music.", accent)
                    3 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(20.dp).height(1.dp).background(accent))
                            Spacer(Modifier.width(8.dp))
                            Text("PERSONALIZE", style = MaterialTheme.typography.labelLarge, color = accent)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Who's\nListening?", style = MaterialTheme.typography.displayLarge, color = AntText)
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Your name...", style = MaterialTheme.typography.bodyLarge, color = AntText3) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = AntGlassBorder, focusedTextColor = AntText, unfocusedTextColor = AntText, cursorColor = accent, focusedContainerColor = AntSurface1, unfocusedContainerColor = AntSurface1),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (step < 3) step++ else onFinish(name.trim())
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = isNextEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, disabledContainerColor = AntSurface1)
                ) {
                    Text(if (step == 3) "START LISTENING" else "NEXT", style = MaterialTheme.typography.labelLarge, color = if (isNextEnabled) Color.White else AntText3)
                }
            }
        }
    }
}

@Composable
private fun StepContent(tag: String, title: String, subtitle: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(20.dp).height(1.dp).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(tag, style = MaterialTheme.typography.labelLarge, color = accent)
    }
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.displayLarge, color = AntText)
    Spacer(Modifier.height(12.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AntText2)
}
