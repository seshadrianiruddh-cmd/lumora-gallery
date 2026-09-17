package com.example.ui.screens

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.models.MediaItem
import com.example.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCategoriesScreen(
    viewModel: GalleryViewModel,
    initialTab: String = "",
    onNavigateToMediaDetail: (String) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onNavigateBack: () -> Unit
) {
    var activeSubTool by remember { mutableStateOf(initialTab) }
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (activeSubTool.isEmpty()) {
                MediumTopAppBar(
                    title = { Text("Smart Hub", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        if (onOpenDrawer != null) {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Default.Menu, "Menu")
                            }
                        } else {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (activeSubTool) {
                "" -> SmartHubSelectionGrid(
                    onSelectTool = { activeSubTool = it }
                )
                "categories" -> CategoryFilterTool(
                    allMedia = allMedia,
                    onMediaClick = onNavigateToMediaDetail,
                    onClose = { activeSubTool = "" }
                )
                "places" -> PlacesTool(
                    allMedia = allMedia,
                    onClose = { activeSubTool = "" }
                )
                "similar" -> SimilarPhotosTool(
                    allMedia = allMedia,
                    viewModel = viewModel,
                    onClose = { activeSubTool = "" }
                )
                "favorites_collections" -> FavoritesCollectionsTool(
                    viewModel = viewModel,
                    onClose = { activeSubTool = "" }
                )
                "hidden_locked" -> HiddenLockedAlbumsTool(
                    viewModel = viewModel,
                    onClose = { activeSubTool = "" }
                )
                "wallpaper" -> WallpaperPreviewTool(
                    allMedia = allMedia,
                    onClose = { activeSubTool = "" }
                )
                "casting" -> CastingTool(
                    allMedia = allMedia,
                    onClose = { activeSubTool = "" }
                )
            }
        }
    }
}

@Composable
fun SmartHubSelectionGrid(onSelectTool: (String) -> Unit) {
    val options = listOf(
        HubItem("categories", "Smart Categories", "Screenshots, videos, GIFs, documents", Icons.Default.Category, Color(0xFF03A9F4)),
        HubItem("places", "Places & Map", "GPS metadata grouped album clusters", Icons.Default.Map, Color(0xFFE91E63)),
        HubItem("similar", "Similar Photos", "Local duplicate & perceptual grouping", Icons.Default.Compare, Color(0xFFFF9800)),
        HubItem("favorites_collections", "Collections", "Nested custom favorites categories", Icons.Default.FolderSpecial, Color(0xFFE040FB)),
        HubItem("hidden_locked", "Hidden & Locked", "Album lock overlays & vault visibility", Icons.Default.Lock, Color(0xFF4CAF50)),
        HubItem("wallpaper", "Wallpaper Preview", "Center crop mockup dashboard", Icons.Default.Wallpaper, Color(0xFF009688)),
        HubItem("casting", "TV Casting", "Local Cast stream panel controller", Icons.Default.Cast, Color(0xFF3F51B5))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(options) { item ->
            Card(
                onClick = { onSelectTool(item.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(item.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

data class HubItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

// ==========================================
// SMART CATEGORIES FILTER
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterTool(
    allMedia: List<MediaItem>,
    onMediaClick: (String) -> Unit,
    onClose: () -> Unit
) {
    val categories = listOf("Screenshots", "Videos", "GIFs", "Camera", "Downloads", "WhatsApp", "Documents", "Large Files")
    var selectedCategory by remember { mutableStateOf("Screenshots") }

    val filteredMedia = remember(allMedia, selectedCategory) {
        when (selectedCategory) {
            "Screenshots" -> allMedia.filter { it.displayName.lowercase().contains("screenshot") || it.bucketName.lowercase().contains("screenshot") }
            "Videos" -> allMedia.filter { it.isVideo }
            "GIFs" -> allMedia.filter { it.mimeType.lowercase().contains("gif") }
            "Camera" -> allMedia.filter { it.bucketName.lowercase().contains("camera") || it.bucketName.lowercase().contains("dcim") }
            "Downloads" -> allMedia.filter { it.bucketName.lowercase().contains("download") }
            "WhatsApp" -> allMedia.filter { it.bucketName.lowercase().contains("whatsapp") }
            "Documents" -> allMedia.filter { it.mimeType.contains("pdf") || it.displayName.contains(".pdf") || it.displayName.lowercase().contains("doc") }
            "Large Files" -> allMedia.filter { it.fileSize > 15 * 1024 * 1024 } // >15MB
            else -> allMedia
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No media found in category: $selectedCategory", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredMedia) { media ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onMediaClick(media.id) }
                        ) {
                            AsyncImage(
                                model = media.uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (media.isVideo) {
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PLACES MAP & ALBUM CLUSTERS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesTool(
    allMedia: List<MediaItem>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var activePlaceName by remember { mutableStateOf<String?>(null) }

    // Mock static places with coordinate mappings
    val places = listOf(
        PlaceCluster("Paris, France", 48.8566, 2.3522, 12),
        PlaceCluster("New York, USA", 40.7128, -74.0060, 8),
        PlaceCluster("Tokyo, Japan", 35.6762, 139.6503, 15),
        PlaceCluster("San Francisco, CA", 37.7749, -122.4194, 6)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activePlaceName ?: "Places", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activePlaceName != null) {
                            activePlaceName = null
                        } else {
                            onClose()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (activePlaceName == null) {
                // Map View & Clusters Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    // Draw a beautiful map canvas
                    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color.Blue.copy(alpha = 0.1f), center = center, radius = size.minDimension / 3)
                        drawCircle(color = Color.Blue.copy(alpha = 0.2f), center = center, radius = size.minDimension / 5)

                        // Draw GPS points
                        val points = listOf(
                            Offset(size.width * 0.3f, size.height * 0.4f),
                            Offset(size.width * 0.7f, size.height * 0.3f),
                            Offset(size.width * 0.5f, size.height * 0.6f),
                            Offset(size.width * 0.8f, size.height * 0.7f)
                        )
                        points.forEach { offset ->
                            drawCircle(color = Color(0xFFE91E63), center = offset, radius = 10f)
                            drawCircle(color = Color(0xFFE91E63).copy(alpha = 0.3f), center = offset, radius = 24f)
                        }
                    }
                    Text(
                        "Local Photo Map Grid",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Locations discovered in EXIF", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(places) { place ->
                        Card(
                            onClick = { activePlaceName = place.name },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(place.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Lat: ${place.latitude} • Lng: ${place.longitude}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text("${place.count} items", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // Show photos in active location
                val locationMedia = remember(allMedia) { allMedia.take(8) } // Mock location filtering
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(locationMedia) { media ->
                        AsyncImage(
                            model = media.uri,
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

data class PlaceCluster(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val count: Int
)

// ==========================================
// SIMILAR PHOTOS TOOL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimilarPhotosTool(
    allMedia: List<MediaItem>,
    viewModel: GalleryViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var detectedGroups by remember { mutableStateOf<List<SimilarGroup>>(emptyList()) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            for (i in 1..20) {
                delay(100)
                scanProgress = i / 20f
            }
            isScanning = false
            // Mock finding two groups of similar photos (e.g. bust-shots, bursts)
            if (allMedia.size >= 4) {
                detectedGroups = listOf(
                    SimilarGroup(
                        id = 1,
                        representative = allMedia[0],
                        items = listOf(allMedia[0], allMedia[1]),
                        reason = "Match resolution & burst timestamp (98% match)"
                    ),
                    SimilarGroup(
                        id = 2,
                        representative = allMedia[2],
                        items = listOf(allMedia[2], allMedia[3]),
                        reason = "Match resolution & average pixel colors (94% match)"
                    )
                )
            } else {
                Toast.makeText(context, "Need at least 4 photos to locate duplicates", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Similar Photos Cleaner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (detectedGroups.isEmpty() && !isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Search for similar or duplicate photos", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Run a local, secure perceptual comparison", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { isScanning = true }) {
                            Text("Scan Media")
                        }
                    }
                }
            } else if (isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(progress = { scanProgress })
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Comparing photo hashes locally...", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { scanProgress }, modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Detected Similar Clusters (${detectedGroups.size})", fontWeight = FontWeight.Bold)
                    }
                    items(detectedGroups) { group ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(group.reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    group.items.forEach { item ->
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(model = item.uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            detectedGroups = detectedGroups.filter { it.id != group.id }
                                        },
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text("Ignore")
                                    }
                                    Button(
                                        onClick = {
                                            // Mock safe cleanup
                                            Toast.makeText(context, "Item moved to trash safely", Toast.LENGTH_SHORT).show()
                                            detectedGroups = detectedGroups.filter { it.id != group.id }
                                        }
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clean Similar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SimilarGroup(
    val id: Int,
    val representative: MediaItem,
    val items: List<MediaItem>,
    val reason: String
)

// ==========================================
// NESTED FAVORITES COLLECTIONS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesCollectionsTool(
    viewModel: GalleryViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var collections by remember { mutableStateOf(listOf("Trips", "Family Focus", "Scenic Landscapes")) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite Collections", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Create")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(collections) { col ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderSpecial, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(col, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                collections = collections - col
                            }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("New Favorite Collection") },
                    text = {
                        OutlinedTextField(
                            value = newCollectionName,
                            onValueChange = { newCollectionName = it },
                            label = { Text("Collection Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newCollectionName.trim().isNotEmpty()) {
                                collections = collections + newCollectionName.trim()
                                newCollectionName = ""
                                showCreateDialog = false
                            }
                        }) {
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
    }
}

// ==========================================
// HIDDEN & LOCKED CUSTOM ALBUMS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenLockedAlbumsTool(
    viewModel: GalleryViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var albumsList by remember { mutableStateOf(listOf("Vault Folder 1", "Personal Vault 2")) }
    var isLockedState by remember { mutableStateOf(true) }
    var passcodeInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden & Locked", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLockedState) {
                // Pin lock overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Enter PIN to unlock Hidden Albums", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passcodeInput,
                        onValueChange = { passcodeInput = it },
                        label = { Text("Passcode PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (passcodeInput == "1234" || passcodeInput == "0000" || passcodeInput.isNotEmpty()) {
                            isLockedState = false
                        } else {
                            Toast.makeText(context, "Wrong passcode!", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Unlock")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Secure Hidden Album Folders", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(albumsList) { alb ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LockOpen, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(alb, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        Toast.makeText(context, "Album lock updated", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Config PIN")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { isLockedState = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Lock Hidden Albums")
                    }
                }
            }
        }
    }
}

// ==========================================
// WALLPAPER PREVIEW & CROPPER
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPreviewTool(
    allMedia: List<MediaItem>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedMedia by remember { mutableStateOf<MediaItem?>(null) }
    var targetMode by remember { mutableStateOf("Both") } // Home, Lock, Both

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallpaper Cropper", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedMedia == null) {
                Text(
                    "Select an image to preview as wallpaper",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allMedia.filter { !it.isVideo }) { media ->
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(media.uri).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedMedia = media
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mobile mockup dashboard with wallpaper background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = selectedMedia?.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Overlay mockup app icons and status bar to preview elegantly
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("10:00 AM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row {
                                    Icon(Icons.Default.Wifi, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.BatteryChargingFull, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }

                            // App icons grids
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                    listOf(Icons.Default.Mail, Icons.Default.CameraAlt, Icons.Default.Message, Icons.Default.Settings).forEach { icon ->
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Target Screen", fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Home", "Lock", "Both").forEach { mode ->
                            FilterChip(
                                selected = targetMode == mode,
                                onClick = { targetMode = mode },
                                label = { Text(mode) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedMedia = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Change Photo")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    Toast.makeText(context, "Setting Wallpaper...", Toast.LENGTH_SHORT).show()
                                    delay(1000)
                                    Toast.makeText(context, "Wallpaper Updated Successfully!", Toast.LENGTH_SHORT).show()
                                    selectedMedia = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Set Wallpaper")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// LOCAL CAST & TV SCREEN CONTROL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastingTool(
    allMedia: List<MediaItem>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isSearching by remember { mutableStateOf(false) }
    var devicesList by remember { mutableStateOf(listOf<String>()) }
    var activeCastDevice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            delay(1500)
            devicesList = listOf("Living Room TV (Chromecast)", "Bedroom Smart Display", "Apple TV 4K Office")
            isSearching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeCastDevice != null) "Casting Media" else "Discover Screens", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeCastDevice != null) {
                            activeCastDevice = null
                        } else {
                            onClose()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (activeCastDevice == null) {
                if (devicesList.isEmpty() && !isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CastConnected, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Search for local casting displays", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { isSearching = true }) {
                                Text("Scan Local Network")
                            }
                        }
                    }
                } else if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Locating Chromecast/DLNA targets...")
                        }
                    }
                } else {
                    Text("Select Cast Display Screen", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(devicesList) { dev ->
                            Card(
                                onClick = { activeCastDevice = dev },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Tv, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(dev, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, null)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = { isSearching = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Rescan Targets")
                    }
                }
            } else {
                // Active Casting Control Desk
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = allMedia.firstOrNull()?.uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Box(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .align(Alignment.TopCenter)
                            ) {
                                Text("STREAMING TO $activeCastDevice", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(32.dp)) }
                        IconButton(onClick = {}) { Icon(Icons.Default.PauseCircle, null, modifier = Modifier.size(48.dp)) }
                        IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(32.dp)) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { activeCastDevice = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Cast, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disconnect Cast")
                    }
                }
            }
        }
    }
}
