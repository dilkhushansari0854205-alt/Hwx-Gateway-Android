package com.example.data.repository

import android.content.Context
import com.example.core.network.ApiClient
import com.example.core.security.TokenStorage
import com.example.data.models.UserProfile
import com.example.data.models.UserStats
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val tokenStorage = TokenStorage.getInstance(context)
    private val apiService by lazy { ApiClient.getService(context) }

    suspend fun getFirebaseIdToken(): String? {
        val user = auth.currentUser ?: return null
        val tokenResult = user.getIdToken(true).await()
        val token = tokenResult.token
        if (!token.isNullOrBlank()) {
            tokenStorage.saveAuthToken(token)
            tokenStorage.saveUser(user.uid, user.email ?: "", user.displayName)
        }
        return token
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<UserProfile> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: throw Exception("User not found after sign in")
            // Never carry a device credential from a previously signed-in account.
            if (!tokenStorage.getUserUid().isNullOrBlank() && tokenStorage.getUserUid() != user.uid) {
                tokenStorage.saveRegisteredDevice("", "", "")
            }
            val token = user.getIdToken(true).await().token ?: throw Exception("Failed to retrieve ID token")

            tokenStorage.saveAuthToken(token)
            tokenStorage.saveUser(user.uid, user.email ?: "", user.displayName)

            // Sync with backend profile
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                // Fallback to minimal profile if backend profile is creating
                val profile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: email,
                    displayName = user.displayName ?: email.substringBefore("@"),
                    emailVerified = user.isEmailVerified,
                    createdAt = "",
                    updatedAt = "",
                    stats = UserStats()
                )
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(name: String, email: String, pass: String): Result<UserProfile> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: throw Exception("User creation failed")

            // Send verification email
            user.sendEmailVerification().await()

            val token = user.getIdToken(true).await().token ?: throw Exception("Failed to retrieve ID token")
            tokenStorage.saveAuthToken(token)
            tokenStorage.saveUser(user.uid, email, name)

            // Trigger backend profile creation by hitting /v1/auth/me.
            // If this fails, surface the failure instead of silently treating the
            // account as fully provisioned; retry after network/backend recovery.
            val profileResponse = apiService.getCurrentUser()
            if (!profileResponse.isSuccessful) {
                throw Exception("Account created, but backend setup failed (${profileResponse.code()}). Please sign in again after the server is available.")
            }

            val profile = UserProfile(
                uid = user.uid,
                email = email,
                displayName = name,
                emailVerified = false,
                createdAt = "",
                updatedAt = "",
                stats = UserStats()
            )
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("Not signed in")
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadUser(): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            user.reload().await()
            user.isEmailVerified
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchUserProfile(): Result<UserProfile> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(
                UserProfile(
                    uid = "demo_user_123",
                    email = "demo@hwxgateway.io",
                    displayName = "Firoz Ansari",
                    emailVerified = true,
                    status = "active",
                    createdAt = "2026-01-01T00:00:00Z",
                    updatedAt = "2026-11-01T00:00:00Z",
                    stats = UserStats(
                        smsSentCount = 1250,
                        smsReceivedCount = 380,
                        apiRequestsCount = 4920,
                        activeDevicesCount = 3,
                        lastActiveAt = "Just now"
                    )
                )
            )
        }

        return try {
            // Refresh token first
            getFirebaseIdToken()
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val user = auth.currentUser
                if (user != null) {
                    Result.success(
                        UserProfile(
                            uid = user.uid,
                            email = user.email ?: "",
                            displayName = user.displayName ?: user.email?.substringBefore("@"),
                            emailVerified = user.isEmailVerified,
                            createdAt = "",
                            updatedAt = "",
                            stats = UserStats()
                        )
                    )
                } else {
                    Result.failure(Exception("Failed to load user profile"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            // Ignore
        }
        tokenStorage.clear()
    }
}
