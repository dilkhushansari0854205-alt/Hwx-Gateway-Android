package com.example.services.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.example.core.constants.AppConstants
import com.example.core.network.ApiClient
import com.example.core.security.TokenStorage
import com.example.data.models.DeviceSmsResultRequest
import com.example.services.websocket.DeviceWebSocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SmsStatusReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val jobId = intent.getStringExtra(AppConstants.EXTRA_JOB_ID) ?: return
        val action = intent.action ?: return
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        when (action) {
            AppConstants.ACTION_SMS_SENT -> {
                val resultCode = resultCode
                val status = if (resultCode == Activity.RESULT_OK) "sent" else "failed"
                val errorCode = when (resultCode) {
                    Activity.RESULT_OK -> null
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "GENERIC_FAILURE"
                    SmsManager.RESULT_ERROR_NO_SERVICE -> "NO_SERVICE"
                    SmsManager.RESULT_ERROR_NULL_PDU -> "NULL_PDU"
                    SmsManager.RESULT_ERROR_RADIO_OFF -> "RADIO_OFF"
                    else -> "CODE_$resultCode"
                }
                val errorMessage = if (errorCode != null) "SmsManager error: $errorCode" else null

                Log.d("SmsStatusReceiver", "SMS_SENT for $jobId: status=$status, code=$errorCode")
                reportResult(context, jobId, status, errorCode, errorMessage, now)
            }

            AppConstants.ACTION_SMS_DELIVERED -> {
                val resultCode = resultCode
                val status = if (resultCode == Activity.RESULT_OK) "delivered" else "failed"
                Log.d("SmsStatusReceiver", "SMS_DELIVERED for $jobId: status=$status")
                reportResult(context, jobId, status, null, null, now)
            }
        }
    }

    private fun reportResult(
        context: Context,
        jobId: String,
        status: String,
        errorCode: String?,
        errorMessage: String?,
        timestamp: String
    ) {
        val wsService = DeviceWebSocketService.getInstance(context)
        val reportedOverWs = wsService.reportSmsResult(jobId, status, errorCode, errorMessage, timestamp)

        if (!reportedOverWs) {
            // Fallback to HTTP endpoint
            scope.launch {
                try {
                    val tokenStorage = TokenStorage.getInstance(context)
                    val deviceId = tokenStorage.getDeviceId() ?: return@launch
                    val deviceToken = tokenStorage.getDeviceToken() ?: return@launch

                    ApiClient.getService(context).reportSmsResultHttp(
                        deviceId = deviceId,
                        deviceToken = deviceToken,
                        request = DeviceSmsResultRequest(
                            jobId = jobId,
                            status = status,
                            errorCode = errorCode,
                            errorMessage = errorMessage,
                            timestamp = timestamp
                        )
                    )
                } catch (e: Exception) {
                    Log.e("SmsStatusReceiver", "Failed to report SMS result over HTTP", e)
                }
            }
        }
    }
}
