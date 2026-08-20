package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.PushPin
import com.example.data.local.FileItemEntity
import java.util.Locale

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    fileItem: FileItemEntity,
    onClick: () -> Unit,
    onCopyClick: () -> Unit = {},
    onMoveClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onPublicUploadClick: () -> Unit = {},
    onToggleFavoriteClick: () -> Unit = {},
    onTogglePinClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isUploading: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val (icon, iconTint) = when {
        fileItem.mimeType.startsWith("image/") -> Icons.Default.Image to Color(0xFF10B981)
        fileItem.mimeType.startsWith("video/") -> Icons.Default.VideoFile to Color(0xFFEF4444)
        fileItem.mimeType.startsWith("audio/") -> Icons.Default.AudioFile to Color(0xFFF59E0B)
        fileItem.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf to Color(0xFFEC4899)
        fileItem.mimeType.startsWith("text/") -> Icons.Default.Description to Color(0xFF3B82F6)
        else -> Icons.Default.InsertDriveFile to Color(0xFF6366F1)
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) {
                            onToggleSelection()
                        } else {
                            onClick()
                        }
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isSelectionMode) {
                            onToggleSelection()
                        } else {
                            onLongClick()
                        }
                    }
                )
                .testTag("file_item_${fileItem.id}"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Icon container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = iconTint,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // File Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (fileItem.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "مثبت",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (fileItem.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "مفضل",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = fileItem.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = formatFileSizeArabic(fileItem.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Options Menu Button (only shown when not selecting)
                if (!isSelectionMode) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات الملف",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Dropdown Menu
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (fileItem.isPinned) "إلغاء التثبيت من الأعلى" else "تثبيت الملف في الأعلى 📌") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = if (fileItem.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    menuExpanded = false
                    onTogglePinClick()
                }
            )
            DropdownMenuItem(
                text = { Text(if (fileItem.isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة") },
                leadingIcon = {
                    Icon(
                        imageVector = if (fileItem.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (fileItem.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    menuExpanded = false
                    onToggleFavoriteClick()
                }
            )
            DropdownMenuItem(
                text = { Text("فتح الملف") },
                leadingIcon = { Icon(icon, contentDescription = null, tint = iconTint) },
                onClick = {
                    menuExpanded = false
                    onClick()
                }
            )
            DropdownMenuItem(
                text = { Text("مشاركة الملف") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onShareClick()
                }
            )
            DropdownMenuItem(
                text = { Text("رفع للعامة (Google Drive) ☁️") },
                leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    menuExpanded = false
                    onPublicUploadClick()
                }
            )
            DropdownMenuItem(
                text = { Text("تغيير اسم الملف") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onRenameClick()
                }
            )
            DropdownMenuItem(
                text = { Text("نسخ الملف") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onCopyClick()
                }
            )
            DropdownMenuItem(
                text = { Text("نقل الملف") },
                leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onMoveClick()
                }
            )
            DropdownMenuItem(
                text = { Text("نقل إلى سلة المهملات", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuExpanded = false
                    onDeleteClick()
                }
            )
        }
    }
}

fun formatFileSizeArabic(bytes: Long): String {
    if (bytes < 1024) return "$bytes بايت"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f ك.ب", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.getDefault(), "%.1f م.ب", mb)
    val gb = mb / 1024.0
    return String.format(Locale.getDefault(), "%.2f ج.ب", gb)
}
