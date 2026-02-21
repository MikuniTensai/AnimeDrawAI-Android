package com.doyouone.drawai.util

import com.doyouone.drawai.data.model.Workflow
import com.doyouone.drawai.data.model.GeneratedImage

/**
 * Dummy data untuk testing UI
 */
object DummyData {
    
    fun getDummyWorkflows(): List<Workflow> {
        return listOf(
            Workflow(
                id = "anime_portrait",
                name = "Anime Style Portrait",
                description = "Beautiful detailed face with expressive eyes",
                estimatedTime = "20 seconds",
                category = "Anime"
            ),
            Workflow(
                id = "anime_sit_pose",
                name = "Anime Sit Pose",
                description = "Elegant sitting pose with cute style",
                estimatedTime = "24 seconds",
                category = "Anime"
            ),
            Workflow(
                id = "anime_action",
                name = "Anime Action Dynamic",
                description = "Dynamic action with energy effects",
                estimatedTime = "30 seconds",
                category = "Anime"
            ),
            Workflow(
                id = "anime_chibi",
                name = "Anime Kawaii Chibi",
                description = "Cute chibi style with pastel colors",
                estimatedTime = "12 seconds",
                category = "Anime"
            ),
            Workflow(
                id = "anime_fantasy",
                name = "Anime Fantasy Magic",
                description = "Magical effects with mystical atmosphere",
                estimatedTime = "36 seconds",
                category = "Anime"
            ),
            Workflow(
                id = "anime_school",
                name = "Anime School Uniform",
                description = "School girl with cheerful expression",
                estimatedTime = "20 seconds",
                category = "Anime"
            ),
            Workflow(
                id = "anime_cyberpunk",
                name = "Anime Cyberpunk Neon",
                description = "Futuristic with neon lights",
                estimatedTime = "32 seconds",
                category = "Anime"
            ),
            Workflow(
                id = "anime_warrior",
                name = "Anime Warrior Battle",
                description = "Battle stance with epic scene",
                estimatedTime = "40 seconds",
                category = "Anime"
            ),
            Workflow(
                id = "anime_idol",
                name = "Anime Idol Stage",
                description = "Idol performing with stage lights",
                estimatedTime = "24 seconds",
                category = "Anime"
            ),
            Workflow(
                id = "anime_kimono",
                name = "Anime Elegant Kimono",
                description = "Traditional kimono with grace",
                estimatedTime = "28 seconds",
                category = "Anime"
            )
        )
    }
    
    fun getDummyGalleryImages(): List<GeneratedImage> {
        return listOf(
            GeneratedImage(
                id = "1",
                prompt = "anime girl, beautiful, detailed face",
                negativePrompt = "ugly, blurry",
                workflow = "Anime_Style_Portrait",
                imageUrl = "https://via.placeholder.com/512",
                createdAt = "2024-01-01T10:00:00Z",
                isFavorite = true
            ),
            GeneratedImage(
                id = "2",
                prompt = "anime warrior, epic battle scene",
                negativePrompt = "low quality",
                workflow = "Anime_Warrior_Battle",
                imageUrl = "https://via.placeholder.com/512",
                createdAt = "2024-01-01T09:00:00Z",
                isFavorite = false
            ),
            GeneratedImage(
                id = "3",
                prompt = "anime idol, stage performance",
                negativePrompt = "",
                workflow = "Anime_Idol_Stage",
                imageUrl = "https://via.placeholder.com/512",
                createdAt = "2024-01-01T08:00:00Z",
                isFavorite = true
            )
        )
    }
}
