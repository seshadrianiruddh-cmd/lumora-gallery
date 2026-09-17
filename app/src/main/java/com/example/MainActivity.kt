package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppOrchestrator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppOrchestrator() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val viewModel: GalleryViewModel = viewModel()

    // Determine initial startup route depending on permissions granted
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val hasPermissions = requiredPermissions.all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    val startDestination = if (hasPermissions) "main_photos" else "permissions"

    // Track active navigation destination backstack
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    val navItems = listOf(
        DrawerItem("Photos", "main_photos", Icons.Default.GridView),
        DrawerItem("Albums", "main_albums", Icons.Default.Folder),
        DrawerItem("Vault", "main_vault", Icons.Default.Lock),
        DrawerItem("Smart Hub", "smart_hub", Icons.Default.Category),
        DrawerItem("Settings", "settings", Icons.Default.Settings)
    )

    val showBottomBar = currentRoute in listOf("main_photos", "main_albums", "main_vault", "smart_hub", "settings")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
                composable("permissions") {
                    PermissionScreen(
                        onPermissionsGranted = {
                            navController.navigate("main_photos") {
                                popUpTo("permissions") { inclusive = true }
                            }
                        }
                    )
                }

                composable("main_photos") {
                    PhotosScreen(
                        viewModel = viewModel,
                        onNavigateToViewer = { index ->
                            navController.navigate("viewer/$index")
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToSearch = {
                            navController.navigate("search")
                        },
                        onNavigateToCreativeStudio = {
                            navController.navigate("creative_studio")
                        },
                        onNavigateToSmartHub = {
                            navController.navigate("smart_hub")
                        },
                        onOpenDrawer = {}
                    )
                }

                composable("main_albums") {
                    AlbumsScreen(
                        viewModel = viewModel,
                        onNavigateToAlbumDetail = { albumId, title, isCustom ->
                            navController.navigate("album_detail/$albumId/$title/$isCustom")
                        },
                        onOpenDrawer = {}
                    )
                }

                composable("main_vault") {
                    VaultScreen(
                        viewModel = viewModel,
                        onOpenDrawer = {}
                    )
                }

            composable(
                route = "album_detail/{albumId}/{albumName}/{isCustom}",
                arguments = listOf(
                    navArgument("albumId") { type = NavType.StringType },
                    navArgument("albumName") { type = NavType.StringType },
                    navArgument("isCustom") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("albumId") ?: ""
                val name = backStackEntry.arguments?.getString("albumName") ?: ""
                val isCustom = backStackEntry.arguments?.getBoolean("isCustom") ?: false

                AlbumDetailScreen(
                    viewModel = viewModel,
                    albumId = id,
                    albumName = name,
                    isCustom = isCustom,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { index ->
                        navController.navigate("viewer/$index")
                    }
                )
            }

            composable(
                route = "viewer/{initialIndex}",
                arguments = listOf(navArgument("initialIndex") { type = NavType.IntType })
            ) { backStackEntry ->
                val idx = backStackEntry.arguments?.getInt("initialIndex") ?: 0
                ViewerScreen(
                    viewModel = viewModel,
                    initialIndex = idx,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditor = { id ->
                        navController.navigate("editor/$id")
                    },
                    onNavigateToVideoPlayer = { id ->
                        navController.navigate("video_player/$id")
                    }
                )
            }

            composable(
                route = "video_player/{mediaId}",
                arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("mediaId") ?: ""
                VideoPlayerScreen(
                    viewModel = viewModel,
                    mediaId = id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "editor/{mediaId}",
                arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("mediaId") ?: ""
                PhotoEditorScreen(
                    viewModel = viewModel,
                    mediaId = id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToViewer = { index ->
                        navController.navigate("viewer/$index")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToTrash = { navController.navigate("trash") },
                    onNavigateToCleaner = { navController.navigate("cleaner") },
                    onNavigateToDuplicates = { navController.navigate("duplicates") },
                    onOpenDrawer = {}
                )
            }

            composable("trash") {
                TrashScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("cleaner") {
                CleanerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("duplicates") {
                DuplicateFinderScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("creative_studio") {
                CreativeStudioScreen(
                    viewModel = viewModel,
                    onOpenDrawer = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("smart_hub") {
                SmartCategoriesScreen(
                    viewModel = viewModel,
                    onNavigateToMediaDetail = { mediaId ->
                        // Navigate to viewer screen at correct index if possible, or just open viewer
                        val index = viewModel.displayedMedia.value.indexOfFirst { it.id == mediaId }
                        if (index != -1) {
                            navController.navigate("viewer/$index")
                        }
                    },
                    onOpenDrawer = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private data class DrawerItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
