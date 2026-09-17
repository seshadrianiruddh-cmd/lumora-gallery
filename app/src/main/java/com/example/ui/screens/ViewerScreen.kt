package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.models.MediaItem
import com.example.ui.theme.FavoriteColor
import com.example.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    viewModel: GalleryViewModel,
    initialIndex: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToVideoPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val displayedMedia by viewModel.displayedMedia.collectAsStateWithLifecycle()

    if (displayedMedia.isEmpty() || initialIndex < 0 || initialIndex >= displayedMedia.size) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) { displayedMedia.size }
    val currentItem = displayedMedia.getOrNull(pagerState.currentPage)

    var showControls by remember { mutableStateOf(true) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var isSlideshowActive by remember { mutableStateOf(false) }

    // Slideshow Effect
    LaunchedEffect(isSlideshowActive) {
        if (isSlideshowActive) {
            while (true) {
                kotlinx.coroutines.delay(3000)
                val nextIdx = (pagerState.currentPage + 1) % displayedMedia.size
                pagerState.animateScrollToPage(nextIdx)
            }
        }
    }

    // Zoom and Pan State
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var rotationAngle by remember { mutableStateOf(0f) }

    // Smooth animation properties for scale, translation offsets, and rotation
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "animatedScale"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "animatedOffsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "animatedOffsetY"
    )
    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "animatedRotation"
    )

    // Reset zoom and rotation state on page change
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        rotationAngle = 0f
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TopAppBar(
                    title = {
                        currentItem?.let {
                            Column {
                                Text(
                                    text = it.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = it.bucketName,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotate 90°",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { isSlideshowActive = !isSlideshowActive }) {
                            Icon(
                                imageVector = if (isSlideshowActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Slideshow",
                                tint = if (isSlideshowActive) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                        IconButton(onClick = { showDetailsDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
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
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    currentItem?.let { item ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = item.mimeType
                                            putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(item.uri))
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share File"))
                                    }
                                }
                                .padding(8.dp)
                                .width(64.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Share", color = Color.White, fontSize = 11.sp)
                        }

                        // Action: Edit
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = currentItem?.isVideo == false) {
                                    currentItem?.let { item ->
                                        onNavigateToEditor(item.id)
                                    }
                                }
                                .padding(8.dp)
                                .width(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = if (currentItem?.isVideo == false) Color.White else Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Edit", color = if (currentItem?.isVideo == false) Color.White else Color.Gray, fontSize = 11.sp)
                        }

                        // Action: Favorite
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    currentItem?.let { viewModel.toggleFavorite(it) }
                                }
                                .padding(8.dp)
                                .width(64.dp)
                        ) {
                            Icon(
                                imageVector = if (currentItem?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (currentItem?.isFavorite == true) FavoriteColor else Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Favorite", color = Color.White, fontSize = 11.sp)
                        }

                        // Action: Delete (Move to Trash)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    currentItem?.let { item ->
                                        viewModel.moveItemToTrash(item)
                                        onNavigateBack()
                                    }
                                }
                                .padding(8.dp)
                                .width(64.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Delete", color = Color.White, fontSize = 11.sp)
                        }

                        // Action: Hide (Move to Secure Vault)
                        var showHideConfirmDialog by remember { mutableStateOf(false) }
                        if (showHideConfirmDialog && currentItem != null) {
                            AlertDialog(
                                onDismissRequest = { showHideConfirmDialog = false },
                                title = { Text("Move to Private Vault?") },
                                text = { Text("This will encrypt the file and hide it behind your Private Safe security lock.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showHideConfirmDialog = false
                                            currentItem?.let { item ->
                                                viewModel.importSingleToVault(item)
                                                onNavigateBack()
                                            }
                                        }
                                    ) {
                                        Text("Confirm")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showHideConfirmDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showHideConfirmDialog = true
                                }
                                .padding(8.dp)
                                .width(64.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Hide", tint = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Hide", color = Color.White, fontSize = 11.sp)
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
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2.5f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    )
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = scale == 1f, // Disable pager swipe while zoomed in
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val item = displayedMedia.getOrNull(page)
                if (item != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = if (page == pagerState.currentPage) animatedScale else 1f,
                                scaleY = if (page == pagerState.currentPage) animatedScale else 1f,
                                translationX = if (page == pagerState.currentPage) animatedOffsetX else 0f,
                                translationY = if (page == pagerState.currentPage) animatedOffsetY else 0f,
                                rotationZ = if (page == pagerState.currentPage) animatedRotation else 0f
                            )
                            // Double-tap zoom focusing on the tapped point and single-tap controls on the image page itself with stable key to prevent session restarts!
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showControls = !showControls },
                                    onDoubleTap = { tapOffset ->
                                        val width = size.width.toFloat()
                                        val height = size.height.toFloat()
                                        if (scale > 1f) {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            val targetScale = 2.5f
                                            scale = targetScale
                                            
                                            // Focus zoom precisely on double-tap coordinates!
                                            val centerX = width / 2f
                                            val centerY = height / 2f
                                            val targetOffsetX = (centerX - tapOffset.x) * (targetScale - 1f)
                                            val targetOffsetY = (centerY - tapOffset.y) * (targetScale - 1f)
                                            
                                            // Apply strict boundary clamping to keep image within screen borders
                                            val maxOffsetX = (targetScale - 1f) * (width / 2f)
                                            val maxOffsetY = (targetScale - 1f) * (height / 2f)
                                            offsetX = targetOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                            offsetY = targetOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                        }
                                    }
                                )
                            }
                            // Custom continuous gesture controller with stable key (Unit) to prevent pinch stuttering!
                            .pointerInput(Unit) {
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val canceled = event.changes.any { it.isConsumed }
                                        if (!canceled) {
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()
                                            
                                            val isMultiTouch = event.changes.size > 1
                                            val shouldConsume = scale > 1f || isMultiTouch
                                            
                                            if (shouldConsume) {
                                                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                                
                                                // 1. Calculate focal point zoom adjustments based on pinch centroid
                                                if (zoomChange != 1f) {
                                                    val centroid = event.calculateCentroid(useCurrent = false)
                                                    if (centroid != Offset.Unspecified) {
                                                        val centerX = width / 2f
                                                        val centerY = height / 2f
                                                        val scaleFactor = newScale / scale
                                                        offsetX = (offsetX - (centroid.x - centerX)) * scaleFactor + (centroid.x - centerX)
                                                        offsetY = (offsetY - (centroid.y - centerY)) * scaleFactor + (centroid.y - centerY)
                                                    }
                                                }
                                                
                                                // 2. Add pan translation
                                                offsetX += panChange.x * newScale
                                                offsetY += panChange.y * newScale
                                                
                                                scale = newScale
                                                
                                                // 3. Apply strict boundary clamping based on current scale
                                                if (newScale > 1f) {
                                                    val maxOffsetX = (newScale - 1f) * (width / 2f)
                                                    val maxOffsetY = (newScale - 1f) * (height / 2f)
                                                    offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                                    offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                                } else {
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                                
                                                event.changes.forEach {
                                                    it.consume()
                                                }
                                            }
                                        }
                                    } while (!canceled && event.changes.any { it.pressed })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.uri)
                                .build(),
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Big Play Overlay if it's a Video
                        if (item.isVideo) {
                            IconButton(
                                onClick = { onNavigateToVideoPlayer(item.id) },
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(36.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isSlideshowActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 90.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .clickable { isSlideshowActive = false }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Slideshow Active (Tap to pause)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Media Details Dialog
    if (showDetailsDialog && currentItem != null) {
        val item = currentItem!!
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Information") },
            text = {
                Column {
                    DetailRow(label = "Filename", value = item.displayName)
                    DetailRow(label = "Folder", value = item.bucketName)
                    DetailRow(label = "Date Taken", value = formatFullDate(item.dateAdded))
                    DetailRow(label = "Size", value = formatFileSize(item.fileSize))
                    DetailRow(label = "Format", value = item.mimeType)
                    if (item.width > 0 && item.height > 0) {
                        DetailRow(label = "Resolution", value = "${item.width} x ${item.height}")
                    }
                    if (item.isVideo) {
                        DetailRow(label = "Duration", value = formatDurationForDetails(item.duration))
                    }
                    DetailRow(label = "Storage Uri", value = item.uri)
                }
            },
            confirmButton = {
                Button(onClick = { showDetailsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun formatFullDate(seconds: Long): String {
    val sdf = java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(seconds * 1000L))
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatDurationForDetails(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))
    return if (hours > 0) {
        "${hours}h ${minutes}m ${seconds}s"
    } else {
        "${minutes}m ${seconds}s"
    }
}
