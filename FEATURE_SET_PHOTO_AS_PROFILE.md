# Feature: Set AI Photo as Character Profile

## Backend Implementation

### 1. Add Endpoint to api_server.py

Tambahkan method ini setelah `toggle_character_notification` (sekitar line 2204):

```python
def update_character_profile_image(self):
    """Update character profile image from a photo URL."""
    try:
        from flask import g
        
        user_id = getattr(g, 'user_id', None)
        if not user_id:
            return jsonify({"success": False, "error": "Unauthorized"}), 401
        
        data = request.get_json()
        char_id = data.get('characterId')
        image_url = data.get('imageUrl')
        
        if not char_id or not image_url:
            return jsonify({"success": False, "error": "Missing characterId or imageUrl"}), 400
        
        char_ref = self.config.db.collection('characters').document(char_id)
        char_doc = char_ref.get()
        
        if not char_doc.exists:
            return jsonify({"success": False, "error": "Character not found"}), 404
        
        char_data = char_doc.to_dict()
        
        # Verify ownership
        if char_data.get('userId') and char_data.get('userId') != user_id:
            return jsonify({"success": False, "error": "Not your character"}), 403
        
        # Update profile image
        char_ref.update({
            'imageUrl': image_url,
            'profileUpdatedAt': datetime.now().isoformat()
        })
        
        return jsonify({
            "success": True,
            "message": "Profile image updated successfully",
            "imageUrl": image_url
        })
        
    except Exception as e:
        self.logger.error(f"Error updating profile image: {e}")
        return jsonify({"success": False, "error": str(e)}), 500
```

### 2. Route sudah ditambahkan (line 853):
```python
self.app.route('/character/update-profile-image', methods=['POST'])(self.update_character_profile_image)
```

## Android Implementation

### 1. Add API Service Method

File: `DrawAIApiService.kt`

```kotlin
@POST("character/update-profile-image")
suspend fun updateCharacterProfileImage(
    @Body request: UpdateProfileImageRequest
): Response<UpdateProfileImageResponse>
```

### 2. Add Data Models

File: `Character.kt`

```kotlin
data class UpdateProfileImageRequest(
    val characterId: String,
    val imageUrl: String
)

data class UpdateProfileImageResponse(
    val success: Boolean,
    val message: String?,
    val imageUrl: String?
)
```

### 3. Add Repository Method

File: `DrawAIRepository.kt`

```kotlin
suspend fun updateCharacterProfileImage(
    characterId: String,
    imageUrl: String
): Result<UpdateProfileImageResponse> = withContext(Dispatchers.IO) {
    try {
        val response = apiService.updateCharacterProfileImage(
            UpdateProfileImageRequest(characterId, imageUrl)
        )
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            val errorBody = response.errorBody()?.string()
            Result.failure(Exception("Update failed: ${response.code()} $errorBody"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error updating profile image", e)
        Result.failure(e)
    }
}
```

### 4. UI Implementation - Image Click Dialog

File: `CharacterChatScreen.kt`

Ketika user klik foto yang dikirim AI, tampilkan dialog dengan opsi:

```kotlin
// State untuk dialog
var showImageOptionsDialog by remember { mutableStateOf(false) }
var selectedImageUrl by remember { mutableStateOf<String?>(null) }

// Dialog untuk opsi gambar
if (showImageOptionsDialog && selectedImageUrl != null) {
    AlertDialog(
        onDismissRequest = { showImageOptionsDialog = false },
        title = { Text("Photo Options") },
        text = { 
            Column {
                Text("What would you like to do with this photo?")
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Selected photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        val result = drawAIRepository.updateCharacterProfileImage(
                            characterId = characterId,
                            imageUrl = selectedImageUrl!!
                        )
                        result.onSuccess { res ->
                            // Update local character state
                            character = character?.copy(imageUrl = selectedImageUrl!!)
                            Toast.makeText(
                                context,
                                "Profile photo updated!",
                                Toast.LENGTH_SHORT
                            ).show()
                            showImageOptionsDialog = false
                        }.onFailure { e ->
                            Toast.makeText(
                                context,
                                e.message ?: "Failed to update",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            ) {
                Text("Set as Profile Photo")
            }
        },
        dismissButton = {
            TextButton(onClick = { showImageOptionsDialog = false }) {
                Text("Cancel")
            }
        }
    )
}

// Dalam ChatMessageBubble, tambahkan onClick untuk image:
if (message.imageUrl != null) {
    AsyncImage(
        model = message.imageUrl,
        contentDescription = "Shared photo",
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                selectedImageUrl = message.imageUrl
                showImageOptionsDialog = true
            },
        contentScale = ContentScale.Crop
    )
}
```

## Testing Steps

1. **Backend**: Restart server
2. **Android**: 
   - Request photo dari character AI
   - Klik foto yang dikirim
   - Dialog muncul dengan preview dan tombol "Set as Profile Photo"
   - Klik tombol
   - Foto profil character berubah
   - Verify di UI bahwa avatar character sudah update

## Notes

- Foto yang di-set akan langsung terlihat di:
  - Character list
  - Chat header
  - Profile tab
- Perubahan disimpan di Firestore field `imageUrl`
- Field `profileUpdatedAt` mencatat waktu update terakhir
