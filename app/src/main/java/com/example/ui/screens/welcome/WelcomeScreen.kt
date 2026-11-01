package com.example.ui.screens.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.HWXButton
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit,
    onLiveDemoClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HWXBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Logo & Branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(HWXCardSurface)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hwx_logo),
                        contentDescription = "HWX Gateway Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "HWX",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HWXCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Gateway",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )
                Text(
                    text = "SMS Gateway Control Panel",
                    fontSize = 12.sp,
                    color = HWXTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Hero Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Convert your\nAndroid phone\ninto an SMS\ngateway.",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HWXTextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Send & Receive SMS, Manage Bots, Control Devices and more...",
                    fontSize = 14.sp,
                    color = HWXTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Features Pill Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureRow(
                    icon1 = Icons.Filled.Send,
                    title1 = "Send & Receive SMS",
                    icon2 = Icons.Filled.SmartToy,
                    title2 = "Manage Multiple Bots"
                )
                FeatureRow(
                    icon1 = Icons.Filled.SimCard,
                    title1 = "Dual SIM Support",
                    icon2 = Icons.Filled.Security,
                    title2 = "Secure REST & WebSocket"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HWXButton(
                    text = "Get Started",
                    onClick = onGetStartedClick,
                    trailingIcon = Icons.Filled.ArrowForward,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "welcome_get_started_button"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.dp, HWXCyan.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                        .background(HWXSecondaryBackground)
                        .clickable(onClick = onLiveDemoClick)
                        .testTag("welcome_live_demo_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Live Demo",
                            tint = HWXEmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Demo (Sandbox)",
                            color = HWXTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon1: ImageVector,
    title1: String,
    icon2: ImageVector,
    title2: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FeaturePill(icon = icon1, title = title1, modifier = Modifier.weight(1f))
        FeaturePill(icon = icon2, title = title2, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FeaturePill(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(HWXCardBackground)
            .border(0.8.dp, HWXCardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HWXCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = HWXTextSecondary,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
