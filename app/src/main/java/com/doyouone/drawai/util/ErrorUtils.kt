package com.doyouone.drawai.util

object ErrorUtils {
    fun getSafeErrorMessage(e: Throwable?): String {
        val message = e?.message ?: return "An unknown error occurred"
        return when {
            message.contains("Unable to resolve host", ignoreCase = true) -> "Please check your internet connection."
            message.contains("Failed to connect", ignoreCase = true) -> "Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Please check your internet connection."
            message.contains("SSL", ignoreCase = true) -> "Please check your internet connection."
            message.contains("EAI_NODATA", ignoreCase = true) -> "Please check your internet connection."
            message.contains("HTTP", ignoreCase = true) -> "Message not sent."
            else -> "Message not sent."
        }
    }
}
