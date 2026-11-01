package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: T? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "warning") val warning: String? = null,
    @Json(name = "error") val error: ApiErrorDetail? = null
)

@JsonClass(generateAdapter = true)
data class ApiErrorDetail(
    @Json(name = "code") val code: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthCheckResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "service") val service: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "firestore") val firestore: String? = null,
    @Json(name = "uptimeSeconds") val uptimeSeconds: Long? = null
)
