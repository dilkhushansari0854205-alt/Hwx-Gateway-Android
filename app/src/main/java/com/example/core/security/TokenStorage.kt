package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hwx_gateway_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(hasValidSession())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        _isLoggedIn.value = true
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUser(uid: String, email: String, displayName: String?) {
        prefs.edit()
            .putString(KEY_USER_UID, uid)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, displayName)
            .apply()
    }

    fun getUserUid(): String? = prefs.getString(KEY_USER_UID, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun saveRegisteredDevice(deviceId: String, deviceToken: String, deviceName: String) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_DEVICE_TOKEN, deviceToken)
            .putString(KEY_DEVICE_NAME, deviceName)
            .apply()
    }

    fun getDeviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)
    fun getDeviceToken(): String? = prefs.getString(KEY_DEVICE_TOKEN, null)
    fun getDeviceName(): String? = prefs.getString(KEY_DEVICE_NAME, null)

    // Demo mode is disabled in production builds. Never treat a local flag as authentication.
    fun setDemoMode(isDemo: Boolean) {
        prefs.edit().remove(KEY_DEMO_MODE).apply()
        _isDemoMode.value = false
    }

    fun isDemoMode(): Boolean = false

    fun hasValidSession(): Boolean = !getAuthToken().isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
        _isDemoMode.value = false
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_USER_UID = "key_user_uid"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_DEVICE_ID = "key_device_id"
        private const val KEY_DEVICE_TOKEN = "key_device_token"
        private const val KEY_DEVICE_NAME = "key_device_name"
        private const val KEY_DEMO_MODE = "key_demo_mode"

        @Volatile
        private var INSTANCE: TokenStorage? = null

        fun getInstance(context: Context): TokenStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenStorage(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
