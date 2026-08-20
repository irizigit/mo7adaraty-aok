package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.CircularProgressIndicator
import com.example.data.local.FolderEntity
import com.example.ui.theme.parseHexColor

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: FolderEntity,
    subfolderCount: Int,
    fileCount: Int,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onAddSubfolderClick: () -> Unit,
    onCopyClick: () -> Unit = {},
    onMoveClick: () -> Unit = {},
    onToggleFavoriteClick: () -> Unit = {},
    onTogglePinClick: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onExportZipClick: () -> Unit = {},
    onPublicUploadClick: () -> Unit = {},
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
    val folderColor = remember(folder.color) { parseHexColor(folder.color) }

    val (cardWidth, cardHeight, iconSize, fontSize, badgeFontSize) = when (folder.size.lowercase()) {
        "small" -> Tuple5(95.dp, 105.dp, 42.dp, 12.sp, 9.sp)
        "large" -> Tuple5(150.dp, 155.dp, 68.dp, 15.sp, 11.sp)
        "xlarge" -> Tuple5(180.dp, 180.dp, 82.dp, 16.sp, 12.sp)
        else -> Tuple5(120.dp, 130.dp, 54.dp, 13.sp, 10.sp) // "medium"
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else null

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .clip(RoundedCornerShape(16.dp))
                .testTag("folder_card_${folder.id}")
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
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            border = borderStroke,
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header bar inside card: Checkbox or Favorite & Lock indicators + More menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (folder.isPinned) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "مثبت",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            if (folder.isFavorite) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "مفضل",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (folder.isLocked) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "محمي برمز",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (!isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                .combinedClickable(
                                    onClick = { menuExpanded = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات المجلد",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Center Icon with Uploading Indicator support
                Box(contentAlignment = Alignment.Center) {
                    FolderIcon(
                        color = folderColor,
                        sizeDp = iconSize
                    )
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(iconSize),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Folder title
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subtitle badge
                val detailsText = buildString {
                    if (subfolderCount > 0) append("$subfolderCount مجلد ")
                    if (fileCount > 0) {
                        if (isNotEmpty()) append("• ")
                        append("$fileCount ملف")
                    }
                    if (isEmpty()) append("فارغ")
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = folderColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = detailsText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = badgeFontSize),
                        color = folderColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Context menu popup
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (folder.isPinned) "إلغاء التثبيت من الأعلى" else "تثبيت المجلد في الأعلى 📌") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = if (folder.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    menuExpanded = false
                    onTogglePinClick()
                }
            )
            DropdownMenuItem(
                text = { Text(if (folder.isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة") },
                leadingIcon = {
                    Icon(
                        imageVector = if (folder.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (folder.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    menuExpanded = false
                    onToggleFavoriteClick()
                }
            )
            DropdownMenuItem(
                text = { Text(if (folder.isLocked) "إلغاء قفل المجلد" else "قفل المجلد برمز PIN") },
                leadingIcon = {
                    Icon(
                        imageVector = if (folder.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                onClick = {
                    menuExpanded = false
                    onLockClick()
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
                text = { Text("تصدير ومشاركة (ZIP)") },
                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onExportZipClick()
                }
            )
            DropdownMenuItem(
                text = { Text("فتح المجلد") },
                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onClick()
                }
            )
            DropdownMenuItem(
                text = { Text("تعديل المجلد") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onEditClick()
                }
            )
            DropdownMenuItem(
                text = { Text("إضافة مجلد فرعي") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onAddSubfolderClick()
                }
            )
            DropdownMenuItem(
                text = { Text("نسخ المجلد") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onCopyClick()
                }
            )
            DropdownMenuItem(
                text = { Text("نقل المجلد") },
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

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
