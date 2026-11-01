package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionBottomSheet(
    onDismiss: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onCreateBotClick: () -> Unit,
    onSendSmsClick: () -> Unit,
    onGenerateApiKeyClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HWXSecondaryBackground,
        contentColor = HWXTextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HWXTextMuted.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HWXTextPrimary
            )
            Text(
                text = "Choose an instant action to manage your SMS gateway",
                fontSize = 13.sp,
                color = HWXTextSecondary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionItem(
                    title = "Add Device",
                    icon = Icons.Filled.PhoneAndroid,
                    gradient = HWXPrimaryGradient,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onAddDeviceClick()
                    }
                )
                QuickActionItem(
                    title = "Create Bot",
                    icon = Icons.Filled.SmartToy,
                    gradient = HWXCyanGreenGradient,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onCreateBotClick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionItem(
                    title = "Send SMS",
                    icon = Icons.Filled.Send,
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF0072FF), Color(0xFF00C6FF))),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onSendSmsClick()
                    }
                )
                QuickActionItem(
                    title = "New API Key",
                    icon = Icons.Filled.Key,
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onGenerateApiKeyClick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(HWXCardBackground)
            .border(1.dp, HWXCardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = HWXTextPrimary
        )
    }
}
