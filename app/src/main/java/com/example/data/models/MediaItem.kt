package com.example.data.models

data class MediaItem(
    val id: String,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val fileSize: Long,
    val dateAdded: Long, // seconds
    val dateModified: Long, // seconds
    val width: Int,
    val height: Int,
    val duration: Long = 0L, // video duration in millis
    val bucketId: String,
    val bucketName: String,
    val isVideo: Boolean,
    val isFavorite: Boolean = false
)

