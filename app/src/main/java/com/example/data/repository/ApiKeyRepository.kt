package com.example.data.repository

import android.content.Context
import com.example.core.network.ApiClient
import com.example.core.security.TokenStorage
import com.example.data.models.ApiKeyRecord
import com.example.data.models.CreateApiKeyData
import com.example.data.models.CreateApiKeyRequest

class ApiKeyRepository(private val context: Context) {
    private val apiService by lazy { ApiClient.getService(context) }
    private val tokenStorage = TokenStorage.getInstance(context)

    suspend fun createApiKey(name: String, expiresAt: String? = null): Result<CreateApiKeyData> {
        if (tokenStorage.isDemoMode()) {
            val randomSuffix = (1000..9999).random()
            return Result.success(
                CreateApiKeyData(
                    id = "key_demo_$randomSuffix",
                    name = name,
                    apiKey = "hwx_live_a1b2c3d4e5f6g7h8_${randomSuffix}demo_secret",
                    prefix = "hwx_live_a1b2...",
                    createdAt = "2026-11-01T00:00:00Z",
                    expiresAt = expiresAt
                )
            )
        }

        return try {
            val response = apiService.createApiKey(CreateApiKeyRequest(name, expiresAt))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val err = response.body()?.error?.message ?: response.message()
                Result.failure(Exception("Failed to generate API Key: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listApiKeys(): Result<List<ApiKeyRecord>> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(getDemoApiKeys())
        }

        return try {
            val response = apiService.listApiKeys()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to load API keys"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeApiKey(keyId: String): Result<Unit> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(Unit)
        }

        return try {
            val response = apiService.revokeApiKey(keyId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to revoke API key"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDemoApiKeys(): List<ApiKeyRecord> {
        return listOf(
            ApiKeyRecord(
                id = "key_1",
                uid = "demo_user_123",
                name = "Production Backend Server",
                keyPrefix = "hwx_live_8f3a...",
                createdAt = "2026-10-10T11:00:00Z",
                lastUsedAt = "5 min ago",
                usageCount = 3842,
                status = "active"
            ),
            ApiKeyRecord(
                id = "key_2",
                uid = "demo_user_123",
                name = "Staging Webhook Dispatcher",
                keyPrefix = "hwx_live_2c9d...",
                createdAt = "2026-10-22T16:30:00Z",
                lastUsedAt = "2 hours ago",
                usageCount = 612,
                status = "active"
            ),
            ApiKeyRecord(
                id = "key_3",
                uid = "demo_user_123",
                name = "Legacy Server Key",
                keyPrefix = "hwx_live_5e1b...",
                createdAt = "2026-09-01T08:00:00Z",
                lastUsedAt = "2 weeks ago",
                usageCount = 466,
                status = "revoked"
            )
        )
    }
}
