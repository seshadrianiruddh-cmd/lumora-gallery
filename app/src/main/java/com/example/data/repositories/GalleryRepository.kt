package com.example.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.database.*
import com.example.data.models.MediaItem
import com.example.data.vault.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class GalleryRepository(
    private val context: Context,
    private val mediaStoreService: MediaStoreService,
    private val galleryDao: GalleryDao,
    val vaultManager: VaultManager
) {
    // Cache of the latest queried MediaStore media
    private val _deviceMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val deviceMedia: StateFlow<List<MediaItem>> = _deviceMedia.asStateFlow()

    // Observe Favorites from SQLite and combine with device media
    val favoritesFlow: Flow<List<MediaItem>> = galleryDao.getFavoritesFlow()
        .combine(deviceMedia) { favEntities, mediaItems ->
            val favIds = favEntities.map { it.mediaId }.toSet()
            mediaItems.filter { it.id in favIds }.map { it.copy(isFavorite = true) }
        }.flowOn(Dispatchers.IO)

    // Observe Custom Albums
    val customAlbumsFlow: Flow<List<AlbumEntity>> = galleryDao.getAlbumsFlow()

    // Observe Vault Items
    val vaultItemsFlow: Flow<List<VaultItemEntity>> = galleryDao.getVaultItemsFlow()

    // Observe Trash Items
    val trashItemsFlow: Flow<List<TrashItemEntity>> = galleryDao.getTrashItemsFlow()

    /**
     * Queries MediaStore, updates the cache, and checks favorite state
     */
    suspend fun refreshMediaList() = withContext(Dispatchers.IO) {
        val rawMedia = mediaStoreService.queryDeviceMedia()
        // Query favorited and trashed IDs from db
        val favs = galleryDao.getFavoritesFlow().first().map { it.mediaId }.toSet()
        val trashedIds = galleryDao.getTrashItemsFlow().first().map { it.mediaId }.toSet()
        val processed = rawMedia
            .filter { it.id !in trashedIds }
            .map { item ->
                if (item.id in favs) item.copy(isFavorite = true) else item
            }
        _deviceMedia.value = processed
    }

    // --- Favorites Logic ---
    suspend fun toggleFavorite(item: MediaItem) = withContext(Dispatchers.IO) {
        val isCurrentlyFav = galleryDao.isFavorite(item.id)
        if (isCurrentlyFav) {
            galleryDao.deleteFavorite(item.id)
        } else {
            galleryDao.insertFavorite(FavoriteEntity(mediaId = item.id))
        }
        // Force refresh cache to update UI state
        refreshMediaList()
    }

    suspend fun removeFavorite(mediaId: String) = withContext(Dispatchers.IO) {
        galleryDao.deleteFavorite(mediaId)
        refreshMediaList()
    }

    // --- Custom Album Logic ---
    suspend fun createCustomAlbum(name: String): Int = withContext(Dispatchers.IO) {
        galleryDao.insertAlbum(AlbumEntity(name = name)).toInt()
    }

    suspend fun renameCustomAlbum(albumId: Int, newName: String) = withContext(Dispatchers.IO) {
        galleryDao.renameAlbum(albumId, newName)
    }

    suspend fun deleteCustomAlbum(albumId: Int) = withContext(Dispatchers.IO) {
        galleryDao.deleteAlbumMediaByAlbum(albumId)
        galleryDao.deleteAlbum(albumId)
    }

    suspend fun addMediaToAlbum(albumId: Int, mediaId: String) = withContext(Dispatchers.IO) {
        galleryDao.insertAlbumMedia(AlbumMediaEntity(albumId = albumId, mediaId = mediaId))
        // Set the album's cover Uri if possible
        val item = _deviceMedia.value.find { it.id == mediaId }
        if (item != null) {
            galleryDao.updateAlbumCover(albumId, item.uri)
        }
    }

    suspend fun removeMediaFromAlbum(albumId: Int, mediaId: String) = withContext(Dispatchers.IO) {
        galleryDao.removeMediaFromAlbum(albumId, mediaId)
        // Reset cover if needed
        val remainingIds = galleryDao.getMediaIdsInAlbum(albumId)
        if (remainingIds.isEmpty()) {
            galleryDao.updateAlbumCover(albumId, null)
        } else {
            val nextCover = _deviceMedia.value.find { it.id == remainingIds.first() }?.uri
            galleryDao.updateAlbumCover(albumId, nextCover)
        }
    }

    fun getMediaInAlbumFlow(albumId: Int): Flow<List<MediaItem>> {
        return galleryDao.getMediaIdsInAlbumFlow(albumId)
            .combine(deviceMedia) { albumMediaIds, allMedia ->
                val idSet = albumMediaIds.toSet()
                allMedia.filter { it.id in idSet }
            }.flowOn(Dispatchers.IO)
    }

    // --- Private Vault Logic ---
    suspend fun importToVault(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(item.uri)
            val vaultEntity = vaultManager.importToVault(
                sourceUri = uri,
                originalName = item.displayName,
                mimeType = item.mimeType,
                fileSize = item.fileSize,
                duration = item.duration,
                width = item.width,
                height = item.height
            ) ?: return@withContext false

            galleryDao.insertVaultItem(vaultEntity)
            
            // Delete original file from device
            deleteMediaPermanently(item)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun restoreFromVault(vaultItem: VaultItemEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // Restore back to app's public directory or MediaStore
            val downloadsDir = File(context.getExternalFilesDir(null), "LumoraRestored")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val outputFile = File(downloadsDir, vaultItem.originalName)
            FileOutputStream(outputFile).use { out ->
                val success = vaultManager.decryptVaultItem(vaultItem, out)
                if (!success) return@withContext false
            }

            // Remove from vault db and delete encrypted source file
            galleryDao.deleteVaultItem(vaultItem.id)
            vaultManager.deleteEncryptedFile(vaultItem)
            
            // Trigger MediaStore scan so the gallery sees the restored photo
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(outputFile.absolutePath),
                arrayOf(vaultItem.mimeType),
                null
            )

            refreshMediaList()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun deleteFromVaultPermanently(vaultItem: VaultItemEntity) = withContext(Dispatchers.IO) {
        galleryDao.deleteVaultItem(vaultItem.id)
        vaultManager.deleteEncryptedFile(vaultItem)
    }

    // --- Trash / Deletion Logic ---
    suspend fun moveMediaToTrash(item: MediaItem) = withContext(Dispatchers.IO) {
        // Log original details in SQLite trash table
        val trashEntity = TrashItemEntity(
            mediaId = item.id,
            displayName = item.displayName,
            mimeType = item.mimeType,
            originalUri = item.uri,
            duration = item.duration,
            isVideo = item.isVideo,
            fileSize = item.fileSize
        )
        galleryDao.insertTrashItem(trashEntity)
        
        // Remove from MediaStore (or try deleting/removing from main view)
        // Note: Under modern Scoped Storage, true delete from mediaStore requires launcher request.
        // We will mock/hide deleted items from the local list to guarantee absolute success and clean UX.
        // Also try to perform native deletion where possible!
        try {
            val uri = Uri.parse(item.uri)
            context.contentResolver.delete(uri, null, null)
        } catch (e: SecurityException) {
            // Under Scoped Storage on modern Android, this may fail unless we have system prompts,
            // so we fallback to hiding/caching as Trash in SQLite.
        }
        
        // Remove from favorites if it was favorited
        galleryDao.deleteFavorite(item.id)
        refreshMediaList()
    }

    suspend fun restoreMediaFromTrash(trashItem: TrashItemEntity) = withContext(Dispatchers.IO) {
        // Delete log from Trash SQLite database
        galleryDao.deleteTrashItem(trashItem.mediaId)
        refreshMediaList()
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        galleryDao.clearTrash()
    }

    suspend fun deleteMediaPermanently(item: MediaItem) = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(item.uri)
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        galleryDao.deleteFavorite(item.id)
        refreshMediaList()
    }

    // --- Search Logic ---
    fun searchMedia(query: String): List<MediaItem> {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return emptyList()

        return _deviceMedia.value.filter { item ->
            item.displayName.lowercase().contains(q) ||
            item.bucketName.lowercase().contains(q) ||
            item.mimeType.lowercase().contains(q)
        }
    }

    // --- Duplicate Finder logic ---
    suspend fun findExactDuplicates(
        onProgress: (Float) -> Unit = {}
    ): Map<String, List<MediaItem>> = withContext(Dispatchers.IO) {
        val allItems = _deviceMedia.value
        val hashMap = mutableMapOf<String, MutableList<MediaItem>>()
        val total = allItems.size.toFloat()

        allItems.forEachIndexed { index, item ->
            try {
                val hash = calculateFileHash(Uri.parse(item.uri))
                if (hash != null) {
                    val list = hashMap.getOrPut(hash) { mutableListOf() }
                    list.add(item)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (index % 5 == 0 || index == allItems.lastIndex) {
                onProgress((index + 1) / total)
            }
        }

        // Return only groups containing 2 or more files
        hashMap.filter { it.value.size >= 2 }
    }

    private fun calculateFileHash(uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generateSampleMedia(): Boolean = withContext(Dispatchers.IO) {
        val samples = listOf(
            Triple("Sunrise_Horizon.jpg", android.graphics.Color.rgb(251, 140, 0), android.graphics.Color.rgb(142, 36, 170)),
            Triple("Cyberpunk_Grid.jpg", android.graphics.Color.rgb(233, 30, 99), android.graphics.Color.rgb(0, 188, 212)),
            Triple("Emerald_Forest.jpg", android.graphics.Color.rgb(76, 175, 80), android.graphics.Color.rgb(9, 82, 40)),
            Triple("Mountain_Summit.jpg", android.graphics.Color.rgb(0, 150, 136), android.graphics.Color.rgb(63, 81, 181)),
            Triple("Sunrise_Horizon_Copy.jpg", android.graphics.Color.rgb(251, 140, 0), android.graphics.Color.rgb(142, 36, 170))
        )

        var successCount = 0
        samples.forEach { (fileName, colorStart, colorEnd) ->
            try {
                val width = 1080
                val height = 1080
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                
                val gradient = android.graphics.LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    colorStart, colorEnd,
                    android.graphics.Shader.TileMode.CLAMP
                )
                val paint = android.graphics.Paint().apply {
                    shader = gradient
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                val elementPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    alpha = 60
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), 300f, elementPaint)

                val linePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    alpha = 40
                    strokeWidth = 4f
                }
                for (i in 0..width step 120) {
                    canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), linePaint)
                    canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), linePaint)
                }

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 64f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText(fileName.replace(".jpg", "").replace("_", " "), (width / 2).toFloat(), (height / 2 + 20).toFloat(), textPaint)

                val subPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    alpha = 200
                    textSize = 36f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("Lumora Test Asset | ${width}x${height}", (width / 2).toFloat(), (height / 2 + 100).toFloat(), subPaint)

                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(android.provider.MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LumoraTest")
                    }
                }

                val collectionUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val newUri = context.contentResolver.insert(collectionUri, values)
                if (newUri != null) {
                    context.contentResolver.openOutputStream(newUri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(newUri, values, null, null)
                    }
                    successCount++
                }
                bitmap.recycle()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (successCount > 0) {
            refreshMediaList()
            true
        } else {
            false
        }
    }
}
