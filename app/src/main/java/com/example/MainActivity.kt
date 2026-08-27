package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.DrigoTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

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
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userMode by viewModel.userMode.collectAsState()
    val isDriverOnline by viewModel.isDriverOnline.collectAsState()

    // Handle back button presses according to screen stack
    BackHandler(enabled = currentScreen != AppScreen.WELCOME && currentScreen != AppScreen.HOME_PLACEHOLDER && currentScreen != AppScreen.SIGN_IN) {
        when (currentScreen) {
            AppScreen.SIGN_UP -> viewModel.navigateTo(AppScreen.SIGN_IN)
            else -> viewModel.navigateTo(AppScreen.SIGN_IN)
        }
    }

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
                    viewModel.setUserMode(mode)
                },
                onSignOutClick = {
                    viewModel.signOut()
                }
            )
        }
    }
}
