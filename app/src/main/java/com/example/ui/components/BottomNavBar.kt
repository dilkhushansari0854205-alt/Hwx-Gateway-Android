package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class BottomNavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    DEVICES("devices", "Devices", Icons.Filled.PhoneAndroid, Icons.Outlined.PhoneAndroid),
    BOTS("bots", "Bots", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    MORE("more", "More", Icons.Filled.Menu, Icons.Outlined.Menu)
}

@Composable
fun FloatingBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCentralPlusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(34.dp), ambientColor = HWXCyan.copy(alpha = 0.2f))
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(HWXCyan.copy(alpha = 0.35f), HWXPrimaryBlue.copy(alpha = 0.2f), HWXEmeraldGreen.copy(alpha = 0.35f))
                    ),
                    shape = RoundedCornerShape(34.dp)
                ),
            shape = RoundedCornerShape(34.dp),
            color = HWXSecondaryBackground.copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 1. Home
                NavPillItem(
                    destination = BottomNavDestination.HOME,
                    isSelected = currentRoute == BottomNavDestination.HOME.route,
                    onClick = { onNavigate(BottomNavDestination.HOME.route) }
                )

                // 2. Devices
                NavPillItem(
                    destination = BottomNavDestination.DEVICES,
                    isSelected = currentRoute == BottomNavDestination.DEVICES.route,
                    onClick = { onNavigate(BottomNavDestination.DEVICES.route) }
                )

                // 3. Central Plus Button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(HWXPrimaryGradient)
                        .border(1.5.dp, HWXCyan, CircleShape)
                        .clickable(onClick = onCentralPlusClick)
                        .testTag("central_plus_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Quick Actions",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 4. Bots
                NavPillItem(
                    destination = BottomNavDestination.BOTS,
                    isSelected = currentRoute == BottomNavDestination.BOTS.route,
                    onClick = { onNavigate(BottomNavDestination.BOTS.route) }
                )

                // 5. More
                NavPillItem(
                    destination = BottomNavDestination.MORE,
                    isSelected = currentRoute == "more" || currentRoute in listOf("sims", "sms_logs", "api_keys", "webhooks", "profile", "settings"),
                    onClick = { onNavigate(BottomNavDestination.MORE.route) }
                )
            }
        }
    }
}

@Composable
private fun NavPillItem(
    destination: BottomNavDestination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) HWXCyan else HWXTextMuted,
        label = "iconColor"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("nav_item_${destination.route}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = destination.title,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = destination.title,
            color = iconColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
