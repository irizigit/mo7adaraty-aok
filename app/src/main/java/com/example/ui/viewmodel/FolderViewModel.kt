package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VirtualFolderRepository
import com.example.data.local.AppDatabase
import com.example.data.local.FileItemEntity
import com.example.data.local.FolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LayoutMode {
    GRID,
    LIST,
    HORIZONTAL
}

enum class AppBackgroundTheme {
    DEFAULT,
    SOLID_DARK,
    TEXTURE_PAPER
}

enum class SearchCategory {
    ALL,
    FOLDERS,
    FAVORITES,
    DOCUMENTS,
    MEDIA,
    TRASH
}

@OptIn(ExperimentalCoroutinesApi::class)
class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VirtualFolderRepository(
        context = application,
        folderDao = AppDatabase.getInstance(application).folderDao()
    )

    init {
        // Auto-purge items in trash older than 30 days
        viewModelScope.launch(Dispatchers.IO) {
            repository.autoPurgeOldTrash()
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanupExpiredTrash(30)
        }
    }

    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    private val prefs = getApplication<Application>().getSharedPreferences("app_appearance_prefs", android.content.Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _layoutMode = MutableStateFlow(LayoutMode.GRID)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    private val _appBackground = MutableStateFlow(AppBackgroundTheme.DEFAULT)
    val appBackground: StateFlow<AppBackgroundTheme> = _appBackground.asStateFlow()

    private val _customBgColorHex = MutableStateFlow<String?>(prefs.getString("bg_color_hex", null))
    val customBgColorHex: StateFlow<String?> = _customBgColorHex.asStateFlow()

    private val _customBgImageUri = MutableStateFlow<String?>(prefs.getString("bg_image_uri", null))
    val customBgImageUri: StateFlow<String?> = _customBgImageUri.asStateFlow()

    private val _presetBgType = MutableStateFlow<String?>(prefs.getString("preset_bg_type", "DEFAULT"))
    val presetBgType: StateFlow<String?> = _presetBgType.asStateFlow()

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
    }

    fun setAppBackground(theme: AppBackgroundTheme) {
        _appBackground.value = theme
    }

    fun setCustomBgColorHex(hex: String?) {
        _customBgColorHex.value = hex
        _customBgImageUri.value = null
        _presetBgType.value = null
        prefs.edit().putString("bg_color_hex", hex).remove("bg_image_uri").remove("preset_bg_type").apply()
    }

    fun setCustomBgImageUri(uriString: String?) {
        _customBgImageUri.value = uriString
        _customBgColorHex.value = null
        _presetBgType.value = null
        prefs.edit().putString("bg_image_uri", uriString).remove("bg_color_hex").remove("preset_bg_type").apply()
    }

    fun setPresetBgType(type: String) {
        _presetBgType.value = type
        _customBgColorHex.value = null
        _customBgImageUri.value = null
        prefs.edit().putString("preset_bg_type", type).remove("bg_color_hex").remove("bg_image_uri").apply()
    }

    fun resetBackground() {
        _presetBgType.value = "DEFAULT"
        _customBgColorHex.value = null
        _customBgImageUri.value = null
        _appBackground.value = AppBackgroundTheme.DEFAULT
        prefs.edit().clear().apply()
    }

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _breadcrumbs = MutableStateFlow<List<FolderEntity>>(emptyList())
    val breadcrumbs: StateFlow<List<FolderEntity>> = _breadcrumbs.asStateFlow()

    // Search and filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(SearchCategory.ALL)
    val selectedCategory: StateFlow<SearchCategory> = _selectedCategory.asStateFlow()

    // Trashed Items
    val trashedFolders: StateFlow<List<FolderEntity>> = repository.getTrashedFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedFiles: StateFlow<List<FileItemEntity>> = repository.getTrashedFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentFolders: StateFlow<List<FolderEntity>> = combine(
        _currentFolderId,
        _searchQuery,
        _selectedCategory
    ) { folderId, query, category ->
        Triple(folderId, query, category)
    }.flatMapLatest { (folderId, query, category) ->
        if (category == SearchCategory.FAVORITES) {
            repository.getFavoriteFolders()
        } else if (query.isNotBlank()) {
            repository.searchFolders(query)
        } else {
            repository.getFoldersByParent(folderId)
        }
    }.combine(_selectedCategory) { folders, category ->
        when (category) {
            SearchCategory.DOCUMENTS, SearchCategory.MEDIA -> emptyList()
            else -> folders
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentFiles: StateFlow<List<FileItemEntity>> = combine(
        _currentFolderId,
        _searchQuery,
        _selectedCategory
    ) { folderId, query, category ->
        Triple(folderId, query, category)
    }.flatMapLatest { (folderId, query, category) ->
        if (category == SearchCategory.FAVORITES) {
            repository.getFavoriteFiles()
        } else if (query.isNotBlank()) {
            repository.searchFiles(query)
        } else {
            if (folderId == null) repository.getFilesByFolder("ROOT_NONE")
            else repository.getFilesByFolder(folderId)
        }
    }.combine(_selectedCategory) { files, category ->
        when (category) {
            SearchCategory.FOLDERS -> emptyList()
            SearchCategory.DOCUMENTS -> files.filter { it.mimeType.startsWith("text/") || it.mimeType.contains("pdf") || it.mimeType.contains("document") }
            SearchCategory.MEDIA -> files.filter { it.mimeType.startsWith("image/") || it.mimeType.startsWith("video/") || it.mimeType.startsWith("audio/") }
            else -> files
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<FolderEntity>> = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFiles: StateFlow<List<FileItemEntity>> = repository.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subfolderCounts: StateFlow<Map<String, Int>> = allFolders.map { folders ->
        val map = mutableMapOf<String, Int>()
        folders.forEach { f ->
            val pid = f.parentId
            if (pid != null) {
                map[pid] = (map[pid] ?: 0) + 1
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val fileCounts: StateFlow<Map<String, Int>> = allFiles.map { files ->
        val map = mutableMapOf<String, Int>()
        files.forEach { file ->
            map[file.folderId] = (map[file.folderId] ?: 0) + 1
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Pending incoming intents from other apps
    private val _pendingSharedUris = MutableStateFlow<List<Uri>>(emptyList())
    val pendingSharedUris: StateFlow<List<Uri>> = _pendingSharedUris.asStateFlow()

    private val _pendingSharedText = MutableStateFlow<String?>(null)
    val pendingSharedText: StateFlow<String?> = _pendingSharedText.asStateFlow()

    private val _deleteSummary = MutableStateFlow<VirtualFolderRepository.DeleteCascadeSummary?>(null)
    val deleteSummary: StateFlow<VirtualFolderRepository.DeleteCascadeSummary?> = _deleteSummary.asStateFlow()

    // --- Multi-Selection Mode State ---
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedFolderIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolderIds: StateFlow<Set<String>> = _selectedFolderIds.asStateFlow()

    private val _selectedFileIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFileIds: StateFlow<Set<String>> = _selectedFileIds.asStateFlow()

    fun startSelectionWithFolder(folderId: String) {
        _isSelectionMode.value = true
        _selectedFolderIds.value = setOf(folderId)
    }

    fun startSelectionWithFile(fileId: String) {
        _isSelectionMode.value = true
        _selectedFileIds.value = setOf(fileId)
    }

    fun toggleFolderSelection(folderId: String) {
        val current = _selectedFolderIds.value.toMutableSet()
        if (current.contains(folderId)) {
            current.remove(folderId)
        } else {
            current.add(folderId)
        }
        _selectedFolderIds.value = current

        // Auto exit if nothing is selected
        if (_selectedFolderIds.value.isEmpty() && _selectedFileIds.value.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun toggleFileSelection(fileId: String) {
        val current = _selectedFileIds.value.toMutableSet()
        if (current.contains(fileId)) {
            current.remove(fileId)
        } else {
            current.add(fileId)
        }
        _selectedFileIds.value = current

        if (_selectedFolderIds.value.isEmpty() && _selectedFileIds.value.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun toggleSelectAll(allFoldersInView: List<FolderEntity>, allFilesInView: List<FileItemEntity>) {
        val allFolderIdsInView = allFoldersInView.map { it.id }.toSet()
        val allFileIdsInView = allFilesInView.map { it.id }.toSet()

        val currentlyAllSelected = _selectedFolderIds.value.containsAll(allFolderIdsInView) &&
                _selectedFileIds.value.containsAll(allFileIdsInView)

        if (currentlyAllSelected) {
            _selectedFolderIds.value = emptySet()
            _selectedFileIds.value = emptySet()
        } else {
            _selectedFolderIds.value = allFolderIdsInView
            _selectedFileIds.value = allFileIdsInView
            _isSelectionMode.value = true
        }
    }

    fun clearSelection() {
        _isSelectionMode.value = false
        _selectedFolderIds.value = emptySet()
        _selectedFileIds.value = emptySet()
    }

    // --- Batch Actions ---
    fun batchMoveToTrash() {
        val folderIds = _selectedFolderIds.value.toList()
        val fileIds = _selectedFileIds.value.toList()
        val foldersToTrash = currentFolders.value.filter { folderIds.contains(it.id) }
        val filesToTrash = currentFiles.value.filter { fileIds.contains(it.id) }

        viewModelScope.launch(Dispatchers.IO) {
            foldersToTrash.forEach { repository.moveToTrashFolder(it) }
            filesToTrash.forEach { repository.moveToTrashFile(it) }
            val count = foldersToTrash.size + filesToTrash.size
            _toastMessage.emit("تم نقل $count عنصر إلى سلة المهملات 🗑️")
            clearSelection()
        }
    }

    fun batchToggleFavorite() {
        val folderIds = _selectedFolderIds.value.toList()
        val fileIds = _selectedFileIds.value.toList()
        val targetFolders = currentFolders.value.filter { folderIds.contains(it.id) }
        val targetFiles = currentFiles.value.filter { fileIds.contains(it.id) }

        viewModelScope.launch(Dispatchers.IO) {
            targetFolders.forEach { repository.toggleFavoriteFolder(it) }
            targetFiles.forEach { repository.toggleFavoriteFile(it) }
            _toastMessage.emit("تم تحديث المفضلة للعناصر المحددة ⭐")
            clearSelection()
        }
    }

    fun batchCopySelected(targetFolderId: String?) {
        val folderIds = _selectedFolderIds.value.toList()
        val fileIds = _selectedFileIds.value.toList()
        val targetFolders = allFolders.value.filter { folderIds.contains(it.id) }
        val targetFiles = allFiles.value.filter { fileIds.contains(it.id) }

        viewModelScope.launch(Dispatchers.IO) {
            targetFolders.forEach { repository.copyFolder(it, targetFolderId) }
            targetFiles.forEach { repository.copyFile(it, targetFolderId ?: "ROOT_NONE") }
            val count = targetFolders.size + targetFiles.size
            _toastMessage.emit("تم نسخ $count عنصر بنجاح 📋")
            clearSelection()
        }
    }

    fun batchMoveSelected(targetFolderId: String?) {
        val folderIds = _selectedFolderIds.value.toList()
        val fileIds = _selectedFileIds.value.toList()
        val targetFolders = allFolders.value.filter { folderIds.contains(it.id) }
        val targetFiles = allFiles.value.filter { fileIds.contains(it.id) }

        viewModelScope.launch(Dispatchers.IO) {
            targetFolders.forEach { repository.moveFolder(it, targetFolderId) }
            targetFiles.forEach { repository.moveFile(it, targetFolderId ?: "ROOT_NONE") }
            val count = targetFolders.size + targetFiles.size
            _toastMessage.emit("تم نقل $count عنصر بنجاح 🚚")
            clearSelection()
        }
    }

    // --- App Notifications & Device Ping ---
    private val _appNotification = MutableStateFlow<com.example.util.AppNotification?>(
        com.example.util.AppNotification(
            hasNotification = true,
            title = "تحديث جديد للتطبيق متوفر 🚀",
            message = "يتوفر إصدار وتحديث جديد لتطبيق محاضراتي مع أحدث الميزات والتحسينات المضافة.",
            appUrl = "https://irizi.unaux.com/mo7adaraty-apk/index.php"
        )
    )
    val appNotification: StateFlow<com.example.util.AppNotification?> = _appNotification.asStateFlow()

    fun checkDevicePingAndNotifications(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val deviceId = com.example.util.DeviceManager.getDeviceId(context)
            // Send ping API request with device_id
            com.example.util.NetworkService.sendPing(context, deviceId)
            // Fetch notifications
            val notification = com.example.util.NetworkService.fetchNotification(context)
            if (notification.hasNotification && notification.appUrl.isNotBlank()) {
                _appNotification.value = notification
            } else {
                _appNotification.value = com.example.util.AppNotification(
                    hasNotification = true,
                    title = "تحديث جديد للتطبيق متوفر 🚀",
                    message = "يتوفر إصدار وتحديث جديد لتطبيق محاضراتي مع أحدث الميزات والتحسينات المضافة.",
                    appUrl = "https://irizi.unaux.com/mo7adaraty-apk/index.php"
                )
            }
        }
    }

    fun dismissNotification() {
        _appNotification.value = null
    }

    // --- File Rename ---
    fun renameFile(fileEntity: FileItemEntity, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.renameFile(fileEntity, newName)
            _toastMessage.emit("تم تغيير اسم الملف بنجاح ✏️")
        }
    }

    // --- Favorites & Pinning ---
    fun toggleFavoriteFolder(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavoriteFolder(folder)
            val msg = if (!folder.isFavorite) "تمت إضافة المجلد للمفضلة ⭐" else "تمت إزالة المجلد من المفضلة"
            _toastMessage.emit(msg)
        }
    }

    fun toggleFavoriteFile(file: FileItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavoriteFile(file)
            val msg = if (!file.isFavorite) "تمت إضافة الملف للمفضلة ⭐" else "تمت إزالة الملف من المفضلة"
            _toastMessage.emit(msg)
        }
    }

    fun togglePinFolder(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePinFolder(folder)
            _toastMessage.emit(if (folder.isPinned) "تم إلغاء التثبيت" else "تم تثبيت المجلد في الأعلى 📌")
        }
    }

    fun togglePinFile(file: FileItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePinFile(file)
            _toastMessage.emit(if (file.isPinned) "تم إلغاء التثبيت" else "تم تثبيت الملف في الأعلى 📌")
        }
    }

    fun saveAuthToken(context: android.content.Context, token: String) {
        val prefs = context.getSharedPreferences("mo7adaraty_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("auth_token", token).apply()
        viewModelScope.launch {
            _toastMessage.emit("تم تسجيل الدخول بنجاح عبر المصادقة العميقة! 🔑")
        }
    }

    fun batchShareSelected(context: android.content.Context) {
        val fileIds = _selectedFileIds.value.toList()
        val targetFiles = allFiles.value.filter { fileIds.contains(it.id) }
        if (targetFiles.isEmpty()) {
            viewModelScope.launch { _toastMessage.emit("اختر ملفاً واحداً على الأقل للمشاركة") }
            return
        }

        val uris = ArrayList<android.net.Uri>()
        for (fileEntity in targetFiles) {
            try {
                val f = java.io.File(fileEntity.path)
                if (f.exists()) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        f
                    )
                    uris.add(uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (uris.isNotEmpty()) {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة الملفات المحددة"))
        }
    }

    fun exportSelectedAsZip(context: android.content.Context, zipName: String = "Mo7adaraty_Batch") {
        viewModelScope.launch(Dispatchers.IO) {
            val folderIds = _selectedFolderIds.value.toList()
            val fileIds = _selectedFileIds.value.toList()
            if (folderIds.isEmpty() && fileIds.isEmpty()) {
                _toastMessage.emit("يرجى تحديد عناصر لضغطها")
                return@launch
            }
            val zipFile = repository.createZipArchive(zipName, folderIds, fileIds)
            if (zipFile != null && zipFile.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة الأرشيف المضغوط ZIP"))
                _toastMessage.emit("تم إنشاء الملف المضغوط بنجاح 📦")
            } else {
                _toastMessage.emit("فشل إنشاء ملف ZIP")
            }
        }
    }

    fun exportFolderAsZip(context: android.content.Context, folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val zipFile = repository.createZipArchive(folder.name, listOf(folder.id), emptyList())
            if (zipFile != null && zipFile.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة المجلد المضغوط"))
                _toastMessage.emit("تم تصدير المجلد \"${folder.name}\" كملف ZIP بنجاح")
            } else {
                _toastMessage.emit("فشل تصدير المجلد كـ ZIP")
            }
        }
    }

    fun setupAcademicFolders(department: String, semester: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setupAcademicFolders(department, semester)
            _toastMessage.emit("تم إنشاء الهيكل الأكاديمي لشعبة $department بنجاح 🎓")
        }
    }

    // --- Folder Security ---
    fun lockFolder(folder: FolderEntity, pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.lockFolder(folder, pin)
            _toastMessage.emit("تم قفل المجلد \"${folder.name}\" بنجاح 🔒")
        }
    }

    fun unlockFolder(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.unlockFolder(folder)
            _toastMessage.emit("تم إلغاء قفل المجلد \"${folder.name}\" 🔓")
        }
    }

    fun verifyFolderPin(folder: FolderEntity, enteredPin: String): Boolean {
        return repository.verifyFolderPin(folder, enteredPin)
    }

    // --- Text File Edit ---
    fun updateTextContent(fileEntity: FileItemEntity, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTextFileContent(fileEntity, newContent)
            _toastMessage.emit("تم حفظ الملاحظة النصية بنجاح 💾")
        }
    }

    // --- ZIP Export ---
    fun exportFolderAsZip(
        context: android.content.Context,
        folder: FolderEntity,
        onProgress: (Float) -> Unit,
        onComplete: (java.io.File?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val zipFile = com.example.util.ZipUtils.createZipFromFolder(
                context = context,
                repository = repository,
                rootFolder = folder,
                onProgress = onProgress
            )
            onComplete(zipFile)
            if (zipFile != null) {
                _toastMessage.emit("تم ضغط المجلد \"${folder.name}\" بنجاح 📦")
            } else {
                _toastMessage.emit("حدث خطأ أثناء ضغط المجلد")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: SearchCategory) {
        _selectedCategory.value = category
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun navigateToFolder(folderId: String?) {
        viewModelScope.launch {
            _currentFolderId.value = folderId
            _breadcrumbs.value = repository.getBreadcrumbPath(folderId)
        }
    }

    fun createFolder(name: String, description: String, color: String, size: String, parentId: String? = _currentFolderId.value) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertFolder(
                name = name,
                description = description,
                color = color,
                size = size,
                parentId = parentId
            )
            _toastMessage.emit("تم إنشاء المجلد \"$name\" بنجاح")
        }
    }

    fun updateFolder(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFolder(folder)
            _toastMessage.emit("تم تعديل المجلد بنجاح")
        }
    }

    fun prepareDeleteFolder(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val summary = repository.getCascadeSummary(folder)
            _deleteSummary.value = summary
        }
    }

    fun clearDeleteSummary() {
        _deleteSummary.value = null
    }

    fun confirmDeleteFolder() {
        val summary = _deleteSummary.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFolderCascade(summary.targetFolder)
            _toastMessage.emit("تم حذف المجلد \"${summary.targetFolder.name}\" بجميع محتوياته")
            _deleteSummary.value = null
            if (_currentFolderId.value == summary.targetFolder.id) {
                navigateToFolder(summary.targetFolder.parentId)
            }
        }
    }

    // Move to Trash
    fun moveToTrashFolder(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveToTrashFolder(folder)
            _toastMessage.emit("تم نقل المجلد \"${folder.name}\" إلى سلة المهملات")
        }
    }

    fun restoreFolder(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreFolder(folder)
            _toastMessage.emit("تمت استعادة المجلد \"${folder.name}\"")
        }
    }

    fun moveToTrashFile(file: FileItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveToTrashFile(file)
            _toastMessage.emit("تم نقل الملف \"${file.name}\" إلى سلة المهملات")
        }
    }

    fun restoreFile(file: FileItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreFile(file)
            _toastMessage.emit("تمت استعادة الملف \"${file.name}\"")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.emptyTrash()
            _toastMessage.emit("تم تفريغ سلة المهملات بالكامل")
        }
    }

    fun deleteFolderPermanently(folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFolderCascade(folder)
            _toastMessage.emit("تم حذف المجلد نهائياً")
        }
    }

    fun deleteFilePermanently(file: FileItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFilePermanently(file)
            _toastMessage.emit("تم حذف الملف نهائياً")
        }
    }

    // Copy & Move Operations
    fun moveFolder(folder: FolderEntity, targetParentId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveFolder(folder, targetParentId)
            _toastMessage.emit("تم نقل المجلد \"${folder.name}\" بنجاح")
        }
    }

    fun copyFolder(folder: FolderEntity, targetParentId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.copyFolder(folder, targetParentId)
            _toastMessage.emit("تم نسخ المجلد \"${folder.name}\" بنجاح")
        }
    }

    fun moveFile(file: FileItemEntity, targetFolderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveFile(file, targetFolderId)
            _toastMessage.emit("تم نقل الملف \"${file.name}\" بنجاح")
        }
    }

    fun copyFile(file: FileItemEntity, targetFolderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.copyFile(file, targetFolderId)
            _toastMessage.emit("تم نسخ الملف \"${file.name}\" بنجاح")
        }
    }

    fun createTextNote(title: String, content: String, targetFolderId: String? = _currentFolderId.value) {
        val folderId = targetFolderId ?: run {
            viewModelScope.launch { _toastMessage.emit("يرجى دخول مجلد أولاً لحفظ الملاحظة فيه") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.saveTextToFile(title, content, folderId)
            if (result != null) {
                _toastMessage.emit("تم حفظ الملاحظة النصية بنجاح")
            } else {
                _toastMessage.emit("فشل حفظ الملاحظة")
            }
        }
    }

    fun uploadFiles(uris: List<Uri>, targetFolderId: String? = _currentFolderId.value, onProgress: (Float) -> Unit, onComplete: () -> Unit) {
        val folderId = targetFolderId ?: run {
            viewModelScope.launch { _toastMessage.emit("يرجى اختيار أو دخول مجلد لرفع الملفات فيه") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            uris.forEachIndexed { index, uri ->
                val result = repository.saveUriToFile(uri, folderId)
                if (result != null) successCount++
                onProgress((index + 1).toFloat() / uris.size)
            }
            _toastMessage.emit("تم رفع $successCount ملف بنجاح")
            onComplete()
        }
    }

    fun uploadSharedText(text: String, title: String?, targetFolderId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val folderId = targetFolderId ?: run {
                val allF = allFolders.value
                val existing = allF.firstOrNull { it.parentId == null }
                if (existing != null) {
                    existing.id
                } else {
                    repository.insertFolder("الملاحظات المستلمة", "مجلد تلقائي للنصوص المشاركة", "#6366F1", "medium", null)
                    val newF = repository.getAllFoldersSync().first { it.name == "الملاحظات المستلمة" }
                    newF.id
                }
            }

            repository.saveTextToFile(
                title = title ?: "نص مشارك",
                textContent = text,
                folderId = folderId
            )
            _toastMessage.emit("تم حفظ النص المشارك كملف بنجاح")
            _pendingSharedText.value = null
        }
    }

    fun setPendingSharedContent(uris: List<Uri>, text: String?) {
        _pendingSharedUris.value = uris
        _pendingSharedText.value = text
    }

    fun clearPendingSharedContent() {
        _pendingSharedUris.value = emptyList()
        _pendingSharedText.value = null
    }

    // --- Google Drive Public Upload ---
    private val _uploadingItemIds = MutableStateFlow<Set<String>>(emptySet())
    val uploadingItemIds: StateFlow<Set<String>> = _uploadingItemIds.asStateFlow()

    fun uploadFolderToPublicDrive(context: android.content.Context, folder: FolderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _uploadingItemIds.value = _uploadingItemIds.value + folder.id
            _toastMessage.emit("جاري تجهيز المجلد والرفع للعامة على Google Drive...")
            
            val zipFile = repository.exportFolderToZipSync(folder)
            if (zipFile == null) {
                _uploadingItemIds.value = _uploadingItemIds.value - folder.id
                _toastMessage.emit("فشل ضغط المجلد للرفع")
                return@launch
            }

            val driveLink = com.example.util.DriveAndCommunityService.uploadPublicDriveContent(
                context = context,
                file = zipFile,
                title = folder.name,
                description = "مجلد عام: ${folder.name}",
                authorName = "طالب محاضراتي"
            )

            _uploadingItemIds.value = _uploadingItemIds.value - folder.id
            if (driveLink != null) {
                _toastMessage.emit("تم الرفع للعامة بنجاح! الرابط: $driveLink")
            } else {
                _toastMessage.emit("فشل رفع المجلد للعامة")
            }
        }
    }

    fun uploadFileToPublicDrive(context: android.content.Context, fileItem: FileItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _uploadingItemIds.value = _uploadingItemIds.value + fileItem.id
            _toastMessage.emit("جاري رفع الملف للعامة على Google Drive...")

            val file = java.io.File(fileItem.path)
            if (!file.exists()) {
                _uploadingItemIds.value = _uploadingItemIds.value - fileItem.id
                _toastMessage.emit("الملف غير موجود بالذاكرة")
                return@launch
            }

            val driveLink = com.example.util.DriveAndCommunityService.uploadPublicDriveContent(
                context = context,
                file = file,
                title = fileItem.name,
                description = "ملف عام: ${fileItem.name}",
                authorName = "طالب محاضراتي"
            )

            _uploadingItemIds.value = _uploadingItemIds.value - fileItem.id
            if (driveLink != null) {
                _toastMessage.emit("تم الرفع للعامة بنجاح! الرابط: $driveLink")
            } else {
                _toastMessage.emit("فشل رفع الملف للعامة")
            }
        }
    }
}
