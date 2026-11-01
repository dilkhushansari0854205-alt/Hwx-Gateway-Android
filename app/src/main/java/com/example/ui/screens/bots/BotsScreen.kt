package com.example.ui.screens.bots

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
import com.example.data.models.Bot
import com.example.data.models.CreateBotRequest
import com.example.data.models.UpdateBotRequest
import com.example.data.repository.BotRepository
import com.example.ui.components.HWXButton
import com.example.ui.components.HWXCard
import com.example.ui.components.HWXTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BotsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { BotRepository(context) }

    var bots by remember { mutableStateOf<List<Bot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedBotForEdit by remember { mutableStateOf<Bot?>(null) }

    fun refreshBots() {
        scope.launch {
            isLoading = true
            val res = repository.listBots()
            isLoading = false
            if (res.isSuccess) {
                bots = res.getOrNull() ?: emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshBots()
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
                        modifier = Modifier.testTag("bots_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HWXTextPrimary
                        )
                    }
                    Text(
                        text = "Bots Management",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary
                    )
                }

                // Create Bot Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(HWXEmeraldGreen)
                        .clickable { showCreateDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("create_bot_pill_button"),
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
                            text = "New Bot",
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
                    items(bots) { bot ->
                        BotCard(
                            bot = bot,
                            onClick = { selectedBotForEdit = bot }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateBotDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { req ->
                scope.launch {
                    val res = repository.createBot(req)
                    showCreateDialog = false
                    if (res.isSuccess) {
                        refreshBots()
                    }
                }
            }
        )
    }

    if (selectedBotForEdit != null) {
        val bot = selectedBotForEdit!!
        EditBotDialog(
            bot = bot,
            onDismiss = { selectedBotForEdit = null },
            onToggleStatus = {
                val newStatus = if (bot.status == "active") "inactive" else "active"
                scope.launch {
                    repository.updateBot(bot.id, UpdateBotRequest(status = newStatus))
                    selectedBotForEdit = null
                    refreshBots()
                }
            },
            onDelete = {
                scope.launch {
                    repository.deleteBot(bot.id)
                    selectedBotForEdit = null
                    refreshBots()
                }
            }
        )
    }
}

@Composable
private fun BotCard(
    bot: Bot,
    onClick: () -> Unit
) {
    val isActive = bot.status == "active"

    HWXCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HWXEmeraldGreen.copy(alpha = 0.2f))
                    .border(1.dp, HWXEmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = HWXEmeraldGreen,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bot.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) HWXDarkGreen else Color(0xFF2C2205))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isActive) "ACTIVE" else "INACTIVE",
                            color = if (isActive) HWXEmeraldGreen else HWXWarning,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!bot.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bot.description,
                        fontSize = 12.sp,
                        color = HWXTextSecondary,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BotPillBadge("Mode: ${bot.simSelectionMode}")
                    if (!bot.webhookUrl.isNullOrBlank()) {
                        BotPillBadge("Webhook: Set")
                    }
                }
            }
        }
    }
}

@Composable
private fun BotPillBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(HWXSecondaryBackground)
            .border(0.5.dp, HWXCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = HWXCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CreateBotDialog(
    onDismiss: () -> Unit,
    onConfirm: (CreateBotRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var simMode by remember { mutableStateOf("bot_fixed") }
    var webhookUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HWXCardBackground,
        title = {
            Text(text = "Create New Bot", color = HWXTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HWXTextField(value = name, onValueChange = { name = it }, placeholder = "Bot Name (e.g. OTP Bot)")
                HWXTextField(value = description, onValueChange = { description = it }, placeholder = "Description (Optional)")
                HWXTextField(value = webhookUrl, onValueChange = { webhookUrl = it }, placeholder = "Webhook URL (Optional)")

                Text("SIM Selection Mode", color = HWXTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("bot_fixed", "auto_switch", "manual").forEach { mode ->
                        FilterChip(
                            selected = simMode == mode,
                            onClick = { simMode = mode },
                            label = { Text(mode.replace("_", " "), fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            CreateBotRequest(
                                name = name.trim(),
                                description = description.trim().takeIf { it.isNotBlank() },
                                assignedDeviceId = "dev_galaxy_s24",
                                assignedSimId = "sim_1",
                                simSelectionMode = simMode,
                                webhookUrl = webhookUrl.trim().takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HWXEmeraldGreen)
            ) {
                Text("Create Bot", color = HWXBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HWXTextMuted)
            }
        }
    )
}

@Composable
fun EditBotDialog(
    bot: Bot,
    onDismiss: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HWXCardBackground,
        title = {
            Text(text = bot.name, color = HWXTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SIM Mode: ${bot.simSelectionMode}", color = HWXTextSecondary, fontSize = 13.sp)
                Text("Status: ${bot.status.uppercase()}", color = if (bot.status == "active") HWXEmeraldGreen else HWXWarning, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (!bot.webhookUrl.isNullOrBlank()) {
                    Text("Webhook: ${bot.webhookUrl}", color = HWXCyan, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onToggleStatus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bot.status == "active") HWXWarning else HWXEmeraldGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (bot.status == "active") "Pause Bot" else "Activate Bot", color = HWXBackground, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = HWXError)) {
                Text("Delete Bot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HWXTextMuted)
            }
        }
    )
}
