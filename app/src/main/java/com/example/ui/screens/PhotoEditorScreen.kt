package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    viewModel: GalleryViewModel,
    mediaId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val displayedMedia by viewModel.displayedMedia.collectAsStateWithLifecycle()
    val mediaItem = displayedMedia.find { it.id == mediaId }

    if (mediaItem == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    var selectedTool by remember { mutableStateOf("adjust") } // adjust, filter, doodle, text, crop, transform

    // Filter states
    var selectedFilter by remember { mutableStateOf("Normal") }

    // Doodle states
    val doodlePaths = remember { mutableStateListOf<DoodlePath>() }
    var activeDoodleColor by remember { mutableStateOf(android.graphics.Color.RED) }
    var activeDoodleWidth by remember { mutableStateOf(8f) }

    // Text states
    val addedTexts = remember { mutableStateListOf<AddedText>() }
    var activeTextColor by remember { mutableStateOf(android.graphics.Color.WHITE) }
    var activeTextSize by remember { mutableStateOf(24f) }

    // Edit adjustment states
    var brightness by remember { mutableStateOf(0f) } // -100 to 100
    var contrast by remember { mutableStateOf(0f) }   // -100 to 100
    var saturation by remember { mutableStateOf(0f) } // -100 to 100

    // Transform states
    var rotationDegrees by remember { mutableStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }

    // Crop state
    var cropRatioLabel by remember { mutableStateOf("Free") } // Free, 1:1, 4:3, 16:9
    var cropLeft by remember { mutableStateOf(0.05f) }
    var cropTop by remember { mutableStateOf(0.05f) }
    var cropRight by remember { mutableStateOf(0.95f) }
    var cropBottom by remember { mutableStateOf(0.95f) }

    LaunchedEffect(cropRatioLabel, mediaItem) {
        val targetRatio = when (cropRatioLabel) {
            "1:1" -> 1.0f
            "4:3" -> 4f / 3f
            "16:9" -> 16f / 9f
            else -> null
        }
        
        if (targetRatio != null) {
            val imgW = mediaItem.width.toFloat()
            val imgH = mediaItem.height.toFloat()
            if (imgW > 0 && imgH > 0) {
                val currentRatio = imgW / imgH
                if (currentRatio > targetRatio) {
                    val cropWFrac = targetRatio / currentRatio
                    cropLeft = ((1f - cropWFrac) / 2f).coerceAtLeast(0f)
                    cropRight = (cropLeft + cropWFrac).coerceAtMost(1f)
                    cropTop = 0.05f
                    cropBottom = 0.95f
                } else {
                    val cropHFrac = currentRatio / targetRatio
                    cropTop = ((1f - cropHFrac) / 2f).coerceAtLeast(0f)
                    cropBottom = (cropTop + cropHFrac).coerceAtMost(1f)
                    cropLeft = 0.05f
                    cropRight = 0.95f
                }
            } else {
                cropLeft = 0.1f
                cropTop = 0.1f
                cropRight = 0.9f
                cropBottom = 0.9f
            }
        } else {
            cropLeft = 0.05f
            cropTop = 0.05f
            cropRight = 0.95f
            cropBottom = 0.95f
        }
    }

    // Save saving status
    var isSaving by remember { mutableStateOf(false) }

    // Color Matrix calculation for real-time visual filter preview
    val colorMatrix = remember(brightness, contrast, saturation, selectedFilter) {
        val cm = android.graphics.ColorMatrix()
        
        // Apply Preset Filter first
        when (selectedFilter) {
            "B&W" -> {
                val bwMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(bwMatrix)
            }
            "Vintage" -> {
                val vintageMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(vintageMatrix)
            }
            "Warm" -> {
                val warmMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    1.15f, 0f, 0f, 0f, 10f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 0.90f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(warmMatrix)
            }
            "Cool" -> {
                val coolMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    0.90f, 0f, 0f, 0f, -10f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 1.15f, 0f, 15f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(coolMatrix)
            }
            "Cyberpunk" -> {
                val cyberpunkMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    1.2f, 0f, 0f, 0f, 20f,
                    0f, 0.8f, 0f, 0f, -10f,
                    0f, 0f, 1.4f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(cyberpunkMatrix)
            }
        }
        
        // Adjust brightness
        val b = brightness * 2.55f // map to 255 scale
        val brightnessMatrix = android.graphics.ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, b,
            0f, 1f, 0f, 0f, b,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(brightnessMatrix)

        // Adjust contrast
        val c = (contrast + 100f) / 100f
        val scale = c
        val translate = (-.5f * scale + .5f) * 255f
        val contrastMatrix = android.graphics.ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)

        // Adjust saturation
        val s = (saturation + 100f) / 100f
        val saturationMatrix = android.graphics.ColorMatrix()
        saturationMatrix.setSaturation(s)
        cm.postConcat(saturationMatrix)

        ColorMatrix(cm.array)
    }

    val imageAspectRatio = remember(mediaItem) {
        if (mediaItem.width > 0 && mediaItem.height > 0) {
            mediaItem.width.toFloat() / mediaItem.height.toFloat()
        } else {
            1f
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lumora Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Reset all tools
                        brightness = 0f
                        contrast = 0f
                        saturation = 0f
                        rotationDegrees = 0f
                        flipHorizontal = false
                        flipVertical = false
                        cropRatioLabel = "Free"
                    }) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset All")
                    }
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val success = saveAsCopy(
                                    context = context,
                                    imageUriStr = mediaItem.uri,
                                    brightness = brightness,
                                    contrast = contrast,
                                    saturation = saturation,
                                    rotation = rotationDegrees,
                                    flipH = flipHorizontal,
                                    flipV = flipVertical,
                                    cropLeft = cropLeft,
                                    cropTop = cropTop,
                                    cropRight = cropRight,
                                    cropBottom = cropBottom,
                                    filter = selectedFilter,
                                    doodlePaths = doodlePaths,
                                    addedTexts = addedTexts
                                )
                                isSaving = false
                                if (success) {
                                    Toast.makeText(context, "Saved as copy successfully", Toast.LENGTH_SHORT).show()
                                    viewModel.refreshMedia()
                                    onNavigateBack()
                                } else {
                                    Toast.makeText(context, "Failed to save edited photo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Text("Save Copy")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column {
                    // Tool Customization Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        when (selectedTool) {
                            "adjust" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Brightness Slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Brightness", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("${brightness.toInt()}%", fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = brightness,
                                            onValueChange = { brightness = it },
                                            valueRange = -100f..100f
                                        )
                                    }

                                    // Contrast Slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Contrast", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("${contrast.toInt()}%", fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = contrast,
                                            onValueChange = { contrast = it },
                                            valueRange = -100f..100f
                                        )
                                    }

                                    // Saturation Slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Saturation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("${saturation.toInt()}%", fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = saturation,
                                            onValueChange = { saturation = it },
                                            valueRange = -100f..100f
                                        )
                                    }
                                }
                            }
                            "crop" -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val ratios = listOf("Free", "1:1", "4:3", "16:9")
                                    ratios.forEach { ratio ->
                                        ElevatedFilterChip(
                                            selected = cropRatioLabel == ratio,
                                            onClick = { cropRatioLabel = ratio },
                                            label = { Text(ratio) }
                                        )
                                    }
                                }
                            }
                            "filter" -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val filtersList = listOf("Normal", "B&W", "Vintage", "Warm", "Cool", "Cyberpunk")
                                    filtersList.forEach { filterName ->
                                        ElevatedFilterChip(
                                            selected = selectedFilter == filterName,
                                            onClick = { selectedFilter = filterName },
                                            label = { Text(filterName) }
                                        )
                                    }
                                }
                            }
                            "doodle" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Brush Width", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("${activeDoodleWidth.toInt()}px", fontSize = 11.sp)
                                    }
                                    Slider(
                                        value = activeDoodleWidth,
                                        onValueChange = { activeDoodleWidth = it },
                                        valueRange = 2f..40f,
                                        modifier = Modifier.height(24.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        val colors = listOf(
                                            "Red" to android.graphics.Color.RED,
                                            "Green" to android.graphics.Color.GREEN,
                                            "Blue" to android.graphics.Color.BLUE,
                                            "Yellow" to android.graphics.Color.YELLOW,
                                            "White" to android.graphics.Color.WHITE
                                        )
                                        colors.forEach { (colorName, colorValue) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(Color(colorValue), CircleShape)
                                                    .border(
                                                        width = if (activeDoodleColor == colorValue) 2.dp else 0.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { activeDoodleColor = colorValue }
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Button(
                                            onClick = { doodlePaths.clear() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Clear All", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            "text" -> {
                                var textToEnter by remember { mutableStateOf("") }
                                var showTextInputDialog by remember { mutableStateOf(false) }
                                
                                if (showTextInputDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showTextInputDialog = false },
                                        title = { Text("Add Text to Photo") },
                                        text = {
                                            OutlinedTextField(
                                                value = textToEnter,
                                                onValueChange = { textToEnter = it },
                                                label = { Text("Enter text") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    if (textToEnter.isNotBlank()) {
                                                        addedTexts.add(
                                                            AddedText(
                                                                text = textToEnter,
                                                                x = 0.2f,
                                                                y = 0.4f,
                                                                color = activeTextColor,
                                                                fontSize = activeTextSize
                                                            )
                                                        )
                                                        textToEnter = ""
                                                    }
                                                    showTextInputDialog = false
                                                }
                                            ) {
                                                Text("Add")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showTextInputDialog = false }) {
                                                Text("Cancel")
                                            }
                                        }
                                    )
                                }
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { showTextInputDialog = true },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Text", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Text", fontSize = 11.sp)
                                        }
                                        
                                        Button(
                                            onClick = { addedTexts.clear() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Clear Texts", fontSize = 11.sp)
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        val colors = listOf(
                                            "White" to android.graphics.Color.WHITE,
                                            "Black" to android.graphics.Color.BLACK,
                                            "Yellow" to android.graphics.Color.YELLOW,
                                            "Red" to android.graphics.Color.RED,
                                            "Cyan" to android.graphics.Color.CYAN
                                        )
                                        colors.forEach { (colorName, colorValue) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(Color(colorValue), CircleShape)
                                                    .border(
                                                        width = if (activeTextColor == colorValue) 2.dp else 0.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { activeTextColor = colorValue }
                                            )
                                        }
                                    }
                                }
                            }
                            "transform" -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    IconButton(onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Rotate")
                                            Text("Rotate 90", fontSize = 9.sp)
                                        }
                                    }
                                    IconButton(onClick = { flipHorizontal = !flipHorizontal }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.Flip, contentDescription = "Flip Horizontal")
                                            Text("Flip Horiz", fontSize = 9.sp)
                                        }
                                    }
                                    IconButton(onClick = { flipVertical = !flipVertical }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(imageVector = Icons.Default.Flip, contentDescription = "Flip Vertical", modifier = Modifier.graphicsLayer(rotationZ = 90f))
                                            Text("Flip Vert", fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider()

                    // Main Tool Categories selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Adjust
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTool = "adjust" }
                                .padding(8.dp)
                                .width(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Adjust",
                                tint = if (selectedTool == "adjust") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Adjust", fontSize = 11.sp, color = if (selectedTool == "adjust") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Filter
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTool = "filter" }
                                .padding(8.dp)
                                .width(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Filter",
                                tint = if (selectedTool == "filter") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Filter", fontSize = 11.sp, color = if (selectedTool == "filter") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Doodle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTool = "doodle" }
                                .padding(8.dp)
                                .width(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Doodle",
                                tint = if (selectedTool == "doodle") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Doodle", fontSize = 11.sp, color = if (selectedTool == "doodle") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTool = "text" }
                                .padding(8.dp)
                                .width(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Title,
                                contentDescription = "Text",
                                tint = if (selectedTool == "text") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Text", fontSize = 11.sp, color = if (selectedTool == "text") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Crop
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTool = "crop" }
                                .padding(8.dp)
                                .width(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Crop",
                                tint = if (selectedTool == "crop") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Crop", fontSize = 11.sp, color = if (selectedTool == "crop") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Transform
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTool = "transform" }
                                .padding(8.dp)
                                .width(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Transform,
                                contentDescription = "Transform",
                                tint = if (selectedTool == "transform") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Transform", fontSize = 11.sp, color = if (selectedTool == "transform") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
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
                .background(Color(0xFF0F0E17)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .aspectRatio(imageAspectRatio)
                    .graphicsLayer(
                        rotationZ = rotationDegrees,
                        scaleX = if (flipHorizontal) -1f else 1f,
                        scaleY = if (flipVertical) -1f else 1f
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Apply colorMatrix to our AsyncImage using colorFilter parameter!
                AsyncImage(
                    model = mediaItem.uri,
                    contentDescription = null,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // 1. Interactive Drawing Canvas for Doodles
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedTool, activeDoodleColor, activeDoodleWidth) {
                            if (selectedTool == "doodle") {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val startPt = DoodlePoint(offset.x / size.width, offset.y / size.height)
                                        doodlePaths.add(
                                            DoodlePath(
                                                points = listOf(startPt),
                                                color = activeDoodleColor,
                                                strokeWidth = activeDoodleWidth
                                            )
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (doodlePaths.isNotEmpty()) {
                                            val lastPath = doodlePaths.last()
                                            val position = change.position
                                            val newPt = DoodlePoint(position.x / size.width, position.y / size.height)
                                            doodlePaths[doodlePaths.lastIndex] = lastPath.copy(
                                                points = lastPath.points + newPt
                                            )
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    // Draw saved doodle paths
                    doodlePaths.forEach { path ->
                        val paintColor = Color(path.color)
                        val strokePx = path.strokeWidth.dp.toPx()
                        
                        val w = size.width
                        val h = size.height
                        
                        if (path.points.size > 1) {
                            for (i in 0 until path.points.size - 1) {
                                val p1 = path.points[i]
                                val p2 = path.points[i + 1]
                                drawLine(
                                    color = paintColor,
                                    start = Offset(p1.x * w, p1.y * h),
                                    end = Offset(p2.x * w, p2.y * h),
                                    strokeWidth = strokePx,
                                    cap = StrokeCap.Round
                                )
                            }
                        } else if (path.points.isNotEmpty()) {
                            val p = path.points.first()
                            drawCircle(
                                color = paintColor,
                                center = Offset(p.x * w, p.y * h),
                                radius = strokePx / 2f
                            )
                        }
                    }
                }

                // 2. Interactive Draggable Text Annotations
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val wDp = maxWidth
                    val hDp = maxHeight
                    
                    addedTexts.forEachIndexed { index, at ->
                        var currentX by remember(at) { mutableStateOf(at.x) }
                        var currentY by remember(at) { mutableStateOf(at.y) }
                        
                        Text(
                            text = at.text,
                            color = Color(at.color),
                            fontSize = at.fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .offset(
                                    x = (currentX * wDp.value).dp,
                                    y = (currentY * hDp.value).dp
                                )
                                .pointerInput(selectedTool) {
                                    if (selectedTool == "text") {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val dx = dragAmount.x / size.width
                                                val dy = dragAmount.y / size.height
                                                currentX = (currentX + dx).coerceIn(0f, 0.9f)
                                                currentY = (currentY + dy).coerceIn(0f, 0.9f)
                                                addedTexts[index] = at.copy(x = currentX, y = currentY)
                                            }
                                        )
                                    }
                                }
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Optional Crop Visual Borders Overlay
                if (selectedTool == "crop") {
                    val strokeColor = MaterialTheme.colorScheme.primary
                    var activeHandle by remember { mutableStateOf<String?>(null) } // "tl", "tr", "bl", "br", "move", null

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(cropRatioLabel) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        val xFrac = offset.x / w
                                        val yFrac = offset.y / h
                                        
                                        val tlDist = dist(xFrac, yFrac, cropLeft, cropTop)
                                        val trDist = dist(xFrac, yFrac, cropRight, cropTop)
                                        val blDist = dist(xFrac, yFrac, cropLeft, cropBottom)
                                        val brDist = dist(xFrac, yFrac, cropRight, cropBottom)
                                        
                                        val threshold = 0.08f // 8% of image size tolerance
                                        
                                        activeHandle = when {
                                            tlDist < threshold -> "tl"
                                            trDist < threshold -> "tr"
                                            blDist < threshold -> "bl"
                                            brDist < threshold -> "br"
                                            xFrac in cropLeft..cropRight && yFrac in cropTop..cropBottom -> "move"
                                            else -> null
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        val dx = dragAmount.x / w
                                        val dy = dragAmount.y / h
                                        
                                        val minGap = 0.15f // Minimum crop box size limit
                                        
                                        when (activeHandle) {
                                            "tl" -> {
                                                if (cropRatioLabel == "Free") {
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minGap)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minGap)
                                                } else {
                                                    val ratio = getRatioValue(cropRatioLabel, w, h)
                                                    val candidateLeft = (cropLeft + dx).coerceIn(0f, cropRight - minGap)
                                                    val candidateTop = cropBottom - (cropRight - candidateLeft) / ratio
                                                    if (candidateTop >= 0f) {
                                                        cropLeft = candidateLeft
                                                        cropTop = candidateTop
                                                    }
                                                }
                                            }
                                            "tr" -> {
                                                if (cropRatioLabel == "Free") {
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + minGap, 1f)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minGap)
                                                } else {
                                                    val ratio = getRatioValue(cropRatioLabel, w, h)
                                                    val candidateRight = (cropRight + dx).coerceIn(cropLeft + minGap, 1f)
                                                    val candidateTop = cropBottom - (candidateRight - cropLeft) / ratio
                                                    if (candidateTop >= 0f) {
                                                        cropRight = candidateRight
                                                        cropTop = candidateTop
                                                    }
                                                }
                                            }
                                            "bl" -> {
                                                if (cropRatioLabel == "Free") {
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minGap)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + minGap, 1f)
                                                } else {
                                                    val ratio = getRatioValue(cropRatioLabel, w, h)
                                                    val candidateLeft = (cropLeft + dx).coerceIn(0f, cropRight - minGap)
                                                    val candidateBottom = cropTop + (cropRight - candidateLeft) / ratio
                                                    if (candidateBottom <= 1f) {
                                                        cropLeft = candidateLeft
                                                        cropBottom = candidateBottom
                                                    }
                                                }
                                            }
                                            "br" -> {
                                                if (cropRatioLabel == "Free") {
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + minGap, 1f)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + minGap, 1f)
                                                } else {
                                                    val ratio = getRatioValue(cropRatioLabel, w, h)
                                                    val candidateRight = (cropRight + dx).coerceIn(cropLeft + minGap, 1f)
                                                    val candidateBottom = cropTop + (candidateRight - cropLeft) / ratio
                                                    if (candidateBottom <= 1f) {
                                                        cropRight = candidateRight
                                                        cropBottom = candidateBottom
                                                    }
                                                }
                                            }
                                            "move" -> {
                                                val width = cropRight - cropLeft
                                                val height = cropBottom - cropTop
                                                val newLeft = (cropLeft + dx).coerceIn(0f, 1f - width)
                                                val newTop = (cropTop + dy).coerceIn(0f, 1f - height)
                                                cropLeft = newLeft
                                                cropTop = newTop
                                                cropRight = newLeft + width
                                                cropBottom = newTop + height
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        activeHandle = null
                                    }
                                )
                            }
                    ) {
                        val imgW = size.width
                        val imgH = size.height
                        
                        val left = cropLeft * imgW
                        val top = cropTop * imgH
                        val right = cropRight * imgW
                        val bottom = cropBottom * imgH
                        val cropW = right - left
                        val cropH = bottom - top

                        // 1. Draw semi-transparent darkened overlay mask outside the crop area
                        // Top mask
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(0f, 0f),
                            size = Size(imgW, top)
                        )
                        // Bottom mask
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(0f, bottom),
                            size = Size(imgW, imgH - bottom)
                        )
                        // Left mask
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(0f, top),
                            size = Size(left, cropH)
                        )
                        // Right mask
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(right, top),
                            size = Size(imgW - right, cropH)
                        )

                        // 2. Draw high-contrast primary colored crop outer border
                        drawRect(
                            color = strokeColor,
                            topLeft = Offset(left, top),
                            size = Size(cropW, cropH),
                            style = Stroke(width = 2.5.dp.toPx())
                        )

                        // 3. Draw 3x3 fine gridlines inside the crop box
                        val v1 = left + cropW / 3f
                        val v2 = left + 2f * cropW / 3f
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(v1, top),
                            end = Offset(v1, bottom),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(v2, top),
                            end = Offset(v2, bottom),
                            strokeWidth = 1.dp.toPx()
                        )
                        
                        val h1 = top + cropH / 3f
                        val h2 = top + 2f * cropH / 3f
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(left, h1),
                            end = Offset(right, h1),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(left, h2),
                            end = Offset(right, h2),
                            strokeWidth = 1.dp.toPx()
                        )

                        // 4. Draw thick professional corner accent handles
                        val handleLen = 20.dp.toPx()
                        val handleThick = 4.dp.toPx()

                        // Top-Left corner
                        drawLine(color = Color.White, start = Offset(left - handleThick/2, top), end = Offset(left + handleLen, top), strokeWidth = handleThick)
                        drawLine(color = Color.White, start = Offset(left, top - handleThick/2), end = Offset(left, top + handleLen), strokeWidth = handleThick)

                        // Top-Right corner
                        drawLine(color = Color.White, start = Offset(right + handleThick/2, top), end = Offset(right - handleLen, top), strokeWidth = handleThick)
                        drawLine(color = Color.White, start = Offset(right, top - handleThick/2), end = Offset(right, top + handleLen), strokeWidth = handleThick)

                        // Bottom-Left corner
                        drawLine(color = Color.White, start = Offset(left - handleThick/2, bottom), end = Offset(left + handleLen, bottom), strokeWidth = handleThick)
                        drawLine(color = Color.White, start = Offset(left, bottom + handleThick/2), end = Offset(left, bottom - handleLen), strokeWidth = handleThick)

                        // Bottom-Right corner
                        drawLine(color = Color.White, start = Offset(right + handleThick/2, bottom), end = Offset(right - handleLen, bottom), strokeWidth = handleThick)
                        drawLine(color = Color.White, start = Offset(right, bottom + handleThick/2), end = Offset(right, bottom - handleLen), strokeWidth = handleThick)
                    }
                }
            }
        }
    }
}


