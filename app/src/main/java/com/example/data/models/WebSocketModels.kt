package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WsAuthMessage(
    @Json(name = "type") val type: String = "AUTH",
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "deviceToken") val deviceToken: String
)

@JsonClass(generateAdapter = true)
data class WsHeartbeatMessage(
    @Json(name = "type") val type: String = "HEARTBEAT",
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "batteryLevel") val batteryLevel: Int? = null,
    @Json(name = "isCharging") val isCharging: Boolean? = null,
    @Json(name = "networkType") val networkType: String? = null
)

@JsonClass(generateAdapter = true)
data class WsSmsResultMessage(
    @Json(name = "type") val type: String = "SMS_RESULT",
    @Json(name = "jobId") val jobId: String,
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "status") val status: String, // "sent", "delivered", "failed"
    @Json(name = "errorCode") val errorCode: String? = null,
    @Json(name = "errorMessage") val errorMessage: String? = null,
    @Json(name = "timestamp") val timestamp: String
)

@JsonClass(generateAdapter = true)
data class WsIncomingSmsMessage(
    @Json(name = "type") val type: String = "SMS_INCOMING",
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "simSlot") val simSlot: Int,
    @Json(name = "subscriptionId") val subscriptionId: Int? = null,
    @Json(name = "from") val from: String,
    @Json(name = "message") val message: String,
    @Json(name = "timestamp") val timestamp: String
)

@JsonClass(generateAdapter = true)
data class WsServerCommandSendSms(
    @Json(name = "type") val type: String = "COMMAND_SEND_SMS",
    @Json(name = "commandId") val commandId: String = "",
    @Json(name = "jobId") val jobId: String = "",
    @Json(name = "to") val to: String = "",
    @Json(name = "message") val message: String = "",
    @Json(name = "simSlot") val simSlot: Int = 0,
    @Json(name = "subscriptionId") val subscriptionId: Int? = null
)

@JsonClass(generateAdapter = true)
data class WsServerGenericResponse(
    @Json(name = "type") val type: String = "",
    @Json(name = "deviceId") val deviceId: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "ackId") val ackId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null
)
