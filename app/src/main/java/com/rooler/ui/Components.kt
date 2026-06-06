package com.rooler.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Кнопка с градиентной заливкой — главное действие. */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    brush: Brush = R.GradPrimary,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) brush else Brush.linearGradient(listOf(R.S3, R.S3)))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color.White else R.T3,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize
        )
    }
}

/** Обведённая кнопка — вторичное действие. */
@Composable
fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = R.PR2,
    height: Dp = 46.dp,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.45f)), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** «Стеклянная» карточка-поверхность со скруглением. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    color: Color = R.S2,
    radius: Dp = 18.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(color)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)), RoundedCornerShape(radius))
    ) { content() }
}

/** Маленький бейдж-пилюля статуса. */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
