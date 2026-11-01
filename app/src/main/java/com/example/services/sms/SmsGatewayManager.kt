package com.example.services.sms

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.core.constants.AppConstants

object SmsGatewayManager {
    private const val TAG = "SmsGatewayManager"

    fun sendSms(
        context: Context,
        jobId: String,
        to: String,
        message: String,
        simSlot: Int,
        subscriptionId: Int? = null
    ): Boolean {
        try {
            // Require an explicitly selected, currently identified subscription.
            // Never fall back to the device's default SIM when identity is missing.
            if (subscriptionId == null || subscriptionId <= 0) {
                Log.w(TAG, "SMS blocked: no verified subscription selected for job $jobId")
                return false
            }
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "SMS blocked: SEND_SMS permission not granted")
                return false
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return false
            val smsManager: SmsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)

            val sentIntent = Intent(AppConstants.ACTION_SMS_SENT).apply {
                setPackage(context.packageName)
                putExtra(AppConstants.EXTRA_JOB_ID, jobId)
                putExtra(AppConstants.EXTRA_SIM_SLOT, simSlot)
            }

            val deliveryIntent = Intent(AppConstants.ACTION_SMS_DELIVERED).apply {
                setPackage(context.packageName)
                putExtra(AppConstants.EXTRA_JOB_ID, jobId)
                putExtra(AppConstants.EXTRA_SIM_SLOT, simSlot)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val sentPendingIntent = PendingIntent.getBroadcast(
                context,
                jobId.hashCode(),
                sentIntent,
                flags
            )

            val deliveryPendingIntent = PendingIntent.getBroadcast(
                context,
                jobId.hashCode() + 1,
                deliveryIntent,
                flags
            )

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>()
                val deliveryIntents = ArrayList<PendingIntent>()
                for (i in parts.indices) {
                    if (i == parts.size - 1) {
                        sentIntents.add(sentPendingIntent)
                        deliveryIntents.add(deliveryPendingIntent)
                    } else {
                        sentIntents.add(sentPendingIntent)
                        deliveryIntents.add(deliveryPendingIntent)
                    }
                }
                smsManager.sendMultipartTextMessage(to, null, parts, sentIntents, deliveryIntents)
            } else {
                smsManager.sendTextMessage(to, null, message, sentPendingIntent, deliveryPendingIntent)
            }

            Log.d(TAG, "Dispatched SMS job $jobId via configured SIM slot $simSlot")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS for job $jobId", e)
            return false
        }
    }
}
