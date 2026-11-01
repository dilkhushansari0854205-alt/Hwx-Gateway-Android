package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.network.ApiClient
import com.example.core.security.TokenStorage
import com.example.data.models.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.DeviceRepository
import com.example.data.repository.SmsRepository
import com.example.services.websocket.DeviceWebSocketService
import com.example.ui.components.HWXCard
import com.example.ui.components.HWXStatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onNavigateToDevices: () -> Unit,
    onNavigateToBots: () -> Unit,
    onNavigateToSmsLogs: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSims: () -> Unit,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }
    val deviceRepository = remember { DeviceRepository(context) }
    val smsRepository = remember { SmsRepository(context) }
    val wsService = remember { DeviceWebSocketService.getInstance(context) }
    val tokenStorage = remember { TokenStorage.getInstance(context) }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var totalDevicesCount by remember { mutableIntStateOf(4) }
    var totalBotsCount by remember { mutableIntStateOf(8) }
    var smsSentCount by remember { mutableIntStateOf(1250) }
    var smsFailedCount by remember { mutableIntStateOf(23) }
    var serverHealthy by remember { mutableStateOf(true) }
    val isWsConnected by wsService.isConnected.collectAsState()

    LaunchedEffect(Unit) {
        // Connect device socket if registered
        wsService.connect()

        scope.launch {
            val userRes = authRepository.fetchUserProfile()
            if (userRes.isSuccess) {
                userProfile = userRes.getOrNull()
                userProfile?.stats?.let { stats ->
                    smsSentCount = stats.smsSentCount
                }
            }
        }

        scope.launch {
            try {
                val health = ApiClient.getService(context).getHealth()
                serverHealthy = health.isSuccessful && health.body()?.status == "healthy"
            } catch (e: Exception) {
                serverHealthy = false
            }
        }

        scope.launch {
            val devRes = deviceRepository.listDevices()
            if (devRes.isSuccess) {
                totalDevicesCount = devRes.getOrNull()?.size ?: totalDevicesCount
            }
        }

        scope.launch {
            val smsRes = smsRepository.listSmsLogs(limit = 100)
            if (smsRes.isSuccess) {
                val logs = smsRes.getOrNull() ?: emptyList()
                smsFailedCount = logs.count { it.status == "failed" }
            }
        }
    }

    val displayName = userProfile?.displayName?.ifBlank { "Firoz Ansari" } ?: "Firoz Ansari"
    val avatarInitial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HWXBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp)
    ) {
        // 1. Top Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.testTag("dashboard_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = HWXTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HWX",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HWXCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Gateway",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToSmsLogs) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = HWXTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HWXPrimaryBlue)
                            .clickable(onClick = onNavigateToProfile)
                            .testTag("dashboard_profile_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarInitial,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Demo Mode Banner (if sandbox)
        if (tokenStorage.isDemoMode()) {
            item {
                Surface(
                    color = Color(0xFF2C2205),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HWXWarning.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = HWXWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Live Demo Sandbox Active. Real SMS commands will not be sent to carrier networks.",
                            color = HWXWarning,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 2. Welcome Card
        item {
            HWXCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                hasGlowBorder = true
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Welcome Back!",
                            fontSize = 15.sp,
                            color = HWXTextSecondary
                        )
                        Text(
                            text = "$displayName 👋",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = HWXTextPrimary
                        )
                        Text(
                            text = "Last Login: 1h ago",
                            fontSize = 12.sp,
                            color = HWXTextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    HWXStatusBadge(
                        isOnline = isWsConnected || serverHealthy,
                        label = if (isWsConnected) "WebSocket Live" else if (serverHealthy) "Online" else "Offline"
                    )
                }
            }
        }

        // 3. 2x2 Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatCard(
                    title = "Total Devices",
                    value = String.format("%02d", totalDevicesCount),
                    icon = Icons.Filled.PhoneAndroid,
                    iconBgColor = HWXPrimaryBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDevices
                )
                StatCard(
                    title = "Total Bots",
                    value = String.format("%02d", totalBotsCount),
                    icon = Icons.Filled.SmartToy,
                    iconBgColor = HWXEmeraldGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToBots
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatCard(
                    title = "SMS Sent",
                    value = "%,d".format(smsSentCount),
                    icon = Icons.Filled.Send,
                    iconBgColor = HWXCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSmsLogs
                )
                StatCard(
                    title = "Failed SMS",
                    value = "$smsFailedCount",
                    icon = Icons.Filled.Warning,
                    iconBgColor = HWXError,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSmsLogs
                )
            }
        }

        // 4. Server Connection Card
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(HWXServerStatusGradient)
                    .border(1.dp, HWXEmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(HWXEmeraldGreen.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SignalCellularAlt,
                            contentDescription = "Server Connected",
                            tint = HWXEmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (serverHealthy) "Server Connected" else "Server Connecting...",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "hwxgetaway.onrender.com",
                            color = HWXEmeraldGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 5. Quick Actions Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Actions",
                    color = HWXTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onNavigateToDevices)
                ) {
                    Text(
                        text = "View All",
                        color = HWXCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = HWXCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionSquare(
                    title = "Devices",
                    icon = Icons.Filled.PhoneAndroid,
                    bgColor = Color(0xFF6C5CE7),
                    onClick = onNavigateToDevices
                )
                QuickActionSquare(
                    title = "Bots",
                    icon = Icons.Filled.SmartToy,
                    bgColor = HWXEmeraldGreen,
                    onClick = onNavigateToBots
                )
                QuickActionSquare(
                    title = "SMS Logs",
                    icon = Icons.Filled.Forum,
                    bgColor = Color(0xFFE84393),
                    onClick = onNavigateToSmsLogs
                )
                QuickActionSquare(
                    title = "Settings",
                    icon = Icons.Filled.Settings,
                    bgColor = HWXCyan,
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    HWXCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconBgColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = HWXTextSecondary,
                    maxLines = 1
                )
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )
            }
        }
    }
}

@Composable
private fun QuickActionSquare(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(bgColor.copy(alpha = 0.15f))
                .border(1.dp, bgColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = bgColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = HWXTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
