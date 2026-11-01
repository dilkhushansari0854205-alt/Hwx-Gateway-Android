package com.example.ui.screens.sms

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
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.models.SendSmsRequest
import com.example.data.models.SmsJob
import com.example.data.repository.SmsRepository
import com.example.ui.components.HWXButton
import com.example.ui.components.HWXCard
import com.example.ui.components.HWXTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SmsLogsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SmsRepository(context) }

    var logs by remember { mutableStateOf<List<SmsJob>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("all") }
    var isLoading by remember { mutableStateOf(true) }
    var showSendDialog by remember { mutableStateOf(false) }
    var selectedLogForDetails by remember { mutableStateOf<SmsJob?>(null) }

    fun refreshLogs() {
        scope.launch {
            isLoading = true
            val res = repository.listSmsLogs(limit = 100)
            isLoading = false
            if (res.isSuccess) {
                logs = res.getOrNull() ?: emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshLogs()
    }

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == "all") logs
        else logs.filter { it.status == selectedFilter }
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
                        modifier = Modifier.testTag("sms_logs_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HWXTextPrimary
                        )
                    }
                    Text(
                        text = "SMS Logs",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary
                    )
                }

                // Send SMS Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(HWXPrimaryBlue)
                        .clickable { showSendDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("send_sms_pill_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Send SMS",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all", "delivered", "sent", "failed", "queued").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HWXCyan)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLogs) { job ->
                        SmsJobCard(
                            job = job,
                            onClick = { selectedLogForDetails = job }
                        )
                    }
                }
            }
        }
    }

    if (showSendDialog) {
        SendSmsDialog(
            onDismiss = { showSendDialog = false },
            onConfirm = { req ->
                scope.launch {
                    val res = repository.sendSms(req)
                    showSendDialog = false
                    if (res.isSuccess) {
                        refreshLogs()
                    }
                }
            }
        )
    }

    if (selectedLogForDetails != null) {
        val job = selectedLogForDetails!!
        AlertDialog(
            onDismissRequest = { selectedLogForDetails = null },
            containerColor = HWXCardBackground,
            title = {
                Text(text = "SMS Job: ${job.id}", color = HWXTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("To: ${job.to}", color = HWXTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Status: ${job.status.uppercase()}", color = getStatusColor(job.status), fontWeight = FontWeight.Bold)
                    Text("SIM Slot: SIM ${job.simSlot + 1}", color = HWXTextSecondary)
                    Text("Device ID: ${job.deviceId}", color = HWXTextSecondary)
                    Text("Message: ${job.message}", color = HWXTextPrimary)
                    if (!job.errorMessage.isNullOrBlank()) {
                        Text("Error: ${job.errorMessage}", color = HWXError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLogForDetails = null }) {
                    Text("Close", color = HWXCyan)
                }
            }
        )
    }
}

@Composable
private fun SmsJobCard(
    job: JobOrMessage,
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getStatusColor(job.status).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (job.status) {
                        "delivered" -> Icons.Filled.CheckCircle
                        "sent" -> Icons.Filled.Done
                        "failed" -> Icons.Filled.ErrorOutline
                        else -> Icons.Filled.Schedule
                    },
                    contentDescription = null,
                    tint = getStatusColor(job.status),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = job.to,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary
                    )
                    StatusPill(job.status)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = job.message,
                    fontSize = 13.sp,
                    color = HWXTextSecondary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "SIM Slot ${job.simSlot + 1} • ${job.createdAt.take(16).replace("T", " ")}",
                    fontSize = 11.sp,
                    color = HWXTextMuted
                )
            }
        }
    }
}

private typealias JobOrMessage = SmsJob

@Composable
private fun StatusPill(status: String) {
    val color = getStatusColor(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getStatusColor(status: String): Color {
    return when (status) {
        "delivered" -> HWXEmeraldGreen
        "sent" -> HWXCyan
        "failed" -> HWXError
        "queued" -> HWXWarning
        else -> HWXTextSecondary
    }
}

@Composable
fun SendSmsDialog(
    onDismiss: () -> Unit,
    onConfirm: (SendSmsRequest) -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HWXCardBackground,
        title = {
            Text(text = "Send Programmable SMS", color = HWXTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HWXTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    placeholder = "Recipient Phone (+91...)",
                    leadingIcon = Icons.Filled.Phone
                )
                HWXTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = "SMS Message Content",
                    leadingIcon = Icons.Filled.Chat
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (recipient.isNotBlank() && message.isNotBlank()) {
                        onConfirm(
                            SendSmsRequest(
                                bot_id = "bot_1",
                                device_id = "dev_galaxy_s24",
                                to = recipient.trim(),
                                message = message.trim(),
                                idempotency_key = "idemp_${System.currentTimeMillis()}"
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HWXPrimaryBlue)
            ) {
                Text("Dispatch SMS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HWXTextMuted)
            }
        }
    )
}
