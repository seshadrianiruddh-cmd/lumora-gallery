package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.MediaItem

import com.example.ui.components.EmptyState
import com.example.ui.components.MediaGridItem
import com.example.ui.viewmodel.GalleryViewModel

private fun getMediaIdAtOffset(
    offset: androidx.compose.ui.geometry.Offset,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    displayedIds: Set<String>
): String? {
    val visibleItems = gridState.layoutInfo.visibleItemsInfo
    val hit = visibleItems.firstOrNull { info ->
        val x = info.offset.x.toFloat()
        val y = info.offset.y.toFloat()
        val w = info.size.width.toFloat()
        val h = info.size.height.toFloat()
        offset.x >= x && offset.x <= (x + w) && offset.y >= y && offset.y <= (y + h)
    }
    val keyStr = hit?.key?.toString() ?: return null
    return if (displayedIds.contains(keyStr)) keyStr else null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    viewModel: GalleryViewModel,
    onNavigateToViewer: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCreativeStudio: () -> Unit,
    onNavigateToSmartHub: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayedMedia by viewModel.displayedMedia.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val timelineMedia by viewModel.timelineMedia.collectAsStateWithLifecycle()
    val selectedMediaIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val gridDensity by viewModel.gridDensity.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filterType.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortOrder.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    val customAlbums by viewModel.customAlbums.collectAsStateWithLifecycle()

    val gridState = rememberLazyGridState()
    val displayedIds = remember(displayedMedia) { displayedMedia.map { it.id }.toSet() }
    var dragStartSelected by remember { mutableStateOf<Boolean?>(null) }
    val touchedIds = remember { mutableStateListOf<String>() }

    var gridScaleTarget by remember { mutableStateOf(1f) }
    val animatedGridScale by animateFloatAsState(
        targetValue = gridScaleTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "animatedGridScale"
    )

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedMediaIds.size} Selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                LargeTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Lumora Gallery",
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToCreativeStudio) {
                            Icon(imageVector = Icons.Default.Brush, contentDescription = "Creative Studio")
                        }
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.Outlined.Sort, contentDescription = "Sort Options")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                Surface(
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Action: Share
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val selectedUris = displayedMedia
                                        .filter { it.id in selectedMediaIds }
                                        .map { Uri.parse(it.uri) }
                                    
                                    if (selectedUris.isNotEmpty()) {
                                        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                            type = "image/* video/*"
                                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selectedUris))
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Media"))
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Share", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Action: Add to Album
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAddToAlbumDialog = true }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Add to Album")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Album", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Action: Hide into secure Private Vault
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.importSelectedToVault()
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Move to Vault")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Vault", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Action: Trash
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.moveSelectedToTrash() }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Move to Trash",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Trash",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Horizontal Scroll Row
            if (!isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "all" to "All Media",
                        "photos" to "Photos",
                        "videos" to "Videos",
                        "favorites" to "Favorites"
                    )
                    filters.forEach { (type, label) ->
                        FilterChip(
                            selected = currentFilter == type,
                            onClick = { viewModel.setFilterType(type) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else if (displayedMedia.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptyState(
                        icon = Icons.Default.PhotoLibrary,
                        title = if (currentFilter == "favorites") "No Favorites Yet" else "Gallery is Empty",
                        description = if (currentFilter == "favorites") 
                            "Tap the heart icon on any photo or video to add it to your favorites." 
                            else "No photos or videos found on your device.",
                        actionText = "Refresh Library",
                        onActionClick = { viewModel.refreshMedia() }
                    )
                }
            } else {
                val currentDensityState = rememberUpdatedState(gridDensity)
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(gridDensity),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .graphicsLayer(
                            scaleX = animatedGridScale,
                            scaleY = animatedGridScale,
                            transformOrigin = TransformOrigin.Center
                        )
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                var accumulativeZoom = 1f
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val canceled = event.changes.any { it.isConsumed }
                                    if (!canceled) {
                                        val zoomChange = event.calculateZoom()
                                        if (zoomChange != 1f) {
                                            accumulativeZoom *= zoomChange
                                            gridScaleTarget = accumulativeZoom
                                            
                                            if (accumulativeZoom > 1.35f) {
                                                // Pinch Out (spread fingers): Zoom IN -> Show fewer columns (larger items)
                                                val densityVal = currentDensityState.value
                                                val newDensity = (densityVal - 1).coerceAtLeast(2)
                                                if (newDensity != densityVal) {
                                                    viewModel.setGridDensity(newDensity)
                                                }
                                                accumulativeZoom = 1f
                                                gridScaleTarget = 1f
                                            } else if (accumulativeZoom < 0.70f) {
                                                // Pinch In (bring fingers together): Zoom OUT -> Show more columns (smaller items)
                                                val densityVal = currentDensityState.value
                                                val newDensity = (densityVal + 1).coerceAtMost(8)
                                                if (newDensity != densityVal) {
                                                    viewModel.setGridDensity(newDensity)
                                                }
                                                accumulativeZoom = 1f
                                                gridScaleTarget = 1f
                                            }
                                            
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (!canceled && event.changes.any { it.pressed })
                                
                                gridScaleTarget = 1f
                            }
                        },
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Render date grouped sections or sequential grid
                    if (currentSort == "newest_first" && currentFilter == "all") {
                        timelineMedia.forEach { (dateHeader, items) ->
                            item(span = { GridItemSpan(gridDensity) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateHeader,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    if (isSelectionMode) {
                                        val allInGroupSelected = items.all { selectedMediaIds.contains(it.id) }
                                        TextButton(
                                            onClick = { viewModel.toggleGroupSelection(items) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (allInGroupSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (allInGroupSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (allInGroupSelected) "Deselect All" else "Select All",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (allInGroupSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            items(items, key = { it.id }) { item ->
                                val overallIndex = displayedMedia.indexOfFirst { it.id == item.id }
                                MediaGridItem(
                                    item = item,
                                    isSelected = selectedMediaIds.contains(item.id),
                                    isInSelectionMode = isSelectionMode,
                                    onItemClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleSelection(item.id)
                                        } else {
                                            onNavigateToViewer(overallIndex)
                                        }
                                    },
                                    onItemLongClick = {
                                        viewModel.toggleSelection(item.id)
                                    }
                                )
                            }
                        }
                    } else {
                        // Standard sequential grid (e.g. sorted by size or name)
                        itemsIndexed(displayedMedia, key = { _, item -> item.id }) { index, item ->
                            MediaGridItem(
                                item = item,
                                isSelected = selectedMediaIds.contains(item.id),
                                isInSelectionMode = isSelectionMode,
                                onItemClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(item.id)
                                    } else {
                                        onNavigateToViewer(index)
                                    }
                                },
                                onItemLongClick = {
                                    viewModel.toggleSelection(item.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Sort Dropdown Menu Dialogue
    if (showSortMenu) {
        AlertDialog(
            onDismissRequest = { showSortMenu = false },
            title = { Text("Sort Media By") },
            text = {
                Column {
                    val sortOptions = listOf(
                        "newest_first" to "Newest First",
                        "oldest_first" to "Oldest First",
                        "name" to "File Name",
                        "size" to "File Size"
                    )
                    sortOptions.forEach { (type, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSortOrder(type)
                                    showSortMenu = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSort == type,
                                onClick = {
                                    viewModel.setSortOrder(type)
                                    showSortMenu = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortMenu = false }) { Text("Cancel") }
            }
        )
    }

    // Add To Album Dialog
    if (showAddToAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showAddToAlbumDialog = false },
            title = { Text("Add Selected to Album") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (customAlbums.isEmpty()) {
                        Text(
                            "You don't have any custom albums created yet. Go to the Albums tab to create a new folder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                            items(customAlbums) { album ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addSelectedToAlbum(album.id)
                                            showAddToAlbumDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(album.name, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToAlbumDialog = false }) { Text("Close") }
            }
        )
    }
}
