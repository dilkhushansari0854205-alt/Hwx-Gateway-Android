package com.example.ui.screens.deviceapproval

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DeviceRepository
import com.example.ui.components.HWXButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DeviceApprovalScreen(
    requestId: String,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DeviceRepository(context) }
    var status by remember { mutableStateOf("pending") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(true) }
    var refreshTick by remember { mutableStateOf(0) }

    suspend fun checkStatus() {
        val result = repository.getDeviceEnrollmentStatus(requestId)
        if (result.isSuccess) {
            val value = result.getOrNull()!!
            status = value.status
            error = null
            if (value.status == "approved" && !value.deviceToken.isNullOrBlank()) {
                onNavigateToDashboard()
            }
            if (value.status == "consumed") {
                error = "Approval was already completed on another attempt. Please request approval again."
            }
            if (value.status == "rejected") error = "The existing phone rejected this request."
            if (value.status == "expired") error = "This request expired. Please sign in again to request approval."
        } else {
            error = result.exceptionOrNull()?.message ?: "Could not check approval status."
        }
    }

    LaunchedEffect(requestId, refreshTick) {
        val started = System.currentTimeMillis()
        while (status == "pending" && System.currentTimeMillis() - started < 10 * 60 * 1000L) {
            checkStatus()
            if (status == "pending") delay(3000)
        }
        checking = false
        if (status == "pending") {
            status = "expired"
            error = "This request timed out. Please sign in again to request approval."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(HWXBackground).statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = HWXCyan, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(20.dp))
        Text("Device approval required", color = HWXTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "A request has been sent to your existing HWX Gateway phone. Approve this new phone there to finish setup.",
            color = HWXTextSecondary, fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        if (status == "pending" && checking) CircularProgressIndicator(color = HWXCyan)
        Spacer(Modifier.height(12.dp))
        Text("Status: ${status.uppercase()}", color = HWXCyan, fontWeight = FontWeight.SemiBold)
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = HWXError, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        if (status == "pending") {
            HWXButton(
                text = "Check approval now",
                onClick = { checking = true; refreshTick++ },
                modifier = Modifier.fillMaxWidth(),
                isPrimary = false
            )
        }
    }
}
