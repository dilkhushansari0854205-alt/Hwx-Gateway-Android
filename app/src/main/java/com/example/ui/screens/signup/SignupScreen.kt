package com.example.ui.screens.signup

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.ui.components.HWXButton
import com.example.ui.components.HWXTextField
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
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
                    modifier = Modifier.testTag("signup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HWXTextPrimary
                    )
                }
            }

            // User Plus Icon Illustration
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(HWXCardBackground)
                    .border(1.5.dp, HWXCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Create Account",
                    tint = HWXCyan,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Create Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HWXTextPrimary
            )
            Text(
                text = "Join HWX SMS Gateway network",
                fontSize = 14.sp,
                color = HWXTextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

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

            // Form Inputs
            HWXTextField(
                value = fullName,
                onValueChange = { fullName = it; errorMessage = null },
                placeholder = "Full Name",
                leadingIcon = Icons.Filled.Person,
                testTag = "signup_name_input"
            )

            Spacer(modifier = Modifier.height(12.dp))

            HWXTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                placeholder = "Email Address",
                leadingIcon = Icons.Filled.Email,
                testTag = "signup_email_input"
            )

            Spacer(modifier = Modifier.height(12.dp))

            HWXTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                placeholder = "Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                testTag = "signup_password_input"
            )

            Spacer(modifier = Modifier.height(12.dp))

            HWXTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                placeholder = "Confirm Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                testTag = "signup_confirm_password_input"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Button
            HWXButton(
                text = "Sign Up",
                onClick = {
                    when {
                        fullName.isBlank() -> errorMessage = "Please enter your full name"
                        email.isBlank() -> errorMessage = "Please enter a valid email address"
                        password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                        password != confirmPassword -> errorMessage = "Passwords do not match"
                        else -> {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                val result = authRepository.signUpWithEmail(fullName.trim(), email.trim(), password)
                                isLoading = false
                                if (result.isSuccess) {
                                    onNavigateToEmailVerification(email.trim())
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                                }
                            }
                        }
                    }
                },
                isLoading = isLoading,
                trailingIcon = Icons.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth(),
                testTag = "signup_submit_button"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Back to Login link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have an account? ",
                    fontSize = 14.sp,
                    color = HWXTextSecondary
                )
                Text(
                    text = "Sign In",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HWXCyan,
                    modifier = Modifier
                        .clickable(onClick = onNavigateToLogin)
                        .testTag("signup_login_link")
                )
            }
        }
    }
}
