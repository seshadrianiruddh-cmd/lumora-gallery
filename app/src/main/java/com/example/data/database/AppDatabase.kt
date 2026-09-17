package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        AlbumEntity::class,
        AlbumMediaEntity::class,
        VaultItemEntity::class,
        TrashItemEntity::class,
        RecentlyViewedEntity::class,
        FavoriteCollectionEntity::class,
        CollectionMediaEntity::class,
        HiddenAlbumEntity::class,
        LockedAlbumEntity::class,
        ExifEditEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun galleryDao(): GalleryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lumora_gallery_db"
                )
                .fallbackToDestructiveMigration() // Simple strategy for initial app phases
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
