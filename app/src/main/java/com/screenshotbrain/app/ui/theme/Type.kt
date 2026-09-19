package com.trove.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.trove.app.R

// Only the Bold weight ships, so every style is bold; hierarchy comes from size and color.
private val Rounded = FontFamily(Font(R.font.helvetica_rounded_bold, FontWeight.Bold))

private fun style(size: Int, lineHeight: Int) =
    TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = lineHeight.sp)

val AppTypography = Typography(
    displaySmall = style(34, 40),
    headlineMedium = style(28, 34),
    headlineSmall = style(24, 30),
    titleLarge = style(20, 26),
    titleMedium = style(16, 22),
    titleSmall = style(14, 20),
    bodyLarge = style(16, 24),
    bodyMedium = style(14, 20),
    bodySmall = style(12, 16),
    labelLarge = style(14, 20),
    labelMedium = style(12, 16),
    labelSmall = style(11, 16),
)
