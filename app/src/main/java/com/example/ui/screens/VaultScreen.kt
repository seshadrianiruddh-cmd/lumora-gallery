package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.database.VaultItemEntity
import com.example.ui.components.EmptyState
import com.example.ui.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: GalleryViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSetup by viewModel.vaultIsSetup.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.vaultUnlocked.collectAsStateWithLifecycle()
    val vaultItems by viewModel.vaultItems.collectAsStateWithLifecycle()

    var enteredPin by remember { mutableStateOf("") }
    var setupStep by remember { mutableStateOf(1) } // 1 = enter pin, 2 = confirm pin
    var firstEnteredPin by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf("") }

    // Auto-lock vault on application pause
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.lockVault()
                enteredPin = ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private Vault", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    if (isUnlocked) {
                        IconButton(onClick = {
                            viewModel.lockVault()
                            enteredPin = ""
                        }) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Lock Vault")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isSetup) {
                // Setup Vault PIN workflow
                SetupVaultUi(
                    step = setupStep,
                    enteredPin = enteredPin,
                    errorMessage = pinErrorMessage,
                    onKeyClick = { digit ->
                        if (enteredPin.length < 4) {
                            enteredPin += digit
                        }
                    },
                    onBackspace = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    },
                    onNext = {
                        if (enteredPin.length == 4) {
                            if (setupStep == 1) {
                                firstEnteredPin = enteredPin
                                enteredPin = ""
                                setupStep = 2
                                pinErrorMessage = ""
                            } else {
                                if (enteredPin == firstEnteredPin) {
                                    val success = viewModel.setupVault(enteredPin)
                                    if (!success) {
                                        pinErrorMessage = "Failed to setup vault"
                                    }
                                } else {
                                    pinErrorMessage = "PINs do not match. Try again."
                                    enteredPin = ""
                                    setupStep = 1
                                }
                            }
                        }
                    }
                )
            } else if (!isUnlocked) {
                // Unlock Vault workflow
                UnlockVaultUi(
                    enteredPin = enteredPin,
                    errorMessage = pinErrorMessage,
                    onKeyClick = { digit ->
                        if (enteredPin.length < 4) {
                            val nextPin = enteredPin + digit
                            enteredPin = nextPin
                            if (nextPin.length == 4) {
                                val success = viewModel.unlockVault(nextPin)
                                if (success) {
                                    enteredPin = ""
                                    pinErrorMessage = ""
                                } else {
                                    pinErrorMessage = "Incorrect PIN. Try again."
                                    enteredPin = ""
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    }
                )
            } else {
                // Main Private Vault Media Grid
                if (vaultItems.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Lock,
                        title = "Your Vault is Empty",
                        description = "Move photos and videos here by holding/selecting items on the main Photos tab."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(vaultItems, key = { it.id }) { item ->
                            VaultGridItem(
                                item = item,
                                onRestore = { viewModel.restoreVaultItem(item) },
                                onDelete = { viewModel.deleteVaultItemPermanently(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupVaultUi(
    step: Int,
    enteredPin: String,
    errorMessage: String,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (step == 1) "Create Private Vault PIN" else "Confirm Your PIN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your PIN is used to derive encryption keys locally. Lumora never saves plaintext passwords.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // PIN entry indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { idx ->
                    val isEntered = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEntered) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer
                            )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }

        // Numeric Keypad
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PinKeypad(
                onKeyClick = onKeyClick,
                onBackspace = onBackspace,
                onAction = onNext,
                actionIcon = Icons.Default.ArrowForward,
                actionEnabled = enteredPin.length == 4
            )
        }
    }
}

@Composable
fun UnlockVaultUi(
    enteredPin: String,
    errorMessage: String,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enter Vault PIN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // PIN entry indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { idx ->
                    val isEntered = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEntered) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer
                            )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }

        // Numeric Keypad
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PinKeypad(
                onKeyClick = onKeyClick,
                onBackspace = onBackspace,
                onAction = {},
                actionIcon = null,
                actionEnabled = false
            )
        }
    }
}

@Composable
fun PinKeypad(
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onAction: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    actionEnabled: Boolean
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("backspace", "0", "action")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    when (key) {
                        "backspace" -> {
                            IconButton(
                                onClick = onBackspace,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Backspace, contentDescription = "Backspace", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        "action" -> {
                            if (actionIcon != null) {
                                IconButton(
                                    onClick = onAction,
                                    enabled = actionEnabled,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            if (actionEnabled) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = actionIcon,
                                        contentDescription = "Confirm",
                                        tint = if (actionEnabled) Color.White else Color.Gray
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(64.dp))
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onKeyClick(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun VaultGridItem(
    item: VaultItemEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { showMenu = true }
    ) {
        // Enforce placeholder secure logo overlay since file is strongly encrypted on disk and cannot be read normally
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (item.mimeType.startsWith("video")) Icons.Default.Videocam else Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.originalName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        // Action dropdown menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Restore to Public Gallery") },
                onClick = {
                    onRestore()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDelete()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}
