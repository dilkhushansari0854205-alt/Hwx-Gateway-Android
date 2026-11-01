package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.core.network.ApiClient
import com.example.core.security.TokenStorage
import com.example.data.models.*
import com.example.services.telephony.TelephonyManagerHelper

class DeviceRepository(private val context: Context) {
    private val apiService by lazy { ApiClient.getService(context) }
    private val tokenStorage = TokenStorage.getInstance(context)
    private val telephonyHelper = TelephonyManagerHelper(context)

    suspend fun registerCurrentDevice(customName: String? = null): Result<RegisterDeviceData> {
        val deviceName = customName ?: "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val appVersion = "1.0.0"
        val detectedSims = telephonyHelper.toSimRegistrationInfoList()

        if (tokenStorage.isDemoMode()) {
            val demoData = RegisterDeviceData(
                device = Device(
                    id = "dev_demo_galaxy_s24",
                    uid = "demo_user_123",
                    name = deviceName,
                    androidVersion = androidVersion,
                    appVersion = appVersion,
                    status = "online",
                    batteryLevel = 87,
                    isCharging = true,
                    networkType = "WIFI",
                    lastSeen = "Just now",
                    registeredAt = "2026-11-01T00:00:00Z",
                    updatedAt = "2026-11-01T00:00:00Z",
                    sims = detectedSims.mapIndexed { index, sim ->
                        SimCard(
                            id = "sim_demo_$index",
                            subscriptionId = sim.subscriptionId,
                            slotIndex = sim.slotIndex,
                            carrierName = sim.carrierName,
                            displayName = sim.displayName,
                            phoneNumber = sim.phoneNumber ?: "+91 98765 43210",
                            isVerified = true,
                            createdAt = "2026-11-01T00:00:00Z",
                            updatedAt = "2026-11-01T00:00:00Z"
                        )
                    },
                    isSocketConnected = true
                ),
                deviceToken = "hwx_dev_token_demo_sample_secret"
            )
            tokenStorage.saveRegisteredDevice(demoData.device.id, demoData.deviceToken, demoData.device.name)
            return Result.success(demoData)
        }

        return try {
            val request = RegisterDeviceRequest(
                name = deviceName,
                androidVersion = androidVersion,
                appVersion = appVersion,
                sims = detectedSims
            )
            val response = apiService.registerDevice(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                tokenStorage.saveRegisteredDevice(data.device.id, data.deviceToken, data.device.name)
                Result.success(data)
            } else {
                val err = response.body()?.error?.message ?: response.message()
                Result.failure(Exception("Registration failed: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Called after Firebase login on this phone. The first phone bootstraps the
     * account; any additional phone must wait for approval from a trusted phone.
     * Returns null when ready, or an enrollment request ID while approval is pending.
     */
    suspend fun initializeAfterLogin(): Result<String?> {
        if (!tokenStorage.getDeviceId().isNullOrBlank() && !tokenStorage.getDeviceToken().isNullOrBlank()) {
            return Result.success(null)
        }
        val existing = listDevices()
        if (existing.isFailure) return Result.failure(existing.exceptionOrNull() ?: Exception("Could not check registered devices"))
        if (existing.getOrNull().isNullOrEmpty()) {
            val firstDevice = registerCurrentDevice()
            return if (firstDevice.isSuccess) Result.success(null)
            else Result.failure(firstDevice.exceptionOrNull() ?: Exception("Could not register first phone"))
        }
        val request = requestDeviceEnrollment()
        return if (request.isSuccess) Result.success(request.getOrNull()!!.requestId)
        else Result.failure(request.exceptionOrNull() ?: Exception("Could not request device approval"))
    }

    suspend fun requestDeviceEnrollment(customName: String? = null): Result<EnrollmentRequestCreated> {
        val deviceName = customName ?: "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val request = RegisterDeviceRequest(
            name = deviceName,
            androidVersion = androidVersion,
            appVersion = "1.0.0",
            sims = null // SIM details are shared only after the trusted phone approves this device.
        )
        return try {
            val response = apiService.createDeviceEnrollment(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.error?.message ?: "Could not request device approval (${response.code()})"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun listPendingDeviceEnrollments(): Result<List<PendingDeviceEnrollment>> = try {
        val response = apiService.listPendingDeviceEnrollments()
        if (response.isSuccessful && response.body()?.data != null) Result.success(response.body()!!.data!!)
        else Result.failure(Exception(response.body()?.error?.message ?: "Could not load approval requests"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun approveDeviceEnrollment(requestId: String): Result<Unit> = try {
        val response = apiService.approveDeviceEnrollment(requestId)
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(response.body()?.error?.message ?: "Could not approve device"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun rejectDeviceEnrollment(requestId: String): Result<Unit> = try {
        val response = apiService.rejectDeviceEnrollment(requestId)
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(response.body()?.error?.message ?: "Could not reject device"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getDeviceEnrollmentStatus(requestId: String): Result<DeviceEnrollmentStatus> = try {
        val response = apiService.getDeviceEnrollmentStatus(requestId)
        val data = response.body()?.data
        if (response.isSuccessful && data != null) {
            if (data.status == "approved" && !data.deviceToken.isNullOrBlank() && data.device != null) {
                tokenStorage.saveRegisteredDevice(data.device.id, data.deviceToken, data.device.name)
                // SIM metadata is synchronized only after approval; authorization flags stay false.
                runCatching {
                    apiService.syncDeviceSims(
                        data.device.id,
                        SyncDeviceSimsRequest(telephonyHelper.toSimRegistrationInfoList())
                    )
                }
            }
            Result.success(data)
        } else Result.failure(Exception(response.body()?.error?.message ?: "Could not check approval status"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun listDevices(): Result<List<Device>> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(getDemoDevices())
        }

        return try {
            val response = apiService.listDevices()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to load devices: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDevice(deviceId: String): Result<Device> {
        if (tokenStorage.isDemoMode()) {
            val dev = getDemoDevices().find { it.id == deviceId } ?: getDemoDevices().first()
            return Result.success(dev)
        }

        return try {
            val response = apiService.getDevice(deviceId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Device not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDevice(deviceId: String): Result<Unit> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(Unit)
        }

        return try {
            val response = apiService.deleteDevice(deviceId)
            if (response.isSuccessful) {
                if (tokenStorage.getDeviceId() == deviceId) {
                    tokenStorage.saveRegisteredDevice("", "", "")
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete device"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSimAuthorization(
        deviceId: String,
        simId: String,
        request: UpdateSimRequest
    ): Result<SimCard> {
        if (tokenStorage.isDemoMode()) {
            return Result.success(
                SimCard(
                    id = simId,
                    subscriptionId = 1,
                    slotIndex = 0,
                    carrierName = "Jio",
                    displayName = request.displayName ?: "SIM 1",
                    phoneNumber = request.phoneNumber ?: "+91 98765 43210",
                    isVerified = request.isVerified ?: true,
                    isAuthorizedFallback = request.isAuthorizedFallback ?: true,
                    createdAt = "2026-11-01T00:00:00Z",
                    updatedAt = "2026-11-01T00:00:00Z"
                )
            )
        }

        return try {
            val response = apiService.updateSimAuthorization(deviceId, simId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to update SIM"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDemoDevices(): List<Device> {
        return listOf(
            Device(
                id = "dev_galaxy_s24",
                uid = "demo_user_123",
                name = "Galaxy S24 Ultra",
                androidVersion = "Android 14 (API 34)",
                appVersion = "1.0.0",
                status = "online",
                batteryLevel = 87,
                isCharging = true,
                networkType = "5G / WiFi",
                lastSeen = "2 min ago",
                registeredAt = "2026-10-15T10:00:00Z",
                updatedAt = "2026-11-01T02:00:00Z",
                sims = listOf(
                    SimCard(
                        id = "sim_1",
                        subscriptionId = 1,
                        slotIndex = 0,
                        carrierName = "Jio",
                        displayName = "SIM 1",
                        phoneNumber = "+91 98765 43210",
                        isVerified = true,
                        isAuthorizedFallback = true,
                        createdAt = "2026-10-15T10:00:00Z",
                        updatedAt = "2026-10-15T10:00:00Z"
                    ),
                    SimCard(
                        id = "sim_2",
                        subscriptionId = 2,
                        slotIndex = 1,
                        carrierName = "Airtel",
                        displayName = "SIM 2",
                        phoneNumber = "+91 87654 32109",
                        isVerified = false,
                        isAuthorizedFallback = false,
                        createdAt = "2026-10-15T10:00:00Z",
                        updatedAt = "2026-10-15T10:00:00Z"
                    )
                ),
                isSocketConnected = true
            ),
            Device(
                id = "dev_redmi_note13",
                uid = "demo_user_123",
                name = "Redmi Note 13",
                androidVersion = "Android 13 (API 33)",
                appVersion = "1.0.0",
                status = "offline",
                batteryLevel = 42,
                isCharging = false,
                networkType = "4G LTE",
                lastSeen = "1 hour ago",
                registeredAt = "2026-09-20T12:00:00Z",
                updatedAt = "2026-11-01T01:00:00Z",
                sims = listOf(
                    SimCard(
                        id = "sim_3",
                        subscriptionId = 3,
                        slotIndex = 0,
                        carrierName = "Vodafone",
                        displayName = "SIM 1",
                        phoneNumber = "+91 76543 21098",
                        isVerified = true,
                        createdAt = "2026-09-20T12:00:00Z",
                        updatedAt = "2026-09-20T12:00:00Z"
                    )
                ),
                isSocketConnected = false
            ),
            Device(
                id = "dev_oneplus_12",
                uid = "demo_user_123",
                name = "OnePlus 12",
                androidVersion = "Android 14 (API 34)",
                appVersion = "1.0.0",
                status = "online",
                batteryLevel = 65,
                isCharging = false,
                networkType = "WiFi",
                lastSeen = "5 min ago",
                registeredAt = "2026-10-28T08:00:00Z",
                updatedAt = "2026-11-01T02:20:00Z",
                sims = listOf(
                    SimCard(
                        id = "sim_4",
                        subscriptionId = 4,
                        slotIndex = 0,
                        carrierName = "Jio",
                        displayName = "SIM 1",
                        phoneNumber = "+91 99887 76655",
                        isVerified = true,
                        createdAt = "2026-10-28T08:00:00Z",
                        updatedAt = "2026-10-28T08:00:00Z"
                    )
                ),
                isSocketConnected = true
            )
        )
    }
}
