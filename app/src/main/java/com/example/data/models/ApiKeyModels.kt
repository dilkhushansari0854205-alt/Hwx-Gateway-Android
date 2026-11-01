package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiKeyRecord(
    @Json(name = "id") val id: String = "",
    @Json(name = "uid") val uid: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "keyPrefix") val keyPrefix: String = "",
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "expiresAt") val expiresAt: String? = null,
    @Json(name = "lastUsedAt") val lastUsedAt: String? = null,
    @Json(name = "usageCount") val usageCount: Int = 0,
    @Json(name = "status") val status: String = "active"
)

@JsonClass(generateAdapter = true)
data class CreateApiKeyRequest(
    @Json(name = "name") val name: String,
    @Json(name = "expiresAt") val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateApiKeyData(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "apiKey") val apiKey: String = "",
    @Json(name = "prefix") val prefix: String = "",
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "expiresAt") val expiresAt: String? = null
)
