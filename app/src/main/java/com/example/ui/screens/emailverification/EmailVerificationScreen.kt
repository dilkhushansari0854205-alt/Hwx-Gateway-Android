package com.example.ui.screens.emailverification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.ui.components.HWXButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }

    var isResending by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("verify_email_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HWXTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email illustration
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(HWXCardBackground)
                    .border(1.5.dp, HWXCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MarkEmailRead,
                    contentDescription = "Verify Email",
                    tint = HWXCyan,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Verify Your Email",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HWXTextPrimary
            )
            Text(
                text = "We've sent a verification link to\n${email.ifBlank { "your email address" }}.",
                fontSize = 14.sp,
                color = HWXTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Notice Box
            Surface(
                color = HWXCardBackground,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HWXCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = HWXCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Please check your inbox and click the verification link to activate your account and enable sending SMS.",
                        color = HWXTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = HWXEmeraldGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HWXEmeraldGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage ?: "",
                        color = HWXEmeraldGreen,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = HWXError.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HWXError.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = HWXError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Resend Email Button
            HWXButton(
                text = "Resend Email",
                onClick = {
                    isResending = true
                    statusMessage = null
                    errorMessage = null
                    scope.launch {
                        val result = authRepository.sendEmailVerification()
                        isResending = false
                        if (result.isSuccess) {
                            statusMessage = "Verification email sent successfully!"
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to resend email"
                        }
                    }
                },
                isLoading = isResending,
                trailingIcon = Icons.Filled.Refresh,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth(),
                testTag = "resend_email_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Check Status Button
            HWXButton(
                text = "Check Verification Status",
                onClick = {
                    isChecking = true
                    statusMessage = null
                    errorMessage = null
                    scope.launch {
                        val isVerified = authRepository.reloadUser()
                        isChecking = false
                        if (isVerified) {
                            onNavigateToDashboard()
                        } else {
                            statusMessage = "Email not yet verified. Please click the link in your email and tap Check again."
                        }
                    }
                },
                isLoading = isChecking,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth(),
                testTag = "check_verification_status_button"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Back to Login",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = HWXCyan,
                modifier = Modifier
                    .clickable(onClick = onNavigateToLogin)
                    .testTag("verify_email_back_to_login")
            )
        }
    }
}
