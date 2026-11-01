package com.example.core.network

import com.example.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("health")
    suspend fun getHealth(): Response<HealthCheckResponse>

    // Auth
    @GET("v1/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<UserProfile>>

    // Devices
    @POST("v1/devices/register")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): Response<ApiResponse<RegisterDeviceData>>

    @POST("v1/devices/enrollment-requests")
    suspend fun createDeviceEnrollment(
        @Body request: RegisterDeviceRequest
    ): Response<ApiResponse<EnrollmentRequestCreated>>

    @GET("v1/devices/enrollment-requests/pending")
    suspend fun listPendingDeviceEnrollments(): Response<ApiResponse<List<PendingDeviceEnrollment>>>

    @GET("v1/devices/enrollment-requests/{requestId}")
    suspend fun getDeviceEnrollmentStatus(
        @Path("requestId") requestId: String
    ): Response<ApiResponse<DeviceEnrollmentStatus>>

    @POST("v1/devices/enrollment-requests/{requestId}/approve")
    suspend fun approveDeviceEnrollment(
        @Path("requestId") requestId: String
    ): Response<ApiResponse<Any>>

    @POST("v1/devices/enrollment-requests/{requestId}/reject")
    suspend fun rejectDeviceEnrollment(
        @Path("requestId") requestId: String
    ): Response<ApiResponse<Any>>

    @GET("v1/devices")
    suspend fun listDevices(): Response<ApiResponse<List<Device>>>

    @GET("v1/devices/{deviceId}")
    suspend fun getDevice(
        @Path("deviceId") deviceId: String
    ): Response<ApiResponse<Device>>

    @DELETE("v1/devices/{deviceId}")
    suspend fun deleteDevice(
        @Path("deviceId") deviceId: String
    ): Response<ApiResponse<Unit>>

    @POST("v1/devices/{deviceId}/heartbeat")
    suspend fun sendDeviceHeartbeat(
        @Path("deviceId") deviceId: String,
        @Header("x-device-token") deviceToken: String,
        @Body request: HeartbeatRequest
    ): Response<ApiResponse<Any>>

    @POST("v1/devices/{deviceId}/sims/sync")
    suspend fun syncDeviceSims(
        @Path("deviceId") deviceId: String,
        @Body request: SyncDeviceSimsRequest
    ): Response<ApiResponse<List<SimCard>>>

    @GET("v1/devices/{deviceId}/sims")
    suspend fun listDeviceSims(
        @Path("deviceId") deviceId: String
    ): Response<ApiResponse<List<SimCard>>>

    @PATCH("v1/devices/{deviceId}/sims/{simId}")
    suspend fun updateSimAuthorization(
        @Path("deviceId") deviceId: String,
        @Path("simId") simId: String,
        @Body request: UpdateSimRequest
    ): Response<ApiResponse<SimCard>>

    // Bots
    @POST("v1/bots")
    suspend fun createBot(
        @Body request: CreateBotRequest
    ): Response<ApiResponse<Bot>>

    @GET("v1/bots")
    suspend fun listBots(): Response<ApiResponse<List<Bot>>>

    @GET("v1/bots/{botId}")
    suspend fun getBot(
        @Path("botId") botId: String
    ): Response<ApiResponse<Bot>>

    @PATCH("v1/bots/{botId}")
    suspend fun updateBot(
        @Path("botId") botId: String,
        @Body request: UpdateBotRequest
    ): Response<ApiResponse<Bot>>

    @DELETE("v1/bots/{botId}")
    suspend fun deleteBot(
        @Path("botId") botId: String
    ): Response<ApiResponse<Unit>>

    // SMS
    @POST("v1/sms/send")
    suspend fun sendSms(
        @Body request: SendSmsRequest,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): Response<ApiResponse<SendSmsData>>

    @GET("v1/sms/logs")
    suspend fun listSmsLogs(
        @Query("bot_id") botId: String? = null,
        @Query("device_id") deviceId: String? = null,
        @Query("direction") direction: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<List<SmsJob>>>

    @GET("v1/sms/{messageId}")
    suspend fun getSmsById(
        @Path("messageId") messageId: String
    ): Response<ApiResponse<SmsJob>>

    // Device SMS HTTP Fallback
    @POST("v1/device/sms/received")
    suspend fun reportIncomingSmsHttp(
        @Header("x-device-id") deviceId: String,
        @Header("x-device-token") deviceToken: String,
        @Body request: DeviceIncomingSmsRequest
    ): Response<ApiResponse<Unit>>

    @POST("v1/device/sms/result")
    suspend fun reportSmsResultHttp(
        @Header("x-device-id") deviceId: String,
        @Header("x-device-token") deviceToken: String,
        @Body request: DeviceSmsResultRequest
    ): Response<ApiResponse<Unit>>

    // API Keys
    @POST("v1/api-keys")
    suspend fun createApiKey(
        @Body request: CreateApiKeyRequest
    ): Response<ApiResponse<CreateApiKeyData>>

    @GET("v1/api-keys")
    suspend fun listApiKeys(): Response<ApiResponse<List<ApiKeyRecord>>>

    @DELETE("v1/api-keys/{keyId}")
    suspend fun revokeApiKey(
        @Path("keyId") keyId: String
    ): Response<ApiResponse<Unit>>

    // Webhooks
    @POST("v1/webhooks")
    suspend fun createWebhook(
        @Body request: CreateWebhookRequest
    ): Response<ApiResponse<WebhookConfig>>

    @GET("v1/webhooks")
    suspend fun listWebhooks(): Response<ApiResponse<List<WebhookConfig>>>

    @PATCH("v1/webhooks/{webhookId}")
    suspend fun updateWebhook(
        @Path("webhookId") webhookId: String,
        @Body request: UpdateWebhookRequest
    ): Response<ApiResponse<WebhookConfig>>

    @DELETE("v1/webhooks/{webhookId}")
    suspend fun deleteWebhook(
        @Path("webhookId") webhookId: String
    ): Response<ApiResponse<Unit>>

    @POST("v1/webhooks/test")
    suspend fun testWebhook(
        @Body request: TestWebhookRequest
    ): Response<ApiResponse<TestWebhookData>>
}
