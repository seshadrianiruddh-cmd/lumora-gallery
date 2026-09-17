package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyState
import com.example.ui.components.MediaGridItem
import com.example.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.flow.map


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    viewModel: GalleryViewModel,
    albumId: String,
    albumName: String,
    isCustom: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect album-specific media
    val albumMediaFlow = if (isCustom) {
        viewModel.getMediaInAlbum(albumId.toInt())
    } else {
        viewModel.allMedia.map { items -> items.filter { it.bucketId == albumId } }
    }
    
    val albumMedia by albumMediaFlow.collectAsState(initial = emptyList())
    val selectedMediaIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val gridDensity by viewModel.gridDensity.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelection()
        }
    }

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
                        IconButton(onClick = {
                            val albumMediaIds = albumMedia.map { it.id }.toSet()
                            viewModel.setSelectedIds(albumMediaIds)
                        }) {
                            Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(albumName, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (isCustom) {
                            IconButton(onClick = {
                                viewModel.deleteAlbum(albumId.toInt())
                                onNavigateBack()
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Album", tint = MaterialTheme.colorScheme.error)
                            }
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
                                    val selectedUris = albumMedia
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

                        // Action: Remove from Album (only for custom albums)
                        if (isCustom) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.removeSelectedFromAlbum(albumId.toInt())
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close, 
                                    contentDescription = "Remove from Album",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Remove", 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Action: Move to Trash (delete from device)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.moveSelectedToTrash()
                                }
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
        if (albumMedia.isEmpty()) {
            Box(modifier = Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Album is Empty",
                    description = "Add photos or videos to this custom album from the main Photos tab."
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridDensity),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 4.dp)
            ) {
                itemsIndexed(albumMedia, key = { _, item -> item.id }) { index, item ->
                    MediaGridItem(
                        item = item,
                        isSelected = selectedMediaIds.contains(item.id),
                        isInSelectionMode = isSelectionMode,
                        onItemClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSelection(item.id)
                            } else {
                                // Find overall index of this item in the global displaying feed so viewer swipes across all
                                val overallIndex = viewModel.displayedMedia.value.indexOfFirst { it.id == item.id }
                                if (overallIndex != -1) {
                                    onNavigateToViewer(overallIndex)
                                } else {
                                    // Fallback to local viewer
                                    onNavigateToViewer(index)
                                }
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
