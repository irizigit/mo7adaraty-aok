package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FileItemEntity
import com.example.data.local.FolderEntity
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.CopyMoveAction
import com.example.ui.components.CopyMoveDialog
import com.example.ui.components.CreateNoteDialog
import com.example.ui.components.DeleteFolderDialog
import com.example.ui.components.FileListItem
import com.example.ui.components.FileUploadDialog
import com.example.ui.components.FileViewerDialog
import com.example.ui.components.FolderCard
import com.example.ui.components.FolderDialog
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.components.AcademicSetupDialog
import com.example.ui.components.NotificationCenterDialog
import com.example.ui.components.RenameFileDialog
import com.example.ui.components.SetPinDialog
import com.example.ui.components.SharedImportDialog
import com.example.ui.components.StatsHeaderBar
import com.example.ui.components.TrashBinDialog
import com.example.ui.components.VerifyPinDialog
import com.example.ui.components.WifiShareDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.AppBackgroundContainer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Palette
import com.example.ui.viewmodel.AppBackgroundTheme
import com.example.ui.viewmodel.FolderViewModel
import com.example.ui.viewmodel.LayoutMode
import com.example.ui.viewmodel.SearchCategory
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainVirtualFolderScreen(
    viewModel: FolderViewModel
) {
    val context = LocalContext.current
    val currentFolderId by viewModel.currentFolderId.collectAsStateWithLifecycle()
    val folders by viewModel.currentFolders.collectAsStateWithLifecycle()
    val files by viewModel.currentFiles.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val breadcrumbs by viewModel.breadcrumbs.collectAsStateWithLifecycle()

    val subfolderCounts by viewModel.subfolderCounts.collectAsStateWithLifecycle()
    val fileCounts by viewModel.fileCounts.collectAsStateWithLifecycle()

    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val layoutMode by viewModel.layoutMode.collectAsStateWithLifecycle()
    val appBackground by viewModel.appBackground.collectAsStateWithLifecycle()
    val customBgColorHex by viewModel.customBgColorHex.collectAsStateWithLifecycle()
    val customBgImageUri by viewModel.customBgImageUri.collectAsStateWithLifecycle()
    val presetBgType by viewModel.presetBgType.collectAsStateWithLifecycle()
    val deleteSummary by viewModel.deleteSummary.collectAsStateWithLifecycle()
    val uploadingItemIds by viewModel.uploadingItemIds.collectAsStateWithLifecycle()

    val pendingSharedUris by viewModel.pendingSharedUris.collectAsStateWithLifecycle()
    val pendingSharedText by viewModel.pendingSharedText.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val trashedFolders by viewModel.trashedFolders.collectAsStateWithLifecycle()
    val trashedFiles by viewModel.trashedFiles.collectAsStateWithLifecycle()
    val appNotification by viewModel.appNotification.collectAsStateWithLifecycle()

    // Multi-Selection State
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedFolderIds by viewModel.selectedFolderIds.collectAsStateWithLifecycle()
    val selectedFileIds by viewModel.selectedFileIds.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog States
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<FolderEntity?>(null) }
    var subfolderParentId by remember { mutableStateOf<String?>(null) }

    var showFileUploadDialog by remember { mutableStateOf(false) }
    var selectedUploadUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploadingFiles by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showTrashBinDialog by remember { mutableStateOf(false) }
    var showNotificationCenterDialog by remember { mutableStateOf(false) }
    var showWifiShareDialog by remember { mutableStateOf(false) }
    var showAcademicSetupDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var selectedBottomNavItem by remember { mutableIntStateOf(0) }

    // Lock & Viewer States
    var folderToLock by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToVerifyPin by remember { mutableStateOf<FolderEntity?>(null) }
    var selectedFileForViewer by remember { mutableStateOf<FileItemEntity?>(null) }
    var fileToRename by remember { mutableStateOf<FileItemEntity?>(null) }

    // Copy / Move State
    var copyMoveTargetFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var copyMoveTargetFile by remember { mutableStateOf<FileItemEntity?>(null) }
    var copyMoveAction by remember { mutableStateOf<CopyMoveAction?>(null) }
    var isBatchCopyMoveShow by remember { mutableStateOf(false) }

    var isSearchExpanded by remember { mutableStateOf(false) }

    // Launcher for picking multiple files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUploadUris = (selectedUploadUris + uris).distinct()
            showFileUploadDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkDevicePingAndNotifications(context)
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Lifecycle Observer for periodic re-checks on App Resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkDevicePingAndNotifications(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    val totalSelected = selectedFolderIds.size + selectedFileIds.size
                    TopAppBar(
                        title = {
                            Text(
                                text = "تم تحديد $totalSelected عنصر",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد")
                            }
                        },
                        actions = {
                            // Select All / Deselect All
                            IconButton(onClick = { viewModel.toggleSelectAll(folders, files) }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "تحديد الكل")
                            }

                            // Batch Favorite
                            IconButton(onClick = { viewModel.batchToggleFavorite() }) {
                                Icon(Icons.Default.Star, contentDescription = "إضافة للمفضلة", tint = Color(0xFFFFC107))
                            }

                            // Batch Copy
                            IconButton(onClick = {
                                copyMoveAction = CopyMoveAction.COPY_FILE
                                isBatchCopyMoveShow = true
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ المحدد")
                            }

                            // Batch Move
                            IconButton(onClick = {
                                copyMoveAction = CopyMoveAction.MOVE_FILE
                                isBatchCopyMoveShow = true
                            }) {
                                Icon(Icons.Default.DriveFileMove, contentDescription = "نقل المحدد")
                            }

                            // Batch Delete
                            IconButton(onClick = { viewModel.batchMoveToTrash() }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف المحدد", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            if (isSearchExpanded) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("بحث في الملفات والمجلدات...") },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.School,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "محاضراتي",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (isSearchExpanded) {
                                IconButton(onClick = {
                                    isSearchExpanded = false
                                    viewModel.setSearchQuery("")
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "إلغاء البحث")
                                }
                            } else if (currentFolderId != null) {
                                IconButton(onClick = {
                                    val parent = breadcrumbs.lastOrNull()?.parentId
                                    viewModel.navigateToFolder(parent)
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع"
                                    )
                                }
                            }
                        },
                        actions = {
                            if (!isSearchExpanded) {
                                // 1. Notifications Button (زر الإشعارات والتحديثات)
                                IconButton(onClick = { showNotificationCenterDialog = true }) {
                                    if (appNotification != null && appNotification?.hasNotification == true) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Default.Notifications, contentDescription = "الإشعارات والتحديثات")
                                        }
                                    } else {
                                        Icon(Icons.Default.Notifications, contentDescription = "الإشعارات والتحديثات")
                                    }
                                }

                                // 2. Search Button (زر البحث)
                                IconButton(onClick = { isSearchExpanded = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "بحث")
                                }

                                // 3. QR Code Share Button (مشاركة التطبيق عبر QR)
                                IconButton(onClick = { showWifiShareDialog = true }) {
                                    Icon(Icons.Default.QrCode2, contentDescription = "مشاركة التطبيق عبر QR Code")
                                }

                                // 4. Dark/Light Mode Button (الوضع المظلم)
                                IconButton(onClick = { viewModel.toggleDarkMode() }) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "الوضع المظلم"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            },
            floatingActionButton = {
                Box {
                    FloatingActionButton(
                        onClick = { showFabMenu = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_options")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة جديد")
                    }

                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📁 مجلد جديد") },
                            onClick = {
                                showFabMenu = false
                                folderToEdit = null
                                subfolderParentId = currentFolderId
                                showCreateFolderDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📝 ملاحظة جديدة") },
                            onClick = {
                                showFabMenu = false
                                showCreateNoteDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📤 رفع ملف من الجهاز") },
                            onClick = {
                                showFabMenu = false
                                filePickerLauncher.launch("*/*")
                            }
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (isSelectionMode) {
                    val selectedTotal = selectedFolderIds.size + selectedFileIds.size
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "محدد: $selectedTotal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                 // Batch Share Button
                                IconButton(onClick = { viewModel.batchShareSelected(context) }) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة الملاحظات/الملفات المحددة")
                                }

                                // Batch ZIP Compress Button
                                IconButton(onClick = { viewModel.exportSelectedAsZip(context) }) {
                                    Icon(Icons.Default.Archive, contentDescription = "ضغط ومشاركة كـ ZIP")
                                }

                                // Batch Copy Button
                                IconButton(onClick = {
                                    copyMoveAction = CopyMoveAction.COPY_FOLDER
                                    isBatchCopyMoveShow = true
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ المحدد")
                                }

                                // Batch Move Button
                                IconButton(onClick = {
                                    copyMoveAction = CopyMoveAction.MOVE_FOLDER
                                    isBatchCopyMoveShow = true
                                }) {
                                    Icon(Icons.Default.DriveFileMove, contentDescription = "نقل المحدد")
                                }

                                // Batch Favorite Button
                                IconButton(onClick = { viewModel.batchToggleFavorite() }) {
                                    Icon(Icons.Default.Star, contentDescription = "المفضلة للمحدد")
                                }

                                // Batch Trash Button
                                IconButton(onClick = { viewModel.batchMoveToTrash() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "سلة المهملات للمحدد", tint = MaterialTheme.colorScheme.error)
                                }

                                // Clear Selection
                                IconButton(onClick = { viewModel.clearSelection() }) {
                                    Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد")
                                }
                            }
                        }
                    }
                } else {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedBottomNavItem == 0 && currentFolderId == null,
                            onClick = {
                                selectedBottomNavItem = 0
                                viewModel.navigateToFolder(null)
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                            label = { Text("الرئيسية", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedBottomNavItem == 1,
                            onClick = {
                                selectedBottomNavItem = 1
                                showAcademicSetupDialog = true
                            },
                            icon = { Icon(Icons.Default.School, contentDescription = "أكاديمي") },
                            label = { Text("أكاديمي", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedBottomNavItem == 3,
                            onClick = {
                                selectedBottomNavItem = 3
                                showTrashBinDialog = true
                            },
                            icon = {
                                val trashedTotal = trashedFolders.size + trashedFiles.size
                                if (trashedTotal > 0) {
                                    BadgedBox(badge = { Badge { Text("$trashedTotal") } }) {
                                        Icon(Icons.Default.Delete, contentDescription = "سلة المهملات")
                                    }
                                } else {
                                    Icon(Icons.Default.Delete, contentDescription = "سلة المهملات")
                                }
                            },
                            label = { Text("المهملات", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedBottomNavItem == 2 || showWifiShareDialog,
                            onClick = {
                                selectedBottomNavItem = 2
                                showWifiShareDialog = true
                            },
                            icon = { Icon(Icons.Default.QrCode2, contentDescription = "مشاركة QR") },
                            label = { Text("مشاركة QR", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedBottomNavItem == 4 || showSettingsDialog,
                            onClick = {
                                selectedBottomNavItem = 4
                                showSettingsDialog = true
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                            label = { Text("الإعدادات", fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            AppBackgroundContainer(
                customBgColorHex = customBgColorHex,
                customBgImageUri = customBgImageUri,
                presetBgType = presetBgType,
                appBackgroundTheme = appBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                // Breadcrumbs navigation bar
                BreadcrumbBar(
                    breadcrumbs = breadcrumbs,
                    onBreadcrumbClick = { targetFolderId ->
                        viewModel.navigateToFolder(targetFolderId)
                    }
                )

                // Category Filter Bar
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == SearchCategory.ALL,
                            onClick = { viewModel.setSelectedCategory(SearchCategory.ALL) },
                            label = { Text("الكل") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == SearchCategory.FAVORITES,
                            onClick = { viewModel.setSelectedCategory(SearchCategory.FAVORITES) },
                            label = { Text("المفضلة ⭐") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == SearchCategory.FOLDERS,
                            onClick = { viewModel.setSelectedCategory(SearchCategory.FOLDERS) },
                            label = { Text("المجلدات") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == SearchCategory.DOCUMENTS,
                            onClick = { viewModel.setSelectedCategory(SearchCategory.DOCUMENTS) },
                            label = { Text("المستندات والنصوص") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == SearchCategory.MEDIA,
                            onClick = { viewModel.setSelectedCategory(SearchCategory.MEDIA) },
                            label = { Text("الصور والوسائط") }
                        )
                    }
                }

                // Statistics header bar
                StatsHeaderBar(
                    folderCount = if (currentFolderId == null) folders.size else allFolders.size,
                    fileCount = if (currentFolderId == null) allFiles.size else files.size,
                    subfolderCount = folders.size
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Section 1: Virtual Folders Grid
                    if (folders.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "المجلدات (${folders.size}):",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                folders.forEach { folder ->
                                    val isSelected = selectedFolderIds.contains(folder.id)
                                    val isUploading = uploadingItemIds.contains(folder.id)
                                    FolderCard(
                                        folder = folder,
                                        subfolderCount = subfolderCounts[folder.id] ?: 0,
                                        fileCount = fileCounts[folder.id] ?: 0,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = isSelected,
                                        isUploading = isUploading,
                                        onToggleSelection = { viewModel.toggleFolderSelection(folder.id) },
                                        onLongClick = { viewModel.startSelectionWithFolder(folder.id) },
                                        onClick = {
                                            if (folder.isLocked) {
                                                folderToVerifyPin = folder
                                            } else {
                                                viewModel.navigateToFolder(folder.id)
                                            }
                                        },
                                        onEditClick = {
                                            folderToEdit = folder
                                            subfolderParentId = folder.parentId
                                            showCreateFolderDialog = true
                                        },
                                        onAddSubfolderClick = {
                                            folderToEdit = null
                                            subfolderParentId = folder.id
                                            showCreateFolderDialog = true
                                        },
                                        onCopyClick = {
                                            copyMoveTargetFolder = folder
                                            copyMoveAction = CopyMoveAction.COPY_FOLDER
                                        },
                                        onMoveClick = {
                                            copyMoveTargetFolder = folder
                                            copyMoveAction = CopyMoveAction.MOVE_FOLDER
                                        },
                                        onToggleFavoriteClick = {
                                            viewModel.toggleFavoriteFolder(folder)
                                        },
                                        onTogglePinClick = {
                                            viewModel.togglePinFolder(folder)
                                        },
                                        onLockClick = {
                                            if (folder.isLocked) {
                                                viewModel.unlockFolder(folder)
                                            } else {
                                                folderToLock = folder
                                            }
                                        },
                                        onExportZipClick = {
                                            viewModel.exportFolderAsZip(context, folder)
                                        },
                                        onPublicUploadClick = {
                                            viewModel.uploadFolderToPublicDrive(context, folder)
                                        },
                                        onDeleteClick = {
                                            viewModel.moveToTrashFolder(folder)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Section 2: Files List
                    if (files.isNotEmpty()) {
                        item {
                            Text(
                                text = "الملفات والملاحظات المخزنة (${files.size}):",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(files) { fileItem ->
                            val isSelected = selectedFileIds.contains(fileItem.id)
                            val isUploading = uploadingItemIds.contains(fileItem.id)
                            FileListItem(
                                fileItem = fileItem,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                isUploading = isUploading,
                                onToggleSelection = { viewModel.toggleFileSelection(fileItem.id) },
                                onLongClick = { viewModel.startSelectionWithFile(fileItem.id) },
                                onClick = {
                                    selectedFileForViewer = fileItem
                                },
                                onCopyClick = {
                                    copyMoveTargetFile = fileItem
                                    copyMoveAction = CopyMoveAction.COPY_FILE
                                },
                                onMoveClick = {
                                    copyMoveTargetFile = fileItem
                                    copyMoveAction = CopyMoveAction.MOVE_FILE
                                },
                                onRenameClick = {
                                    fileToRename = fileItem
                                },
                                onShareClick = {
                                    shareFile(context, File(fileItem.path), fileItem.mimeType)
                                },
                                onPublicUploadClick = {
                                    viewModel.uploadFileToPublicDrive(context, fileItem)
                                },
                                onToggleFavoriteClick = {
                                    viewModel.toggleFavoriteFile(fileItem)
                                },
                                onTogglePinClick = {
                                    viewModel.togglePinFile(fileItem)
                                },
                                onDeleteClick = {
                                    viewModel.moveToTrashFile(fileItem)
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // Empty State if no folders and no files
                    if (folders.isEmpty() && files.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "لا توجد نتائج مطابقة للبحث"
                                        else if (currentFolderId == null) "مرحباً بك في تطبيق محاضراتي!"
                                        else "هذا المجلد فارغ حالياً",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "أنشئ أول مجلد افتراضي أو أضف ملاحظة نصية جديدة للبدء في تنظيم محاضراتك",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row {
                                        Button(
                                            onClick = {
                                                showCreateNoteDialog = true
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.NoteAdd, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إنشاء ملاحظة")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                folderToEdit = null
                                                subfolderParentId = currentFolderId
                                                showCreateFolderDialog = true
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إنشاء مجلد")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create / Edit Folder Dialog
        if (showCreateFolderDialog) {
            FolderDialog(
                initialFolder = folderToEdit,
                parentId = subfolderParentId,
                onDismiss = { showCreateFolderDialog = false },
                onSave = { name, description, color, size ->
                    if (folderToEdit == null) {
                        viewModel.createFolder(name, description, color, size, subfolderParentId)
                    } else {
                        viewModel.updateFolder(
                            folderToEdit!!.copy(
                                name = name,
                                description = description,
                                color = color,
                                size = size
                            )
                        )
                    }
                    showCreateFolderDialog = false
                }
            )
        }

        // Create Note Dialog
        if (showCreateNoteDialog) {
            CreateNoteDialog(
                onDismiss = { showCreateNoteDialog = false },
                onSaveNote = { title, content ->
                    viewModel.createTextNote(title, content, currentFolderId)
                    showCreateNoteDialog = false
                }
            )
        }

        // Copy / Move Destination Selector Dialog
        val act = copyMoveAction
        if (act != null) {
            val itemName = when (act) {
                CopyMoveAction.COPY_FOLDER, CopyMoveAction.MOVE_FOLDER -> copyMoveTargetFolder?.name ?: ""
                CopyMoveAction.COPY_FILE, CopyMoveAction.MOVE_FILE -> copyMoveTargetFile?.name ?: ""
            }
            val excludeId = if (act == CopyMoveAction.MOVE_FOLDER || act == CopyMoveAction.COPY_FOLDER) copyMoveTargetFolder?.id else null

            CopyMoveDialog(
                itemName = itemName,
                action = act,
                allFolders = allFolders,
                currentFolderId = currentFolderId,
                excludeFolderId = excludeId,
                onDismiss = {
                    copyMoveAction = null
                    copyMoveTargetFolder = null
                    copyMoveTargetFile = null
                },
                onConfirm = { targetFolderId ->
                    when (act) {
                        CopyMoveAction.COPY_FOLDER -> {
                            copyMoveTargetFolder?.let { viewModel.copyFolder(it, targetFolderId) }
                        }
                        CopyMoveAction.MOVE_FOLDER -> {
                            copyMoveTargetFolder?.let { viewModel.moveFolder(it, targetFolderId) }
                        }
                        CopyMoveAction.COPY_FILE -> {
                            val tfId = targetFolderId ?: currentFolderId
                            if (tfId != null && copyMoveTargetFile != null) {
                                viewModel.copyFile(copyMoveTargetFile!!, tfId)
                            } else {
                                Toast.makeText(context, "اختر مجلداً كوجهة للملف", Toast.LENGTH_SHORT).show()
                            }
                        }
                        CopyMoveAction.MOVE_FILE -> {
                            val tfId = targetFolderId ?: currentFolderId
                            if (tfId != null && copyMoveTargetFile != null) {
                                viewModel.moveFile(copyMoveTargetFile!!, tfId)
                            } else {
                                Toast.makeText(context, "اختر مجلداً كوجهة للملف", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    copyMoveAction = null
                    copyMoveTargetFolder = null
                    copyMoveTargetFile = null
                }
            )
        }

        // Trash Bin Dialog
        if (showTrashBinDialog) {
            TrashBinDialog(
                trashedFolders = trashedFolders,
                trashedFiles = trashedFiles,
                onDismiss = { showTrashBinDialog = false },
                onRestoreFolder = { viewModel.restoreFolder(it) },
                onRestoreFile = { viewModel.restoreFile(it) },
                onDeleteFolderPermanently = { viewModel.deleteFolderPermanently(it) },
                onDeleteFilePermanently = { viewModel.deleteFilePermanently(it) },
                onEmptyTrash = { viewModel.emptyTrash() }
            )
        }

        // Cascade Delete Warning Dialog (if needed)
        val currentSummary = deleteSummary
        if (currentSummary != null) {
            DeleteFolderDialog(
                summary = currentSummary,
                onDismiss = { viewModel.clearDeleteSummary() },
                onConfirmDelete = { viewModel.confirmDeleteFolder() }
            )
        }

        // File Upload Dialog
        if (showFileUploadDialog) {
            FileUploadDialog(
                selectedUris = selectedUploadUris,
                onPickMoreFiles = { filePickerLauncher.launch("*/*") },
                onRemoveUri = { uri -> selectedUploadUris = selectedUploadUris.filter { it != uri } },
                onDismiss = { showFileUploadDialog = false },
                onUploadConfirm = {
                    isUploadingFiles = true
                    viewModel.uploadFiles(
                        uris = selectedUploadUris,
                        targetFolderId = currentFolderId,
                        onProgress = { uploadProgress = it },
                        onComplete = {
                            isUploadingFiles = false
                            selectedUploadUris = emptyList()
                            showFileUploadDialog = false
                        }
                    )
                },
                isUploading = isUploadingFiles,
                uploadProgress = uploadProgress
            )
        }

        // Received Shared Content Dialog (from WhatsApp / external apps)
        if (pendingSharedUris.isNotEmpty() || pendingSharedText != null) {
            SharedImportDialog(
                allFolders = allFolders,
                incomingUris = pendingSharedUris,
                incomingText = pendingSharedText,
                onDismiss = { viewModel.clearPendingSharedContent() },
                onSaveSharedContent = { targetFolderId, textTitle ->
                    if (pendingSharedText != null) {
                        viewModel.uploadSharedText(pendingSharedText!!, textTitle, targetFolderId)
                    } else if (pendingSharedUris.isNotEmpty()) {
                        viewModel.uploadFiles(
                            uris = pendingSharedUris,
                            targetFolderId = targetFolderId,
                            onProgress = {},
                            onComplete = { viewModel.clearPendingSharedContent() }
                        )
                    }
                }
            )
        }
        // Set PIN Dialog
        val targetLock = folderToLock
        if (targetLock != null) {
            SetPinDialog(
                folderName = targetLock.name,
                onDismiss = { folderToLock = null },
                onConfirmPin = { pin ->
                    viewModel.lockFolder(targetLock, pin)
                    folderToLock = null
                }
            )
        }

        // Verify PIN Dialog
        val targetVerify = folderToVerifyPin
        if (targetVerify != null) {
            VerifyPinDialog(
                folderName = targetVerify.name,
                onDismiss = { folderToVerifyPin = null },
                onVerifyPin = { enteredPin ->
                    val isCorrect = viewModel.verifyFolderPin(targetVerify, enteredPin)
                    if (isCorrect) {
                        folderToVerifyPin = null
                        viewModel.navigateToFolder(targetVerify.id)
                    }
                    isCorrect
                }
            )
        }

        // In-App File Viewer Dialog
        val targetFileView = selectedFileForViewer
        if (targetFileView != null) {
            FileViewerDialog(
                fileItem = targetFileView,
                onDismiss = { selectedFileForViewer = null },
                onSaveTextContent = { newContent ->
                    viewModel.updateTextContent(targetFileView, newContent)
                }
            )
        }

        // Rename File Dialog
        val targetRenameFile = fileToRename
        if (targetRenameFile != null) {
            RenameFileDialog(
                currentName = targetRenameFile.name,
                onDismiss = { fileToRename = null },
                onConfirmRename = { newName ->
                    viewModel.renameFile(targetRenameFile, newName)
                    fileToRename = null
                }
            )
        }

        // Batch Copy / Move Dialog
        if (isBatchCopyMoveShow && copyMoveAction != null) {
            val totalSelected = selectedFolderIds.size + selectedFileIds.size
            CopyMoveDialog(
                itemName = "العناصر المحددة ($totalSelected)",
                action = copyMoveAction!!,
                allFolders = allFolders,
                currentFolderId = currentFolderId,
                onDismiss = {
                    isBatchCopyMoveShow = false
                    copyMoveAction = null
                },
                onConfirm = { targetFolderId ->
                    if (copyMoveAction == CopyMoveAction.COPY_FILE || copyMoveAction == CopyMoveAction.COPY_FOLDER) {
                        viewModel.batchCopySelected(targetFolderId)
                    } else {
                        viewModel.batchMoveSelected(targetFolderId)
                    }
                    isBatchCopyMoveShow = false
                    copyMoveAction = null
                }
            )
        }

        // Notification Center Dialog
        if (showNotificationCenterDialog) {
            NotificationCenterDialog(
                notification = appNotification,
                onDismiss = { showNotificationCenterDialog = false },
                onRefresh = {
                    viewModel.checkDevicePingAndNotifications(context)
                }
            )
        }

        // Wi-Fi Local APK Share Dialog
        if (showWifiShareDialog) {
            WifiShareDialog(
                onDismiss = { showWifiShareDialog = false }
            )
        }

        // Academic Setup Dialog
        if (showAcademicSetupDialog) {
            AcademicSetupDialog(
                onConfirm = { dept, sem ->
                    viewModel.setupAcademicFolders(dept, sem)
                },
                onDismiss = { showAcademicSetupDialog = false }
            )
        }

        // Notification Center Dialog
        if (showNotificationCenterDialog) {
            NotificationCenterDialog(
                notification = appNotification,
                onDismiss = { showNotificationCenterDialog = false },
                onRefresh = { viewModel.checkDevicePingAndNotifications(context) }
            )
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                currentLayoutMode = layoutMode,
                customBgColorHex = customBgColorHex,
                customBgImageUri = customBgImageUri,
                presetBgType = presetBgType,
                onSelectLayoutMode = { viewModel.setLayoutMode(it) },
                onSelectCustomColor = { viewModel.setCustomBgColorHex(it) },
                onSelectCustomImageUri = { viewModel.setCustomBgImageUri(it) },
                onSelectPresetBg = { viewModel.setPresetBgType(it) },
                onResetBackground = { viewModel.resetBackground() },
                onOpenPinLock = {
                    folderToLock = FolderEntity(
                        id = "APP_GLOBAL_PIN",
                        name = "التطبيق بأكمله",
                        parentId = null,
                        createdAt = System.currentTimeMillis()
                    )
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
            }
    }
}

private fun shareFile(context: android.content.Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الملف عبر"))
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر مشاركة الملف", Toast.LENGTH_SHORT).show()
    }
}

private fun openFileWithApp(context: android.content.Context, fileItem: FileItemEntity) {
    try {
        val file = File(fileItem.path)
        if (!file.exists()) {
            Toast.makeText(context, "الملف غير موجود في التخزين المحلي", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, fileItem.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "فتح بواسطة"))
    } catch (e: Exception) {
        Toast.makeText(context, "لا يوجد تطبيق مثبت لتشغيل هذا الملف", Toast.LENGTH_SHORT).show()
    }
}
