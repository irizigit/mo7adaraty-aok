package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val PrimaryIndigo = Color(0xFF6366F1)
val PrimaryDarkIndigo = Color(0xFF818CF8)
val SecondaryTeal = Color(0xFF14B8A6)
val TertiaryAmber = Color(0xFFF59E0B)

val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)

// 20 Predefined vibrant folder colors
val PresetFolderColors = listOf(
    "#6366F1", // Indigo
    "#3B82F6", // Blue
    "#0EA5E9", // Sky Blue
    "#06B6D4", // Cyan
    "#14B8A6", // Teal
    "#10B981", // Emerald
    "#22C55E", // Green
    "#84CC16", // Lime
    "#EAB308", // Yellow
    "#F59E0B", // Amber
    "#F97316", // Orange
    "#EF4444", // Red
    "#EC4899", // Pink
    "#D946EF", // Fuchsia
    "#A855F7", // Purple
    "#8B5CF6", // Violet
    "#64748B", // Slate
    "#71717A", // Zinc
    "#78716C", // Stone
    "#1E293B"  // Dark Navy
)

fun parseHexColor(hex: String, defaultColor: Color = PrimaryIndigo): Color {
    return try {
        val cleanHex = hex.removePrefix("#").trim()
        when (cleanHex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            3 -> {
                val expanded = cleanHex.map { "$it$it" }.joinToString("")
                Color(android.graphics.Color.parseColor("#$expanded"))
            }
            else -> defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}
