package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SmsJob(
    @Json(name = "id") val id: String = "",
    @Json(name = "uid") val uid: String = "",
    @Json(name = "botId") val botId: String = "",
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "simId") val simId: String = "",
    @Json(name = "simSlot") val simSlot: Int = 0,
    @Json(name = "to") val to: String = "",
    @Json(name = "from") val from: String? = null,
    @Json(name = "message") val message: String = "",
    @Json(name = "status") val status: String = "queued",
    @Json(name = "idempotencyKey") val idempotencyKey: String? = null,
    @Json(name = "replyToMessageId") val replyToMessageId: String? = null,
    @Json(name = "errorMessage") val errorMessage: String? = null,
    @Json(name = "errorCode") val errorCode: String? = null,
    @Json(name = "dispatchedAt") val dispatchedAt: String? = null,
    @Json(name = "sentAt") val sentAt: String? = null,
    @Json(name = "deliveredAt") val deliveredAt: String? = null,
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "updatedAt") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class SmsMessage(
    @Json(name = "id") val id: String = "",
    @Json(name = "uid") val uid: String = "",
    @Json(name = "botId") val botId: String? = null,
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "simId") val simId: String? = null,
    @Json(name = "simSlot") val simSlot: Int? = null,
    @Json(name = "direction") val direction: String = "outbound",
    @Json(name = "replyToMessageId") val replyToMessageId: String? = null,
    @Json(name = "from") val from: String = "",
    @Json(name = "to") val to: String = "",
    @Json(name = "message") val message: String = "",
    @Json(name = "receivedAt") val receivedAt: String? = null,
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "webhookDelivered") val webhookDelivered: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class SendSmsRequest(
    @Json(name = "bot_id") val bot_id: String,
    @Json(name = "device_id") val device_id: String,
    @Json(name = "to") val to: String,
    @Json(name = "message") val message: String,
    @Json(name = "sim_id") val sim_id: String? = null,
    @Json(name = "reply_to_message_id") val reply_to_message_id: String? = null,
    @Json(name = "idempotency_key") val idempotency_key: String? = null
)

@JsonClass(generateAdapter = true)
data class SendSmsData(
    @Json(name = "jobId") val jobId: String = "",
    @Json(name = "botId") val botId: String = "",
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "simId") val simId: String = "",
    @Json(name = "simSlot") val simSlot: Int = 0,
    @Json(name = "to") val to: String = "",
    @Json(name = "status") val status: String = "",
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "dispatchedAt") val dispatchedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviceIncomingSmsRequest(
    @Json(name = "simSlot") val simSlot: Int,
    @Json(name = "subscriptionId") val subscriptionId: Int? = null,
    @Json(name = "from") val from: String,
    @Json(name = "message") val message: String,
    @Json(name = "timestamp") val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviceSmsResultRequest(
    @Json(name = "jobId") val jobId: String,
    @Json(name = "status") val status: String, // "sent", "delivered", "failed"
    @Json(name = "errorCode") val errorCode: String? = null,
    @Json(name = "errorMessage") val errorMessage: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null
)
