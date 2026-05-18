package com.ant.tunes.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.ant.tunes.R
import com.ant.tunes.ui.theme.*

enum class NavTab { HOME, SEARCH, LIBRARY }

@Composable
fun BottomNav(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xD0080808))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(36.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // top shine line
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(1.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon      = R.drawable.ic_home,
                tab       = NavTab.HOME,
                selected  = selected,
                onSelect  = onSelect
            )
            NavItem(
                icon      = R.drawable.ic_search,
                tab       = NavTab.SEARCH,
                selected  = selected,
                onSelect  = onSelect
            )
            NavItem(
                icon      = R.drawable.ic_library,
                tab       = NavTab.LIBRARY,
                selected  = selected,
                onSelect  = onSelect
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: Int,
    tab: NavTab,
    selected: NavTab,
    onSelect: (NavTab) -> Unit
) {
    val isActive = selected == tab
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "navscale"
    )

    // 🟢 Fetch the dynamic accent color
    val accent = LocalAccentColor.current

    Box(
        modifier = Modifier
            .size(56.dp, 42.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive) accent.copy(alpha = 0.35f) else Color.Transparent
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelect(tab) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(icon),
            contentDescription = null,
            tint = if (isActive) accent else AntText3,
            modifier = Modifier.size(22.dp)
        )
    }
}
