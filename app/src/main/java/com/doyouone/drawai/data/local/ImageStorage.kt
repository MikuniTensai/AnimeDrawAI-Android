package com.doyouone.drawai.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.doyouone.drawai.data.model.GeneratedImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Local storage untuk generated images
 */
class ImageStorage(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageStorage"
        private const val IMAGES_DIR = "output/generated_images"  // Updated path
        private const val METADATA_FILE = "images_metadata.json"
    }
    
    private val imagesDir: File by lazy {
        File(context.filesDir, IMAGES_DIR).apply {
            if (!exists()) mkdirs()
            Log.d(TAG, "Images directory: ${absolutePath}")
        }
    }
    
    private val metadataFile: File by lazy {
        File(context.filesDir, METADATA_FILE)
    }
    
    private val gson = Gson()
    
    /**
     * Save image bitmap to local storage
     */
    suspend fun saveImage(
        bitmap: Bitmap,
        prompt: String,
        negativePrompt: String,
        workflow: String,
        seed: Long? = null
    ): GeneratedImage? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val uniqueId = UUID.randomUUID().toString().substring(0, 8)
            
            // Format: workflowname_generated_unique
            // Use "unknown" if workflow is empty
            val workflowName = if (workflow.isNotBlank()) {
                workflow.replace(" ", "_").replace("-", "_").lowercase()
            } else {
                "unknown_workflow"
            }
            val filename = "${workflowName}_generated_${uniqueId}.jpg"
            val imageFile = File(imagesDir, filename)
            
            Log.d(TAG, "Saving image with workflow='$workflow', seed=$seed -> filename='$filename'")
            
            // Save bitmap
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            // Create metadata
            val image = GeneratedImage(
                id = timestamp.toString(),
                prompt = prompt,
                negativePrompt = negativePrompt,
                workflow = workflow,
                imageUrl = imageFile.absolutePath,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                isFavorite = false,
                seed = seed  // Save seed to metadata
            )
            
            // Save to metadata
            val images = getAllImages().toMutableList()
            images.add(0, image) // Add to beginning
            saveMetadata(images)
            
            Log.d(TAG, "Image saved: $filename at ${imageFile.absolutePath}")
            image
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image", e)
            null
        }
    }
    
    /**
     * Save downloaded image from URL
     */
    suspend fun saveImageFromBytes(
        imageBytes: ByteArray,
        prompt: String,
        negativePrompt: String,
        workflow: String,
        seed: Long? = null
    ): GeneratedImage? = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            saveImage(bitmap, prompt, negativePrompt, workflow, seed)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image from bytes", e)
            null
        }
    }
    
    /**
     * Get all saved images
     */
    fun getAllImages(): List<GeneratedImage> {
        return try {
            if (!metadataFile.exists()) {
                emptyList()
            } else {
                val json = metadataFile.readText()
                val type = object : TypeToken<List<GeneratedImage>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading metadata", e)
            emptyList()
        }
    }
    
    /**
     * Toggle favorite status
     */
    fun toggleFavorite(imageId: String): Boolean {
        return try {
            val images = getAllImages().toMutableList()
            val index = images.indexOfFirst { it.id == imageId }
            if (index != -1) {
                images[index] = images[index].copy(isFavorite = !images[index].isFavorite)
                saveMetadata(images)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling favorite", e)
            false
        }
    }
    
    /**
     * Delete image
     */
    fun deleteImage(imageId: String): Boolean {
        return try {
            val images = getAllImages().toMutableList()
            val image = images.find { it.id == imageId }
            
            if (image != null) {
                // Delete file
                val file = File(image.imageUrl)
                if (file.exists()) {
                    file.delete()
                }
                
                // Remove from metadata
                images.removeIf { it.id == imageId }
                saveMetadata(images)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image", e)
            false
        }
    }
    
    /**
     * Save metadata to file
     */
    private fun saveMetadata(images: List<GeneratedImage>) {
        try {
            val json = gson.toJson(images)
            metadataFile.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving metadata", e)
        }
    }
    
    /**
     * Clear all images
     */
    fun clearAll() {
        try {
            imagesDir.listFiles()?.forEach { it.delete() }
            metadataFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing images", e)
        }
    }
}
