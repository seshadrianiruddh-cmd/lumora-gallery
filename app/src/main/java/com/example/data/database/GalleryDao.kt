package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    // --- Favorites ---
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getFavoritesFlow(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    suspend fun isFavorite(mediaId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun deleteFavorite(mediaId: String)

    // --- Custom Albums ---
    @Query("SELECT * FROM custom_albums ORDER BY name ASC")
    fun getAlbumsFlow(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Query("UPDATE custom_albums SET name = :newName WHERE id = :albumId")
    suspend fun renameAlbum(albumId: Int, newName: String)

    @Query("DELETE FROM custom_albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: Int)

    @Query("DELETE FROM custom_album_media WHERE albumId = :albumId")
    suspend fun deleteAlbumMediaByAlbum(albumId: Int)

    // --- Custom Album Media ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumMedia(albumMedia: AlbumMediaEntity)

    @Query("DELETE FROM custom_album_media WHERE albumId = :albumId AND mediaId = :mediaId")
    suspend fun removeMediaFromAlbum(albumId: Int, mediaId: String)

    @Query("SELECT mediaId FROM custom_album_media WHERE albumId = :albumId ORDER BY timestamp DESC")
    fun getMediaIdsInAlbumFlow(albumId: Int): Flow<List<String>>

    @Query("SELECT mediaId FROM custom_album_media WHERE albumId = :albumId")
    suspend fun getMediaIdsInAlbum(albumId: Int): List<String>

    @Query("UPDATE custom_albums SET coverUri = :coverUri WHERE id = :albumId")
    suspend fun updateAlbumCover(albumId: Int, coverUri: String?)

    // --- Vault ---
    @Query("SELECT * FROM vault_items ORDER BY importTime DESC")
    fun getVaultItemsFlow(): Flow<List<VaultItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(vaultItem: VaultItemEntity): Long

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getVaultItemById(id: Int): VaultItemEntity?

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItem(id: Int)

    // --- Trash ---
    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun getTrashItemsFlow(): Flow<List<TrashItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(trashItem: TrashItemEntity)

    @Query("DELETE FROM trash_items WHERE mediaId = :mediaId")
    suspend fun deleteTrashItem(mediaId: String)

    @Query("DELETE FROM trash_items")
    suspend fun clearTrash()

    // --- Recently Viewed ---
    @Query("SELECT * FROM recently_viewed ORDER BY lastViewedAt DESC")
    fun getRecentlyViewedFlow(): Flow<List<RecentlyViewedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyViewed(item: RecentlyViewedEntity)

    @Query("DELETE FROM recently_viewed WHERE mediaId = :mediaId")
    suspend fun deleteRecentlyViewed(mediaId: String)

    @Query("DELETE FROM recently_viewed")
    suspend fun clearRecentlyViewed()

    // --- Favorite Collections ---
    @Query("SELECT * FROM favorite_collections ORDER BY name ASC")
    fun getFavoriteCollectionsFlow(): Flow<List<FavoriteCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteCollection(collection: FavoriteCollectionEntity): Long

    @Query("DELETE FROM favorite_collections WHERE id = :id")
    suspend fun deleteFavoriteCollection(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionMedia(item: CollectionMediaEntity)

    @Query("DELETE FROM collection_media WHERE collectionId = :collectionId AND mediaId = :mediaId")
    suspend fun removeMediaFromCollection(collectionId: Int, mediaId: String)

    @Query("SELECT mediaId FROM collection_media WHERE collectionId = :collectionId ORDER BY timestamp DESC")
    fun getMediaIdsInCollectionFlow(collectionId: Int): Flow<List<String>>

    // --- Hidden Albums ---
    @Query("SELECT * FROM hidden_albums")
    fun getHiddenAlbumsFlow(): Flow<List<HiddenAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenAlbum(album: HiddenAlbumEntity)

    @Query("DELETE FROM hidden_albums WHERE albumId = :albumId")
    suspend fun deleteHiddenAlbum(albumId: String)

    // --- Locked Albums ---
    @Query("SELECT * FROM locked_albums")
    fun getLockedAlbumsFlow(): Flow<List<LockedAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockedAlbum(album: LockedAlbumEntity)

    @Query("DELETE FROM locked_albums WHERE albumId = :albumId")
    suspend fun deleteLockedAlbum(albumId: String)

    // --- EXIF Edits ---
    @Query("SELECT * FROM exif_edits WHERE mediaId = :mediaId")
    suspend fun getExifEdit(mediaId: String): ExifEditEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExifEdit(edit: ExifEditEntity)
}
