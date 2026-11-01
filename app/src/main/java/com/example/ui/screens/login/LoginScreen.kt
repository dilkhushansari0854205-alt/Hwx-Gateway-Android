package com.example.ui.screens.login

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.ui.components.HWXButton
import com.example.ui.components.HWXTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
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
            // Top Bar with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("login_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HWXTextPrimary
                    )
                }
            }

            // Logo & Title
            Box(
                modifier = Modifier
                    .size(68.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Welcome Back!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HWXTextPrimary
            )
            Text(
                text = "Sign in to your account",
                fontSize = 14.sp,
                color = HWXTextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Glowing Lock Graphic
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(HWXCardBackground)
                    .border(1.5.dp, HWXCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Security Shield",
                    tint = HWXCyan,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            // Inputs
            HWXTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                placeholder = "Email Address",
                leadingIcon = Icons.Filled.Email,
                testTag = "login_email_input"
            )

            Spacer(modifier = Modifier.height(14.dp))

            HWXTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                placeholder = "Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                testTag = "login_password_input"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Remember Me & Forgot Password
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = HWXPrimaryBlue,
                            uncheckedColor = HWXTextMuted,
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        text = "Remember Me",
                        fontSize = 13.sp,
                        color = HWXTextSecondary
                    )
                }

                Text(
                    text = "Forgot Password?",
                    fontSize = 13.sp,
                    color = HWXCyan,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable(onClick = onNavigateToForgotPassword)
                        .testTag("login_forgot_password_link")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sign In Button
            HWXButton(
                text = "Sign In",
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter your email and password"
                        return@HWXButton
                    }
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val result = authRepository.signInWithEmail(email.trim(), password)
                        isLoading = false
                        if (result.isSuccess) {
                            onNavigateToDashboard()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Authentication failed"
                        }
                    }
                },
                isLoading = isLoading,
                trailingIcon = Icons.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth(),
                testTag = "login_sign_in_button"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = HWXCardBorder)
                Text(
                    text = " Or continue with ",
                    fontSize = 12.sp,
                    color = HWXTextMuted,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = HWXCardBorder)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue with Google Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White)
                    .clickable {
                        // Do not silently authenticate users with a demo account.
                        // Google Credential Manager sign-in must be configured before enabling this.
                        errorMessage = "Google sign-in is not configured yet. Please use email and password."
                    }
                    .testTag("login_google_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Google",
                        tint = HWXPrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        color = Color(0xFF1F1F1F),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account? ",
                    fontSize = 14.sp,
                    color = HWXTextSecondary
                )
                Text(
                    text = "Sign Up",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXCyan,
                    modifier = Modifier
                        .clickable(onClick = onNavigateToSignup)
                        .testTag("login_signup_link")
                )
            }
        }
    }
}
