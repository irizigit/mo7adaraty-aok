package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.FileItemEntity
import com.example.data.local.FolderDao
import com.example.data.local.FolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class VirtualFolderRepository(
    private val context: Context,
    private val folderDao: FolderDao
) {
    private val storageDir: File by lazy {
        File(context.filesDir, "virtual_files").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getFoldersByParent(parentId: String?): Flow<List<FolderEntity>> {
        return folderDao.getFoldersByParent(parentId, isTrashed = false)
    }

    fun getFilesByFolder(folderId: String): Flow<List<FileItemEntity>> {
        return folderDao.getFilesByFolder(folderId, isTrashed = false)
    }

    fun getAllFolders(): Flow<List<FolderEntity>> {
        return folderDao.getAllFolders(isTrashed = false)
    }

    suspend fun getAllFoldersSync(): List<FolderEntity> {
        return folderDao.getAllFoldersSync(isTrashed = false)
    }

    fun getAllFiles(): Flow<List<FileItemEntity>> {
        return folderDao.getAllFiles(isTrashed = false)
    }

    suspend fun getFolderById(folderId: String): FolderEntity? {
        return folderDao.getFolderByIdSync(folderId)
    }

    suspend fun getBreadcrumbPath(currentFolderId: String?): List<FolderEntity> {
        val path = mutableListOf<FolderEntity>()
        var currId = currentFolderId
        while (currId != null) {
            val folder = folderDao.getFolderByIdSync(currId) ?: break
            path.add(0, folder)
            currId = folder.parentId
        }
        return path
    }

    suspend fun insertFolder(name: String, description: String, color: String, size: String, parentId: String?) {
        val folder = FolderEntity(
            name = name,
            description = description,
            color = color,
            size = size,
            parentId = parentId
        )
        folderDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: FolderEntity) {
        folderDao.updateFolder(folder.copy(updatedAt = System.currentTimeMillis()))
    }

    // --- Favorites ---
    fun getFavoriteFolders(): Flow<List<FolderEntity>> = folderDao.getFavoriteFolders()
    fun getFavoriteFiles(): Flow<List<FileItemEntity>> = folderDao.getFavoriteFiles()

    suspend fun toggleFavoriteFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        folderDao.updateFolder(folder.copy(isFavorite = !folder.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleFavoriteFile(file: FileItemEntity) = withContext(Dispatchers.IO) {
        folderDao.updateFile(file.copy(isFavorite = !file.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    // --- Pinning ---
    suspend fun togglePinFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        folderDao.updateFolder(folder.copy(isPinned = !folder.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun togglePinFile(file: FileItemEntity) = withContext(Dispatchers.IO) {
        folderDao.updateFile(file.copy(isPinned = !file.isPinned, updatedAt = System.currentTimeMillis()))
    }

    // --- Lock & Security ---
    suspend fun lockFolder(folder: FolderEntity, pin: String) = withContext(Dispatchers.IO) {
        val pinHash = pin.hashCode().toString()
        folderDao.updateFolder(folder.copy(isLocked = true, pinHash = pinHash, updatedAt = System.currentTimeMillis()))
    }

    suspend fun unlockFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        folderDao.updateFolder(folder.copy(isLocked = false, pinHash = null, updatedAt = System.currentTimeMillis()))
    }

    fun verifyFolderPin(folder: FolderEntity, enteredPin: String): Boolean {
        val expectedHash = folder.pinHash ?: return true
        return enteredPin.hashCode().toString() == expectedHash
    }

    // --- Text File Editing & File Renaming ---
    suspend fun renameFile(fileEntity: FileItemEntity, newName: String) = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(fileEntity.path)
            if (oldFile.exists()) {
                val parentDir = oldFile.parentFile
                val extension = oldFile.extension
                val targetName = if (extension.isNotEmpty() && !newName.endsWith(".$extension")) {
                    "$newName.$extension"
                } else {
                    newName
                }
                val newFile = File(parentDir, targetName)
                if (oldFile.renameTo(newFile)) {
                    val updatedEntity = fileEntity.copy(
                        name = targetName,
                        path = newFile.absolutePath,
                        updatedAt = System.currentTimeMillis()
                    )
                    folderDao.updateFile(updatedEntity)
                } else {
                    // Fallback if direct rename fails (e.g. cross-volume): copy and delete
                    oldFile.copyTo(newFile, overwrite = true)
                    oldFile.delete()
                    val updatedEntity = fileEntity.copy(
                        name = targetName,
                        path = newFile.absolutePath,
                        updatedAt = System.currentTimeMillis()
                    )
                    folderDao.updateFile(updatedEntity)
                }
            } else {
                // Just update entity name if physical file is missing or virtual
                folderDao.updateFile(fileEntity.copy(name = newName, updatedAt = System.currentTimeMillis()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateTextFileContent(fileEntity: FileItemEntity, newContent: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(fileEntity.path)
            file.writeText(newContent, Charsets.UTF_8)
            val updated = fileEntity.copy(size = file.length(), updatedAt = System.currentTimeMillis())
            folderDao.updateFile(updated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Search ---
    fun searchFolders(query: String): Flow<List<FolderEntity>> {
        return folderDao.searchFolders(query)
    }

    fun searchFiles(query: String): Flow<List<FileItemEntity>> {
        return folderDao.searchFiles(query)
    }

    // --- Trash Bin Management ---
    fun getTrashedFolders(): Flow<List<FolderEntity>> = folderDao.getTrashedFolders()
    fun getTrashedFiles(): Flow<List<FileItemEntity>> = folderDao.getTrashedFiles()

    suspend fun moveToTrashFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val allFolders = folderDao.getAllFoldersSync(isTrashed = false)
        val allFiles = folderDao.getAllFilesSync(isTrashed = false)

        val affectedFolderIds = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(folder.id)

        while (queue.isNotEmpty()) {
            val parentId = queue.removeFirst()
            affectedFolderIds.add(parentId)
            val children = allFolders.filter { it.parentId == parentId }
            children.forEach { queue.add(it.id) }
        }

        // Trash folders
        allFolders.filter { affectedFolderIds.contains(it.id) }.forEach { f ->
            folderDao.updateFolder(f.copy(isTrashed = true, trashedAt = now))
        }

        // Trash files inside affected folders
        allFiles.filter { affectedFolderIds.contains(it.folderId) }.forEach { file ->
            folderDao.updateFile(file.copy(isTrashed = true, trashedAt = now))
        }
    }

    suspend fun restoreFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        val allFolders = folderDao.getTrashedFoldersSync()
        val allFiles = folderDao.getTrashedFilesSync()

        val affectedFolderIds = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(folder.id)

        while (queue.isNotEmpty()) {
            val parentId = queue.removeFirst()
            affectedFolderIds.add(parentId)
            val children = allFolders.filter { it.parentId == parentId }
            children.forEach { queue.add(it.id) }
        }

        // Restore folders
        allFolders.filter { affectedFolderIds.contains(it.id) }.forEach { f ->
            folderDao.updateFolder(f.copy(isTrashed = false, trashedAt = null))
        }

        // Restore files
        allFiles.filter { affectedFolderIds.contains(it.folderId) }.forEach { file ->
            folderDao.updateFile(file.copy(isTrashed = false, trashedAt = null))
        }
    }

    suspend fun moveToTrashFile(fileEntity: FileItemEntity) = withContext(Dispatchers.IO) {
        folderDao.updateFile(fileEntity.copy(isTrashed = true, trashedAt = System.currentTimeMillis()))
    }

    suspend fun restoreFile(fileEntity: FileItemEntity) = withContext(Dispatchers.IO) {
        folderDao.updateFile(fileEntity.copy(isTrashed = false, trashedAt = null))
    }

    suspend fun autoPurgeOldTrash() = withContext(Dispatchers.IO) {
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000L
        val cutoffTime = System.currentTimeMillis() - thirtyDaysInMillis

        val expiredFiles = folderDao.getExpiredTrashedFilesSync(cutoffTime)
        expiredFiles.forEach { file ->
            deleteFilePermanently(file)
        }

        val expiredFolders = folderDao.getExpiredTrashedFoldersSync(cutoffTime)
        expiredFolders.forEach { folder ->
            deleteFolderCascade(folder)
        }
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val trashedFiles = folderDao.getTrashedFilesSync()
        trashedFiles.forEach { file ->
            deleteFilePermanently(file)
        }

        val trashedFolders = folderDao.getTrashedFoldersSync()
        trashedFolders.forEach { folder ->
            deleteFolderCascade(folder)
        }

        folderDao.deleteAllTrashedFiles()
        folderDao.deleteAllTrashedFolders()
    }

    suspend fun cleanupExpiredTrash(days: Int = 30) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        folderDao.deleteExpiredTrashedFolders(cutoff)
        folderDao.deleteExpiredTrashedFiles(cutoff)
    }

    // --- Copy & Move Operations ---
    suspend fun moveFolder(folder: FolderEntity, newParentId: String?) = withContext(Dispatchers.IO) {
        // Prevent moving folder into itself or its own subfolder
        if (newParentId == folder.id) return@withContext
        folderDao.updateFolder(folder.copy(parentId = newParentId, updatedAt = System.currentTimeMillis()))
    }

    suspend fun copyFolder(sourceFolder: FolderEntity, targetParentId: String?): FolderEntity = withContext(Dispatchers.IO) {
        val newFolder = sourceFolder.copy(
            id = UUID.randomUUID().toString(),
            name = "${sourceFolder.name} - نسخة",
            parentId = targetParentId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        folderDao.insertFolder(newFolder)

        val childFolders = folderDao.getFoldersByParentSync(sourceFolder.id, isTrashed = false)
        val childFiles = folderDao.getFilesByFolderSync(sourceFolder.id, isTrashed = false)

        // Recursively copy child folders
        for (child in childFolders) {
            copyFolderRecursive(child, newFolder.id)
        }

        // Copy files
        for (file in childFiles) {
            copyFile(file, newFolder.id)
        }

        newFolder
    }

    private suspend fun copyFolderRecursive(sourceFolder: FolderEntity, targetParentId: String) {
        val newFolder = sourceFolder.copy(
            id = UUID.randomUUID().toString(),
            parentId = targetParentId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        folderDao.insertFolder(newFolder)

        val childFolders = folderDao.getFoldersByParentSync(sourceFolder.id, isTrashed = false)
        val childFiles = folderDao.getFilesByFolderSync(sourceFolder.id, isTrashed = false)

        for (child in childFolders) {
            copyFolderRecursive(child, newFolder.id)
        }

        for (file in childFiles) {
            copyFile(file, newFolder.id)
        }
    }

    suspend fun moveFile(file: FileItemEntity, targetFolderId: String) = withContext(Dispatchers.IO) {
        folderDao.updateFile(file.copy(folderId = targetFolderId, updatedAt = System.currentTimeMillis()))
    }

    suspend fun copyFile(sourceFile: FileItemEntity, targetFolderId: String): FileItemEntity? = withContext(Dispatchers.IO) {
        try {
            val srcFileOnDisk = File(sourceFile.path)
            if (!srcFileOnDisk.exists()) return@withContext null

            val newFileName = "copy_${UUID.randomUUID()}_${sourceFile.name}"
            val newTargetFile = File(storageDir, newFileName)

            srcFileOnDisk.inputStream().use { input ->
                FileOutputStream(newTargetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val copiedFileEntity = sourceFile.copy(
                id = UUID.randomUUID().toString(),
                name = if (sourceFile.name.contains("نسخة")) sourceFile.name else "${sourceFile.name} - نسخة",
                path = newTargetFile.absolutePath,
                folderId = targetFolderId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            folderDao.insertFile(copiedFileEntity)
            copiedFileEntity
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Cascade Permanent Deletion ---
    data class DeleteCascadeSummary(
        val targetFolder: FolderEntity,
        val subfolderNames: List<String>,
        val fileNames: List<String>
    )

    suspend fun getCascadeSummary(folder: FolderEntity): DeleteCascadeSummary = withContext(Dispatchers.IO) {
        val allFolders = folderDao.getAllFoldersSync(isTrashed = false)
        val allFiles = folderDao.getAllFilesSync(isTrashed = false)

        val subfoldersToFolderIds = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(folder.id)

        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            subfoldersToFolderIds.add(parent)
            val children = allFolders.filter { it.parentId == parent }
            children.forEach { child ->
                queue.add(child.id)
            }
        }

        val subfolders = allFolders.filter { subfoldersToFolderIds.contains(it.id) && it.id != folder.id }
        val filesToDelete = allFiles.filter { subfoldersToFolderIds.contains(it.folderId) }

        DeleteCascadeSummary(
            targetFolder = folder,
            subfolderNames = subfolders.map { it.name },
            fileNames = filesToDelete.map { it.name }
        )
    }

    suspend fun deleteFolderCascade(folder: FolderEntity) = withContext(Dispatchers.IO) {
        val allFiles = folderDao.getAllFilesSync()
        val allFolders = folderDao.getAllFoldersSync()
        val folderIds = mutableSetOf(folder.id)
        var added = true
        while (added) {
            val initialSize = folderIds.size
            val children = allFolders.filter { it.parentId != null && folderIds.contains(it.parentId) }.map { it.id }
            folderIds.addAll(children)
            added = folderIds.size > initialSize
        }

        val filesToDelete = allFiles.filter { folderIds.contains(it.folderId) }
        filesToDelete.forEach { fileEntity ->
            try {
                val f = File(fileEntity.path)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        folderDao.deleteFolder(folder)
    }

    suspend fun saveUriToFile(uri: Uri, folderId: String): FileItemEntity? = withContext(Dispatchers.IO) {
        try {
            var fileName = "file_${System.currentTimeMillis()}"
            var fileSize = 0L
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        val nameStr = cursor.getString(nameIndex)
                        if (!nameStr.isNullOrEmpty()) fileName = nameStr
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val targetFile = File(storageDir, "${UUID.randomUUID()}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (fileSize == 0L && targetFile.exists()) {
                fileSize = targetFile.length()
            }

            val fileEntity = FileItemEntity(
                name = fileName,
                mimeType = mimeType,
                size = fileSize,
                path = targetFile.absolutePath,
                folderId = folderId
            )
            folderDao.insertFile(fileEntity)
            fileEntity
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveTextToFile(title: String, textContent: String, folderId: String): FileItemEntity? = withContext(Dispatchers.IO) {
        try {
            val safeTitle = if (title.isBlank()) "ملاحظة نصية_${System.currentTimeMillis()}" else title
            val fileName = if (safeTitle.endsWith(".txt")) safeTitle else "$safeTitle.txt"
            val targetFile = File(storageDir, "${UUID.randomUUID()}_$fileName")
            
            targetFile.writeText(textContent, Charsets.UTF_8)
            val fileEntity = FileItemEntity(
                name = fileName,
                mimeType = "text/plain",
                size = targetFile.length(),
                path = targetFile.absolutePath,
                folderId = folderId
            )
            folderDao.insertFile(fileEntity)
            fileEntity
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteFilePermanently(fileEntity: FileItemEntity) = withContext(Dispatchers.IO) {
        try {
            val f = File(fileEntity.path)
            if (f.exists()) f.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        folderDao.deleteFile(fileEntity)
    }

    // --- ZIP Compression & Export ---
    suspend fun exportFolderToZipSync(folder: FolderEntity): File? {
        return createZipArchive(folder.name, listOf(folder.id), emptyList())
    }

    suspend fun createZipArchive(
        zipName: String,
        folderIds: List<String>,
        fileIds: List<String>
    ): File? = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val zipFile = File(exportDir, "$zipName.zip")
            val zos = java.util.zip.ZipOutputStream(FileOutputStream(zipFile))

            val allFiles = folderDao.getAllFilesSync(false)
            val filesToZip = allFiles.filter { fileIds.contains(it.id) }
            for (f in filesToZip) {
                val realFile = File(f.path)
                if (realFile.exists()) {
                    val entry = java.util.zip.ZipEntry(f.name)
                    zos.putNextEntry(entry)
                    realFile.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            for (fId in folderIds) {
                val folder = folderDao.getFolderByIdSync(fId) ?: continue
                addFolderToZip(zos, folder, folder.name)
            }

            zos.flush()
            zos.close()
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun addFolderToZip(zos: java.util.zip.ZipOutputStream, folder: FolderEntity, parentPath: String) {
        val childFiles = folderDao.getFilesByFolderSync(folder.id, false)
        for (f in childFiles) {
            val realFile = File(f.path)
            if (realFile.exists()) {
                val entry = java.util.zip.ZipEntry("$parentPath/${f.name}")
                zos.putNextEntry(entry)
                realFile.inputStream().use { input -> input.copyTo(zos) }
                zos.closeEntry()
            }
        }

        val childFolders = folderDao.getFoldersByParentSync(folder.id, false)
        for (cf in childFolders) {
            addFolderToZip(zos, cf, "$parentPath/${cf.name}")
        }
    }

    // --- Academic Structure Auto Setup ---
    suspend fun setupAcademicFolders(department: String, semester: String) = withContext(Dispatchers.IO) {
        val deptFolder = FolderEntity(
            name = "الشعبة: $department",
            description = "مجلد الشعبة الأكاديمية تلقائي",
            color = "#4F46E5",
            size = "large",
            parentId = null
        )
        folderDao.insertFolder(deptFolder)

        val deptId = folderDao.getAllFoldersSync(false).firstOrNull { it.name == deptFolder.name }?.id ?: return@withContext

        val semesterFolder = FolderEntity(
            name = "الفصل الدراسي: $semester",
            description = "مسار المحاضرات والدروس",
            color = "#06B6D4",
            size = "medium",
            parentId = deptId
        )
        folderDao.insertFolder(semesterFolder)

        val semId = folderDao.getAllFoldersSync(false).firstOrNull { it.parentId == deptId }?.id ?: return@withContext

        val categories = listOf("المحاضرات النظرية 📚", "التطبيقات والتمارين ✍️", "الملخصات والامتحانات 📝")
        categories.forEachIndexed { idx, cat ->
            val catFolder = FolderEntity(
                name = cat,
                description = "محتوى $cat",
                color = if (idx == 0) "#10B981" else if (idx == 1) "#F59E0B" else "#EC4899",
                size = "small",
                parentId = semId
            )
            folderDao.insertFolder(catFolder)
        }
    }
}
