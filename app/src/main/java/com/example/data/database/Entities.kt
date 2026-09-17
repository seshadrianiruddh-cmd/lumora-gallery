package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_album_media", primaryKeys = ["albumId", "mediaId"])
data class AlbumMediaEntity(
    val albumId: Int,
    val mediaId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalName: String,
    val mimeType: String,
    val encryptedFilePath: String, // Path on private internal storage
    val ivHex: String,             // Initialization Vector for AES encryption
    val importTime: Long = System.currentTimeMillis(),
    val duration: Long = 0L,       // Video duration if applicable
    val width: Int = 0,
    val height: Int = 0,
    val fileSize: Long = 0L
)

@Entity(tableName = "trash_items")
data class TrashItemEntity(
    @PrimaryKey val mediaId: String,
    val displayName: String,
    val mimeType: String,
    val originalUri: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val duration: Long = 0L,
    val isVideo: Boolean = false,
    val fileSize: Long = 0L
)

@Entity(tableName = "recently_viewed")
data class RecentlyViewedEntity(
    @PrimaryKey val mediaId: String,
    val lastViewedAt: Long = System.currentTimeMillis(),
    val viewCount: Int = 1
)

@Entity(tableName = "favorite_collections")
data class FavoriteCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "collection_media", primaryKeys = ["collectionId", "mediaId"])
data class CollectionMediaEntity(
    val collectionId: Int,
    val mediaId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_albums")
data class HiddenAlbumEntity(
    @PrimaryKey val albumId: String,
    val albumName: String,
    val hiddenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "locked_albums")
data class LockedAlbumEntity(
    @PrimaryKey val albumId: String,
    val albumName: String,
    val pinHash: String, // Keystore protected or simple hash
    val lockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exif_edits")
data class ExifEditEntity(
    @PrimaryKey val mediaId: String,
    val dateModified: Long?,
    val latitude: Double?,
    val longitude: Double?,
    val cameraModel: String?
)

