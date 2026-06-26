/*
 * =====================================================
 *  MONOWEATHER
 *  Weather App by NEWR Labs
 *  Copyright (c) 2026 NEWR Labs. All rights reserved.
 * =====================================================
 * 
 *  Connect with NEWR Labs:
 *  - X (Twitter)  : @NEWROPLY
 *  - Instagram    : @NEWROPLY
 *  - TikTok       : @NEWROPLY
 *  - Threads      : @NEWROPLY
 *  - Discord      : https://discord.gg/vZU3KzEh6a
 *  - GitHub       : https://github.com/NEWR-LABS
 * 
 *  "Just the weather. Nothing else."
 * =====================================================
 */
package com.newr.monoweather.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.newr.monoweather.R
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val fontName = GoogleFont("DotGothic16")

val NdotFont = FontFamily(
    Font(googleFont = fontName, fontProvider = provider)
)

val defaultTextStyle = TextStyle(
    fontFamily = NdotFont,
    fontWeight = FontWeight.Normal
)

val Typography = Typography(
    displayLarge = defaultTextStyle.copy(
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp
    ),
    displayMedium = defaultTextStyle.copy(
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp
    ),
    displaySmall = defaultTextStyle.copy(
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp
    ),
    headlineLarge = defaultTextStyle.copy(
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp
    ),
    headlineMedium = defaultTextStyle.copy(
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp
    ),
    headlineSmall = defaultTextStyle.copy(
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp
    ),
    titleLarge = defaultTextStyle.copy(
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp
    ),
    titleMedium = defaultTextStyle.copy(
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp, fontWeight = FontWeight.Medium
    ),
    titleSmall = defaultTextStyle.copy(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium
    ),
    labelLarge = defaultTextStyle.copy(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium
    ),
    labelMedium = defaultTextStyle.copy(
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium
    ),
    labelSmall = defaultTextStyle.copy(
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium
    ),
    bodyLarge = defaultTextStyle.copy(
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp
    ),
    bodyMedium = defaultTextStyle.copy(
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp
    ),
    bodySmall = defaultTextStyle.copy(
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    )
)
