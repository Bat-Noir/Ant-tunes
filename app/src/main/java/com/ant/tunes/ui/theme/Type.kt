package com.ant.tunes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ant.tunes.R

// ── FONT FAMILIES ──
// Put these in res/font/ — rename to lowercase_no_spaces:
// ndot57_aligned.otf
// ntype82_regular.otf
// ntype82_bold.otf
// sf_pro_regular.otf
// sf_pro_bold.otf

val NdotFamily = FontFamily(
    Font(R.font.ndot57_aligned, FontWeight.Normal),
)

val NTypeFamily = FontFamily(
    Font(R.font.ntype82_regular, FontWeight.Normal),
    Font(R.font.ntype82_bold,    FontWeight.Bold),
)

val SFProFamily = FontFamily(
    Font(R.font.sf_pro_regular, FontWeight.Normal),
    Font(R.font.sf_pro_bold,    FontWeight.Bold),
)

// ── TYPOGRAPHY ──
val AntTypography = Typography(

    // 🔥 Hero titles — Ndot57 (that dot-matrix drip)
    displayLarge = TextStyle(
        fontFamily    = NdotFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 34.sp,
        lineHeight    = 38.sp,
        letterSpacing = 2.sp
    ),
    displayMedium = TextStyle(
        fontFamily    = NdotFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 26.sp,
        lineHeight    = 30.sp,
        letterSpacing = 1.5.sp
    ),

    // 🎵 Song titles, section headers — NType82 Bold
    titleLarge = TextStyle(
        fontFamily    = NTypeFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 22.sp,
        lineHeight    = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily    = NTypeFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 16.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily    = NTypeFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 13.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp
    ),

    // 📖 Body text — SF Pro Regular
    bodyLarge = TextStyle(
        fontFamily    = SFProFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = SFProFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = SFProFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp
    ),

    // 🏷️ Labels, tags, mono stuff — NType82 Regular
    labelLarge = TextStyle(
        fontFamily    = NTypeFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 11.sp,
        letterSpacing = 2.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = NTypeFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 9.sp,
        letterSpacing = 2.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = NTypeFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 8.sp,
        letterSpacing = 1.5.sp
    ),
)