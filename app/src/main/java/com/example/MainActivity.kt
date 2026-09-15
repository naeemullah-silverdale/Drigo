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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.screens.DriverRegistrationScreen
import com.example.ui.screens.AdminVerificationScreen
import com.example.ui.screens.GoogleDriveDocumentsScreen
import com.example.ui.screens.TripHistoryScreen
import com.example.ui.theme.DrigoTheme
import com.example.util.RideNotificationManager
import com.example.util.ThemeManager
import com.example.util.ThemeMode
import com.example.data.remote.FirebaseRepository
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.UserMode

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ThemeManager.init(applicationContext)
        } catch (_: Exception) {}
        try {
            FirebaseRepository.getInstance(applicationContext)
        } catch (_: Exception) {}
        handleNotificationIntent(intent)
        // Initialize OsmDroid userAgent configuration before map views load
        try {
            Configuration.getInstance().userAgentValue = packageName
        } catch (_: Exception) {}
        enableEdgeToEdge()
        setContent {
            val themeMode by ThemeManager.themeMode.collectAsState()
            DrigoTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DrigoApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra("OPEN_DRIVER_MODE", false) == true) {
            viewModel.setUserMode(UserMode.DRIVER)
            viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
        } else if (intent?.getBooleanExtra("OPEN_PASSENGER_MODE", false) == true) {
            viewModel.setUserMode(UserMode.PASSENGER)
            viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
        }
    }
}

@Composable
fun DrigoApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val notifManager = remember(context) { RideNotificationManager.getInstance(context) }

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
    val isRoleLoaded by viewModel.isRoleLoaded.collectAsState()
    val isDriverOnline by viewModel.isDriverOnline.collectAsState()
    val driverVerification by viewModel.driverVerification.collectAsState()
    val liveRideRequests by viewModel.liveRideRequests.collectAsState()
    val userRecord by viewModel.userRecord.collectAsState()

    // Handle top-level back button presses according to navigation stack
    val canNavigateBack = viewModel.canNavigateBack()
    BackHandler(enabled = canNavigateBack) {
        viewModel.popBackStack()
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
                        viewModel.popBackStack()
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
                        viewModel.popBackStack()
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
                if (!isRoleLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
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
                        onNavigateToTripHistory = {
                            viewModel.navigateTo(AppScreen.HISTORY)
                        },
                        driverVerification = driverVerification,
                        liveRideRequests = liveRideRequests,
                        onRefreshDriverRideRequests = { viewModel.refreshDriverRideRequests() }
                    )
                }
            }
            AppScreen.WALLET -> {
                WalletScreen(
                    user = currentUser,
                    userRole = if (userMode == UserMode.DRIVER) "DRIVER" else "PASSENGER",
                    onBackClick = {
                        viewModel.popBackStack()
                    }
                )
            }
            AppScreen.HISTORY -> {
                TripHistoryScreen(
                    user = currentUser,
                    initialUserMode = userMode,
                    onBackClick = {
                        viewModel.popBackStack()
                    },
                    onRebookTrip = {
                        viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
                    }
                )
            }
            AppScreen.DRIVER_REGISTRATION -> {
                DriverRegistrationScreen(
                    user = currentUser,
                    existingVerification = driverVerification,
                    onBackToPassenger = {
                        viewModel.setUserMode(UserMode.PASSENGER)
                        if (!viewModel.popBackStack()) {
                            viewModel.navigateTo(AppScreen.HOME_PLACEHOLDER)
                        }
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
                        viewModel.popBackStack()
                    }
                )
            }
            AppScreen.GOOGLE_DRIVE_DOCUMENTS -> {
                GoogleDriveDocumentsScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        viewModel.popBackStack()
                    }
                )
            }
        }
    }
}
