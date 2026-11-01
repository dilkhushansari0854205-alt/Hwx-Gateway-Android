package com.example.ui.screens.apikeys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.models.ApiKeyRecord
import com.example.data.models.CreateApiKeyData
import com.example.data.repository.ApiKeyRepository
import com.example.ui.components.HWXButton
import com.example.ui.components.HWXCard
import com.example.ui.components.HWXTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ApiKeysScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ApiKeyRepository(context) }

    var keys by remember { mutableStateOf<List<ApiKeyRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var createdKeyResult by remember { mutableStateOf<CreateApiKeyData?>(null) }
    var keyToRevoke by remember { mutableStateOf<ApiKeyRecord?>(null) }

    fun refreshKeys() {
        scope.launch {
            isLoading = true
            val res = repository.listApiKeys()
            isLoading = false
            if (res.isSuccess) {
                keys = res.getOrNull() ?: emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshKeys()
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
                        modifier = Modifier.testTag("api_keys_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HWXTextPrimary
                        )
                    }
                    Text(
                        text = "API Keys",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(HWXPrimaryBlue)
                        .clickable { showGenerateDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("generate_api_key_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New Key",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(keys) { key ->
                        ApiKeyCard(
                            keyRecord = key,
                            onRevokeClick = { keyToRevoke = key }
                        )
                    }
                }
            }
        }
    }

    if (showGenerateDialog) {
        GenerateKeyDialog(
            onDismiss = { showGenerateDialog = false },
            onConfirm = { name ->
                scope.launch {
                    val res = repository.createApiKey(name)
                    showGenerateDialog = false
                    if (res.isSuccess) {
                        createdKeyResult = res.getOrNull()
                        refreshKeys()
                    }
                }
            }
        )
    }

    if (createdKeyResult != null) {
        val created = createdKeyResult!!
        AlertDialog(
            onDismissRequest = { createdKeyResult = null },
            containerColor = HWXCardBackground,
            title = {
                Text(text = "API Key Generated", color = HWXEmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "IMPORTANT: Copy this key now. For security purposes, it will never be displayed again.",
                        color = HWXWarning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        color = HWXSecondaryBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HWXCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = created.apiKey,
                                color = HWXCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("API Key", created.apiKey))
                                    Toast.makeText(context, "API Key copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { createdKeyResult = null },
                    colors = ButtonDefaults.buttonColors(containerColor = HWXPrimaryBlue)
                ) {
                    Text("I Have Saved My Key")
                }
            }
        )
    }

    if (keyToRevoke != null) {
        val key = keyToRevoke!!
        AlertDialog(
            onDismissRequest = { keyToRevoke = null },
            containerColor = HWXCardBackground,
            title = { Text("Revoke API Key?", color = HWXTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to revoke '${key.name}'? Any systems using this key will immediately be denied access.", color = HWXTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.revokeApiKey(key.id)
                            keyToRevoke = null
                            refreshKeys()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HWXError)
                ) {
                    Text("Revoke Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { keyToRevoke = null }) {
                    Text("Cancel", color = HWXTextMuted)
                }
            }
        )
    }
}

@Composable
private fun ApiKeyCard(
    keyRecord: ApiKeyRecord,
    onRevokeClick: () -> Unit
) {
    val isActive = keyRecord.status == "active"

    HWXCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = keyRecord.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = keyRecord.keyPrefix,
                    fontSize = 13.sp,
                    color = HWXCyan,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Used: ${keyRecord.lastUsedAt ?: "Never"} • Calls: ${keyRecord.usageCount}",
                    fontSize = 11.sp,
                    color = HWXTextMuted
                )
            }

            if (isActive) {
                IconButton(onClick = onRevokeClick) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Revoke", tint = HWXError.copy(alpha = 0.8f))
                }
            } else {
                Text(text = "REVOKED", color = HWXTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GenerateKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HWXCardBackground,
        title = { Text("Generate New API Key", color = HWXTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter a recognizable label for this key.", color = HWXTextSecondary, fontSize = 13.sp)
                HWXTextField(value = name, onValueChange = { name = it }, placeholder = "Key Name (e.g. Production Server)")
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = HWXPrimaryBlue)
            ) {
                Text("Generate Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = HWXTextMuted) }
        }
    )
}
