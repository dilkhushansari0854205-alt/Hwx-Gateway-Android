package com.example.services.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.core.network.ApiClient
import com.example.core.security.TokenStorage
import com.example.data.models.DeviceIncomingSmsRequest
import com.example.services.websocket.DeviceWebSocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SmsBroadcastReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val slot = intent.getIntExtra("slot", 0).coerceIn(0, 1)
        val subscriptionId = intent.getIntExtra("subscription", -1).takeIf { it != -1 }

        val sender = messages[0].displayOriginatingAddress ?: "Unknown"
        val messageBody = messages.joinToString(separator = "") { it.displayMessageBody ?: "" }

        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        Log.d("SmsBroadcastReceiver", "Incoming SMS from $sender on slot $slot: $messageBody")

        // Forward to backend
        val wsService = DeviceWebSocketService.getInstance(context)
        val reportedOverWs = wsService.reportIncomingSms(slot, subscriptionId, sender, messageBody, now)

        if (!reportedOverWs) {
            scope.launch {
                try {
                    val tokenStorage = TokenStorage.getInstance(context)
                    val deviceId = tokenStorage.getDeviceId() ?: return@launch
                    val deviceToken = tokenStorage.getDeviceToken() ?: return@launch

                    ApiClient.getService(context).reportIncomingSmsHttp(
                        deviceId = deviceId,
                        deviceToken = deviceToken,
                        request = DeviceIncomingSmsRequest(
                            simSlot = slot,
                            subscriptionId = subscriptionId,
                            from = sender,
                            message = messageBody,
                            timestamp = now
                        )
                    )
                } catch (e: Exception) {
                    Log.e("SmsBroadcastReceiver", "Failed to forward incoming SMS over HTTP", e)
                }
            }
        }
    }
}
