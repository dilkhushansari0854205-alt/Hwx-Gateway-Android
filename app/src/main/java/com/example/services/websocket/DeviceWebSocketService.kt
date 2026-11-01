package com.example.services.websocket

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import com.example.core.constants.AppConstants
import com.example.core.network.ApiClient
import com.example.core.security.TokenStorage
import com.example.data.models.*
import com.example.services.sms.SmsGatewayManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class DeviceWebSocketService private constructor(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val tokenStorage = TokenStorage.getInstance(context)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private var webSocket: WebSocket? = null
    private val isConnecting = AtomicBoolean(false)
    private var heartbeatJob: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    fun connect() {
        if (_isConnected.value || isConnecting.get()) return
        val deviceId = tokenStorage.getDeviceId()
        val deviceToken = tokenStorage.getDeviceToken()

        if (deviceId.isNullOrBlank() || deviceToken.isNullOrBlank()) {
            Log.d(TAG, "No registered device credentials. Skipping WebSocket connection.")
            return
        }

        isConnecting.set(true)
        val client = ApiClient.getOkHttpClient(context)
        val request = Request.Builder()
            .url(AppConstants.WS_DEVICE_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened. Sending secure AUTH handshake...")
                isConnecting.set(false)

                // Handshake AUTH within 5-second backend window
                val authPayload = JSONObject().apply {
                    put("type", "AUTH")
                    put("deviceId", deviceId)
                    put("deviceToken", deviceToken)
                }.toString()
                ws.send(authPayload)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                _lastMessage.value = text
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "AUTH_SUCCESS" -> {
                            _isConnected.value = true
                            startHeartbeat()
                        }
                        "AUTH_FAILED" -> {
                            _isConnected.value = false
                            Log.e(TAG, "Device auth failed: ${json.optString("message")}")
                        }
                        "COMMAND_SEND_SMS" -> {
                            val cmdId = json.optString("commandId")
                            val jobId = json.optString("jobId")
                            val to = json.optString("to")
                            val message = json.optString("message")
                            val simSlot = json.optInt("simSlot", 0)
                            val subscriptionId = if (json.has("subscriptionId")) json.getInt("subscriptionId") else null

                            // Acknowledge receipt
                            val ack = JSONObject().apply {
                                put("type", "ACK")
                                put("ackId", cmdId.ifBlank { jobId })
                                put("status", "ok")
                            }.toString()
                            ws.send(ack)

                            // Dispatch SMS via SmsManager
                            SmsGatewayManager.sendSms(
                                context = context,
                                jobId = jobId,
                                to = to,
                                message = message,
                                simSlot = simSlot,
                                subscriptionId = subscriptionId
                            )
                        }
                        "PONG" -> {
                            // Heartbeat confirmed
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling WS message", e)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                _isConnected.value = false
                stopHeartbeat()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                _isConnected.value = false
                stopHeartbeat()
                scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message}")
                _isConnected.value = false
                isConnecting.set(false)
                stopHeartbeat()
                scheduleReconnect()
            }
        })
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive && _isConnected.value) {
                delay(25000)
                val deviceId = tokenStorage.getDeviceId() ?: break
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85

                val hb = JSONObject().apply {
                    put("type", "HEARTBEAT")
                    put("deviceId", deviceId)
                    put("batteryLevel", batteryLevel)
                    put("isCharging", false)
                    put("networkType", "WIFI")
                }.toString()

                webSocket?.send(hb)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(5000)
            if (!_isConnected.value && !tokenStorage.getDeviceId().isNullOrBlank()) {
                connect()
            }
        }
    }

    fun reportSmsResult(
        jobId: String,
        status: String,
        errorCode: String?,
        errorMessage: String?,
        timestamp: String
    ): Boolean {
        val ws = webSocket
        val deviceId = tokenStorage.getDeviceId()
        if (ws == null || !_isConnected.value || deviceId.isNullOrBlank()) {
            return false
        }

        return try {
            val payload = JSONObject().apply {
                put("type", "SMS_RESULT")
                put("jobId", jobId)
                put("deviceId", deviceId)
                put("status", status)
                if (errorCode != null) put("errorCode", errorCode)
                if (errorMessage != null) put("errorMessage", errorMessage)
                put("timestamp", timestamp)
            }.toString()
            ws.send(payload)
        } catch (e: Exception) {
            false
        }
    }

    fun reportIncomingSms(
        simSlot: Int,
        subscriptionId: Int?,
        from: String,
        message: String,
        timestamp: String
    ): Boolean {
        val ws = webSocket
        val deviceId = tokenStorage.getDeviceId()
        if (ws == null || !_isConnected.value || deviceId.isNullOrBlank()) {
            return false
        }

        return try {
            val payload = JSONObject().apply {
                put("type", "SMS_INCOMING")
                put("deviceId", deviceId)
                put("simSlot", simSlot)
                if (subscriptionId != null) put("subscriptionId", subscriptionId)
                put("from", from)
                put("message", message)
                put("timestamp", timestamp)
            }.toString()
            ws.send(payload)
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        stopHeartbeat()
        webSocket?.close(1000, "App closed")
        webSocket = null
        _isConnected.value = false
    }

    companion object {
        private const val TAG = "DeviceWebSocket"

        @Volatile
        private var INSTANCE: DeviceWebSocketService? = null

        fun getInstance(context: Context): DeviceWebSocketService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceWebSocketService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
