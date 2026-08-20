package com.example.util

import android.content.Context
import com.example.data.VirtualFolderRepository
import com.example.data.local.FileItemEntity
import com.example.data.local.FolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {

    suspend fun createZipFromFolder(
        context: Context,
        repository: VirtualFolderRepository,
        rootFolder: FolderEntity,
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        val zipDir = File(context.cacheDir, "zip_exports")
        if (!zipDir.exists()) zipDir.mkdirs()

        val sanitizedName = rootFolder.name.replace(Regex("[^a-zA-Z0-9_\\u0600-\\u06FF]"), "_")
        val zipFile = File(zipDir, "${sanitizedName}_${System.currentTimeMillis()}.zip")

        try {
            val fileList = mutableListOf<Pair<String, FileItemEntity>>()
            collectFilesRecursive(repository, rootFolder, "", fileList)

            if (fileList.isEmpty()) {
                // Create zip with empty directory structure
                FileOutputStream(zipFile).use { fos ->
                    ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                        val entry = ZipEntry("${rootFolder.name}/")
                        zos.putNextEntry(entry)
                        zos.closeEntry()
                    }
                }
                return@withContext zipFile
            }

            val totalFiles = fileList.size
            var processed = 0

            FileOutputStream(zipFile).use { fos ->
                ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                    val buffer = ByteArray(8192)

                    for ((folderPath, fileItem) in fileList) {
                        val srcFile = File(fileItem.path)
                        if (srcFile.exists()) {
                            val zipPath = if (folderPath.isEmpty()) {
                                "${rootFolder.name}/${fileItem.name}"
                            } else {
                                "${rootFolder.name}/$folderPath/${fileItem.name}"
                            }

                            val entry = ZipEntry(zipPath)
                            zos.putNextEntry(entry)

                            FileInputStream(srcFile).use { fis ->
                                BufferedInputStream(fis).use { bis ->
                                    var read: Int
                                    while (bis.read(buffer).also { read = it } != -1) {
                                        zos.write(buffer, 0, read)
                                    }
                                }
                            }
                            zos.closeEntry()
                        }
                        processed++
                        onProgress(processed.toFloat() / totalFiles)
                    }
                }
            }
            return@withContext zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private suspend fun collectFilesRecursive(
        repository: VirtualFolderRepository,
        currentFolder: FolderEntity,
        currentPath: String,
        outputList: MutableList<Pair<String, FileItemEntity>>
    ) {
        val files = repository.getFilesByFolder(currentFolder.id).first()
        for (f in files) {
            outputList.add(Pair(currentPath, f))
        }

        val subfolders = repository.getFoldersByParent(currentFolder.id).first()
        for (sub in subfolders) {
            val nextPath = if (currentPath.isEmpty()) sub.name else "$currentPath/${sub.name}"
            collectFilesRecursive(repository, sub, nextPath, outputList)
        }
    }
}
