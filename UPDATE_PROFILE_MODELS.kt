
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
