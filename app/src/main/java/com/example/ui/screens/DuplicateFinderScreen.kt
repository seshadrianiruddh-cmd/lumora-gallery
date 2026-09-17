package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

import com.example.data.models.MediaItem
import com.example.ui.components.EmptyState
import com.example.ui.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateFinderScreen(
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scanProgress by viewModel.duplicateScanProgress.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()

    var selectedForDeletion by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicate Finder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (duplicateGroups.isNotEmpty() && scanProgress == -1f) {
                        IconButton(onClick = { viewModel.scanForDuplicates() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedForDeletion.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedForDeletion.size} duplicates selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Selected")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (scanProgress != -1f) {
                // Scanning Progress UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        progress = scanProgress,
                        modifier = Modifier.size(72.dp),
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Analyzing Media Files...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Calculating digital signatures (MD5 hashes) of all photos on device... ${(scanProgress * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (duplicateGroups.isEmpty()) {
                // Initial or Empty state
                EmptyState(
                    icon = Icons.Default.CopyAll,
                    title = "Find Exact Duplicates",
                    description = "Lumora analyzes your device media on-device to group identical files together so you can easily reclaim wasted storage.",
                    actionText = "Scan Library",
                    onActionClick = { viewModel.scanForDuplicates() }
                )
            } else {
                // Duplicate groups list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Review identical items. Lumora helps you keep at least one copy safe.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    duplicateGroups.forEach { (hash, itemsList) ->
                        item {
                            Text(
                                text = "Group (Hash: ${hash.take(8)}) - ${itemsList.size} copies",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(itemsList) { item ->
                            val isSelected = selectedForDeletion.contains(item.id)
                            DuplicateFileRow(
                                item = item,
                                isSelected = isSelected,
                                onClick = {
                                    selectedForDeletion = if (isSelected) {
                                        selectedForDeletion - item.id
                                    } else {
                                        selectedForDeletion + item.id
                                    }
                                }
                            )
                        }

                        item {
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }

    // Confirm deletion dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Selected Duplicates?") },
            text = { Text("Are you sure you want to permanently delete these ${selectedForDeletion.size} duplicate files from your device? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedForDeletion.forEach { id ->
                            val item = viewModel.allMedia.value.find { it.id == id }
                            if (item != null) {
                                viewModel.deleteItemPermanently(item)
                            }
                        }
                        selectedForDeletion = emptySet()
                        showDeleteConfirmDialog = false
                        viewModel.scanForDuplicates() // re-scan to update list
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DuplicateFileRow(
    item: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Size: ${formatFileSize(item.fileSize)} | Folder: ${item.bucketName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClick() }
        )
    }
}
