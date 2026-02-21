# Android Implementation - Set AI Photo as Profile

## ✅ Backend Sudah Selesai
- Endpoint: `POST /character/update-profile-image`
- Method `update_character_profile_image` sudah ditambahkan di `api_server.py` (line 2205)

## 📱 Android Implementation Steps

### 1. Add Data Models to Character.kt

Tambahkan di akhir file `Character.kt` (setelah `InteractionPatterns`):

```kotlin
// Update Profile Image Models
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

### 2. Add API Service Method to DrawAIApiService.kt

Tambahkan method ini:

```kotlin
@POST("character/update-profile-image")
suspend fun updateCharacterProfileImage(
    @Body request: UpdateProfileImageRequest
): Response<UpdateProfileImageResponse>
```

### 3. Add Repository Method to DrawAIRepository.kt

Tambahkan method ini:

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

### 4. Update CharacterChatScreen.kt

#### A. Add State Variables (di bagian atas composable):

```kotlin
// State untuk image options dialog
var showImageOptionsDialog by remember { mutableStateOf(false) }
var selectedImageUrl by remember { mutableStateOf<String?>(null) }
```

#### B. Add Dialog (setelah dialog-dialog lain):

```kotlin
// Image Options Dialog
if (showImageOptionsDialog && selectedImageUrl != null) {
    AlertDialog(
        onDismissRequest = { showImageOptionsDialog = false },
        title = { Text("Photo Options") },
        text = { 
            Column {
                Text("Set this photo as ${character?.personality?.name}'s profile picture?")
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Selected photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
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
                            android.widget.Toast.makeText(
                                context,
                                "Profile photo updated! ✨",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            showImageOptionsDialog = false
                        }.onFailure { e ->
                            android.widget.Toast.makeText(
                                context,
                                e.message ?: "Failed to update",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            ) {
                Text("Set as Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = { showImageOptionsDialog = false }) {
                Text("Cancel")
            }
        }
    )
}
```

#### C. Update ChatMessageBubble (cari bagian yang render image):

Cari kode yang menampilkan `message.imageUrl` dan tambahkan `.clickable`:

```kotlin
// Dalam ChatMessageBubble, update AsyncImage untuk AI messages:
if (message.imageUrl != null && !message.isUser) {
    AsyncImage(
        model = message.imageUrl,
        contentDescription = "Shared photo",
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                selectedImageUrl = message.imageUrl
                showImageOptionsDialog = true
            },
        contentScale = ContentScale.Crop
    )
}
```

**PENTING**: Hanya foto dari AI (bukan user) yang bisa di-set sebagai profile. Jadi pastikan ada check `!message.isUser`.

### 5. Testing Checklist

- [ ] Backend server restart berhasil
- [ ] Request photo dari character AI
- [ ] AI mengirim foto
- [ ] Klik foto → Dialog muncul
- [ ] Preview foto terlihat di dialog
- [ ] Klik "Set as Profile" → Success toast muncul
- [ ] Foto profil character berubah di:
  - [ ] Character list
  - [ ] Chat header
  - [ ] Profile tab dalam RelationshipStatusDialog
- [ ] Foto tersimpan di Firestore (check field `imageUrl` dan `profileUpdatedAt`)

### 6. Optional Enhancements

#### A. Add Loading State:

```kotlin
var isUpdatingProfile by remember { mutableStateOf(false) }

// Dalam onClick confirmButton:
isUpdatingProfile = true
scope.launch {
    val result = drawAIRepository.updateCharacterProfileImage(...)
    isUpdatingProfile = false
    // ... rest of code
}

// Dalam confirmButton:
TextButton(
    onClick = { /* ... */ },
    enabled = !isUpdatingProfile
) {
    if (isUpdatingProfile) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
    } else {
        Text("Set as Profile")
    }
}
```

#### B. Add Confirmation for Overwrite:

Jika character sudah punya foto profil, tanyakan konfirmasi:

```kotlin
val hasExistingProfile = character?.imageUrl?.isNotEmpty() == true

text = { 
    Column {
        if (hasExistingProfile) {
            Text(
                "This will replace the current profile photo. Continue?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text("Preview:")
        // ... AsyncImage
    }
}
```

## 🎯 User Flow

1. User chat dengan AI character
2. User: "Send me your photo" / "Kirim fotomu"
3. AI: *mengirim foto*
4. User: *klik foto*
5. Dialog: "Set this photo as [Name]'s profile picture?"
6. User: *klik "Set as Profile"*
7. Toast: "Profile photo updated! ✨"
8. Foto profil langsung berubah di semua tempat

## 📝 Notes

- Foto yang di-set akan menjadi `imageUrl` utama character
- Field `profileUpdatedAt` akan ter-update otomatis
- Hanya foto dari AI yang bisa di-set (bukan foto yang dikirim user)
- Perubahan langsung tersimpan di Firestore
- Update langsung terlihat tanpa perlu reload
