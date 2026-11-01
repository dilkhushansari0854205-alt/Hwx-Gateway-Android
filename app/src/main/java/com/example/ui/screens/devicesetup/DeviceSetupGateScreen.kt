package com.example.ui.screens.devicesetup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DeviceRepository
import com.example.data.repository.AuthRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DeviceSetupGateScreen(
    onReady: () -> Unit,
    onApprovalRequired: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DeviceRepository(context) }
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun startSetup() {
        scope.launch {
            loading = true
            error = null
            authRepository.getFirebaseIdToken()
            val result = repository.initializeAfterLogin()
            loading = false
            if (result.isSuccess) {
                val requestId = result.getOrNull()
                if (requestId.isNullOrBlank()) onReady() else onApprovalRequired(requestId)
            } else {
                error = result.exceptionOrNull()?.message ?: "Device setup failed."
            }
        }
    }

    LaunchedEffect(Unit) { startSetup() }

    Column(
        modifier = Modifier.fillMaxSize().background(HWXBackground).statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(color = HWXCyan)
            Spacer(Modifier.height(16.dp))
            Text("Securing this phone with your HWX account…", color = HWXTextPrimary)
        } else if (error != null) {
            Text("Device setup could not finish", color = HWXTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(error!!, color = HWXError, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { startSetup() }) { Text("Try again", color = HWXCyan) }
            TextButton(onClick = onBackToLogin) { Text("Back to login", color = HWXTextSecondary) }
        }
    }
}
