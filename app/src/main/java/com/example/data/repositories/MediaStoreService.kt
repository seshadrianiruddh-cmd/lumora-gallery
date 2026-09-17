package com.example.data.repositories

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreService(private val context: Context) {

    suspend fun queryDeviceMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val contentResolver: ContentResolver = context.contentResolver

        // Define columns we want to query for both images and videos
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        // Query Images
        val imageUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageSortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        contentResolver.query(imageUri, projection, null, null, imageSortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(nameColumn) ?: "Unnamed Image"
                val mimeType = cursor.getString(mimeColumn) ?: "image/jpeg"
                val fileSize = cursor.getLong(sizeColumn)
                val dateAdded = cursor.getLong(addedColumn)
                val dateModified = cursor.getLong(modifiedColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val bucketId = cursor.getString(bucketIdColumn) ?: "unknown_bucket"
                val bucketName = cursor.getString(bucketNameColumn) ?: "Device"
                val contentUri = ContentUris.withAppendedId(imageUri, id).toString()

                mediaList.add(
                    MediaItem(
                        id = "img_$id",
                        uri = contentUri,
                        displayName = displayName,
                        mimeType = mimeType,
                        fileSize = fileSize,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        width = width,
                        height = height,
                        duration = 0L,
                        bucketId = bucketId,
                        bucketName = bucketName,
                        isVideo = false
                    )
                )
            }
        }

        // Query Videos (with duration)
        val videoProjection = projection + arrayOf(MediaStore.Video.VideoColumns.DURATION)
        val videoUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoSortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        contentResolver.query(videoUri, videoProjection, null, null, videoSortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(nameColumn) ?: "Unnamed Video"
                val mimeType = cursor.getString(mimeColumn) ?: "video/mp4"
                val fileSize = cursor.getLong(sizeColumn)
                val dateAdded = cursor.getLong(addedColumn)
                val dateModified = cursor.getLong(modifiedColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val bucketId = cursor.getString(bucketIdColumn) ?: "unknown_bucket"
                val bucketName = cursor.getString(bucketNameColumn) ?: "Device"
                val duration = cursor.getLong(durationColumn)
                val contentUri = ContentUris.withAppendedId(videoUri, id).toString()

                mediaList.add(
                    MediaItem(
                        id = "vid_$id",
                        uri = contentUri,
                        displayName = displayName,
                        mimeType = mimeType,
                        fileSize = fileSize,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        width = width,
                        height = height,
                        duration = duration,
                        bucketId = bucketId,
                        bucketName = bucketName,
                        isVideo = true
                    )
                )
            }
        }

        // Sort combined list by dateAdded descending
        mediaList.sortedByDescending { it.dateAdded }
    }
}
