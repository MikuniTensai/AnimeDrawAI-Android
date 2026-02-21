package com.doyouone.drawai.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DuplicateUserCleaner {
    companion object {
        private const val TAG = "DuplicateUserCleaner"
        
        /**
         * Clean up duplicate users created by faulty account linking
         * This function identifies and removes duplicate user entries
         */
        suspend fun cleanupDuplicateUsers(
            firestore: FirebaseFirestore,
            email: String? = null,
            displayName: String? = null
        ): Result<String> {
            return try {
                Log.d(TAG, "🧹 Starting duplicate user cleanup...")
                
                val usersCollection = firestore.collection("users")
                var duplicatesFound = 0
                var duplicatesRemoved = 0
                
                // Query for potential duplicates
                val query = if (email != null) {
                    usersCollection.whereEqualTo("email", email)
                } else if (displayName != null) {
                    usersCollection.whereEqualTo("displayName", displayName)
                } else {
                    // Get all users and find duplicates by displayName
                    usersCollection.get().await()
                    return Result.failure(Exception("Need email or displayName to identify duplicates"))
                }
                
                val documents = query.get().await()
                Log.d(TAG, "Found ${documents.size()} users with matching criteria")
                
                if (documents.size() <= 1) {
                    return Result.success("No duplicates found")
                }
                
                // Group by displayName to find duplicates
                val userGroups = documents.documents.groupBy { 
                    it.getString("displayName") ?: "unknown" 
                }
                
                for ((name, users) in userGroups) {
                    if (users.size > 1) {
                        duplicatesFound += users.size - 1
                        Log.d(TAG, "Found ${users.size} users with displayName: $name")
                        
                        // Sort by creation date (keep the oldest/first one)
                        val sortedUsers = users.sortedBy { 
                            it.getTimestamp("createdAt")?.toDate()?.time ?: 0L 
                        }
                        
                        val keepUser = sortedUsers.first()
                        val duplicateUsers = sortedUsers.drop(1)
                        
                        Log.d(TAG, "Keeping user: ${keepUser.id} (created: ${keepUser.getTimestamp("createdAt")})")
                        
                        // Remove duplicates
                        for (duplicate in duplicateUsers) {
                            try {
                                Log.d(TAG, "Removing duplicate user: ${duplicate.id} (created: ${duplicate.getTimestamp("createdAt")})")
                                
                                // Also remove from generation_limits if exists
                                val generationLimitDoc = firestore.collection("generation_limits")
                                    .document(duplicate.id)
                                
                                val generationLimitExists = generationLimitDoc.get().await().exists()
                                if (generationLimitExists) {
                                    generationLimitDoc.delete().await()
                                    Log.d(TAG, "Removed generation_limits for user: ${duplicate.id}")
                                }
                                
                                // Remove user document
                                duplicate.reference.delete().await()
                                duplicatesRemoved++
                                
                                Log.d(TAG, "✅ Successfully removed duplicate user: ${duplicate.id}")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Failed to remove duplicate user: ${duplicate.id}", e)
                            }
                        }
                    }
                }
                
                val message = "Cleanup completed: Found $duplicatesFound duplicates, removed $duplicatesRemoved"
                Log.d(TAG, "🎉 $message")
                Result.success(message)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during duplicate cleanup", e)
                Result.failure(e)
            }
        }
        
        /**
         * Find and list duplicate users without removing them
         */
        suspend fun findDuplicateUsers(firestore: FirebaseFirestore): Result<List<String>> {
            return try {
                val usersCollection = firestore.collection("users")
                val documents = usersCollection.get().await()
                
                val userGroups = documents.documents.groupBy { 
                    it.getString("displayName") ?: "unknown" 
                }
                
                val duplicates = mutableListOf<String>()
                
                for ((name, users) in userGroups) {
                    if (users.size > 1) {
                        duplicates.add("DisplayName '$name' has ${users.size} users:")
                        users.forEach { user ->
                            val createdAt = user.getTimestamp("createdAt")
                            val isAnonymous = user.getBoolean("isAnonymous") ?: false
                            val email = user.getString("email") ?: "no email"
                            duplicates.add("  - ${user.id} (created: $createdAt, anonymous: $isAnonymous, email: $email)")
                        }
                    }
                }
                
                Result.success(duplicates)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}