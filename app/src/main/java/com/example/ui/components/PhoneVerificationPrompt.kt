package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CountryCodeItem(val code: String, val country: String, val flag: String)

val PopularCountryCodes = listOf(
    CountryCodeItem("+92", "Pakistan", "🇵🇰"),
    CountryCodeItem("+1", "USA / Canada", "🇺🇸"),
    CountryCodeItem("+44", "United Kingdom", "🇬🇧"),
    CountryCodeItem("+91", "India", "🇮🇳"),
    CountryCodeItem("+971", "UAE", "🇦🇪"),
    CountryCodeItem("+966", "Saudi Arabia", "🇸🇦"),
    CountryCodeItem("+61", "Australia", "🇦🇺"),
    CountryCodeItem("+49", "Germany", "🇩🇪"),
    CountryCodeItem("+234", "Nigeria", "🇳🇬"),
    CountryCodeItem("+254", "Kenya", "🇰🇪"),
    CountryCodeItem("+27", "South Africa", "🇿🇦"),
    CountryCodeItem("+62", "Indonesia", "🇮🇩"),
    CountryCodeItem("+880", "Bangladesh", "🇧🇩")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneVerificationPrompt(
    onDismiss: () -> Unit,
    onVerificationSuccess: () -> Unit,
    onSendOtp: (
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken?
    ) -> Unit,
    onVerifyOtp: suspend (verificationId: String, code: String, phoneNumber: String) -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var selectedCountry by remember { mutableStateOf(PopularCountryCodes.first()) }
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1 = Phone Number Input, 2 = 6-digit OTP Code Input

    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var showCountryDropdown by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(60) }

    val otpFocusRequester = remember { FocusRequester() }

    // Countdown timer effect for Step 2 (OTP Entry)
    LaunchedEffect(step, timerSeconds) {
        if (step == 2 && timerSeconds > 0) {
            delay(1000L)
            timerSeconds -= 1
        }
    }

    fun cleanPhoneNumber(): String {
        val raw = phoneInput.trim().replace(" ", "").replace("-", "")
        val formattedNumber = if (raw.startsWith("0")) raw.substring(1) else raw
        return "${selectedCountry.code}$formattedNumber"
    }

    val callbacks = remember(activity) {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                val autoCode = credential.smsCode
                if (!autoCode.isNullOrBlank()) {
                    otpInput = autoCode
                }
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    val fullPhone = cleanPhoneNumber()
                    val verId = verificationId ?: ""
                    val codeToUse = autoCode ?: otpInput
                    val res = onVerifyOtp(verId, codeToUse, fullPhone)
                    isLoading = false
                    if (res.isSuccess) {
                        successMessage = "Phone number verified successfully!"
                        delay(1200L)
                        onVerificationSuccess()
                    } else {
                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to verify phone code."
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                val msg = e.localizedMessage ?: e.message ?: ""
                errorMessage = when {
                    msg.contains("invalid", ignoreCase = true) -> "Invalid phone number or SMS verification failed."
                    msg.contains("too-many-requests", ignoreCase = true) || msg.contains("quota", ignoreCase = true) -> "SMS rate limit reached. Please wait a few minutes."
                    msg.contains("network", ignoreCase = true) -> "Network error. Please check your internet connection."
                    else -> if (msg.isNotBlank()) msg else "Verification failed. Please check the phone number."
                }
            }

            override fun onCodeSent(
                verId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                isLoading = false
                verificationId = verId
                resendToken = token
                step = 2
                timerSeconds = 60
                errorMessage = null
                successMessage = "SMS code sent successfully!"
            }
        }
    }

    fun requestSmsCode() {
        val rawNumber = phoneInput.trim().replace(" ", "").replace("-", "")
        if (rawNumber.isBlank()) {
            errorMessage = "Please enter your mobile phone number"
            return
        }
        if (rawNumber.length < 7) {
            errorMessage = "Please enter a valid phone number"
            return
        }
        if (activity == null) {
            errorMessage = "Unable to process phone verification on this device."
            return
        }

        errorMessage = null
        successMessage = null
        isLoading = true
        val fullPhone = cleanPhoneNumber()
        onSendOtp(fullPhone, activity, callbacks, resendToken)
    }

    fun submitOtpVerification() {
        val cleanOtp = otpInput.trim()
        if (cleanOtp.length < 6) {
            errorMessage = "Please enter the complete 6-digit OTP code"
            return
        }
        val verId = verificationId
        if (verId.isNullOrBlank()) {
            errorMessage = "Verification session expired. Please request a new SMS code."
            return
        }

        errorMessage = null
        successMessage = null
        isLoading = true
        coroutineScope.launch {
            val fullPhone = cleanPhoneNumber()
            val res = onVerifyOtp(verId, cleanOtp, fullPhone)
            isLoading = false
            if (res.isSuccess) {
                successMessage = "Phone number verified successfully!"
                delay(1200L)
                onVerificationSuccess()
            } else {
                errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Invalid OTP code. Please try again."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("phone_verification_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Shield Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (step == 1) Icons.Default.PhoneAndroid else Icons.Default.MarkEmailRead,
                        contentDescription = "Phone Verification",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (step == 1) "Verify Phone Number" else "Enter OTP Code",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (step == 1)
                        "To protect your Drigo account and receive SMS trip notifications, please verify your mobile number."
                    else
                        "Enter the 6-digit code sent via SMS to ${cleanPhoneNumber()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Error Message Banner
                AnimatedVisibility(visible = errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Success Message Banner
                AnimatedVisibility(visible = successMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (step == 1) {
                    // STEP 1: Phone Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Code Picker Box
                        Box {
                            OutlinedCard(
                                onClick = { showCountryDropdown = true },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("country_code_selector")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedCountry.flag} ${selectedCountry.code}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Country Code"
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showCountryDropdown,
                                onDismissRequest = { showCountryDropdown = false }
                            ) {
                                PopularCountryCodes.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${item.flag} ${item.country} (${item.code})")
                                        },
                                        onClick = {
                                            selectedCountry = item
                                            showCountryDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Phone Number Input Text Field
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { input ->
                                phoneInput = input.filter { it.isDigit() || it == ' ' }
                            },
                            placeholder = { Text("300 1234567") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    requestSmsCode()
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("phone_number_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { requestSmsCode() },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_otp_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending SMS...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send SMS Verification Code")
                        }
                    }
                } else {
                    // STEP 2: OTP Code Input
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { input ->
                            if (input.length <= 6) {
                                otpInput = input.filter { it.isDigit() }
                                if (otpInput.length == 6) {
                                    focusManager.clearFocus()
                                    submitOtpVerification()
                                }
                            }
                        },
                        label = { Text("6-Digit OTP Code") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                submitOtpVerification()
                            }
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(otpFocusRequester)
                            .testTag("otp_code_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6 Visual Digit Boxes Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (0..5).forEach { index ->
                            val digit = otpInput.getOrNull(index)?.toString() ?: ""
                            val isFocusedDigit = otpInput.length == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isFocusedDigit)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = if (isFocusedDigit) 2.dp else 1.dp,
                                        color = if (isFocusedDigit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { submitOtpVerification() },
                        enabled = !isLoading && otpInput.length == 6,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_otp_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify & Continue")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                step = 1
                                otpInput = ""
                                errorMessage = null
                            }
                        ) {
                            Text("Change Number", style = MaterialTheme.typography.bodySmall)
                        }

                        TextButton(
                            onClick = { requestSmsCode() },
                            enabled = timerSeconds == 0 && !isLoading
                        ) {
                            Text(
                                text = if (timerSeconds > 0) "Resend in ${timerSeconds}s" else "Resend SMS",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (timerSeconds == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Dismiss / Skip for now Option
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("skip_phone_verification_button")
                ) {
                    Text(
                        text = "Verify Later / Skip for Now",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
