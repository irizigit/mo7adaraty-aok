package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders WHERE parentId IS :parentId AND isTrashed = :isTrashed ORDER BY isPinned DESC, `order` ASC, createdAt DESC")
    fun getFoldersByParent(parentId: String?, isTrashed: Boolean = false): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId IS :parentId AND isTrashed = :isTrashed ORDER BY isPinned DESC, `order` ASC, createdAt DESC")
    suspend fun getFoldersByParentSync(parentId: String?, isTrashed: Boolean = false): List<FolderEntity>

    @Query("SELECT * FROM files WHERE folderId = :folderId AND isTrashed = :isTrashed ORDER BY isPinned DESC, createdAt DESC")
    fun getFilesByFolder(folderId: String, isTrashed: Boolean = false): Flow<List<FileItemEntity>>

    @Query("SELECT * FROM files WHERE folderId = :folderId AND isTrashed = :isTrashed")
    suspend fun getFilesByFolderSync(folderId: String, isTrashed: Boolean = false): List<FileItemEntity>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    fun getFolderById(folderId: String): Flow<FolderEntity?>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderByIdSync(folderId: String): FolderEntity?

    @Query("SELECT * FROM files WHERE id = :fileId")
    suspend fun getFileByIdSync(fileId: String): FileItemEntity?

    @Query("SELECT * FROM folders WHERE isTrashed = :isTrashed")
    fun getAllFolders(isTrashed: Boolean = false): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isTrashed = :isTrashed")
    suspend fun getAllFoldersSync(isTrashed: Boolean = false): List<FolderEntity>

    @Query("SELECT * FROM files WHERE isTrashed = :isTrashed")
    fun getAllFiles(isTrashed: Boolean = false): Flow<List<FileItemEntity>>

    @Query("SELECT * FROM files WHERE isTrashed = :isTrashed")
    suspend fun getAllFilesSync(isTrashed: Boolean = false): List<FileItemEntity>

    // Trash Queries
    @Query("SELECT * FROM folders WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun getTrashedFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM files WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun getTrashedFiles(): Flow<List<FileItemEntity>>

    @Query("SELECT * FROM folders WHERE isTrashed = 1")
    suspend fun getTrashedFoldersSync(): List<FolderEntity>

    @Query("SELECT * FROM files WHERE isTrashed = 1")
    suspend fun getTrashedFilesSync(): List<FileItemEntity>

    @Query("SELECT * FROM folders WHERE isTrashed = 1 AND trashedAt <= :cutoffTime")
    suspend fun getExpiredTrashedFoldersSync(cutoffTime: Long): List<FolderEntity>

    @Query("SELECT * FROM files WHERE isTrashed = 1 AND trashedAt <= :cutoffTime")
    suspend fun getExpiredTrashedFilesSync(cutoffTime: Long): List<FileItemEntity>

    // Search Queries
    @Query("SELECT * FROM folders WHERE isTrashed = 0 AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchFolders(query: String): Flow<List<FolderEntity>>

    @Query("SELECT * FROM files WHERE isTrashed = 0 AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchFiles(query: String): Flow<List<FileItemEntity>>

    // Favorite Queries
    @Query("SELECT * FROM folders WHERE isFavorite = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun getFavoriteFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM files WHERE isFavorite = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun getFavoriteFiles(): Flow<List<FileItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileItemEntity)

    @Update
    suspend fun updateFile(file: FileItemEntity)

    @Delete
    suspend fun deleteFile(file: FileItemEntity)

    @Query("DELETE FROM files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: String)

    @Query("DELETE FROM folders WHERE isTrashed = 1")
    suspend fun deleteAllTrashedFolders()

    @Query("DELETE FROM files WHERE isTrashed = 1")
    suspend fun deleteAllTrashedFiles()

    @Query("DELETE FROM folders WHERE isTrashed = 1 AND trashedAt IS NOT NULL AND trashedAt < :cutoffTimestamp")
    suspend fun deleteExpiredTrashedFolders(cutoffTimestamp: Long)

    @Query("DELETE FROM files WHERE isTrashed = 1 AND trashedAt IS NOT NULL AND trashedAt < :cutoffTimestamp")
    suspend fun deleteExpiredTrashedFiles(cutoffTimestamp: Long)
}
