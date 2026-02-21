package com.doyouone.drawai.data.model

/**
 * Data class untuk prompt templates dan suggestions
 */
data class PromptTemplate(
    val category: String,
    val suggestions: List<String>
)

/**
 * Predefined prompt templates untuk memudahkan user
 */
object PromptTemplates {
    
    // Quality tags
    val qualityTags = listOf(
        "masterpiece",
        "best quality",
        "high quality",
        "detailed",
        "4k",
        "8k",
        "ultra detailed"
    )
    
    // Style tags
    val styleTags = listOf(
        "anime style",
        "manga style",
        "cel shaded",
        "soft lighting",
        "vibrant colors",
        "pastel colors",
        "dramatic lighting"
    )
    
    // Character features
    val characterFeatures = listOf(
        "beautiful face",
        "detailed eyes",
        "expressive eyes",
        "long hair",
        "short hair",
        "twin tails",
        "ponytail"
    )
    
    // Poses
    val poses = listOf(
        "standing pose",
        "sitting pose",
        "dynamic pose",
        "action pose",
        "elegant pose",
        "casual pose"
    )
    
    // Expressions
    val expressions = listOf(
        "smiling",
        "happy",
        "cheerful",
        "cute",
        "serious",
        "confident",
        "shy"
    )
    
    // Environments
    val environments = listOf(
        "outdoor",
        "indoor",
        "city background",
        "nature background",
        "school setting",
        "fantasy setting"
    )
    
    // Negative prompts (common things to avoid)
    val commonNegatives = listOf(
        "ugly",
        "blurry",
        "low quality",
        "bad anatomy",
        "worst quality",
        "deformed",
        "poorly drawn",
        "bad hands"
    )
    
    /**
     * Get all templates organized by category
     */
    fun getAllTemplates(): List<PromptTemplate> {
        return listOf(
            PromptTemplate("Quality", qualityTags),
            PromptTemplate("Style", styleTags),
            PromptTemplate("Character", characterFeatures),
            PromptTemplate("Pose", poses),
            PromptTemplate("Expression", expressions),
            PromptTemplate("Environment", environments)
        )
    }
    
    /**
     * Get suggestions based on workflow type
     */
    fun getSuggestionsForWorkflow(workflowId: String): List<String> {
        return when {
            workflowId.contains("portrait", ignoreCase = true) -> {
                listOf(
                    "beautiful face",
                    "detailed eyes",
                    "expressive eyes",
                    "soft lighting",
                    "masterpiece"
                )
            }
            workflowId.contains("action", ignoreCase = true) -> {
                listOf(
                    "dynamic pose",
                    "action scene",
                    "energy effects",
                    "dramatic lighting",
                    "high quality"
                )
            }
            workflowId.contains("chibi", ignoreCase = true) -> {
                listOf(
                    "cute",
                    "kawaii",
                    "chibi style",
                    "pastel colors",
                    "cheerful"
                )
            }
            workflowId.contains("cyberpunk", ignoreCase = true) -> {
                listOf(
                    "neon lights",
                    "futuristic",
                    "city background",
                    "vibrant colors",
                    "detailed"
                )
            }
            workflowId.contains("fantasy", ignoreCase = true) -> {
                listOf(
                    "magical effects",
                    "mystical atmosphere",
                    "fantasy setting",
                    "detailed",
                    "vibrant colors"
                )
            }
            else -> qualityTags.take(5)
        }
    }
}
