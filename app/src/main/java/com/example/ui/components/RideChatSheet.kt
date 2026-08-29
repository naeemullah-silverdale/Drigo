package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.remote.FirebaseRepository
import com.example.ui.theme.DrigoBrandPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern real-time in-ride chat sheet for Driver-Passenger communication.
 * Allows passenger to message driver and driver to message passenger once booking is confirmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideChatSheet(
    tripId: String,
    currentUserId: String,
    currentUserName: String,
    isDriver: Boolean,
    partnerName: String,
    partnerRole: String = if (isDriver) "Passenger" else "Driver",
    partnerPhone: String = "+92 300 1234567",
    pickupTitle: String = "Pickup Location",
    destinationTitle: String = "Destination",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    val firebaseRepo = remember { FirebaseRepository.getInstance(context) }
    var messageInputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Live messages list
    var messages by remember { mutableStateOf<List<ChatMessageEntity>>(emptyList()) }

    // Initialize with a welcome system notice if empty
    val initialSystemNotice = remember(tripId) {
        ChatMessageEntity(
            id = "sys_notice_${tripId.takeLast(6)}",
            tripId = tripId,
            senderId = "system",
            senderName = "Drigo Safety",
            isDriver = false,
            messageText = "Ride confirmed! You are now connected with ${if (isDriver) "passenger $partnerName" else "driver $partnerName"}. Chat is active for trip coordination.",
            timestamp = System.currentTimeMillis() - 10000L,
            isRead = true,
            isSystemNotice = true
        )
    }

    // Subscribe to Firebase Realtime / Firestore live stream for this trip/ride
    LaunchedEffect(tripId) {
        // Load any cached local messages first
        launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context, this)
                db.chatDao().getMessagesForTrip(tripId).collectLatest { localMsgs ->
                    if (localMsgs.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            val combined = (listOf(initialSystemNotice) + localMsgs).distinctBy { it.id }.sortedBy { it.timestamp }
                            messages = combined
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Collect live stream from Firebase
        firebaseRepo.listenToCloudMessages(tripId).collectLatest { cloudMsgs ->
            val allList = if (cloudMsgs.isEmpty()) {
                listOf(initialSystemNotice)
            } else {
                (listOf(initialSystemNotice) + cloudMsgs).distinctBy { it.id }.sortedBy { it.timestamp }
            }
            messages = allList

            // Auto scroll to bottom
            if (allList.isNotEmpty()) {
                scope.launch {
                    listState.animateScrollToItem(allList.size - 1)
                }
            }
        }
    }

    // Function to send message
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || isSending) return

        val newMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            senderId = currentUserId,
            senderName = currentUserName.ifBlank { if (isDriver) "Driver" else "Passenger" },
            isDriver = isDriver,
            messageText = trimmed,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            isSystemNotice = false
        )

        // Optimistically add to list
        messages = (messages + newMsg).distinctBy { it.id }.sortedBy { it.timestamp }
        messageInputText = ""
        isSending = true

        scope.launch {
            // Save to local Room DB
            try {
                val db = AppDatabase.getDatabase(context, this)
                db.chatDao().insertMessage(newMsg)
            } catch (_: Exception) {}

            // Push to Firebase Realtime Database & Firestore
            firebaseRepo.pushChatMessageToCloud(newMsg)
            isSending = false

            // Scroll to bottom
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Quick suggestions based on role
    val quickSuggestions = remember(isDriver) {
        if (isDriver) {
            listOf(
                "I'm on my way! 🚗",
                "I have arrived at pickup 📍",
                "Stuck in 2 min traffic ⏳",
                "Near the main gate",
                "What color shirt are you wearing?",
                "OK, see you shortly!"
            )
        } else {
            listOf(
                "I'm at the pickup spot 📍",
                "Coming down in 1 minute 🚶",
                "Which car color & number?",
                "Please turn on AC ❄️",
                "Waiting near main entrance",
                "Thank you!"
            )
        }
    }

    Surface(
        color = Color(0xFF1E2128),
        modifier = Modifier
            .fillMaxSize()
            .testTag("ride_chat_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // --- TOP APP BAR ---
            Surface(
                color = Color(0xFF282B33),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("chat_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Avatar
                    Surface(
                        shape = CircleShape,
                        color = if (isDriver) Color(0xFF4CAF50) else DrigoBrandPurple,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDriver) Icons.Default.Person else Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Partner Info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = partnerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DrigoBrandPurple.copy(alpha = 0.35f),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = partnerRole.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = Color(0xFFE1BEE7),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.size(7.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Active Booking • Connected",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB0BEC5),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Direct Call Button
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$partnerPhone")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DrigoBrandPurple.copy(alpha = 0.25f))
                            .testTag("chat_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call $partnerRole",
                            tint = Color(0xFFCE93D8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // --- TRIP SUMMARY CHIP BANNER ---
            Surface(
                color = Color(0xFF21242C),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pickupTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCFD8DC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " → ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF90A4AE)
                        )
                        Text(
                            text = destinationTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCFD8DC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            // --- MESSAGES LIST ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    if (msg.isSystemNotice) {
                        SystemNoticeItem(message = msg.messageText)
                    } else {
                        val isMyMessage = (msg.isDriver == isDriver) || (msg.senderId == currentUserId)
                        MessageBubbleItem(
                            message = msg,
                            isMyMessage = isMyMessage
                        )
                    }
                }
            }

            // --- QUICK SUGGESTIONS ROW ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF282B33).copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickSuggestions) { chipText ->
                    Surface(
                        onClick = { sendMessage(chipText) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF333742),
                        border = BorderStroke(1.dp, Color(0xFF4A4E5C)),
                        modifier = Modifier.testTag("quick_reply_chip")
                    ) {
                        Text(
                            text = chipText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0E0E0),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // --- INPUT BAR ---
            Surface(
                color = Color(0xFF282B33),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageInputText,
                        onValueChange = { messageInputText = it },
                        placeholder = {
                            Text(
                                text = "Message $partnerName...",
                                color = Color(0xFF888E9E),
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E2128),
                            unfocusedContainerColor = Color(0xFF1E2128),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = DrigoBrandPurple,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                sendMessage(messageInputText)
                                keyboardController?.hide()
                            }
                        ),
                        singleLine = false,
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = messageInputText.isNotBlank() && !isSending
                    IconButton(
                        onClick = {
                            sendMessage(messageInputText)
                            keyboardController?.hide()
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (canSend) DrigoBrandPurple else Color(0xFF424754))
                            .testTag("send_chat_btn")
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = if (canSend) Color.White else Color(0xFF90A4AE),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubbleItem(
    message: ChatMessageEntity,
    isMyMessage: Boolean
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    val bubbleColor = if (isMyMessage) DrigoBrandPurple else Color(0xFF333742)
    val textColor = Color.White
    val shape = if (isMyMessage) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
    ) {
        // Sender Name badge if not my message
        if (!isMyMessage) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (message.isDriver) Color(0xFF81C784) else Color(0xFFCE93D8),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (message.isDriver) "• Driver" else "• Passenger",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF90A4AE),
                    fontSize = 10.sp
                )
            }
        }

        Surface(
            shape = shape,
            color = bubbleColor,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMyMessage) Color.White.copy(alpha = 0.7f) else Color(0xFFB0BEC5),
                        fontSize = 10.sp
                    )
                    if (isMyMessage) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemNoticeItem(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF282B33),
            border = BorderStroke(1.dp, Color(0xFF3F4452)),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = DrigoBrandPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCFD8DC),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
