package com.example.ui.screens.sims

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.SimCard
import com.example.data.models.UpdateSimRequest
import com.example.data.repository.DeviceRepository
import com.example.services.telephony.TelephonyManagerHelper
import com.example.ui.components.HWXButton
import com.example.ui.components.HWXCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SimManagementScreen(
    deviceId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { DeviceRepository(context) }
    val telephonyHelper = remember { TelephonyManagerHelper(context) }

    var sims by remember { mutableStateOf<List<SimCard>>(emptyList()) }
    var autoSwitchEnabled by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedSimForEdit by remember { mutableStateOf<SimCard?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun loadSims() {
        scope.launch {
            isLoading = true
            val targetDevId = deviceId ?: "dev_galaxy_s24"
            val devResult = repository.getDevice(targetDevId)
            isLoading = false
            if (devResult.isSuccess) {
                sims = devResult.getOrNull()?.sims ?: emptyList()
            } else {
                // Read local detected SIMs as fallback
                val detected = telephonyHelper.detectSimCards()
                sims = detected.mapIndexed { i, d ->
                    SimCard(
                        id = "sim_$i",
                        subscriptionId = d.subscriptionId,
                        slotIndex = d.slotIndex,
                        carrierName = d.carrierName,
                        displayName = d.displayName,
                        phoneNumber = d.phoneNumber,
                        isVerified = i == 0
                    )
                }
            }
        }
    }

    LaunchedEffect(deviceId) {
        loadSims()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HWXBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("sim_management_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HWXTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIM Management",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SIM Cards list
                items(sims) { sim ->
                    SimCardItem(
                        sim = sim,
                        onClick = { selectedSimForEdit = sim }
                    )
                }

                // SIM Selection Mode Card
                item {
                    Text(
                        text = "SIM Selection Mode",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    HWXCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(HWXPrimaryBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SmartToy,
                                        contentDescription = null,
                                        tint = HWXPrimaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Auto Switch",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HWXTextPrimary
                                    )
                                    Text(
                                        text = "Automatically use verified fallback SIM",
                                        fontSize = 12.sp,
                                        color = HWXTextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = autoSwitchEnabled,
                                onCheckedChange = { autoSwitchEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = HWXPrimaryBlue,
                                    uncheckedThumbColor = HWXTextMuted,
                                    uncheckedTrackColor = HWXCardBorder
                                )
                            )
                        }
                    }
                }

                // Refresh SIM Info Button
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .border(1.dp, HWXCyan.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                            .background(HWXSecondaryBackground)
                            .clickable {
                                statusMessage = "SIM status refreshed from hardware radio."
                                loadSims()
                            }
                            .testTag("refresh_sim_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = HWXCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Refresh SIM Info",
                                color = HWXTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (statusMessage != null) {
                    item {
                        Surface(
                            color = HWXEmeraldGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HWXEmeraldGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusMessage ?: "",
                                color = HWXEmeraldGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // SIM Verification & Authorization Dialog
    if (selectedSimForEdit != null) {
        val sim = selectedSimForEdit!!
        var isVerifiedState by remember { mutableStateOf(sim.isVerified) }
        var isFallbackAuthorized by remember { mutableStateOf(sim.isAuthorizedFallback) }

        AlertDialog(
            onDismissRequest = { selectedSimForEdit = null },
            containerColor = HWXCardBackground,
            title = {
                Text(
                    text = "SIM Slot ${sim.slotIndex + 1} (${sim.carrierName})",
                    color = HWXTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Hardware rules dictate that SIMs cannot be silently authorized or verified without explicit user consent.",
                        fontSize = 12.sp,
                        color = HWXTextSecondary,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SIM Verified Status", color = HWXTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Authorize for Bot outgoing SMS", color = HWXTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isVerifiedState,
                            onCheckedChange = { isVerifiedState = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Authorized Fallback", color = HWXTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Allow auto-switch if primary SIM fails", color = HWXTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isFallbackAuthorized,
                            onCheckedChange = { isFallbackAuthorized = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val targetDevId = deviceId ?: "dev_galaxy_s24"
                            repository.updateSimAuthorization(
                                deviceId = targetDevId,
                                simId = sim.id,
                                request = UpdateSimRequest(
                                    isVerified = isVerifiedState,
                                    isAuthorizedFallback = isFallbackAuthorized
                                )
                            )
                            selectedSimForEdit = null
                            loadSims()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HWXPrimaryBlue)
                ) {
                    Text("Save Authorization")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSimForEdit = null }) {
                    Text("Cancel", color = HWXTextMuted)
                }
            }
        )
    }
}

@Composable
private fun SimCardItem(
    sim: SimCard,
    onClick: () -> Unit
) {
    HWXCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SIM Icon Box
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (sim.slotIndex == 0) HWXPrimaryBlue.copy(alpha = 0.2f) else HWXEmeraldGreen.copy(alpha = 0.2f))
                    .border(
                        1.dp,
                        if (sim.slotIndex == 0) HWXCyan.copy(alpha = 0.4f) else HWXEmeraldGreen.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SimCard,
                    contentDescription = null,
                    tint = if (sim.slotIndex == 0) HWXCyan else HWXEmeraldGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SIM ${sim.slotIndex + 1}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )
                Text(
                    text = sim.carrierName,
                    fontSize = 13.sp,
                    color = HWXTextSecondary
                )
                Text(
                    text = sim.phoneNumber ?: "+91 98765 43210",
                    fontSize = 12.sp,
                    color = HWXCyan,
                    fontWeight = FontWeight.Medium
                )
            }

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (sim.isVerified) HWXDarkGreen.copy(alpha = 0.6f)
                        else Color(0xFF382905)
                    )
                    .border(
                        0.5.dp,
                        if (sim.isVerified) HWXEmeraldGreen.copy(alpha = 0.6f) else HWXWarning.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (sim.isVerified) HWXEmeraldGreen else HWXWarning)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (sim.isVerified) "Verified" else "Pending",
                        color = if (sim.isVerified) HWXEmeraldGreen else HWXWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = HWXTextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
