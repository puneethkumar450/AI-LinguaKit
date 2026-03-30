package com.puneeth50.ailinguakit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

@Composable
fun highlightCardBrush(): Brush {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.background.luminance() < 0.5f
    val background = colorScheme.background

    /*Brush.verticalGradient(
        colors = listOf(
            BrandBlue,
            Color(0xFF3459D1),
            Color(0xFF2946A7)
        )
    )*/

    val top = colorScheme.surface
        .copy(alpha = if (isDarkTheme) 0.92f else 0.82f)
        .compositeOver(colorScheme.primary.copy(alpha = 0.72f).compositeOver(background))
    val middle = colorScheme.surface
        .copy(alpha = if (isDarkTheme) 0.92f else 0.97f)
        .compositeOver(background)
    val bottom = colorScheme.primary
        .copy(alpha = if (isDarkTheme) 0.76f else 0.88f)
        .compositeOver(colorScheme.surface)

    return Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(
                BrandBlue.copy(alpha = 0.6f),
                Color(0xFF182A6E),
                Color(0xFF102679)
            )
        } else {
            listOf(
                BrandBlue.copy(alpha = 0.82f),
                Color(0xFF3459D1),
                Color(0xFF2946A7)
            )
        })
}
