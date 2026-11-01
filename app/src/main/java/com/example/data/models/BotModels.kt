package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Bot(
    @Json(name = "id") val id: String = "",
    @Json(name = "uid") val uid: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "assignedDeviceId") val assignedDeviceId: String = "",
    @Json(name = "assignedSimId") val assignedSimId: String = "",
    @Json(name = "simSelectionMode") val simSelectionMode: String = "bot_fixed",
    @Json(name = "fallbackSimIds") val fallbackSimIds: List<String>? = null,
    @Json(name = "webhookUrl") val webhookUrl: String? = null,
    @Json(name = "status") val status: String = "active",
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "updatedAt") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class CreateBotRequest(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "assignedDeviceId") val assignedDeviceId: String,
    @Json(name = "assignedSimId") val assignedSimId: String,
    @Json(name = "simSelectionMode") val simSelectionMode: String = "bot_fixed",
    @Json(name = "fallbackSimIds") val fallbackSimIds: List<String>? = null,
    @Json(name = "webhookUrl") val webhookUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateBotRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "assignedDeviceId") val assignedDeviceId: String? = null,
    @Json(name = "assignedSimId") val assignedSimId: String? = null,
    @Json(name = "simSelectionMode") val simSelectionMode: String? = null,
    @Json(name = "fallbackSimIds") val fallbackSimIds: List<String>? = null,
    @Json(name = "webhookUrl") val webhookUrl: String? = null,
    @Json(name = "status") val status: String? = null
)
