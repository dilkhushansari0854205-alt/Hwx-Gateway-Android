package com.example.ui.screens.devices

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
import com.example.data.models.Device
import com.example.data.models.PendingDeviceEnrollment
import com.example.data.repository.DeviceRepository
import com.example.ui.components.HWXButton
import com.example.ui.components.HWXCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun DevicesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSims: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { DeviceRepository(context) }

    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasLoaded by remember { mutableStateOf(false) }
    var pendingRequests by remember { mutableStateOf<List<PendingDeviceEnrollment>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<Device?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshDevices() {
        scope.launch {
            isLoading = !hasLoaded
            val result = repository.listDevices()
            val pendingResult = repository.listPendingDeviceEnrollments()
            isLoading = false
            hasLoaded = true
            if (result.isSuccess) devices = result.getOrNull() ?: emptyList()
            else errorMessage = result.exceptionOrNull()?.message
            if (pendingResult.isSuccess) pendingRequests = pendingResult.getOrNull() ?: emptyList()
            else if (pendingResult.exceptionOrNull()?.message?.contains("401") == false) {
                errorMessage = pendingResult.exceptionOrNull()?.message
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshDevices()
            delay(5000)
        }
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("devices_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HWXTextPrimary
                        )
                    }
                    Text(
                        text = "Devices",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary
                    )
                }

                // + Add Device Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(HWXPrimaryBlue)
                        .clickable { refreshDevices() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("refresh_device_requests_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Refresh",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = HWXError,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HWXCyan)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (pendingRequests.isNotEmpty()) {
                        item {
                            Text("New Device Approval Requests", color = HWXTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        items(pendingRequests, key = { it.id }) { request ->
                            PendingEnrollmentCard(
                                request = request,
                                onApprove = {
                                    scope.launch {
                                        val result = repository.approveDeviceEnrollment(request.id)
                                        if (result.isSuccess) refreshDevices()
                                        else errorMessage = result.exceptionOrNull()?.message
                                    }
                                },
                                onReject = {
                                    scope.launch {
                                        val result = repository.rejectDeviceEnrollment(request.id)
                                        if (result.isSuccess) refreshDevices()
                                        else errorMessage = result.exceptionOrNull()?.message
                                    }
                                }
                            )
                        }
                    }
                    item {
                        Text("Connected Devices", color = HWXTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    items(devices, key = { it.id }) { device ->
                        DeviceCard(device = device, onClick = { selectedDevice = device })
                    }
                }
            }
        }
    }

    // Device Details Dialog
    if (selectedDevice != null) {
        DeviceDetailsDialog(
            device = selectedDevice!!,
            onDismiss = { selectedDevice = null },
            onViewSims = {
                val devId = selectedDevice!!.id
                selectedDevice = null
                onNavigateToSims(devId)
            },
            onDelete = {
                scope.launch {
                    repository.deleteDevice(selectedDevice!!.id)
                    selectedDevice = null
                    refreshDevices()
                }
            }
        )
    }
}

@Composable
private fun PendingEnrollmentCard(
    request: PendingDeviceEnrollment,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    HWXCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(request.name, color = HWXTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${request.androidVersion} · App ${request.appVersion}", color = HWXTextSecondary, fontSize = 12.sp)
            Text("Approve only if you recognize this phone and its owner.", color = HWXTextMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HWXButton(text = "Reject", onClick = onReject, modifier = Modifier.weight(1f), isPrimary = false)
                HWXButton(text = "Approve", onClick = onApprove, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: Device,
    onClick: () -> Unit
) {
    val isOnline = device.status == "online" || device.isSocketConnected

    HWXCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Phone Icon Container
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HWXSecondaryBackground)
                    .border(1.dp, HWXCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Smartphone,
                    contentDescription = null,
                    tint = HWXCyan,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) HWXEmeraldGreen else HWXError)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        fontSize = 12.sp,
                        color = if (isOnline) HWXEmeraldGreen else HWXError,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.Filled.BatteryStd,
                        contentDescription = null,
                        tint = HWXTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${device.batteryLevel ?: 80}%",
                        fontSize = 12.sp,
                        color = HWXTextSecondary
                    )
                    if (device.isCharging == true) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Charging",
                            tint = HWXWarning,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Last Seen: ${device.lastSeen.ifBlank { "Recently" }}",
                    fontSize = 11.sp,
                    color = HWXTextMuted
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Details",
                tint = HWXTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun DeviceDetailsDialog(
    device: Device,
    onDismiss: () -> Unit,
    onViewSims: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HWXCardBackground,
        title = {
            Text(
                text = device.name,
                fontWeight = FontWeight.Bold,
                color = HWXTextPrimary,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "Device ID", value = device.id)
                DetailRow(label = "OS", value = device.androidVersion)
                DetailRow(label = "Status", value = device.status.uppercase())
                DetailRow(label = "SIM Count", value = "${device.sims.size} SIM(s)")
                DetailRow(label = "Network", value = device.networkType ?: "WiFi / Cellular")

                Spacer(modifier = Modifier.height(10.dp))

                HWXButton(
                    text = "Manage SIM Cards",
                    onClick = onViewSims,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = HWXError)
            ) {
                Text("Remove Device")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HWXTextMuted)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = HWXTextSecondary, fontSize = 12.sp)
        Text(text = value, color = HWXTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
