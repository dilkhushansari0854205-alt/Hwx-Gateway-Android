package com.example.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.constants.AppConstants
import com.example.ui.components.HWXCard
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var darkThemeEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    val hasSmsPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }
    val hasPhoneStatePermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HWXBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp)
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
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HWXTextPrimary
                    )
                }
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preferences Card
                HWXCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "App Preferences",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SettingToggleRow(
                        title = "Dark Theme",
                        subtitle = "Electric blue & cyber dark aesthetic",
                        checked = darkThemeEnabled,
                        onCheckedChange = { darkThemeEnabled = it }
                    )

                    HorizontalDivider(color = HWXCardBorder, modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Notifications",
                        subtitle = "Alert on incoming messages and dispatch status",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }

                // Server & Endpoints
                HWXCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Server Configuration",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SettingInfoRow("Production Backend", AppConstants.BASE_URL)
                    SettingInfoRow("WebSocket URL", AppConstants.WS_DEVICE_URL)
                    SettingInfoRow("Health Check", AppConstants.HEALTH_URL)
                    SettingInfoRow("Firebase Project", AppConstants.FIREBASE_PROJECT_ID)
                }

                // Permissions Card
                HWXCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Android System Permissions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    PermissionStatusRow(
                        title = "SMS Transmission (SEND_SMS, RECEIVE_SMS)",
                        granted = hasSmsPermission
                    )

                    PermissionStatusRow(
                        title = "Telephony State (READ_PHONE_STATE, DUAL SIM)",
                        granted = hasPhoneStatePermission
                    )
                }

                // About Card
                HWXCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "About HWX Gateway",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "HWX Gateway converts physical Android devices into high-performance, programmable SMS gateways with dual-SIM routing, automated bots, and secure WebSocket synchronization.",
                        fontSize = 13.sp,
                        color = HWXTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "App Version: 1.0.0 (Production Build)",
                        fontSize = 12.sp,
                        color = HWXCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HWXTextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = HWXTextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = HWXPrimaryBlue
            )
        )
    }
}

@Composable
private fun SettingInfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = HWXTextSecondary, fontSize = 12.sp)
        Text(text = value, color = HWXCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PermissionStatusRow(title: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = HWXTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (granted) "GRANTED" else "REQUIRED",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (granted) HWXEmeraldGreen else HWXWarning
        )
    }
}
