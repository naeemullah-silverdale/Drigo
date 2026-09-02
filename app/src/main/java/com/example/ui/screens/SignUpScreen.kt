package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DrigoBrandPurple
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onBackClick: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onSignUpSuccess: () -> Unit,
    onSignUpWithEmail: suspend (String, String, String) -> Result<Unit>,
    onSignInWithGoogle: suspend () -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun performSignUp() {
        if (fullName.isBlank()) {
            errorMessage = "Please enter your full name"
            return
        }
        if (email.isBlank()) {
            errorMessage = "Please enter your email address"
            return
        }
        if (password.isBlank()) {
            errorMessage = "Please enter a password"
            return
        }
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match"
            return
        }

        errorMessage = null
        isLoading = true
        coroutineScope.launch {
            val result = onSignUpWithEmail(fullName.trim(), email.trim(), password)
            isLoading = false
            if (result.isSuccess) {
                onSignUpSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to create account. Please try again."
            }
        }
    }

    fun performGoogleSignUp() {
        errorMessage = null
        isGoogleLoading = true
        coroutineScope.launch {
            val result = onSignInWithGoogle()
            isGoogleLoading = false
            if (result.isSuccess) {
                onSignUpSuccess()
            } else {
                val msg = result.exceptionOrNull()?.localizedMessage ?: "Google sign up failed"
                if (!msg.contains("cancelled", ignoreCase = true)) {
                    errorMessage = msg
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("sign_up_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DrigoBrandPurple.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DrigoBrandPurple,
                shadowElevation = 6.dp,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Drigo",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Join Drigo to get moving",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Google Sign-Up Primary Option Button
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    performGoogleSignUp()
                },
                enabled = !isLoading && !isGoogleLoading,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_signup_btn")
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(
                        color = DrigoBrandPurple,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider: OR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "or create with email",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Name Input
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    errorMessage = null
                },
                label = { Text("Full Name") },
                placeholder = { Text("John Doe") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Full Name",
                        tint = DrigoBrandPurple
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DrigoBrandPurple,
                    focusedLabelColor = DrigoBrandPurple,
                    cursorColor = DrigoBrandPurple
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_name_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("Email Address") },
                placeholder = { Text("example@drigo.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = DrigoBrandPurple
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DrigoBrandPurple,
                    focusedLabelColor = DrigoBrandPurple,
                    cursorColor = DrigoBrandPurple
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_email_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password (min 6 characters)") },
                placeholder = { Text("••••••••") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password",
                        tint = DrigoBrandPurple
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DrigoBrandPurple,
                    focusedLabelColor = DrigoBrandPurple,
                    cursorColor = DrigoBrandPurple
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_password_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm Password Input
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text("Confirm Password") },
                placeholder = { Text("••••••••") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Confirm Password",
                        tint = DrigoBrandPurple
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        performSignUp()
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DrigoBrandPurple,
                    focusedLabelColor = DrigoBrandPurple,
                    cursorColor = DrigoBrandPurple
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Primary Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    performSignUp()
                },
                enabled = !isLoading && !isGoogleLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DrigoBrandPurple,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("signup_submit_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Sign Up",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Back to Sign In Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = DrigoBrandPurple,
                    modifier = Modifier
                        .clickable { onNavigateToSignIn() }
                        .testTag("navigate_signin_btn")
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
