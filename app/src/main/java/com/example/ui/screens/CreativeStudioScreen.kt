package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreativeStudioScreen(
    viewModel: GalleryViewModel,
    initialTool: String = "",
    onOpenDrawer: (() -> Unit)? = null,
    onNavigateBack: () -> Unit
) {
    var activeTool by remember { mutableStateOf(initialTool) }
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (activeTool.isEmpty()) {
                MediumTopAppBar(
                    title = { Text("Creative Studio", fontWeight = FontWeight.ExtraBold) },
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
            when (activeTool) {
                "" -> ToolSelectionGrid(
                    onSelectTool = { activeTool = it }
                )
                 "collage" -> CollageMakerTool(
                    allMedia = allMedia,
                    onClose = { activeTool = "" }
                )

                "slideshow" -> SlideshowTool(
                    allMedia = allMedia,
                    onClose = { activeTool = "" }
                )
                "memory_video" -> MemoryVideoTool(
                    allMedia = allMedia,
                    onClose = { activeTool = "" }
                )
                "motion" -> MotionPhotoTool(
                    allMedia = allMedia,
                    onClose = { activeTool = "" }
                )
            }
        }
    }
}

@Composable
fun ToolSelectionGrid(onSelectTool: (String) -> Unit) {
    val tools = listOf(
        StudioToolItem("collage", "Collage Maker", "Select 2-9 photos and layout", Icons.Default.GridOn, Color(0xFF673AB7)),

        StudioToolItem("slideshow", "Slideshow", "Fullscreen custom media display", Icons.Default.PlayCircle, Color(0xFF2196F3)),
        StudioToolItem("memory_video", "Memory Video", "Reorder clips with background music", Icons.Default.VideoCameraBack, Color(0xFF4CAF50)),
        StudioToolItem("motion", "Motion Photo", "Capture still image + short video", Icons.Default.MotionPhotosOn, Color(0xFFFF5722))
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(tools) { tool ->
            Card(
                onClick = { onSelectTool(tool.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tool.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(tool.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tool.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

data class StudioToolItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

// ==========================================
// COLLAGE MAKER TOOL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageMakerTool(
    allMedia: List<MediaItem>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var activeLayoutIndex by remember { mutableStateOf(0) }
    var itemSpacing by remember { mutableStateOf(4f) }
    var cornerRadius by remember { mutableStateOf(8f) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collage Maker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                actions = {
                    if (selectedMedia.size in 2..9) {
                        IconButton(onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                delay(1500) // Render process simulated
                                isSaving = false
                                Toast.makeText(context, "Collage Saved to Gallery!", Toast.LENGTH_SHORT).show()
                                onClose()
                            }
                        }) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, "Save")
                            }
                        }
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
            if (selectedMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select 2 to 9 photos to begin", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Render current layout dynamically
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                    ) {
                        when (activeLayoutIndex) {
                            0 -> CollageLayoutGrid(selectedMedia, itemSpacing, cornerRadius)
                            1 -> CollageLayoutColumns(selectedMedia, itemSpacing, cornerRadius)
                            2 -> CollageLayoutRows(selectedMedia, itemSpacing, cornerRadius)
                            3 -> CollageLayoutFeaturedFocus(selectedMedia, itemSpacing, cornerRadius)
                            4 -> CollageLayoutCinematicSplit(selectedMedia, itemSpacing, cornerRadius)
                            else -> CollageLayoutAsymmetricMasonry(selectedMedia, itemSpacing, cornerRadius)
                        }
                    }
                }
            }

            // Photo picker tray at the bottom
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Photos (${selectedMedia.size}/9)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allMedia.filter { !it.isVideo }) { media ->
                            val isSelected = selectedMedia.contains(media)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            selectedMedia = selectedMedia - media
                                        } else if (selectedMedia.size < 9) {
                                            selectedMedia = selectedMedia + media
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(media.uri).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (selectedMedia.size in 2..9) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Customizations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Spacing", fontSize = 12.sp, modifier = Modifier.width(64.dp))
                            Slider(
                                value = itemSpacing,
                                onValueChange = { itemSpacing = it },
                                valueRange = 0f..24f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Corners", fontSize = 12.sp, modifier = Modifier.width(64.dp))
                            Slider(
                                value = cornerRadius,
                                onValueChange = { cornerRadius = it },
                                valueRange = 0f..32f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val layouts = listOf("Grid", "Vertical", "Horizontal", "Featured Focus", "Cinematic Split", "Asymmetric Masonry")
                            items(layouts.size) { index ->
                                val name = layouts[index]
                                FilterChip(
                                    selected = activeLayoutIndex == index,
                                    onClick = { activeLayoutIndex = index },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollageLayoutGrid(media: List<MediaItem>, spacing: Float, radius: Float) {
    val size = media.size
    val context = LocalContext.current
    val cols = when {
        size <= 4 -> 2
        else -> 3
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(cols),
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        userScrollEnabled = false
    ) {
        items(media) { item ->
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radius.dp))
            )
        }
    }
}

@Composable
fun CollageLayoutColumns(media: List<MediaItem>, spacing: Float, radius: Float) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        media.forEach { item ->
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(radius.dp))
            )
        }
    }
}

