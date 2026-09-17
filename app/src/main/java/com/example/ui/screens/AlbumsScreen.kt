package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.components.EmptyState
import com.example.ui.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: GalleryViewModel,
    onNavigateToAlbumDetail: (String, String, Boolean) -> Unit, // (folderId, title, isCustom)
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val customAlbums by viewModel.customAlbums.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Albums", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Create Album")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (albums.isEmpty() && customAlbums.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Folder,
                title = "No Albums Found",
                description = "Create custom albums to group your photos, or grant permissions to discover local folders.",
                actionText = "Create Custom Album",
                onActionClick = { showCreateDialog = true }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // --- CUSTOM ALBUMS SECTION ---
                if (customAlbums.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "My Albums",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp, top = 16.dp, bottom = 12.dp)
                        )
                    }
                    items(customAlbums, key = { "custom_${it.id}" }) { album ->
                        AlbumGridItem(
                            title = album.name,
                            itemCount = 0, // In dynamic real systems we can fetch item count asynchronously
                            coverUri = album.coverUri,
                            onClick = {
                                onNavigateToAlbumDetail(album.id.toString(), album.name, true)
                            }
                        )
                    }
                }

                // --- SYSTEM FOLDERS SECTION ---
                if (albums.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "Device Folders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp, top = 24.dp, bottom = 12.dp)
                        )
                    }
                    items(albums, key = { "system_${it.id}" }) { album ->
                        AlbumGridItem(
                            title = album.name,
                            itemCount = album.itemCount,
                            coverUri = album.coverUri,
                            onClick = {
                                onNavigateToAlbumDetail(album.id, album.name, false)
                            }
                        )
                    }
                }
            }
        }
    }

    // Create Album Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Album") },
            text = {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    label = { Text("Album Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAlbumName.trim().isNotEmpty()) {
                            viewModel.createAlbum(newAlbumName.trim())
                            newAlbumName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AlbumGridItem(
    title: String,
    itemCount: Int,
    coverUri: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        if (itemCount > 0) {
            Text(
                text = "$itemCount items",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
