package com.example.data.repository

import android.content.Context
import com.example.core.network.ApiClient
import com.example.core.security.TokenStorage
import com.example.data.models.*

class WebhookRepository(private val context: Context) {
    private val apiService by lazy { ApiClient.getService(context) }
    private val tokenStorage = TokenStorage.getInstance(context)

    suspend fun createWebhook(request: CreateWebhookRequest): Result<WebhookConfig> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(
                WebhookConfig(
                    id = "wh_demo_${System.currentTimeMillis()}",
                    uid = "demo_user_123",
                    url = request.url,
                    secret = request.secret ?: "hwx_whsec_sample_secret_key",
                    events = request.events,
                    isActive = true,
                    createdAt = "2026-11-01T00:00:00Z",
                    updatedAt = "2026-11-01T00:00:00Z"
                )
            )
        }

        return try {
            val response = apiService.createWebhook(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val err = response.body()?.error?.message ?: response.message()
                Result.failure(Exception("Failed to register webhook: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listWebhooks(): Result<List<WebhookConfig>> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(getDemoWebhooks())
        }

        return try {
            val response = apiService.listWebhooks()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to load webhooks"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWebhook(webhookId: String, request: UpdateWebhookRequest): Result<WebhookConfig> {
        if (tokenStorage.isDemoMode()) {
            val wh = getDemoWebhooks().find { it.id == webhookId } ?: getDemoWebhooks().first()
            return Result.success(wh.copy(isActive = request.isActive ?: wh.isActive))
        }

        return try {
            val response = apiService.updateWebhook(webhookId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to update webhook"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWebhook(webhookId: String): Result<Unit> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(Unit)
        }

        return try {
            val response = apiService.deleteWebhook(webhookId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete webhook"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testWebhook(url: String, secret: String? = null): Result<TestWebhookData> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(
                TestWebhookData(
                    url = url,
                    statusCode = 200,
                    responsePreview = "{\"status\": \"received\", \"event\": \"test.ping\"}"
                )
            )
        }

        return try {
            val response = apiService.testWebhook(TestWebhookRequest(url, secret))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val err = response.body()?.error?.message ?: response.message()
                Result.failure(Exception("Test ping failed: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDemoWebhooks(): List<WebhookConfig> {
        return listOf(
            WebhookConfig(
                id = "wh_1",
                uid = "demo_user_123",
                url = "https://api.mycrm.com/sms/callback",
                secret = "hwx_whsec_884920194821",
                events = listOf("sms.received", "sms.delivered", "sms.failed"),
                isActive = true,
                createdAt = "2026-10-15T12:00:00Z",
                updatedAt = "2026-10-15T12:00:00Z"
            ),
            WebhookConfig(
                id = "wh_2",
                uid = "demo_user_123",
                url = "https://ops.example.com/alerts/device-status",
                secret = "hwx_whsec_991827364510",
                events = listOf("device.online", "device.offline"),
                isActive = true,
                createdAt = "2026-10-24T09:30:00Z",
                updatedAt = "2026-10-24T09:30:00Z"
            )
        )
    }
}
