package com.doyouone.drawai.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Utility for image compression and thumbnail generation
 * Optimizes image sizes for faster loading and reduced memory usage
 */
class ImageCompressor(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageCompressor"
        private const val THUMBNAIL_MAX_DIMENSION = 512 // pixels
        private const val COMPRESSION_QUALITY = 80 // 0-100
        private const val THUMBNAIL_QUALITY = 75 // Lower quality for thumbnails
    }
    
    /**
     * Generate thumbnail from image file
     * @param sourceFile Original image file
     * @param outputFile Optional output file. If null, will create adjacent .thumb.jpg file
     * @return Thumbnail file or null if failed
     */
    suspend fun generateThumbnail(
        sourceFile: File,
        outputFile: File? = null,
        maxDimension: Int = THUMBNAIL_MAX_DIMENSION
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                Log.e(TAG, "Source file does not exist: ${sourceFile.path}")
                return@withContext null
            }
            
            // Decode with inJustDecodeBounds to get dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceFile.path, options)
            
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            
            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.e(TAG, "Invalid image dimensions")
                return@withContext null
            }
            
            // Calculate scale factor
            val scaleFactor = calculateScaleFactor(originalWidth, originalHeight, maxDimension)
            
            // Decode with sampling
            options.apply {
                inJustDecodeBounds = false
                inSampleSize = scaleFactor
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            }
            
            val bitmap = BitmapFactory.decodeFile(sourceFile.path, options)
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap")
                return@withContext null
            }
            
            // Create thumbnail file
            val thumbFile = outputFile ?: File(
                sourceFile.parent,
                sourceFile.nameWithoutExtension + ".thumb.jpg"
            )
            
            // Save compressed thumbnail
            FileOutputStream(thumbFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
            }
            
            bitmap.recycle()
            
            val originalSize = sourceFile.length() / 1024 // KB
            val thumbSize = thumbFile.length() / 1024 // KB
            val savings = ((originalSize - thumbSize).toFloat() / originalSize * 100).toInt()
            
            Log.d(TAG, "✅ Thumbnail created: ${thumbFile.name} (${thumbSize}KB, saved ${savings}%)")
            
            thumbFile
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail", e)
            null
        }
    }
    
    /**
     * Compress image to reduce file size
     * @param sourceFile Original image
     * @param quality Compression quality (0-100)
     * @return Compressed file or null if failed
     */
    suspend fun compressImage(
        sourceFile: File,
        quality: Int = COMPRESSION_QUALITY
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                Log.e(TAG, "Source file does not exist: ${sourceFile.path}")
                return@withContext null
            }
            
            val bitmap = BitmapFactory.decodeFile(sourceFile.path)
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap for compression")
                return@withContext null
            }
            
            // Create compressed file
            val compressedFile = File(
                sourceFile.parent,
                sourceFile.nameWithoutExtension + ".compressed.jpg"
            )
            
            FileOutputStream(compressedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            
            bitmap.recycle()
            
            val originalSize = sourceFile.length() / 1024 // KB
            val compressedSize = compressedFile.length() / 1024 // KB
            val savings = ((originalSize - compressedSize).toFloat() / originalSize * 100).toInt()
            
            Log.d(TAG, "✅ Image compressed: ${compressedFile.name} (${compressedSize}KB, saved ${savings}%)")
            
            compressedFile
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            null
        }
    }
    
    /**
     * Compress bitmap to byte array
     * Useful for network transmission or caching
     */
    suspend fun compressBitmapToBytes(
        bitmap: Bitmap,
        quality: Int = COMPRESSION_QUALITY
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing bitmap to bytes", e)
            null
        }
    }
    
    /**
     * Calculate appropriate sample size for downscaling
     */
    private fun calculateScaleFactor(
        width: Int,
        height: Int,
        maxDimension: Int
    ): Int {
        val maxOriginalDimension = maxOf(width, height)
        var scaleFactor = 1
        
        while (maxOriginalDimension / scaleFactor > maxDimension) {
            scaleFactor *= 2
        }
        
        return scaleFactor
    }
    
    /**
     * Calculate scaled dimensions maintaining aspect ratio
     */
    fun calculateScaledDimensions(
        originalWidth: Int,
        originalHeight: Int,
        maxDimension: Int
    ): Pair<Int, Int> {
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        
        return if (originalWidth > originalHeight) {
            // Landscape
            val newWidth = min(originalWidth, maxDimension)
            val newHeight = (newWidth / aspectRatio).toInt()
            Pair(newWidth, newHeight)
        } else {
            // Portrait or square
            val newHeight = min(originalHeight, maxDimension)
            val newWidth = (newHeight * aspectRatio).toInt()
            Pair(newWidth, newHeight)
        }
    }
    
    /**
     * Get file size in human-readable format
     */
    fun getFileSizeFormatted(file: File): String {
        val bytes = file.length()
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            else -> String.format("%.1fMB", bytes / (1024f * 1024f))
        }
    }
}
