package com.doyouone.drawai.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await


data class LeaderboardEntry(
    val userId: String = "",
    val userName: String = "Anonymous",
    val userPhoto: String? = null,
    val score: Double = 0.0
)

data class LeaderboardData(
    val entries: List<LeaderboardEntry> = emptyList(),
    val updatedAt: Any? = null
)

class LeaderboardRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val leaderboardRef = firestore.collection("leaderboards")

    suspend fun getTopCreators(): Result<List<LeaderboardEntry>> = fetchLeaderboard("top_creators")
    suspend fun getTopCreatorsWeekly(): Result<List<LeaderboardEntry>> = fetchLeaderboard("top_creators_weekly")
    suspend fun getTopCreatorsMonthly(): Result<List<LeaderboardEntry>> = fetchLeaderboard("top_creators_monthly")
    
    suspend fun getTopRomancers(): Result<List<LeaderboardEntry>> = fetchLeaderboard("top_romancers")
    
    suspend fun getCommunityMVPs(): Result<List<LeaderboardEntry>> = fetchLeaderboard("community_mvp")
    
    suspend fun getRisingStars(): Result<List<LeaderboardEntry>> = fetchLeaderboard("rising_stars")
    
    suspend fun getCategoryLikes(category: String): Result<List<LeaderboardEntry>> = fetchLeaderboard("likes_$category")
    
    suspend fun getCategoryDownloads(category: String): Result<List<LeaderboardEntry>> = fetchLeaderboard("downloads_$category")

    private suspend fun fetchLeaderboard(documentId: String): Result<List<LeaderboardEntry>> {
        return try {
            val snapshot = leaderboardRef.document(documentId).get().await()
            if (snapshot.exists()) {
                val data = snapshot.toObject(LeaderboardData::class.java)
                val entries = data?.entries ?: emptyList()
                // Debug log (can use Timber or Log in real app, here checking result)
                if (entries.isEmpty()) {
                   println("Leaderboard $documentId exists but entries empty or parse fail.") 
                }
                Result.success(entries)
            } else {
                println("Leaderboard $documentId does not exist.")
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
