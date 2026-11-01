package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.example.data.models.UserProfile
import com.example.data.repository.AuthRepository
import com.example.ui.components.HWXButton
import com.example.ui.components.HWXCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val res = authRepository.fetchUserProfile()
        isLoading = false
        if (res.isSuccess) {
            userProfile = res.getOrNull()
        }
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
                    modifier = Modifier.testTag("profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HWXTextPrimary
                    )
                }
                Text(
                    text = "Profile & Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User Avatar Circle
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(HWXPrimaryBlue)
                        .border(2.dp, HWXCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userProfile?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = userProfile?.displayName ?: "Firoz Ansari",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXTextPrimary
                )
                Text(
                    text = userProfile?.email ?: "user@hwxgateway.io",
                    fontSize = 14.sp,
                    color = HWXTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(HWXDarkGreen)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ACCOUNT ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXEmeraldGreen
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Section
                HWXCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Account Statistics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = HWXTextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val stats = userProfile?.stats
                    ProfileStatRow("Total SMS Sent", "${stats?.smsSentCount ?: 1250}")
                    ProfileStatRow("Total SMS Received", "${stats?.smsReceivedCount ?: 380}")
                    ProfileStatRow("Total API Requests", "${stats?.apiRequestsCount ?: 4920}")
                    ProfileStatRow("Active Devices", "${stats?.activeDevicesCount ?: 3}")
                    ProfileStatRow("Email Verified", if (userProfile?.emailVerified == true) "Verified" else "Pending")
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Logout Button
                HWXButton(
                    text = "Sign Out",
                    onClick = {
                        authRepository.logout()
                        onLogoutSuccess()
                    },
                    isPrimary = false,
                    trailingIcon = Icons.AutoMirrored.Filled.ExitToApp,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "logout_button"
                )
            }
        }
    }
}

@Composable
private fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = HWXTextSecondary, fontSize = 13.sp)
        Text(text = value, color = HWXTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
