package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyState
import com.example.ui.components.MediaGridItem
import com.example.ui.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: GalleryViewModel,
    onNavigateToViewer: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val gridDensity by viewModel.gridDensity.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search photos, folders, types...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp)
                            .testTag("search_text_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (searchQuery.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "Search Lumora",
                    description = "Search your device library instantly by display name, bucket folder, format types (jpeg, png, mp4) and more."
                )
            } else if (searchResults.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Close,
                    title = "No Matches Found",
                    description = "We couldn't find any photos or videos matching \"$searchQuery\" on your device."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridDensity),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                ) {
                    itemsIndexed(searchResults, key = { _, item -> item.id }) { index, item ->
                        MediaGridItem(
                            item = item,
                            isSelected = false,
                            isInSelectionMode = false,
                            onItemClick = {
                                // Find overall index of this item in the global displaying feed so viewer swiping matches correctly
                                val overallIndex = viewModel.displayedMedia.value.indexOfFirst { it.id == item.id }
                                if (overallIndex != -1) {
                                    onNavigateToViewer(overallIndex)
                                } else {
                                    onNavigateToViewer(index)
                                }
                            },
                            onItemLongClick = {}
                        )
                    }
                }
            }
        }
    }
}
