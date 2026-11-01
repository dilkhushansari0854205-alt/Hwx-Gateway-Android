package com.example.data.repository

import android.content.Context
import com.example.core.network.ApiClient
import com.example.core.security.TokenStorage
import com.example.data.models.SendSmsData
import com.example.data.models.SendSmsRequest
import com.example.data.models.SmsJob

class SmsRepository(private val context: Context) {
    private val apiService by lazy { ApiClient.getService(context) }
    private val tokenStorage = TokenStorage.getInstance(context)

    suspend fun sendSms(request: SendSmsRequest): Result<SendSmsData> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(
                SendSmsData(
                    jobId = "job_demo_${System.currentTimeMillis()}",
                    botId = request.bot_id,
                    deviceId = request.device_id,
                    simId = request.sim_id ?: "sim_1",
                    simSlot = 0,
                    to = request.to,
                    status = "queued",
                    createdAt = "2026-11-01T00:00:00Z",
                    dispatchedAt = null
                )
            )
        }

        return try {
            val response = apiService.sendSms(request, request.idempotency_key)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val err = response.body()?.error?.message ?: response.message()
                Result.failure(Exception("Failed to send SMS: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listSmsLogs(
        botId: String? = null,
        deviceId: String? = null,
        direction: String? = null,
        limit: Int = 50
    ): Result<List<SmsJob>> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(getDemoSmsLogs())
        }

        return try {
            val response = apiService.listSmsLogs(botId, deviceId, direction, limit)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to load SMS logs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDemoSmsLogs(): List<SmsJob> {
        return listOf(
            SmsJob(
                id = "sms_job_101",
                uid = "demo_user_123",
                botId = "bot_1",
                deviceId = "dev_galaxy_s24",
                simId = "sim_1",
                simSlot = 0,
                to = "+91 98765 00112",
                from = "+91 98765 43210",
                message = "Your HWX verification code is 849201. Valid for 10 minutes.",
                status = "delivered",
                createdAt = "2026-11-01T02:15:00Z",
                dispatchedAt = "2026-11-01T02:15:01Z",
                sentAt = "2026-11-01T02:15:02Z",
                deliveredAt = "2026-11-01T02:15:05Z"
            ),
            SmsJob(
                id = "sms_job_102",
                uid = "demo_user_123",
                botId = "bot_2",
                deviceId = "dev_galaxy_s24",
                simId = "sim_1",
                simSlot = 0,
                to = "+91 94567 11223",
                from = "+91 98765 43210",
                message = "Order #HWX-8921 has been packed and scheduled for delivery.",
                status = "sent",
                createdAt = "2026-11-01T01:40:00Z",
                dispatchedAt = "2026-11-01T01:40:01Z",
                sentAt = "2026-11-01T01:40:03Z"
            ),
            SmsJob(
                id = "sms_job_103",
                uid = "demo_user_123",
                botId = "bot_1",
                deviceId = "dev_redmi_note13",
                simId = "sim_3",
                simSlot = 0,
                to = "+91 91234 56789",
                from = "+91 76543 21098",
                message = "Your password reset PIN is 302194.",
                status = "failed",
                errorMessage = "Network unreachable / device offline",
                errorCode = "NO_SERVICE",
                createdAt = "2026-11-01T00:50:00Z",
                dispatchedAt = "2026-11-01T00:50:01Z"
            ),
            SmsJob(
                id = "sms_job_104",
                uid = "demo_user_123",
                botId = "bot_1",
                deviceId = "dev_galaxy_s24",
                simId = "sim_1",
                simSlot = 0,
                to = "+91 98111 22334",
                from = "+91 98765 43210",
                message = "Security Alert: New sign in detected from Chrome on Windows.",
                status = "delivered",
                createdAt = "2026-10-31T23:10:00Z",
                dispatchedAt = "2026-10-31T23:10:02Z",
                sentAt = "2026-10-31T23:10:03Z",
                deliveredAt = "2026-10-31T23:10:06Z"
            )
        )
    }
}
