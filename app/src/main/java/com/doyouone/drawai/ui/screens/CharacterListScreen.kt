package com.doyouone.drawai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.doyouone.drawai.data.model.Character
import com.doyouone.drawai.data.repository.CharacterRepository
import com.doyouone.drawai.ui.theme.*
import com.doyouone.drawai.ui.components.AnimeDrawTopAppBar
import com.doyouone.drawai.ui.components.AnimeDrawAppBarTitle
import com.doyouone.drawai.util.ErrorUtils
import com.doyouone.drawai.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CharacterListScreen(
    onCharacterClick: (String) -> Unit,
    onNewCharacterClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    maxChatLimit: Int = 1
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { CharacterRepository() }
    
    var characters by remember { mutableStateOf<List<Character>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Deletion state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var characterToDelete by remember { mutableStateOf<Character?>(null) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                repository.getUserCharactersFlow().collect { list ->
                    characters = list
                    isLoading = false
                    error = null
                }
            } catch (e: Exception) {
                error = ErrorUtils.getSafeErrorMessage(e)
                isLoading = false
            }
        }
    }
    
    Scaffold(
        floatingActionButton = {
            // Only show FAB if limit not reached
            if (characters.size < maxChatLimit) {
                FloatingActionButton(
                    onClick = onNewCharacterClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.character_list_new_character))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Standardized Header
            com.doyouone.drawai.ui.components.AnimeDrawMainTopBar(
                title = stringResource(R.string.character_list_title),
                onOpenDrawer = onOpenDrawer,
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.character_list_search))
                    }
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (error != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: stringResource(R.string.character_list_unknown_error), 
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                isLoading = true
                                error = null
                                scope.launch {
                                    try {
                                        repository.getUserCharactersFlow().collect { list ->
                                            characters = list
                                            isLoading = false
                                            error = null
                                        }
                                    } catch (e: Exception) {
                                        error = ErrorUtils.getSafeErrorMessage(e)
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(stringResource(R.string.character_list_retry))
                        }
                    }
                } else if (characters.isEmpty()) {
                    EmptyListState(onNewCharacterClick)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(characters) { character ->
                            CharacterListItem(
                                character = character,
                                onClick = { onCharacterClick(character.id) },
                                onLongClick = {
                                    characterToDelete = character
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && characterToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false 
                characterToDelete = null
            },
            title = { Text(stringResource(R.string.character_list_delete_title)) },
            text = { 
                Text(stringResource(R.string.character_list_delete_message)) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        val charId = characterToDelete!!.id
                        val charToDelete = characterToDelete
                        showDeleteDialog = false
                        characterToDelete = null
                        
                        // Optimistic update - remove from list immediately
                        characters = characters.filter { it.id != charId }
                        
                        scope.launch {
                            val result = repository.deleteCharacter(charId)
                            if (result.isFailure) {
                                // Restore character if deletion failed
                                charToDelete?.let { char ->
                                    characters = characters + char
                                }
                                android.util.Log.e("CharacterListScreen", "Failed to delete character: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.character_list_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        characterToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.character_list_cancel))
                }
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CharacterListItem(
    character: Character,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(56.dp)
        ) {
            AsyncImage(
                model = if (!character.imageStorageUrl.isNullOrEmpty()) character.imageStorageUrl else character.imageUrl,
                contentDescription = character.personality.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                error = coil.compose.rememberAsyncImagePainter(
                    model = com.doyouone.drawai.R.drawable.ic_launcher_foreground // Fallback
                )
            )
            
            // Online/Status Indicator (Optional)
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color.Green, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = character.personality.name.ifEmpty { character.personality.archetype },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = formatTime(character.relationship.getLastInteractionString()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Relationship Badge
                Surface(
                    color = Color(character.relationship.stage.color).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = character.relationship.stage.emoji,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
                
                Text(
                    text = character.relationship.stage.displayName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyListState(onNewCharacterClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.character_list_empty_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.character_list_empty_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNewCharacterClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.character_list_create_new))
        }
    }
}

private fun formatTime(isoString: String): String {
    return try {
        // Simple parser, in real app use robust date formatter
        if (isoString.contains("T")) {
            val timePart = isoString.split("T")[1]
            timePart.substring(0, 5) // HH:mm
        } else {
            "Now"
        }
    } catch (e: Exception) {
        ""
    }
}
