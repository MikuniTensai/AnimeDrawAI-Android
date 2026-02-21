package com.doyouone.drawai.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GoogleSignInManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Configure Google Sign-In with updated API
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("961347264067-tes0t1kdn0m30352vb2pefuu2q1ca7b5.apps.googleusercontent.com")
        .requestEmail()
        .requestProfile()
        .build()
    
    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)
    
    // Get sign-in intent
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }
    
    // Handle sign-in result
    suspend fun handleSignInResult(data: Intent?): Result<FirebaseUser> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            if (account != null) {
                firebaseAuthWithGoogle(account)
            } else {
                Result.failure(Exception("Google Sign-In failed: account is null"))
            }
        } catch (e: ApiException) {
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Authenticate with Firebase using Google credentials
    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            
            if (user != null) {
                // Save user info to Firestore
                val displayName = account.displayName ?: account.email?.substringBefore("@") ?: "User"
                saveUserToFirestore(user.uid, displayName, account.email ?: "")
                Result.success(user)
            } else {
                Result.failure(Exception("Firebase authentication failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Save user info to Firestore
    private suspend fun saveUserToFirestore(userId: String, displayName: String, email: String) {
        try {
            // Check if user already exists
            val docRef = firestore.collection("users").document(userId)
            val document = docRef.get().await()
            
            if (!document.exists()) {
                // New user, create document
                val userData = hashMapOf(
                    "displayName" to displayName,
                    "email" to email,
                    "isAnonymous" to false,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "generationCount" to 0,
                    "lastGenerationTime" to null,
                    "provider" to "google"
                )
                docRef.set(userData).await()
            }
            // If user exists, don't overwrite (they might have updated their profile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Sign out
    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }
    
    // Revoke access (complete sign-out)
    suspend fun revokeAccess() {
        try {
            auth.signOut()
            googleSignInClient.revokeAccess().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
