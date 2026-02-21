package com.doyouone.drawai.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import android.os.Parcel
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.TypeParceler

/**
 * Sort type for community posts
 */
enum class SortType {
    POPULAR,  // Sort by likes
    RECENT,   // Sort by createdAt
    TRENDING,  // Combination of likes + recency
    MY_POSTS  // User's own posts
}

/**
 * Parceler for java.util.Date
 */

/**
 * Parceler for java.util.Date
 */
object DateParceler : Parceler<Date?> {
    override fun create(parcel: Parcel): Date? {
        val timestamp = parcel.readLong()
        return if (timestamp == -1L) null else Date(timestamp)
    }

    override fun Date?.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(this?.time ?: -1L)
    }
}

/**
 * Data model for community shared posts
 * Represents an image shared to the public community feed
 */
@Parcelize
@TypeParceler<Date?, DateParceler>()
data class CommunityPost(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String? = null,
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val prompt: String = "",
    val negativePrompt: String = "",
    val workflow: String = "",
    val likes: Int = 0,
    val downloads: Int = 0,
    val views: Int = 0,
    val tags: List<String>? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    val isReported: Boolean = false,
    val reportCount: Int = 0
) : Parcelable {
    /**
     * Convert to Map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "username" to username,
            "userPhotoUrl" to userPhotoUrl,
            "imageUrl" to imageUrl,
            "thumbnailUrl" to thumbnailUrl,
            "prompt" to prompt,
            "negativePrompt" to negativePrompt,
            "workflow" to workflow,
            "likes" to likes,
            "downloads" to downloads,
            "views" to views,
            "tags" to tags,
            "createdAt" to createdAt,
            "isReported" to isReported,
            "reportCount" to reportCount
        )
    }
}
