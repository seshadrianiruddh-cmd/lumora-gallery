package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.models.MediaItem
import com.example.data.repositories.GalleryRepository
import com.example.data.repositories.MediaStoreService
import com.example.data.vault.VaultManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val mediaStoreService = MediaStoreService(application)
    val vaultManager = VaultManager(application)
    
    val repository = GalleryRepository(
        context = application,
        mediaStoreService = mediaStoreService,
        galleryDao = db.galleryDao(),
        vaultManager = vaultManager
    )

    // --- Grid Density and Theme Preference (Stored locally/persisted in VM state) ---
    private val _gridDensity = MutableStateFlow(3) // Default 3 columns
    val gridDensity: StateFlow<Int> = _gridDensity.asStateFlow()

    private val _themePreference = MutableStateFlow("system") // system, light, dark
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    // --- Active Photos Screen state ---
    private val _sortOrder = MutableStateFlow("newest_first") // newest_first, oldest_first, name, size
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    private val _filterType = MutableStateFlow("all") // all, photos, videos, favorites
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    // Full device media list (queried dynamically)
    val allMedia: StateFlow<List<MediaItem>> = repository.deviceMedia

    // Sorted and filtered media list
    val displayedMedia: StateFlow<List<MediaItem>> = combine(
        allMedia,
        _sortOrder,
        _filterType
    ) { media, sort, filter ->
        var list = when (filter) {
            "photos" -> media.filter { !it.isVideo }
            "videos" -> media.filter { it.isVideo }
            "favorites" -> media.filter { it.isFavorite }
            else -> media
        }

        list = when (sort) {
            "oldest_first" -> list.sortedBy { it.dateAdded }
            "name" -> list.sortedBy { it.displayName }
            "size" -> list.sortedBy { it.fileSize }
            else -> list.sortedByDescending { it.dateAdded } // newest_first
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grouped displayed media (Timeline dates)
    val timelineMedia: StateFlow<Map<String, List<MediaItem>>> = displayedMedia.map { mediaList ->
        mediaList.groupBy { item ->
            val timestampMs = item.dateAdded * 1000L
            val sdf = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestampMs))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Selection State ---
    private val _selectedMediaIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMediaIds: StateFlow<Set<String>> = _selectedMediaIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = selectedMediaIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Album Discovery State ---
    val albums: StateFlow<List<AlbumFolder>> = allMedia.map { mediaList ->
        val folderMap = mediaList.groupBy { it.bucketId }
        folderMap.map { (bucketId, items) ->
            AlbumFolder(
                id = bucketId,
                name = items.firstOrNull()?.bucketName ?: "Unknown Folder",
                coverUri = items.firstOrNull()?.uri,
                itemCount = items.size,
                isCustom = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customAlbums = repository.customAlbumsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Private Vault State ---
    val vaultItems = repository.vaultItemsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _vaultUnlocked = MutableStateFlow(false)
    val vaultUnlocked: StateFlow<Boolean> = _vaultUnlocked.asStateFlow()

    private val _vaultIsSetup = MutableStateFlow(vaultManager.isVaultSetup())
    val vaultIsSetup: StateFlow<Boolean> = _vaultIsSetup.asStateFlow()

    // --- Trash / Deleted State ---
    val trashItems = repository.trashItemsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Duplicate Finder State ---
    private val _duplicateGroups = MutableStateFlow<Map<String, List<MediaItem>>>(emptyMap())
    val duplicateGroups: StateFlow<Map<String, List<MediaItem>>> = _duplicateGroups.asStateFlow()

    private val _duplicateScanProgress = MutableStateFlow(-1f) // -1f means not running
    val duplicateScanProgress: StateFlow<Float> = _duplicateScanProgress.asStateFlow()

    // --- Search Query State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val searchResults: StateFlow<List<MediaItem>> = _searchQuery.map { query ->
        repository.searchMedia(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshMedia()
    }

    fun refreshMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshMediaList()
            _isLoading.value = false
        }
    }

    fun generateSamplePhotos(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.generateSampleMedia()
            onComplete(success)
        }
    }

    // --- Sort & Filter ---
    fun setSortOrder(order: String) {
        _sortOrder.value = order
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    // --- Density & Theme ---
    fun setGridDensity(columns: Int) {
        _gridDensity.value = columns.coerceIn(2, 8)
    }

    fun setThemePreference(pref: String) {
        _themePreference.value = pref
    }

    // --- Selection Toolbar Actions ---
    fun toggleSelection(mediaId: String) {
        val current = _selectedMediaIds.value
        _selectedMediaIds.value = if (current.contains(mediaId)) {
            current - mediaId
        } else {
            current + mediaId
        }
    }

    fun clearSelection() {
        _selectedMediaIds.value = emptySet()
    }

    fun setSelectedIds(ids: Set<String>) {
        _selectedMediaIds.value = ids
    }

    fun selectAll() {
        _selectedMediaIds.value = displayedMedia.value.map { it.id }.toSet()
    }

    fun toggleGroupSelection(items: List<MediaItem>) {
        val current = _selectedMediaIds.value
        val itemIds = items.map { it.id }.toSet()
        val allSelected = itemIds.all { current.contains(it) }
        _selectedMediaIds.value = if (allSelected) {
            current - itemIds
        } else {
            current + itemIds
        }
    }

    // --- Favorites ---
    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    // --- Private Vault Actions ---
    fun setupVault(pin: String): Boolean {
        val success = vaultManager.setupVault(pin)
        if (success) {
            _vaultUnlocked.value = true
            _vaultIsSetup.value = true
        }
        return success
    }

    fun unlockVault(pin: String): Boolean {
        val success = vaultManager.unlockVault(pin)
        if (success) {
            _vaultUnlocked.value = true
        }
        return success
    }

    fun lockVault() {
        vaultManager.lock()
        _vaultUnlocked.value = false
    }

    fun changeVaultPin(oldPin: String, newPin: String): Boolean {
        return vaultManager.changePin(oldPin, newPin)
    }

    fun importSingleToVault(item: MediaItem) {
        viewModelScope.launch {
            repository.importToVault(item)
            refreshMedia()
        }
    }

    fun importSelectedToVault() {
        val selectedIds = _selectedMediaIds.value
        viewModelScope.launch {
            displayedMedia.value.filter { it.id in selectedIds }.forEach { item ->
                repository.importToVault(item)
            }
            clearSelection()
            refreshMedia()
        }
    }

    fun restoreVaultItem(item: VaultItemEntity) {
        viewModelScope.launch {
            repository.restoreFromVault(item)
        }
    }

    fun deleteVaultItemPermanently(item: VaultItemEntity) {
        viewModelScope.launch {
            repository.deleteFromVaultPermanently(item)
        }
    }

    // --- Custom Albums ---
    fun createAlbum(name: String) {
        viewModelScope.launch {
            repository.createCustomAlbum(name)
        }
    }

    fun renameAlbum(albumId: Int, newName: String) {
        viewModelScope.launch {
            repository.renameCustomAlbum(albumId, newName)
        }
    }

    fun deleteAlbum(albumId: Int) {
        viewModelScope.launch {
            repository.deleteCustomAlbum(albumId)
        }
    }

    fun addSelectedToAlbum(albumId: Int) {
        val selectedIds = _selectedMediaIds.value
        viewModelScope.launch {
            selectedIds.forEach { mediaId ->
                repository.addMediaToAlbum(albumId, mediaId)
            }
            clearSelection()
        }
    }

    fun removeMediaFromAlbum(albumId: Int, mediaId: String) {
        viewModelScope.launch {
            repository.removeMediaFromAlbum(albumId, mediaId)
        }
    }

    fun removeSelectedFromAlbum(albumId: Int) {
        val selectedIds = _selectedMediaIds.value
        viewModelScope.launch {
            selectedIds.forEach { mediaId ->
                repository.removeMediaFromAlbum(albumId, mediaId)
            }
            clearSelection()
            refreshMedia()
        }
    }

    fun getMediaInAlbum(albumId: Int): Flow<List<MediaItem>> {
        return repository.getMediaInAlbumFlow(albumId)
    }

    // --- Trash / Deletion actions ---
    fun moveSelectedToTrash() {
        val selectedIds = _selectedMediaIds.value
        viewModelScope.launch {
            displayedMedia.value.filter { it.id in selectedIds }.forEach { item ->
                repository.moveMediaToTrash(item)
            }
            clearSelection()
            refreshMedia()
        }
    }

    fun moveItemToTrash(item: MediaItem) {
        viewModelScope.launch {
            repository.moveMediaToTrash(item)
            refreshMedia()
        }
    }

    fun restoreTrashItem(item: TrashItemEntity) {
        viewModelScope.launch {
            repository.restoreMediaFromTrash(item)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun deleteItemPermanently(item: MediaItem) {
        viewModelScope.launch {
            repository.deleteMediaPermanently(item)
        }
    }

    // --- Search ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Duplicate Finder Actions ---
    fun scanForDuplicates() {
        viewModelScope.launch {
            _duplicateScanProgress.value = 0f
            val groups = repository.findExactDuplicates { progress ->
                _duplicateScanProgress.value = progress
            }
            _duplicateGroups.value = groups
            _duplicateScanProgress.value = -1f // Scan done
        }
    }

    // --- Cleaner/Cache Operations ---
    fun getLargeFiles(): List<MediaItem> {
        // Returns files larger than 10MB
        return allMedia.value.filter { it.fileSize > 10 * 1024 * 1024 }.sortedByDescending { it.fileSize }
    }

    // --- Recently Viewed ---
    val recentlyViewed: StateFlow<List<MediaItem>> = db.galleryDao().getRecentlyViewedFlow()
        .combine(allMedia) { rvList, mediaList ->
            val mediaMap = mediaList.associateBy { it.id }
            rvList.mapNotNull { rv ->
                mediaMap[rv.mediaId]
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRecentlyViewed(mediaId: String) {
        viewModelScope.launch {
            db.galleryDao().insertRecentlyViewed(com.example.data.database.RecentlyViewedEntity(mediaId = mediaId))
        }
    }

    fun clearRecentlyViewed() {
        viewModelScope.launch {
            db.galleryDao().clearRecentlyViewed()
        }
    }

    // --- Favorite Collections ---
    val favoriteCollections: StateFlow<List<com.example.data.database.FavoriteCollectionEntity>> = db.galleryDao().getFavoriteCollectionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFavoriteCollection(name: String) {
        viewModelScope.launch {
            db.galleryDao().insertFavoriteCollection(com.example.data.database.FavoriteCollectionEntity(name = name))
        }
    }

    fun deleteFavoriteCollection(id: Int) {
        viewModelScope.launch {
            db.galleryDao().deleteFavoriteCollection(id)
        }
    }

    fun addSelectedToCollection(collectionId: Int) {
        val selectedIds = _selectedMediaIds.value
        viewModelScope.launch {
            selectedIds.forEach { mediaId ->
                db.galleryDao().insertCollectionMedia(com.example.data.database.CollectionMediaEntity(collectionId = collectionId, mediaId = mediaId))
            }
            clearSelection()
        }
    }

    fun removeMediaFromCollection(collectionId: Int, mediaId: String) {
        viewModelScope.launch {
            db.galleryDao().removeMediaFromCollection(collectionId, mediaId)
        }
    }

    fun getMediaInCollection(collectionId: Int): Flow<List<MediaItem>> {
        return db.galleryDao().getMediaIdsInCollectionFlow(collectionId)
            .combine(allMedia) { ids, mediaList ->
                val idSet = ids.toSet()
                mediaList.filter { it.id in idSet }
            }
    }

    // --- Hidden Albums ---
    val hiddenAlbums: StateFlow<List<com.example.data.database.HiddenAlbumEntity>> = db.galleryDao().getHiddenAlbumsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun hideAlbum(albumId: String, albumName: String) {
        viewModelScope.launch {
            db.galleryDao().insertHiddenAlbum(com.example.data.database.HiddenAlbumEntity(albumId = albumId, albumName = albumName))
        }
    }

    fun unhideAlbum(albumId: String) {
        viewModelScope.launch {
            db.galleryDao().deleteHiddenAlbum(albumId)
        }
    }

    // --- Locked Albums ---
    val lockedAlbums: StateFlow<List<com.example.data.database.LockedAlbumEntity>> = db.galleryDao().getLockedAlbumsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun lockAlbum(albumId: String, albumName: String, pin: String) {
        viewModelScope.launch {
            db.galleryDao().insertLockedAlbum(com.example.data.database.LockedAlbumEntity(albumId = albumId, albumName = albumName, pinHash = pin))
        }
    }

    fun unlockAlbum(albumId: String) {
        viewModelScope.launch {
            db.galleryDao().deleteLockedAlbum(albumId)
        }
    }

    // --- EXIF Edits ---
    suspend fun getExifEdit(mediaId: String) = db.galleryDao().getExifEdit(mediaId)

    fun saveExifEdit(mediaId: String, dateModified: Long?, lat: Double?, lng: Double?, camera: String?) {
        viewModelScope.launch {
            db.galleryDao().insertExifEdit(
                com.example.data.database.ExifEditEntity(
                    mediaId = mediaId,
                    dateModified = dateModified,
                    latitude = lat,
                    longitude = lng,
                    cameraModel = camera
                )
            )
            refreshMedia()
        }
    }
}

data class AlbumFolder(
    val id: String,
    val name: String,
    val coverUri: String?,
    val itemCount: Int,
    val isCustom: Boolean
)
