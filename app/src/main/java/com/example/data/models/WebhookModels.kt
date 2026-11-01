package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebhookConfig(
    @Json(name = "id") val id: String = "",
    @Json(name = "uid") val uid: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "secret") val secret: String = "",
    @Json(name = "events") val events: List<String> = emptyList(),
    @Json(name = "isActive") val isActive: Boolean = true,
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "updatedAt") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class CreateWebhookRequest(
    @Json(name = "url") val url: String,
    @Json(name = "events") val events: List<String>,
    @Json(name = "secret") val secret: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateWebhookRequest(
    @Json(name = "url") val url: String? = null,
    @Json(name = "events") val events: List<String>? = null,
    @Json(name = "secret") val secret: String? = null,
    @Json(name = "isActive") val isActive: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class TestWebhookRequest(
    @Json(name = "url") val url: String,
    @Json(name = "secret") val secret: String? = null
)

@JsonClass(generateAdapter = true)
data class TestWebhookData(
    @Json(name = "url") val url: String = "",
    @Json(name = "statusCode") val statusCode: Int? = null,
    @Json(name = "responsePreview") val responsePreview: String? = null
)