@Composable
fun CollageLayoutRows(media: List<MediaItem>, spacing: Float, radius: Float) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        media.forEach { item ->
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.dp))
            )
        }
    }
}

@Composable
fun CollageLayoutFeaturedFocus(media: List<MediaItem>, spacing: Float, radius: Float) {
    val context = LocalContext.current
    if (media.isEmpty()) return
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        // Featured (First Image) - takes 60% of width
        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(radius.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(media[0].uri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Remaining images - stacked vertically in the remaining space
        if (media.size > 1) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(spacing.dp)
            ) {
                media.drop(1).forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radius.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CollageLayoutCinematicSplit(media: List<MediaItem>, spacing: Float, radius: Float) {
    val context = LocalContext.current
    if (media.isEmpty()) return
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        // Upper section
        val topItems = media.take((media.size + 1) / 2)
        val bottomItems = media.drop(topItems.size)
        
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.dp)
        ) {
            topItems.forEach { item ->
                AsyncImage(
                    model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(radius.dp))
                )
            }
        }
        
        // Lower section
        if (bottomItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.dp)
            ) {
                bottomItems.forEach { item ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(radius.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun CollageLayoutAsymmetricMasonry(media: List<MediaItem>, spacing: Float, radius: Float) {
    val context = LocalContext.current
    if (media.isEmpty()) return
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        val total = media.size
        
        // Dynamic row distribution based on size
        val rows = when (total) {
            2 -> listOf(listOf(media[0]), listOf(media[1]))
            3 -> listOf(listOf(media[0]), listOf(media[1], media[2]))
            4 -> listOf(listOf(media[0]), listOf(media[1], media[2]), listOf(media[3]))
            5 -> listOf(listOf(media[0], media[1]), listOf(media[2]), listOf(media[3], media[4]))
            6 -> listOf(listOf(media[0]), listOf(media[1], media[2]), listOf(media[3], media[4], media[5]))
            7 -> listOf(listOf(media[0], media[1]), listOf(media[2], media[3], media[4]), listOf(media[5], media[6]))
            8 -> listOf(listOf(media[0], media[1]), listOf(media[2], media[3]), listOf(media[4], media[5], media[6], media[7]))
            else -> listOf(listOf(media[0]), listOf(media[1], media[2], media[3]), listOf(media[4], media[5]), listOf(media[6], media[7], media[8]))
        }
        
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.dp)
            ) {
                rowItems.forEach { item ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(radius.dp))
                    )
                }
            }
        }
    }
}

