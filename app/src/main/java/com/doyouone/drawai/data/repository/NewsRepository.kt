package com.doyouone.drawai.data.repository

import com.doyouone.drawai.data.model.NewsItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NewsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val newsCollection = firestore.collection("news")

    fun getNews(): Flow<List<NewsItem>> = callbackFlow {
        val listener = newsCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NewsItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    suspend fun seedSampleData() {
        try {
            val snapshot = newsCollection.limit(1).get().await()
            if (snapshot.isEmpty) {
                // Seed Event
                val event = NewsItem(
                    type = "event",
                    title = "Generate Like Contest",
                    description = "Create the most liked generation and win $1! Join the community contest now.",
                    date = Timestamp.now(),
                    imageUrl = "https://placehold.co/600x400/png", // Placeholder image
                    actionUrl = "https://discord.gg/animedrawai"
                )
                newsCollection.add(event).await()

                // Seed Update
                val update = NewsItem(
                    type = "update",
                    title = "Patch 1.0.39",
                    description = "Build 39: Added new Event & Update screen, improved UI performance, and fixed minor bugs.",
                    date = Timestamp.now(),
                    version = "1.0.39"
                )
                newsCollection.add(update).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
