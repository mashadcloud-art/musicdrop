package com.musicdrop.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.ui.viewmodel.MainViewModel

/**
 * TV Search screen — large text input + 3-column grid of results.
 * All search state is shared with the mobile app via [MainViewModel.ytSearchResults].
 */
@Composable
fun TvSearchScreen(
    viewModel: MainViewModel,
    onPlaySong: (YouTubeSearchResult) -> Unit
) {
    val query by viewModel.ytSearchQuery.collectAsState()
    val results by viewModel.ytSearchResults.collectAsState()
    val isLoading by viewModel.ytSearchLoading.collectAsState()
    val searchError by viewModel.ytSearchError.collectAsState()

    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { fieldFocus.requestFocus() } catch (_: Throwable) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        // Search header
        Text(
            "🔍 Search Music",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.setYtSearchQuery(it) },
            placeholder = {
                Text("Type a song, artist, or album…", color = Color.White.copy(0.4f), fontSize = 18.sp)
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(28.dp))
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, color = Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* already debounced */ }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedBorderColor = Color.White.copy(0.2f),
                cursorColor = Color(0xFF7C3AED),
                focusedContainerColor = Color.White.copy(0.05f),
                unfocusedContainerColor = Color.White.copy(0.03f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(fieldFocus)
        )

        Spacer(Modifier.height(24.dp))

        // Results area
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
                    )
                }
                searchError != null && results.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Search, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(searchError ?: "No results", color = Color.White.copy(0.5f), fontSize = 16.sp)
                    }
                }
                results.isEmpty() && query.isBlank() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(0.2f), modifier = Modifier.size(80.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Start typing to search", color = Color.White.copy(0.35f), fontSize = 18.sp)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(results.take(30)) { track ->
                            TvTrackCard(track = track, onClick = { onPlaySong(track) })
                        }
                    }
                }
            }
        }
    }
}
