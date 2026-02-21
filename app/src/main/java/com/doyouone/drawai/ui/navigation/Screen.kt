package com.doyouone.drawai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Generate : Screen("generate/{workflowId}") {
        fun createRoute(workflowId: String) = "generate/$workflowId"
    }
    object Gallery : Screen("gallery")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object Subscription : Screen("subscription")
    object Vision : Screen("vision?workflowId={workflowId}&source={source}") {
        fun createRoute(workflowId: String? = null, source: String? = null): String {
            return if (workflowId != null && source != null) {
                "vision?workflowId=$workflowId&source=$source"
            } else {
                "vision"
            }
        }
    }
    object Explore : Screen("explore")
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object Chat : Screen("chat")
    object CharacterChat : Screen("character_chat/{characterId}") {
        fun createRoute(characterId: String) = "character_chat/$characterId"
    }
    object SummoningAnimation : Screen("summoning/{characterId}/{imageUrl}/{sinCount}/{rarity}") {
        fun createRoute(characterId: String, imageUrl: String, sinCount: Int, rarity: String): String {
            // URL encode the imageUrl to handle special characters
            val encodedUrl = java.net.URLEncoder.encode(imageUrl, "UTF-8")
            val encodedRarity = java.net.URLEncoder.encode(rarity, "UTF-8")
            return "summoning/$characterId/$encodedUrl/$sinCount/$encodedRarity"
        }
    }
    object Profile : Screen("profile")
    object News : Screen("news")
    object Leaderboard : Screen("leaderboard")
    object BackgroundRemover : Screen("background_remover")
    object BackgroundRemoverAdvanced : Screen("background_remover_advanced")
    object UpscaleImage : Screen("upscale_image")
    object MakeBackground : Screen("make_background")
    object MakeBackgroundAdvanced : Screen("make_background_advanced")
    object FaceRestore : Screen("face_restore")
    object SketchToImage : Screen("sketch_to_image")
    object DrawToImage : Screen("draw_to_image")
    object Inventory : Screen("inventory")
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Explore : BottomNavItem(Screen.Explore.route, "Explore", Icons.Default.Search)
    object Gallery : BottomNavItem(Screen.Gallery.route, "Gallery", Icons.Default.PhotoLibrary)
    object Chat : BottomNavItem(Screen.Chat.route, "Chat", androidx.compose.material.icons.Icons.Default.Star)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
}
