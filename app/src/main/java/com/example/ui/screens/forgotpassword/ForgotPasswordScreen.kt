package com.example.ui.screens.forgotpassword

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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
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
import com.example.ui.components.HWXTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }

    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
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
                    modifier = Modifier.testTag("forgot_pass_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HWXTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Glowing Lock Reset Icon
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(HWXCardBackground)
                    .border(1.5.dp, HWXCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LockReset,
                    contentDescription = "Reset Password",
                    tint = HWXCyan,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Forgot Password?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HWXTextPrimary
            )
            Text(
                text = "No worries! Enter your email and we'll send you reset link.",
                fontSize = 14.sp,
                color = HWXTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (isSubmitted) {
                Surface(
                    color = HWXEmeraldGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HWXEmeraldGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Text(
                        text = "If an account exists for $email, a password reset link has been dispatched to your inbox.",
                        color = HWXEmeraldGreen,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = HWXError.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HWXError.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = HWXError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            HWXTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                placeholder = "Email Address",
                leadingIcon = Icons.Filled.Email,
                testTag = "forgot_pass_email_input"
            )

            Spacer(modifier = Modifier.height(24.dp))

            HWXButton(
                text = "Send Reset Link",
                onClick = {
                    if (email.isBlank()) {
                        errorMessage = "Please enter your email address"
                        return@HWXButton
                    }
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        authRepository.sendPasswordReset(email.trim())
                        isLoading = false
                        isSubmitted = true
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
                testTag = "forgot_pass_submit_button"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Back to Login",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = HWXCyan,
                modifier = Modifier
                    .clickable(onClick = onNavigateToLogin)
                    .testTag("forgot_pass_back_to_login")
            )
        }
    }
}
