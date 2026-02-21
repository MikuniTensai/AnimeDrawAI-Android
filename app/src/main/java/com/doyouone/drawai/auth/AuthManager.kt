package com.doyouone.drawai.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import java.text.SimpleDateFormat

class AuthManager(private val context: Context? = null) {
    companion object {
        private const val TAG = "AuthManager"
    }
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var googleSignInManager: GoogleSignInManager? = null
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser
    
    private val _userDisplayName = MutableStateFlow<String>("")
    val userDisplayName: StateFlow<String> = _userDisplayName
    
    init {
        _currentUser.value = auth.currentUser
        updateDisplayName()
        
        // Initialize Google Sign-In if context is provided
        context?.let {
            googleSignInManager = GoogleSignInManager(it)
        }
        
        // Listen to auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            android.util.Log.d("AuthManager", "Auth state changed: ${firebaseAuth.currentUser?.email}")
            _currentUser.value = firebaseAuth.currentUser
            updateDisplayName()
        }
    }
    
    // Force refresh current user (for manual UI updates)
    fun refreshCurrentUser() {
        android.util.Log.d("AuthManager", "Refreshing current user...")
        _currentUser.value = auth.currentUser
        updateDisplayName()
        android.util.Log.d("AuthManager", "Current user: ${_currentUser.value?.email}")
    }
    
    // Sign in anonymously (Guest)
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "🔄 Starting anonymous sign-in...")
            val result = auth.signInAnonymously().await()
            val user = result.user
            if (user != null) {
                Log.d(TAG, "✅ Anonymous sign-in successful!")
                Log.d(TAG, "User UID: ${user.uid}")
                Log.d(TAG, "Is Anonymous: ${user.isAnonymous}")
                _currentUser.value = user
                // Generate random display name for anonymous users
                val randomName = generateRandomName()
                Log.d(TAG, "Generated random name: $randomName")
                saveUserDisplayName(user.uid, randomName, isAnonymous = true)
                updateDisplayName()
                Result.success(user)
            } else {
                Log.e(TAG, "❌ Anonymous sign-in failed: user is null")
                Result.failure(Exception("Failed to sign in anonymously"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Anonymous sign-in error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Sign in with email and password
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                _currentUser.value = user
                updateDisplayName()
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to sign in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Sign up with email and password
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                _currentUser.value = user
                saveUserDisplayName(user.uid, displayName, isAnonymous = false)
                updateDisplayName()
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to create account"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get Google Sign-In intent
    fun getGoogleSignInIntent(): Intent? {
        return googleSignInManager?.getSignInIntent()
    }
    
    // Handle Google Sign-In result
    suspend fun handleGoogleSignInResult(data: Intent?): Result<FirebaseUser> {
        return try {
            val result = googleSignInManager?.handleSignInResult(data)
                ?: return Result.failure(Exception("Google Sign-In not initialized"))
            
            result.onSuccess { user ->
                _currentUser.value = user
                updateDisplayName()
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Link anonymous account to Google (upgrade)
    suspend fun linkAnonymousToGoogle(data: Intent?): Result<FirebaseUser> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null || !currentUser.isAnonymous) {
                return Result.failure(Exception("Current user is not anonymous"))
            }
            
            // Get Google Sign-In account
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            if (account == null) {
                return Result.failure(Exception("Google Sign-In failed: account is null"))
            }
            
            // Create Google credential
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            
            // Link the credential to the current anonymous user
            val result = currentUser.linkWithCredential(credential).await()
            val linkedUser = result.user
            
            if (linkedUser != null) {
                _currentUser.value = linkedUser
                
                // Get the Google display name
                val googleDisplayName = account.displayName ?: account.email?.substringBefore("@") ?: "User"
                
                // Update Firestore to mark as non-anonymous and add Google info
                val userId = linkedUser.uid // This will be the SAME UID as the anonymous user
                firestore.collection("users")
                    .document(userId)
                    .update(
                        mapOf(
                            "isAnonymous" to false,
                            "provider" to "google",
                            "email" to (account.email ?: ""),
                            "displayName" to googleDisplayName
                        )
                    )
                    .await()
                
                // Immediately update the display name in memory
                _userDisplayName.value = googleDisplayName
                
                Log.d(TAG, "✅ Anonymous account successfully linked to Google")
                Log.d(TAG, "User UID: ${linkedUser.uid} (SAME AS BEFORE)")
                Log.d(TAG, "Email: ${linkedUser.email}")
                Log.d(TAG, "Display Name: $googleDisplayName")
                Log.d(TAG, "Updated display name to: ${_userDisplayName.value}")
                
                // Also call updateDisplayName to ensure consistency
                updateDisplayName()
                Result.success(linkedUser)
            } else {
                Result.failure(Exception("Account linking failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error linking anonymous account to Google", e)
            
            // Handle the case where Google account already exists
            if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                Log.d(TAG, "🔄 Google account already exists, switching to existing account...")
                
                try {
                    // Get Google Sign-In account
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(ApiException::class.java)
                    
                    if (account != null) {
                        // Create Google credential
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        
                        // Sign in with the existing Google account
                        val signInResult = auth.signInWithCredential(credential).await()
                        val googleUser = signInResult.user
                        
                        if (googleUser != null) {
                            _currentUser.value = googleUser
                            
                            // Get the Google display name
                            val googleDisplayName = account.displayName ?: account.email?.substringBefore("@") ?: "User"
                            
                            // Update display name immediately
                            _userDisplayName.value = googleDisplayName
                            
                            Log.d(TAG, "✅ Successfully switched to existing Google account")
                            Log.d(TAG, "User UID: ${googleUser.uid}")
                            Log.d(TAG, "Email: ${googleUser.email}")
                            Log.d(TAG, "Display Name: $googleDisplayName")
                            
                            // Update display name to ensure consistency
                            updateDisplayName()
                            
                            return Result.success(googleUser)
                        }
                    }
                } catch (switchException: Exception) {
                    Log.e(TAG, "Error switching to existing Google account", switchException)
                }
            }
            
            Result.failure(e)
        }
    }
    
    // Sign out
    fun signOut() {
        try {
            auth.signOut()
            googleSignInManager?.signOut()
            
            // Clear local images on sign out to prevent next user from seeing them
            context?.let { 
                com.doyouone.drawai.data.local.ImageStorage(it).clearAll() 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
        } finally {
            _currentUser.value = null
            _userDisplayName.value = ""
        }
    }
    
    // Get current user ID
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    // Check if user is anonymous
    fun isAnonymous(): Boolean {
        return auth.currentUser?.isAnonymous == true
    }
    
    // Generate random display name for anonymous users
    private fun generateRandomName(): String {
        val adjectives = listOf(
            "Happy", "Brave", "Swift", "Clever", "Mighty",
            "Silent", "Golden", "Silver", "Crystal", "Shadow",
            "Mystic", "Noble", "Wild", "Bright", "Dark"
        )
        val nouns = listOf(
            "Dragon", "Phoenix", "Tiger", "Wolf", "Eagle",
            "Warrior", "Mage", "Knight", "Ninja", "Samurai",
            "Artist", "Creator", "Dreamer", "Hunter", "Explorer"
        )
        val number = Random().nextInt(9999)
        return "${adjectives.random()}${nouns.random()}$number"
    }
    
    // Save user display name to Firestore
    private suspend fun saveUserDisplayName(userId: String, displayName: String, isAnonymous: Boolean) {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val userData = hashMapOf(
                "displayName" to displayName,
                "isAnonymous" to isAnonymous,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "generationCount" to 0,
                "dailyGenerationCount" to 0,
                "lastGenerationTime" to null,
                "lastGenerationDate" to today,
                "lastResetDate" to today,
                // Subscription fields
                "subscriptionPlan" to "FREE",
                "subscriptionActive" to true,
                "subscriptionExpiryDate" to null
            )
            firestore.collection("users")
                .document(userId)
                .set(userData)
                .await()
            _userDisplayName.value = displayName
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Update display name from Firestore
    private fun updateDisplayName() {
        val userId = getCurrentUserId() ?: return
        val currentUser = auth.currentUser
        
        Log.d(TAG, "🔄 Updating display name for user: $userId")
        
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firestoreDisplayName = document.getString("displayName")
                    if (!firestoreDisplayName.isNullOrEmpty()) {
                        _userDisplayName.value = firestoreDisplayName
                        Log.d(TAG, "✅ Using Firestore display name: $firestoreDisplayName")
                    } else {
                        // Fallback to Firebase Auth display name
                        val authDisplayName = currentUser?.displayName
                        if (!authDisplayName.isNullOrEmpty()) {
                            _userDisplayName.value = authDisplayName
                            Log.d(TAG, "✅ Using Firebase Auth display name: $authDisplayName")
                        } else {
                            _userDisplayName.value = "User"
                            Log.d(TAG, "⚠️ No display name found, using default: User")
                        }
                    }
                } else {
                    Log.d(TAG, "❌ No document found in Firestore for user $userId")
                    // Fallback to Firebase Auth display name
                    val authDisplayName = currentUser?.displayName
                    if (!authDisplayName.isNullOrEmpty()) {
                        _userDisplayName.value = authDisplayName
                        Log.d(TAG, "✅ Using Firebase Auth display name: $authDisplayName")
                    } else {
                        _userDisplayName.value = "User"
                        Log.d(TAG, "⚠️ No display name found, using default: User")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Failed to get display name from Firestore", exception)
                // Fallback to Firebase Auth display name
                val authDisplayName = currentUser?.displayName
                if (!authDisplayName.isNullOrEmpty()) {
                    _userDisplayName.value = authDisplayName
                    Log.d(TAG, "✅ Using Firebase Auth display name as fallback: $authDisplayName")
                } else {
                    _userDisplayName.value = "User"
                    Log.d(TAG, "⚠️ No display name found, using default: User")
                }
            }
    }
    
    // Get user display name
    suspend fun getUserDisplayName(): String {
        val userId = getCurrentUserId() ?: return "Guest"
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            document.getString("displayName") ?: "User"
        } catch (e: Exception) {
            "User"
        }
    }
    
    // Update user display name
    suspend fun updateUserDisplayName(newName: String): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        return try {
            // Update Firestore
            firestore.collection("users")
                .document(userId)
                .update("displayName", newName)
                .await()
                
            // Update local state
            _userDisplayName.value = newName
            
            // Update Firebase Auth profile (optional but good for consistency)
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            auth.currentUser?.updateProfile(profileUpdates)?.await()
            
            Log.d(TAG, "✅ Display name updated to: $newName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating display name", e)
            Result.failure(e)
        }
    }
}
