package com.example.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.components.FloatingBottomBar
import com.example.ui.components.QuickActionBottomSheet
import com.example.ui.screens.apikeys.ApiKeysScreen
import com.example.ui.screens.apikeys.GenerateKeyDialog
import com.example.ui.screens.bots.BotsScreen
import com.example.ui.screens.bots.CreateBotDialog
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.devices.DevicesScreen
import com.example.ui.screens.deviceapproval.DeviceApprovalScreen
import com.example.ui.screens.devicesetup.DeviceSetupGateScreen
import com.example.ui.screens.emailverification.EmailVerificationScreen
import com.example.ui.screens.forgotpassword.ForgotPasswordScreen
import com.example.ui.screens.login.LoginScreen
import com.example.ui.screens.more.MoreScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.sims.SimManagementScreen
import com.example.ui.screens.signup.SignupScreen
import com.example.ui.screens.sms.SendSmsDialog
import com.example.ui.screens.sms.SmsLogsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.welcome.WelcomeScreen
import com.example.data.repository.BotRepository
import com.example.data.repository.SmsRepository
import com.example.data.repository.ApiKeyRepository
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val FORGOT_PASSWORD = "forgot_password"
    const val EMAIL_VERIFICATION = "email_verification/{email}"
    const val DEVICE_SETUP = "device_setup"
    const val DEVICE_APPROVAL = "device_approval/{requestId}"
    const val DASHBOARD = "dashboard"
    const val DEVICES = "devices"
    const val SIMS = "sims?deviceId={deviceId}"
    const val BOTS = "bots"
    const val SMS_LOGS = "sms_logs"
    const val API_KEYS = "api_keys"
    const val WEBHOOKS = "webhooks"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val MORE = "more"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.SPLASH

    var showQuickActionSheet by remember { mutableStateOf(false) }
    var showQuickCreateBot by remember { mutableStateOf(false) }
    var showQuickSendSms by remember { mutableStateOf(false) }
    var showQuickGenerateKey by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val botRepo = remember { BotRepository(context) }
    val smsRepo = remember { SmsRepository(context) }
    val apiKeyRepo = remember { ApiKeyRepository(context) }

    val unauthenticatedRoutes = listOf(
        Routes.SPLASH,
        Routes.WELCOME,
        Routes.LOGIN,
        Routes.SIGNUP,
        Routes.FORGOT_PASSWORD,
        Routes.EMAIL_VERIFICATION,
        Routes.DEVICE_SETUP,
        Routes.DEVICE_APPROVAL
    )

    val showBottomBar = !unauthenticatedRoutes.any { unauthRoute ->
        currentRoute.startsWith(unauthRoute.substringBefore("/{"))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Routes.DEVICE_SETUP) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToWelcome = {
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.WELCOME) {
                WelcomeScreen(
                    onGetStartedClick = { navController.navigate(Routes.LOGIN) },
                    onLiveDemoClick = {
                        // Sandbox access is disabled; require real Firebase authentication.
                        navController.navigate(Routes.LOGIN)
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDashboard = {
                        navController.navigate(Routes.DEVICE_SETUP) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
                    onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
                )
            }

            composable(Routes.SIGNUP) {
                SignupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEmailVerification = { email ->
                        navController.navigate("email_verification/$email")
                    },
                    onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
                )
            }

            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
                )
            }

            composable(
                route = Routes.EMAIL_VERIFICATION,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                EmailVerificationScreen(
                    email = email,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDashboard = {
                        navController.navigate(Routes.DEVICE_SETUP) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
                )
            }

            composable(Routes.DEVICE_SETUP) {
                DeviceSetupGateScreen(
                    onReady = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.DEVICE_SETUP) { inclusive = true }
                        }
                    },
                    onApprovalRequired = { requestId ->
                        navController.navigate("device_approval/$requestId") {
                            popUpTo(Routes.DEVICE_SETUP) { inclusive = true }
                        }
                    },
                    onBackToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.DEVICE_SETUP) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.DEVICE_APPROVAL,
                arguments = listOf(navArgument("requestId") { type = NavType.StringType })
            ) { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId") ?: return@composable
                DeviceApprovalScreen(
                    requestId = requestId,
                    onNavigateToDashboard = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.DEVICE_APPROVAL) { inclusive = true }
                        }
                    }
                )
            }

            // Authenticated Destinations
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigateToDevices = { navController.navigate(Routes.DEVICES) },
                    onNavigateToBots = { navController.navigate(Routes.BOTS) },
                    onNavigateToSmsLogs = { navController.navigate(Routes.SMS_LOGS) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                    onNavigateToSims = { navController.navigate("sims") },
                    onMenuClick = { navController.navigate(Routes.MORE) }
                )
            }

            composable(Routes.DEVICES) {
                DevicesScreen(
                    onNavigateBack = { navController.navigate(Routes.DASHBOARD) },
                    onNavigateToSims = { devId -> navController.navigate("sims?deviceId=$devId") }
                )
            }

            composable(
                route = Routes.SIMS,
                arguments = listOf(navArgument("deviceId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId")
                SimManagementScreen(
                    deviceId = deviceId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.BOTS) {
                BotsScreen(
                    onNavigateBack = { navController.navigate(Routes.DASHBOARD) }
                )
            }

            composable(Routes.SMS_LOGS) {
                SmsLogsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.API_KEYS) {
                ApiKeysScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.WEBHOOKS) {
                com.example.ui.screens.webhooks.WebhooksScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLogoutSuccess = {
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.MORE) {
                MoreScreen(
                    onNavigateToSims = { navController.navigate("sims") },
                    onNavigateToSmsLogs = { navController.navigate(Routes.SMS_LOGS) },
                    onNavigateToApiKeys = { navController.navigate(Routes.API_KEYS) },
                    onNavigateToWebhooks = { navController.navigate(Routes.WEBHOOKS) },
                    onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
        }

        // Floating Bottom Navigation Bar
        if (showBottomBar) {
            FloatingBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCentralPlusClick = { showQuickActionSheet = true }
            )
        }

        // Quick Action Bottom Sheet
        if (showQuickActionSheet) {
            QuickActionBottomSheet(
                onDismiss = { showQuickActionSheet = false },
                onAddDeviceClick = { navController.navigate(Routes.DEVICES) },
                onCreateBotClick = { showQuickCreateBot = true },
                onSendSmsClick = { showQuickSendSms = true },
                onGenerateApiKeyClick = { showQuickGenerateKey = true }
            )
        }

        if (showQuickCreateBot) {
            CreateBotDialog(
                onDismiss = { showQuickCreateBot = false },
                onConfirm = { req ->
                    scope.launch {
                        botRepo.createBot(req)
                        showQuickCreateBot = false
                        navController.navigate(Routes.BOTS)
                    }
                }
            )
        }

        if (showQuickSendSms) {
            SendSmsDialog(
                onDismiss = { showQuickSendSms = false },
                onConfirm = { req ->
                    scope.launch {
                        smsRepo.sendSms(req)
                        showQuickSendSms = false
                        navController.navigate(Routes.SMS_LOGS)
                    }
                }
            )
        }

        if (showQuickGenerateKey) {
            GenerateKeyDialog(
                onDismiss = { showQuickGenerateKey = false },
                onConfirm = { name ->
                    scope.launch {
                        apiKeyRepo.createApiKey(name)
                        showQuickGenerateKey = false
                        navController.navigate(Routes.API_KEYS)
                    }
                }
            )
        }
    }
}
