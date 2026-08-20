package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FolderIcon(
    color: Color,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 80.dp
) {
    Canvas(modifier = modifier.size(sizeDp)) {
        val width = size.width
        val height = size.height

        val tabWidth = width * 0.42f
        val tabHeight = height * 0.18f
        val corner = width * 0.08f

        val backColor = color.copy(alpha = 0.85f)
        val frontColor = color
        val paperColor = Color.White.copy(alpha = 0.9f)
        val shadowColor = Color.Black.copy(alpha = 0.15f)

        // 1. Shadow under folder
        drawRoundRect(
            color = shadowColor,
            topLeft = Offset(width * 0.05f, height * 0.25f),
            size = Size(width * 0.9f, height * 0.72f),
            cornerRadius = CornerRadius(corner, corner)
        )

        // 2. Back Tab & Back Body
        val backPath = Path().apply {
            moveTo(corner, height * 0.18f)
            lineTo(tabWidth, height * 0.18f)
            lineTo(tabWidth + height * 0.08f, height * 0.28f)
            lineTo(width - corner, height * 0.28f)
            quadraticTo(width, height * 0.28f, width, height * 0.28f + corner)
            lineTo(width, height * 0.9f - corner)
            quadraticTo(width, height * 0.9f, width - corner, height * 0.9f)
            lineTo(corner, height * 0.9f)
            quadraticTo(0f, height * 0.9f, 0f, height * 0.9f - corner)
            lineTo(0f, height * 0.18f + corner)
            quadraticTo(0f, height * 0.18f, corner, height * 0.18f)
            close()
        }
        drawPath(path = backPath, color = backColor, style = Fill)

        // 3. Papers inside folder
        drawRoundRect(
            color = paperColor,
            topLeft = Offset(width * 0.15f, height * 0.22f),
            size = Size(width * 0.7f, height * 0.45f),
            cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f)
        )

        // 4. Front Cover Flap
        val frontPath = Path().apply {
            moveTo(0f, height * 0.38f + corner)
            quadraticTo(0f, height * 0.38f, corner, height * 0.38f)
            lineTo(width - corner, height * 0.38f)
            quadraticTo(width, height * 0.38f, width, height * 0.38f + corner)
            lineTo(width, height * 0.92f - corner)
            quadraticTo(width, height * 0.92f, width - corner, height * 0.92f)
            lineTo(corner, height * 0.92f)
            quadraticTo(0f, height * 0.92f, 0f, height * 0.92f - corner)
            close()
        }
        drawPath(path = frontPath, color = frontColor, style = Fill)

        // 5. Subtle highlight line on front flap edge
        val highlightPath = Path().apply {
            moveTo(corner, height * 0.39f)
            lineTo(width - corner, height * 0.39f)
        }
        drawPath(
            path = highlightPath,
            color = Color.White.copy(alpha = 0.35f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = width * 0.02f)
        )
    }
}
