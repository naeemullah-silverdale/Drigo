package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.screens.DriverRegistrationScreen
import com.example.ui.screens.AdminVerificationScreen
import com.example.ui.screens.GoogleDriveDocumentsScreen
import com.example.ui.theme.DrigoTheme
import com.example.util.RideNotificationManager
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.UserMode

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize OsmDroid userAgent configuration before map views load
        try {
            Configuration.getInstance().userAgentValue = packageName
        } catch (_: Exception) {}
        enableEdgeToEdge()
        setContent {
            DrigoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DrigoApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun DrigoApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val notifManager = remember(context) { RideNotificationManager.getInstance(context) }
    val inAppNotification by notifManager.inAppNotification.collectAsState()

    // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userMode by viewModel.userMode.collectAsState()
    val isDriverOnline by viewModel.isDriverOnline.collectAsState()
    val driverVerification by viewModel.driverVerification.collectAsState()

    // Handle back button presses according to screen stack
    BackHandler(enabled = currentScreen != AppScreen.WELCOME && currentScreen != AppScreen.HOME_PLACEHOLDER && currentScreen != AppScreen.SIGN_IN) {
        when (currentScreen) {
            AppScreen.SIGN_UP -> viewModel.navigateTo(AppScreen.SIGN_IN)
            AppScreen.WALLET -> viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
            AppScreen.DRIVER_REGISTRATION -> viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
            AppScreen.GOOGLE_DRIVE_DOCUMENTS -> viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
            else -> viewModel.navigateTo(AppScreen.SIGN_IN)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppScreen.WELCOME -> {
                WelcomeScreen(
                    onTimeout = {
                        if (currentUser != null) {
                            viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
                        } else {
                            viewModel.navigateTo(AppScreen.SIGN_IN)
                        }
                    }
                )
            }
            AppScreen.SIGN_IN -> {
                SignInScreen(
                    onBackClick = {
                        // Stay on Sign In
                    },
                    onNavigateToSignUp = {
                        viewModel.navigateTo(AppScreen.SIGN_UP)
                    },
                    onSignInSuccess = {
                        viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
                    },
                    onSignInWithEmail = { email, password ->
                        viewModel.signIn(email, password)
                    },
                    onSignInWithGoogle = {
                        viewModel.signInWithGoogle(context)
                    },
                    onForgotPassword = { email ->
                        viewModel.sendPasswordReset(email)
                    },
                    onContinueAsGuest = {
                        viewModel.continueAsGuest()
                    }
                )
            }
            AppScreen.SIGN_UP -> {
                SignUpScreen(
                    onBackClick = {
                        viewModel.navigateTo(AppScreen.SIGN_IN)
                    },
                    onNavigateToSignIn = {
                        viewModel.navigateTo(AppScreen.SIGN_IN)
                    },
                    onSignUpSuccess = {
                        viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
                    },
                    onSignUpWithEmail = { name, email, password ->
                        viewModel.signUp(name, email, password)
                    },
                    onSignInWithGoogle = {
                        viewModel.signInWithGoogle(context)
                    },
                    onContinueAsGuest = {
                        viewModel.continueAsGuest()
                    }
                )
            }
            AppScreen.HOME_PLACEHOLDER -> {
                HomeScreen(
                    user = currentUser,
                    userMode = userMode,
                    isDriverOnline = isDriverOnline,
                    onToggleDriverOnline = {
                        viewModel.toggleDriverOnline()
                    },
                    onSwitchUserMode = { mode ->
                        viewModel.attemptSwitchUserMode(mode)
                    },
                    onSignOutClick = {
                        viewModel.signOut()
                    },
                    onNavigateToWallet = {
                        viewModel.navigateTo(AppScreen.WALLET)
                    },
                    onNavigateToGoogleDrive = {
                        viewModel.navigateTo(AppScreen.GOOGLE_DRIVE_DOCUMENTS)
                    },
                    driverVerification = driverVerification
                )
            }
            AppScreen.WALLET -> {
                WalletScreen(
                    user = currentUser,
                    userRole = if (userMode == UserMode.DRIVER) "DRIVER" else "PASSENGER",
                    onBackClick = {
                        viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
                    }
                )
            }
            AppScreen.DRIVER_REGISTRATION -> {
                DriverRegistrationScreen(
                    user = currentUser,
                    existingVerification = driverVerification,
                    onBackToPassenger = {
                        viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
                    },
                    onVerificationCompleted = { ver ->
                        viewModel.updateDriverVerification(ver)
                    },
                    onConfirmedAndSwitchToDriver = {
                        viewModel.setUserMode(UserMode.DRIVER)
                        viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
                    },
                    onNavigateToAdminPortal = {
                        viewModel.navigateTo(AppScreen.ADMIN_VERIFICATION)
                    }
                )
            }
            AppScreen.ADMIN_VERIFICATION -> {
                AdminVerificationScreen(
                    onBackClick = {
                        viewModel.navigateTo(AppScreen.DRIVER_REGISTRATION)
                    }
                )
            }
            AppScreen.GOOGLE_DRIVE_DOCUMENTS -> {
                GoogleDriveDocumentsScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
                    }
                )
            }
        }

        // Global In-App Interactive Notification Banner
        InAppNotificationBanner(
            notification = inAppNotification,
            onDismiss = { notifManager.dismissInAppNotification() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
