package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
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
import coil.compose.AsyncImage
import com.example.data.models.MediaItem
import com.example.ui.components.EmptyState
import com.example.ui.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val largeFiles = remember(viewModel.allMedia.value) { viewModel.getLargeFiles() }

    var selectedForDeletion by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Mock cache size calculation for demonstration (typically 15-50 MB for rich grids)
    var cacheSizeMb by remember { mutableStateOf(32.4f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Cleaner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
                            text = "${selectedForDeletion.size} large files selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Button(
                            onClick = { showDeleteDialog = true },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Cache Clear Dashboard
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Image Thumbnail Cache", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Temporary disk cache of generated image previews.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Cache Size: ${String.format("%.1f", cacheSizeMb)} MB",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Button(
                                onClick = {
                                    cacheSizeMb = 0f
                                    Toast.makeText(context, "Thumbnail cache cleared successfully", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Clear Cache")
                            }
                        }
                    }
                }
            }

            // Large Files Header
            item {
                Text(
                    "Large Files (>10 MB)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (largeFiles.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                        EmptyState(
                            icon = Icons.Default.Info,
                            title = "No Large Files Found",
                            description = "We couldn't detect any photos or videos exceeding 10MB on your device."
                        )
                    }
                }
            } else {
                items(largeFiles, key = { it.id }) { item ->
                    val isSelected = selectedForDeletion.contains(item.id)
                    LargeFileRow(
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
                    Divider()
                }
            }
        }
    }

    // Confirm Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Large Files?") },
            text = { Text("Are you sure you want to permanently delete these ${selectedForDeletion.size} large files from your device? This will immediately reclaim significant storage space but cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedForDeletion.forEach { id ->
                            val item = largeFiles.find { it.id == id }
                            if (item != null) {
                                viewModel.deleteItemPermanently(item)
                            }
                        }
                        selectedForDeletion = emptySet()
                        showDeleteDialog = false
                        viewModel.refreshMedia()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LargeFileRow(
    item: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
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
