package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SimCard(
    @Json(name = "id") val id: String = "",
    @Json(name = "subscriptionId") val subscriptionId: Int = 0,
    @Json(name = "slotIndex") val slotIndex: Int = 0,
    @Json(name = "carrierName") val carrierName: String = "",
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "isVerified") val isVerified: Boolean = false,
    @Json(name = "assignedBotId") val assignedBotId: String? = null,
    @Json(name = "isAuthorizedFallback") val isAuthorizedFallback: Boolean = false,
    @Json(name = "lastVerifiedAt") val lastVerifiedAt: String? = null,
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "updatedAt") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class Device(
    @Json(name = "id") val id: String = "",
    @Json(name = "uid") val uid: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "androidVersion") val androidVersion: String = "",
    @Json(name = "appVersion") val appVersion: String = "",
    @Json(name = "status") val status: String = "offline",
    @Json(name = "batteryLevel") val batteryLevel: Int? = null,
    @Json(name = "isCharging") val isCharging: Boolean? = null,
    @Json(name = "networkType") val networkType: String? = null,
    @Json(name = "ipAddress") val ipAddress: String? = null,
    @Json(name = "lastSeen") val lastSeen: String = "",
    @Json(name = "registeredAt") val registeredAt: String = "",
    @Json(name = "updatedAt") val updatedAt: String = "",
    @Json(name = "sims") val sims: List<SimCard> = emptyList(),
    @Json(name = "isSocketConnected") val isSocketConnected: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SimRegistrationInfo(
    @Json(name = "subscriptionId") val subscriptionId: Int,
    @Json(name = "slotIndex") val slotIndex: Int,
    @Json(name = "carrierName") val carrierName: String,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "displayName") val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterDeviceRequest(
    @Json(name = "name") val name: String,
    @Json(name = "androidVersion") val androidVersion: String,
    @Json(name = "appVersion") val appVersion: String,
    @Json(name = "sims") val sims: List<SimRegistrationInfo>? = null
)

@JsonClass(generateAdapter = true)
data class RegisterDeviceData(
    @Json(name = "device") val device: Device,
    @Json(name = "deviceToken") val deviceToken: String
)

@JsonClass(generateAdapter = true)
data class HeartbeatRequest(
    @Json(name = "batteryLevel") val batteryLevel: Int? = null,
    @Json(name = "isCharging") val isCharging: Boolean? = null,
    @Json(name = "networkType") val networkType: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateSimRequest(
    @Json(name = "isVerified") val isVerified: Boolean? = null,
    @Json(name = "isAuthorizedFallback") val isAuthorizedFallback: Boolean? = null,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null
)


@JsonClass(generateAdapter = true)
data class EnrollmentRequestCreated(
    @Json(name = "requestId") val requestId: String,
    @Json(name = "expiresAt") val expiresAt: String
)

@JsonClass(generateAdapter = true)
data class PendingDeviceEnrollment(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "androidVersion") val androidVersion: String = "",
    @Json(name = "appVersion") val appVersion: String = "",
    @Json(name = "requestedAt") val requestedAt: String = "",
    @Json(name = "expiresAt") val expiresAt: String = ""
)

@JsonClass(generateAdapter = true)
data class DeviceEnrollmentStatus(
    @Json(name = "status") val status: String,
    @Json(name = "device") val device: Device? = null,
    @Json(name = "deviceToken") val deviceToken: String? = null,
    @Json(name = "expiresAt") val expiresAt: String? = null
)


@JsonClass(generateAdapter = true)
data class DeviceEnrollmentStart(
    @Json(name = "requestId") val requestId: String? = null
)


@JsonClass(generateAdapter = true)
data class SyncDeviceSimsRequest(
    @Json(name = "sims") val sims: List<SimRegistrationInfo>
)