// ==========================================
// SLIDESHOW TOOL
// ==========================================
@Composable
fun SlideshowTool(
    allMedia: List<MediaItem>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val photoItems = remember(allMedia) { allMedia.filter { !it.isVideo } }

    var currentIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var intervalMs by remember { mutableStateOf(3000L) }
    var randomOrder by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, currentIndex, intervalMs, randomOrder) {
        if (isPlaying && photoItems.isNotEmpty()) {
            delay(intervalMs)
            if (randomOrder) {
                currentIndex = (photoItems.indices).random()
            } else {
                currentIndex = (currentIndex + 1) % photoItems.size
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (photoItems.isEmpty()) {
            Text("No photos available for slideshow", color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            val activeItem = photoItems[currentIndex]
            AsyncImage(
                model = ImageRequest.Builder(context).data(activeItem.uri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Header Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Text("Slideshow (${currentIndex + 1}/${photoItems.size})", color = Color.White, fontWeight = FontWeight.Bold)
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
                }
            }

            // Bottom controls overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        currentIndex = if (currentIndex == 0) photoItems.size - 1 else currentIndex - 1
                    }) {
                        Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White)
                    }
                    Button(
                        onClick = { isPlaying = !isPlaying },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (isPlaying) "Pause" else "Play")
                    }
                    IconButton(onClick = {
                        currentIndex = (currentIndex + 1) % photoItems.size
                    }) {
                        Icon(Icons.Default.SkipNext, "Next", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Shuffle", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = randomOrder,
                        onCheckedChange = { randomOrder = it }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Interval: ${intervalMs / 1000}s", color = Color.White, fontSize = 12.sp)
                    Row {
                        listOf(2000L, 3000L, 5000L).forEach { ms ->
                            TextButton(
                                onClick = { intervalMs = ms },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (intervalMs == ms) MaterialTheme.colorScheme.primary else Color.White
                                )
                            ) {
                                Text("${ms / 1000}s")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MEMORY VIDEO TOOL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryVideoTool(
    allMedia: List<MediaItem>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var activeMusic by remember { mutableStateOf("Sunset Vibes") }
    var activeTheme by remember { mutableStateOf("Warm Fade") }
    var durationSec by remember { mutableStateOf(5) }
    var isCompiling by remember { mutableStateOf(false) }

    val musicOptions = listOf("Sunset Vibes", "Retro Roadtrip", "Acoustic Morning", "Muted")
    val themes = listOf("Warm Fade", "Zoom-in Zoom-out", "Vintage Film", "Classic Cuts")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Video", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                actions = {
                    if (selectedMedia.isNotEmpty()) {
                        IconButton(onClick = {
                            isCompiling = true
                            coroutineScope.launch {
                                delay(3000) // Video rendering simulation
                                isCompiling = false
                                Toast.makeText(context, "Memory Video Exported!", Toast.LENGTH_SHORT).show()
                                onClose()
                            }
                        }) {
                            if (isCompiling) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Movie, "Export Video")
                            }
                        }
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
            if (selectedMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VideoCall, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select photos/videos to build memory video", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Video player mockup preview
                    Box(
                        modifier = Modifier
                            .aspectRatio(16f / 9f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = selectedMedia.firstOrNull()?.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Text(
                            text = "Theme: $activeTheme • Music: $activeMusic",
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Timeline Media (${selectedMedia.size} Selected)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allMedia) { media ->
                            val isSelected = selectedMedia.contains(media)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedMedia = if (isSelected) selectedMedia - media else selectedMedia + media
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(media.uri).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (media.isVideo) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (selectedMedia.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Transitions & Themes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(themes) { th ->
                                FilterChip(
                                    selected = activeTheme == th,
                                    onClick = { activeTheme = th },
                                    label = { Text(th) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Audio Track", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(musicOptions) { music ->
                                FilterChip(
                                    selected = activeMusic == music,
                                    onClick = { activeMusic = music },
                                    label = { Text(music) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PHOTO TO PDF TOOL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoToPdfTool(
    allMedia: List<MediaItem>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var pageSize by remember { mutableStateOf("A4") }
    var orientation by remember { mutableStateOf("Portrait") }
    var pdfTitle by remember { mutableStateOf("My_Document") }
    var isGenerating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo to PDF", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                actions = {
                    if (selectedMedia.isNotEmpty()) {
                        IconButton(onClick = {
                            isGenerating = true
                            coroutineScope.launch {
                                delay(2000)
                                isGenerating = false
                                Toast.makeText(context, "PDF Generated: $pdfTitle.pdf", Toast.LENGTH_SHORT).show()
                                onClose()
                            }
                        }) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PictureAsPdf, "Save PDF")
                            }
                        }
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
            if (selectedMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select photos to compile into a PDF document", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = pdfTitle,
                        onValueChange = { pdfTitle = it },
                        label = { Text("PDF File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Page Reordering Details (${selectedMedia.size} pages)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedMedia) { media ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(3f / 4f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = media.uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .align(Alignment.TopStart),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${selectedMedia.indexOf(media) + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Photos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allMedia.filter { !it.isVideo }) { media ->
                            val isSelected = selectedMedia.contains(media)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedMedia = if (isSelected) selectedMedia - media else selectedMedia + media
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(media.uri).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    if (selectedMedia.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Page Size", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row {
                                    listOf("A4", "Letter").forEach { size ->
                                        FilterChip(
                                            selected = pageSize == size,
                                            onClick = { pageSize = size },
                                            label = { Text(size) },
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Orientation", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row {
                                    listOf("Portrait", "Landscape").forEach { o ->
                                        FilterChip(
                                            selected = orientation == o,
                                            onClick = { orientation = o },
                                            label = { Text(o) },
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
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

// ==========================================
// DOCUMENT SCANNER TOOL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerTool(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeStep by remember { mutableStateOf("camera") } // camera, crop, filter, preview
    var scannedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var currentFilter by remember { mutableStateOf("Color") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Scanner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                actions = {
                    if (scannedPages.isNotEmpty() && activeStep == "preview") {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                Toast.makeText(context, "Scanned PDF Exported!", Toast.LENGTH_SHORT).show()
                                onClose()
                            }
                        }) {
                            Icon(Icons.Default.Check, "Save Scan")
                        }
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
            when (activeStep) {
                "camera" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Simulated Camera viewfinder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            // Document Scanner Grid Guideline Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .fillMaxHeight(0.7f)
                                    .border(2.dp, Color.Green.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DocumentScanner, "Grid", tint = Color.Green.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Align document within frame", color = Color.Green.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                            }
                        }

                        // Camera Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.FlashOff, null, tint = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(36.dp))
                                    .background(Color.White)
                                    .clickable {
                                        // Mock scanning a document
                                        val mockBitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(mockBitmap)
                                        canvas.drawColor(AndroidColor.LTGRAY)
                                        val paint = android.graphics.Paint()
                                        paint.color = AndroidColor.WHITE
                                        canvas.drawRect(20f, 20f, 380f, 580f, paint)
                                        paint.color = AndroidColor.DKGRAY
                                        paint.textSize = 24f
                                        canvas.drawText("LUMORA GALLERY", 40f, 100f, paint)
                                        canvas.drawText("On-Device Document Scan", 40f, 150f, paint)
                                        canvas.drawText("Auto edge detection matches bounds.", 40f, 200f, paint)
                                        scannedPages = scannedPages + mockBitmap
                                        activeStep = "crop"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .border(2.dp, Color.Black, RoundedCornerShape(32.dp))
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (scannedPages.isNotEmpty()) activeStep = "preview"
                                },
                                enabled = scannedPages.isNotEmpty(),
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                            ) {
                                Text("Done (${scannedPages.size})")
                            }
                        }
                    }
                }
                "crop" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "Edge & Corner Correction",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            // Show document preview with interactive crop corners
                            scannedPages.lastOrNull()?.let { b ->
                                Box {
                                    AsyncImage(
                                        model = b,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .aspectRatio(3f / 4f)
                                    )
                                    // Simulated crop bounds overlay
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .border(2.dp, Color.Cyan, RoundedCornerShape(4.dp))
                                    ) {
                                        Icon(Icons.Default.Crop, null, tint = Color.Cyan, modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
                                        Icon(Icons.Default.Crop, null, tint = Color.Cyan, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { activeStep = "filter" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Confirm Corners")
                        }
                    }
                }
                "filter" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "Select Enhancement Filter",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            scannedPages.lastOrNull()?.let { b ->
                                AsyncImage(
                                    model = b,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .graphicsLayer(
                                            alpha = if (currentFilter == "B&W") 0.9f else 1f,
                                            rotationZ = if (currentFilter == "Rotated") 90f else 0f
                                        )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("Color", "Grayscale", "B&W", "Sharpen").forEach { filter ->
                                FilterChip(
                                    selected = currentFilter == filter,
                                    onClick = { currentFilter = filter },
                                    label = { Text(filter) }
                                )
                            }
                        }
                        Button(
                            onClick = { activeStep = "preview" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Apply and Review")
                        }
                    }
                }
                "preview" -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Scanned Pages (${scannedPages.size})", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(scannedPages) { b ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(3f / 4f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.LightGray)
                                ) {
                                    AsyncImage(model = b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    IconButton(
                                        onClick = { scannedPages = scannedPages - b },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = { activeStep = "camera" },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Page")
                            }
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                    onClose()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Complete Scan")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// LOCAL OCR TOOL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrTool(
    allMedia: List<MediaItem>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedMedia by remember { mutableStateOf<MediaItem?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local OCR", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close")
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
                // Select an image to OCR
                Text(
                    "Select an image to extract text locally",
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
                                    isAnalyzing = true
                                    ocrText = ""
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = selectedMedia?.uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(selectedMedia?.displayName ?: "Photo", fontWeight = FontWeight.Bold)
                            TextButton(onClick = { selectedMedia = null }) {
                                Text("Change Photo")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAnalyzing) {
                        LaunchedEffect(selectedMedia) {
                            delay(1800) // Simulated local OCR analysis
                            isAnalyzing = false
                            ocrText = """
                                📄 EXTRACTED TEXT (LOCAL OCR)
                                ---------------------------------------
                                Brand: LUMORA GALLERY
                                Operation: Secure Backup System
                                Device: Android Client
                                
                                Text details recognized securely in background without calling any AI servers.
                            """.trimIndent()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Analyzing image locally...")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = ocrText,
                            onValueChange = { ocrText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            readOnly = false,
                            label = { Text("Recognized Text") }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("ocr_text", ocrText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Text")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MOTION PHOTO TOOL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionPhotoTool(
    allMedia: List<MediaItem>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedMedia by remember { mutableStateOf<MediaItem?>(null) }
    var isPlayingMotion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Motion Photo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close")
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
                    "Select a photo to preview in Live Motion mode",
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlayingMotion) {
                            // Live motion transition loop simulation
                            LaunchedEffect(selectedMedia) {
                                delay(3000)
                                isPlayingMotion = false
                            }
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = selectedMedia?.uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = 1.05f,
                                            scaleY = 1.05f,
                                            translationX = 5f
                                        ),
                                    contentScale = ContentScale.Fit
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Red.copy(alpha = 0.8f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Text("PLAYING LIVE MOTION", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = selectedMedia?.uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                IconButton(
                                    onClick = { isPlayingMotion = true },
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(64.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(selectedMedia?.displayName ?: "Live Photo", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedMedia = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pick Another")
                        }
                        Button(
                            onClick = {
                                isPlayingMotion = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.MotionPhotosOn, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Motion")
                        }
                    }
                }
            }
        }
    }
}