private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

private fun getRatioValue(label: String, width: Float, height: Float): Float {
    return when (label) {
        "1:1" -> 1.0f * (height / width)
        "4:3" -> (4f / 3f) * (height / width)
        "16:9" -> (16f / 9f) * (height / width)
        else -> 1.0f
    }
}

// Background processing for actual file modifications and saving as copy to MediaStore
private suspend fun saveAsCopy(
    context: Context,
    imageUriStr: String,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    rotation: Float,
    flipH: Boolean,
    flipV: Boolean,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    filter: String,
    doodlePaths: List<DoodlePath>,
    addedTexts: List<AddedText>
): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        val uri = Uri.parse(imageUriStr)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext false
        inputStream.close()

        // Create editing matrix
        val matrix = Matrix()
        if (rotation != 0f) {
            matrix.postRotate(rotation)
        }
        val sx = if (flipH) -1f else 1f
        val sy = if (flipV) -1f else 1f
        if (flipH || flipV) {
            matrix.postScale(sx, sy)
        }

        // Apply transformations
        var modifiedBitmap = Bitmap.createBitmap(
            originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
        )

        // Apply crop if selected
        val originalW = modifiedBitmap.width
        val originalH = modifiedBitmap.height

        val leftPx = (cropLeft * originalW).toInt().coerceIn(0, originalW - 1)
        val topPx = (cropTop * originalH).toInt().coerceIn(0, originalH - 1)
        val rightPx = (cropRight * originalW).toInt().coerceIn(leftPx + 10, originalW)
        val bottomPx = (cropBottom * originalH).toInt().coerceIn(topPx + 10, originalH)

        val cropW = rightPx - leftPx
        val cropH = bottomPx - topPx

        if (cropW < originalW || cropH < originalH || leftPx > 0 || topPx > 0) {
            val croppedBitmap = Bitmap.createBitmap(modifiedBitmap, leftPx, topPx, cropW, cropH)
            if (croppedBitmap != modifiedBitmap) {
                modifiedBitmap.recycle()
                modifiedBitmap = croppedBitmap
            }
        }

        // Apply brightness, contrast, saturation and Preset Filters
        if (brightness != 0f || contrast != 0f || saturation != 0f || filter != "Normal") {
            val adjustedBitmap = Bitmap.createBitmap(
                modifiedBitmap.width, modifiedBitmap.height, modifiedBitmap.config ?: Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(adjustedBitmap)
            val paint = android.graphics.Paint()

            // Calculate ColorMatrix adjustments matching our preview
            val cm = android.graphics.ColorMatrix()
            
            // Apply Preset Filter first
            when (filter) {
                "B&W" -> {
                    val bwMatrix = android.graphics.ColorMatrix(floatArrayOf(
                        0.33f, 0.59f, 0.11f, 0f, 0f,
                        0.33f, 0.59f, 0.11f, 0f, 0f,
                        0.33f, 0.59f, 0.11f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    cm.postConcat(bwMatrix)
                }
                "Vintage" -> {
                    val vintageMatrix = android.graphics.ColorMatrix(floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    cm.postConcat(vintageMatrix)
                }
                "Warm" -> {
                    val warmMatrix = android.graphics.ColorMatrix(floatArrayOf(
                        1.15f, 0f, 0f, 0f, 10f,
                        0f, 1.05f, 0f, 0f, 5f,
                        0f, 0f, 0.90f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    cm.postConcat(warmMatrix)
                }
                "Cool" -> {
                    val coolMatrix = android.graphics.ColorMatrix(floatArrayOf(
                        0.90f, 0f, 0f, 0f, -10f,
                        0f, 1.05f, 0f, 0f, 5f,
                        0f, 0f, 1.15f, 0f, 15f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    cm.postConcat(coolMatrix)
                }
                "Cyberpunk" -> {
                    val cyberpunkMatrix = android.graphics.ColorMatrix(floatArrayOf(
                        1.2f, 0f, 0f, 0f, 20f,
                        0f, 0.8f, 0f, 0f, -10f,
                        0f, 0f, 1.4f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    cm.postConcat(cyberpunkMatrix)
                }
            }

            // Brightness
            val b = brightness * 2.55f
            val brightnessMatrix = android.graphics.ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, b,
                0f, 1f, 0f, 0f, b,
                0f, 0f, 1f, 0f, b,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(brightnessMatrix)

            // Contrast
            val c = (contrast + 100f) / 100f
            val scale = c
            val translate = (-.5f * scale + .5f) * 255f
            val contrastMatrix = android.graphics.ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastMatrix)

            // Saturation
            val s = (saturation + 100f) / 100f
            val saturationMatrix = android.graphics.ColorMatrix()
            saturationMatrix.setSaturation(s)
            cm.postConcat(saturationMatrix)

            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(modifiedBitmap, 0f, 0f, paint)
            modifiedBitmap = adjustedBitmap
        }

        // Draw custom Doodles and Texts onto final bitmap before saving
        if (doodlePaths.isNotEmpty() || addedTexts.isNotEmpty()) {
            val drawBitmap = if (modifiedBitmap.isMutable) {
                modifiedBitmap
            } else {
                val mutableCopy = modifiedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                modifiedBitmap.recycle()
                mutableCopy
            }
            val canvas = android.graphics.Canvas(drawBitmap)
            val w = drawBitmap.width.toFloat()
            val h = drawBitmap.height.toFloat()

            // Draw Doodle paths
            doodlePaths.forEach { path ->
                val paint = android.graphics.Paint().apply {
                    color = path.color
                    strokeWidth = path.strokeWidth * (w / 360f) // scale stroke relative to canvas size
                    style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }
                val osPath = android.graphics.Path()
                path.points.forEachIndexed { idx, pt ->
                    if (idx == 0) {
                        osPath.moveTo(pt.x * w, pt.y * h)
                    } else {
                        osPath.lineTo(pt.x * w, pt.y * h)
                    }
                }
                canvas.drawPath(osPath, paint)
            }

            // Draw Added Texts
            addedTexts.forEach { at ->
                val paint = android.graphics.Paint().apply {
                    color = at.color
                    textSize = at.fontSize * (w / 360f) // scale font size
                    textAlign = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                
                // Draw a beautiful dark background capsule/rect for text readability matching preview
                val bounds = android.graphics.Rect()
                paint.getTextBounds(at.text, 0, at.text.length, bounds)
                val textW = bounds.width().toFloat()
                val textH = bounds.height().toFloat()
                
                val textX = at.x * w
                val textY = at.y * h
                
                val bgPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(102, 0, 0, 0) // 0.4 opacity black
                    style = android.graphics.Paint.Style.FILL
                }
                
                val paddingX = 8f * (w / 360f)
                val paddingY = 4f * (w / 360f)
                
                canvas.drawRoundRect(
                    textX - paddingX,
                    textY - textH - paddingY,
                    textX + textW + paddingX,
                    textY + paddingY,
                    4f * (w / 360f),
                    4f * (w / 360f),
                    bgPaint
                )
                
                canvas.drawText(at.text, textX, textY, paint)
            }
            
            modifiedBitmap = drawBitmap
        }

        // Save back to MediaStore as a copy
        val displayName = "lumora_edit_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val newUri = context.contentResolver.insert(collectionUri, values) ?: return@withContext false

        val outStream: OutputStream? = context.contentResolver.openOutputStream(newUri)
        if (outStream != null) {
            modifiedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
            outStream.close()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(newUri, values, null, null)
        }

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

data class DoodlePoint(val x: Float, val y: Float)

data class DoodlePath(
    val points: List<DoodlePoint>,
    val color: Int, // argb
    val strokeWidth: Float
)

data class AddedText(
    val text: String,
    val x: Float, // Normalized x
    val y: Float, // Normalized y
    val color: Int, // argb
    val fontSize: Float
)
