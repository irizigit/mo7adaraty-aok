package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.AppBackgroundTheme

@Composable
fun AppBackgroundContainer(
    customBgColorHex: String?,
    customBgImageUri: String?,
    presetBgType: String?,
    appBackgroundTheme: AppBackgroundTheme,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // 1. CUSTOM GALLERY IMAGE FROM PHONE
            !customBgImageUri.isNullOrEmpty() -> {
                AsyncImage(
                    model = customBgImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.9f
                )
            }

            // 2. CUSTOM SOLID COLOR
            !customBgColorHex.isNullOrEmpty() -> {
                val colorVal = parseHexColor(customBgColorHex)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorVal)
                )
            }

            // 3. PRESET BACKGROUND TEXTURES & CANVASES
            !presetBgType.isNullOrEmpty() && presetBgType != "DEFAULT" -> {
                when (presetBgType) {
                    "PARCHMENT" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFAF6EE))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val step = 40.dp.toPx()
                                for (y in 0..size.height.toInt() step step.toInt()) {
                                    drawLine(
                                        color = Color(0xFFE2D7C3).copy(alpha = 0.35f),
                                        start = Offset(0f, y.toFloat()),
                                        end = Offset(size.width, y.toFloat()),
                                        strokeWidth = 1f
                                    )
                                }
                            }
                        }
                    }

                    "GEOMETRIC" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF0F172A),
                                            Color(0xFF1E293B),
                                            Color(0xFF334155)
                                        )
                                    )
                                )
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val path = Path()
                                path.moveTo(0f, size.height * 0.3f)
                                path.cubicTo(
                                    size.width * 0.4f, size.height * 0.1f,
                                    size.width * 0.6f, size.height * 0.5f,
                                    size.width, size.height * 0.2f
                                )
                                drawPath(
                                    path = path,
                                    color = Color(0xFF38BDF8).copy(alpha = 0.12f),
                                    style = Stroke(width = 4f)
                                )
                            }
                        }
                    }

                    "DARK_GALAXY" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF1E1035),
                                            Color(0xFF0B0714),
                                            Color(0xFF030206)
                                        ),
                                        center = Offset(500f, 300f),
                                        radius = 1200f
                                    )
                                )
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color(0xFFA855F7).copy(alpha = 0.15f),
                                    radius = 250f,
                                    center = Offset(size.width * 0.8f, size.height * 0.2f)
                                )
                                drawCircle(
                                    color = Color(0xFF06B6D4).copy(alpha = 0.12f),
                                    radius = 350f,
                                    center = Offset(size.width * 0.2f, size.height * 0.7f)
                                )
                            }
                        }
                    }

                    "BLUEPRINT" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0B2545))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val gridGap = 30.dp.toPx()
                                for (x in 0..size.width.toInt() step gridGap.toInt()) {
                                    drawLine(
                                        color = Color(0xFF134074).copy(alpha = 0.4f),
                                        start = Offset(x.toFloat(), 0f),
                                        end = Offset(x.toFloat(), size.height),
                                        strokeWidth = 1f
                                    )
                                }
                                for (y in 0..size.height.toInt() step gridGap.toInt()) {
                                    drawLine(
                                        color = Color(0xFF134074).copy(alpha = 0.4f),
                                        start = Offset(0f, y.toFloat()),
                                        end = Offset(size.width, y.toFloat()),
                                        strokeWidth = 1f
                                    )
                                }
                            }
                        }
                    }

                    "MINT" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFF0FDF4),
                                            Color(0xFFDCFCE7),
                                            Color(0xFFBBF7D0)
                                        )
                                    )
                                )
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }
                }
            }

            // 4. LEGACY APP BACKGROUND THEME
            else -> {
                val fallbackColor = when (appBackgroundTheme) {
                    AppBackgroundTheme.SOLID_DARK -> Color(0xFF0F172A)
                    AppBackgroundTheme.TEXTURE_PAPER -> Color(0xFFFAF8F5)
                    AppBackgroundTheme.DEFAULT -> MaterialTheme.colorScheme.background
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fallbackColor)
                )
            }
        }

        // Overlay main content over background
        content()
    }
}
