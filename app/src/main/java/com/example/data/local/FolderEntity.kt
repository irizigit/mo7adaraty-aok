package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentId"]), Index(value = ["isTrashed"])]
)
data class FolderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val color: String = "#6366f1",
    val size: String = "medium", // small, medium, large, xlarge
    val parentId: String? = null,
    val order: Int = 0,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val isLocked: Boolean = false,
    val pinHash: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
