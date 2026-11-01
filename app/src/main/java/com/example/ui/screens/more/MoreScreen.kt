package com.example.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HWXCard
import com.example.ui.theme.*

@Composable
fun MoreScreen(
    onNavigateToSims: () -> Unit,
    onNavigateToSmsLogs: () -> Unit,
    onNavigateToApiKeys: () -> Unit,
    onNavigateToWebhooks: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "More Options",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MoreNavCard(
                        title = "SIM Management",
                        subtitle = "Dual SIM detection, auto-switch & carrier configuration",
                        icon = Icons.Filled.SimCard,
                        iconColor = HWXCyan,
                        testTag = "more_sim_management",
                        onClick = onNavigateToSims
                    )
                }

                item {
                    MoreNavCard(
                        title = "SMS Logs & History",
                        subtitle = "Sent, delivered, failed and incoming messages audit",
                        icon = Icons.Filled.Forum,
                        iconColor = Color(0xFFE84393),
                        testTag = "more_sms_logs",
                        onClick = onNavigateToSmsLogs
                    )
                }

                item {
                    MoreNavCard(
                        title = "API Keys",
                        subtitle = "Generate and revoke developer REST API credentials",
                        icon = Icons.Filled.Key,
                        iconColor = HWXPrimaryBlue,
                        testTag = "more_api_keys",
                        onClick = onNavigateToApiKeys
                    )
                }

                item {
                    MoreNavCard(
                        title = "Webhooks",
                        subtitle = "Configure HTTP callback endpoints for SMS events",
                        icon = Icons.Filled.Webhook,
                        iconColor = HWXEmeraldGreen,
                        testTag = "more_webhooks",
                        onClick = onNavigateToWebhooks
                    )
                }

                item {
                    MoreNavCard(
                        title = "Profile & Account",
                        subtitle = "User details, account quotas, and session logout",
                        icon = Icons.Filled.Person,
                        iconColor = Color(0xFFA29BFE),
                        testTag = "more_profile",
                        onClick = onNavigateToProfile
                    )
                }

                item {
                    MoreNavCard(
                        title = "Settings",
                        subtitle = "App preferences, server endpoints & permissions",
                        icon = Icons.Filled.Settings,
                        iconColor = HWXTextSecondary,
                        testTag = "more_settings",
                        onClick = onNavigateToSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    HWXCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = HWXTextSecondary,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = HWXTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
