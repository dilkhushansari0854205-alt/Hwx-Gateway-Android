package com.example.ui.screens.webhooks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.models.CreateWebhookRequest
import com.example.data.models.TestWebhookData
import com.example.data.models.UpdateWebhookRequest
import com.example.data.models.WebhookConfig
import com.example.data.repository.WebhookRepository
import com.example.ui.components.HWXButton
import com.example.ui.components.HWXCard
import com.example.ui.components.HWXTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun WebhooksScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { WebhookRepository(context) }

    var webhooks by remember { mutableStateOf<List<WebhookConfig>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestWebhookData?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    fun refreshWebhooks() {
        scope.launch {
            isLoading = true
            val res = repository.listWebhooks()
            isLoading = false
            if (res.isSuccess) {
                webhooks = res.getOrNull() ?: emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshWebhooks()
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
                        modifier = Modifier.testTag("webhooks_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HWXTextPrimary
                        )
                    }
                    Text(
                        text = "Webhooks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(HWXEmeraldGreen)
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("add_webhook_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = HWXBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Webhook",
                            color = HWXBackground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HWXEmeraldGreen)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(webhooks) { webhook ->
                        WebhookCard(
                            webhook = webhook,
                            onToggleActive = { active ->
                                scope.launch {
                                    repository.updateWebhook(webhook.id, UpdateWebhookRequest(isActive = active))
                                    refreshWebhooks()
                                }
                            },
                            onTestClick = {
                                isTesting = true
                                scope.launch {
                                    val res = repository.testWebhook(webhook.url, webhook.secret)
                                    isTesting = false
                                    testResult = res.getOrNull()
                                }
                            },
                            onDeleteClick = {
                                scope.launch {
                                    repository.deleteWebhook(webhook.id)
                                    refreshWebhooks()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddWebhookDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { req ->
                scope.launch {
                    val res = repository.createWebhook(req)
                    showAddDialog = false
                    if (res.isSuccess) {
                        refreshWebhooks()
                    }
                }
            }
        )
    }

    if (testResult != null) {
        val result = testResult!!
        AlertDialog(
            onDismissRequest = { testResult = null },
            containerColor = HWXCardBackground,
            title = {
                Text(text = "Webhook Test Result", color = HWXEmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Target: ${result.url}", color = HWXTextSecondary, fontSize = 12.sp)
                    Text("HTTP Status: ${result.statusCode ?: 200} OK", color = HWXEmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Response Payload:", color = HWXTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Surface(
                        color = HWXSecondaryBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.responsePreview ?: "Ping delivered",
                            color = HWXCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { testResult = null }) {
                    Text("Close", color = HWXCyan)
                }
            }
        )
    }
}

@Composable
private fun WebhookCard(
    webhook: WebhookConfig,
    onToggleActive: (Boolean) -> Unit,
    onTestClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    HWXCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = webhook.url,
                    color = HWXTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = webhook.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = HWXEmeraldGreen
                    )
                )
            }

            Text(
                text = "Events: ${webhook.events.joinToString(", ")}",
                color = HWXCyan,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onTestClick) {
                    Icon(imageVector = Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = HWXEmeraldGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Ping", color = HWXEmeraldGreen, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDeleteClick) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = HWXError.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun AddWebhookDialog(
    onDismiss: () -> Unit,
    onConfirm: (CreateWebhookRequest) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    val availableEvents = listOf("sms.received", "sms.sent", "sms.delivered", "sms.failed", "device.online")
    val selectedEvents = remember { mutableStateListOf("sms.received", "sms.delivered") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HWXCardBackground,
        title = { Text("Register Webhook", color = HWXTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HWXTextField(value = url, onValueChange = { url = it }, placeholder = "Webhook URL (https://...)")
                HWXTextField(value = secret, onValueChange = { secret = it }, placeholder = "Secret Key (min 8 chars)")

                Text("Subscribed Events", color = HWXTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                availableEvents.forEach { event ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedEvents.contains(event)) selectedEvents.remove(event)
                                else selectedEvents.add(event)
                            }
                    ) {
                        Checkbox(checked = selectedEvents.contains(event), onCheckedChange = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(event, color = HWXTextPrimary, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank() && selectedEvents.isNotEmpty()) {
                        onConfirm(
                            CreateWebhookRequest(
                                url = url.trim(),
                                events = selectedEvents.toList(),
                                secret = secret.trim().takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HWXEmeraldGreen)
            ) {
                Text("Register", color = HWXBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = HWXTextMuted) }
        }
    )
}
