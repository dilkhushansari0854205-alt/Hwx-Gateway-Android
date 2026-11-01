package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserStats(
    @Json(name = "smsSentCount") val smsSentCount: Int = 0,
    @Json(name = "smsReceivedCount") val smsReceivedCount: Int = 0,
    @Json(name = "apiRequestsCount") val apiRequestsCount: Int = 0,
    @Json(name = "activeDevicesCount") val activeDevicesCount: Int = 0,
    @Json(name = "lastActiveAt") val lastActiveAt: String? = null
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    @Json(name = "uid") val uid: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "photoURL") val photoURL: String? = null,
    @Json(name = "emailVerified") val emailVerified: Boolean = false,
    @Json(name = "status") val status: String = "active",
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "updatedAt") val updatedAt: String = "",
    @Json(name = "stats") val stats: UserStats = UserStats()
)
