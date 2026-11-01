package com.example.services.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.data.models.SimRegistrationInfo

data class DetectedSim(
    val slotIndex: Int,
    val subscriptionId: Int,
    val carrierName: String,
    val displayName: String,
    val phoneNumber: String?,
    val countryIso: String?
)

class TelephonyManagerHelper(private val context: Context) {

    private val subscriptionManager: SubscriptionManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        } else {
            null
        }
    }

    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    fun hasPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun detectSimCards(): List<DetectedSim> {
        val detected = mutableListOf<DetectedSim>()

        // Never invent a subscription ID or guess the default SIM. The caller must
        // request READ_PHONE_STATE at the point SIM management/device registration is used.
        if (!hasPhoneStatePermission() || subscriptionManager == null) return emptyList()

        try {
            val subList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList
            if (!subList.isNullOrEmpty()) {
                subList.forEach { info ->
                    val slot = info.simSlotIndex.coerceAtLeast(0)
                    val carrier = info.carrierName?.toString()?.ifBlank { "SIM ${slot + 1}" } ?: "SIM ${slot + 1}"
                    val disp = info.displayName?.toString()?.ifBlank { carrier } ?: carrier
                    val number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            subscriptionManager?.getPhoneNumber(info.subscriptionId)
                        } catch (e: Exception) {
                            info.number
                        }
                    } else {
                        info.number
                    }

                    detected.add(
                        DetectedSim(
                            slotIndex = slot,
                            subscriptionId = info.subscriptionId,
                            carrierName = carrier,
                            displayName = disp,
                            phoneNumber = number?.takeIf { it.isNotBlank() },
                            countryIso = info.countryIso
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // SIM state is ambiguous/unavailable: return no SIMs and block routing.
            return emptyList()
        }

        return detected.sortedBy { it.slotIndex }
    }

    fun toSimRegistrationInfoList(): List<SimRegistrationInfo> {
        return detectSimCards().map {
            SimRegistrationInfo(
                subscriptionId = it.subscriptionId,
                slotIndex = it.slotIndex,
                carrierName = it.carrierName,
                phoneNumber = it.phoneNumber,
                displayName = it.displayName
            )
        }
    }
}
