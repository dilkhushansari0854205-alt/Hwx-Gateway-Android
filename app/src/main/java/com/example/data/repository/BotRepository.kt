package com.example.data.repository

import android.content.Context
import com.example.core.network.ApiClient
import com.example.core.security.TokenStorage
import com.example.data.models.Bot
import com.example.data.models.CreateBotRequest
import com.example.data.models.UpdateBotRequest

class BotRepository(private val context: Context) {
    private val apiService by lazy { ApiClient.getService(context) }
    private val tokenStorage = TokenStorage.getInstance(context)

    suspend fun createBot(request: CreateBotRequest): Result<Bot> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(
                Bot(
                    id = "bot_demo_${System.currentTimeMillis()}",
                    uid = "demo_user_123",
                    name = request.name,
                    description = request.description,
                    assignedDeviceId = request.assignedDeviceId,
                    assignedSimId = request.assignedSimId,
                    simSelectionMode = request.simSelectionMode,
                    fallbackSimIds = request.fallbackSimIds,
                    webhookUrl = request.webhookUrl,
                    status = "active",
                    createdAt = "2026-11-01T00:00:00Z",
                    updatedAt = "2026-11-01T00:00:00Z"
                )
            )
        }

        return try {
            val response = apiService.createBot(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val err = response.body()?.error?.message ?: response.message()
                Result.failure(Exception("Failed to create bot: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listBots(): Result<List<Bot>> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(getDemoBots())
        }

        return try {
            val response = apiService.listBots()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to load bots"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBot(botId: String, request: UpdateBotRequest): Result<Bot> {
        if (tokenStorage.isDemoMode()) {
            val bot = getDemoBots().find { it.id == botId } ?: getDemoBots().first()
            return Result.success(bot.copy(name = request.name ?: bot.name, status = request.status ?: bot.status))
        }

        return try {
            val response = apiService.updateBot(botId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to update bot"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBot(botId: String): Result<Unit> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(Unit)
        }

        return try {
            val response = apiService.deleteBot(botId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete bot"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDemoBots(): List<Bot> {
        return listOf(
            Bot(
                id = "bot_1",
                uid = "demo_user_123",
                name = "OTP Verification Bot",
                description = "Automated OTP authentication & delivery for web logins",
                assignedDeviceId = "dev_galaxy_s24",
                assignedSimId = "sim_1",
                simSelectionMode = "auto_switch",
                fallbackSimIds = listOf("sim_2"),
                webhookUrl = "https://example.com/webhooks/otp",
                status = "active",
                createdAt = "2026-10-18T10:00:00Z",
                updatedAt = "2026-10-18T10:00:00Z"
            ),
            Bot(
                id = "bot_2",
                uid = "demo_user_123",
                name = "Order Alerts Bot",
                description = "Customer transactional updates and order confirmations",
                assignedDeviceId = "dev_galaxy_s24",
                assignedSimId = "sim_1",
                simSelectionMode = "bot_fixed",
                webhookUrl = "https://example.com/webhooks/orders",
                status = "active",
                createdAt = "2026-10-20T14:30:00Z",
                updatedAt = "2026-10-20T14:30:00Z"
            ),
            Bot(
                id = "bot_3",
                uid = "demo_user_123",
                name = "Customer Support SMS",
                description = "Two-way client communication via SMS support desk",
                assignedDeviceId = "dev_oneplus_12",
                assignedSimId = "sim_4",
                simSelectionMode = "manual",
                webhookUrl = "https://example.com/webhooks/support",
                status = "inactive",
                createdAt = "2026-10-25T09:15:00Z",
                updatedAt = "2026-10-25T09:15:00Z"
            )
        )
    }
}
