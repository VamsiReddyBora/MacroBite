package com.macrobite.app.ui.chat

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.macrobite.app.domain.model.ContactCandidate
import com.macrobite.app.domain.model.JarvisActionPayload
import com.macrobite.app.domain.model.JarvisActionType
import com.macrobite.app.ui.chat.jarvis.JarvisActionDispatcher
import com.macrobite.app.ui.chat.jarvis.JarvisDispatchResult
import com.macrobite.app.ui.chat.voice.InAppSpeechRecognizer
import com.macrobite.app.ui.scanner.BarcodeNutritionScannerDialog
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Matrix
import android.graphics.SweepGradient
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.macrobite.app.ui.common.ModernFloatingSnackbarHost
import com.macrobite.app.ui.common.copilotRotatingBorder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.macrobite.app.R
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.formatMacroOneDecimal
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.FatsCoral
import com.macrobite.app.ui.theme.ProteinBlue
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    isPageActive: Boolean = true,
    widgetAction: String? = null,
    onWidgetActionHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val selectedChatDate by viewModel.selectedChatDate.collectAsState()
    val chatHistoryDates by viewModel.chatHistoryDates.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val customModels by viewModel.customModels.collectAsState()
    val allChatModels = remember(customModels) {
        availableChatModels + customModels.map { ChatModelOption(it, "$it (Custom)") }
    }
    val raayaName by viewModel.raayaName.collectAsState()
    val raayaAvatar by viewModel.raayaAvatar.collectAsState()
    val raayaWebSearchEnabled by viewModel.raayaWebSearchEnabled.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var pendingImagePath by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showHistorySidebar by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var showRaayaProfileScreen by remember { mutableStateOf(false) }
    var dateToDelete by remember { mutableStateOf<String?>(null) }
    var showClearCurrentDialog by remember { mutableStateOf(false) }

    val isToday = selectedChatDate == AppDate.todayIso()

    var showPhotoChoiceDialog by remember { mutableStateOf(false) }
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val localPath = copyUriToInternalStorage(context, tempCameraUri!!)
            if (localPath != null) {
                pendingImagePath = localPath
            }
        }
    }

    val launchCameraAction = {
        try {
            val photoFile = File(context.cacheDir, "chat_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (_: SecurityException) {
            // Handled gracefully
        } catch (_: Throwable) {
            // Handled gracefully
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraAction()
        }
    }

    val requestCameraAndLaunch = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCameraAction()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = copyUriToInternalStorage(context, uri)
            if (localPath != null) {
                pendingImagePath = localPath
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    var lastInputWasVoice by remember { mutableStateOf(false) }

    // In-App Speech Recognizer (microphone runs directly in-app with soothing ripple UI, avoiding external Google popup)
    val speechRecognizer = remember {
        InAppSpeechRecognizer(
            context = context,
            onResult = { spokenText ->
                if (spokenText.isNotBlank()) {
                    lastInputWasVoice = true
                    viewModel.sendMessage(spokenText, pendingImagePath)
                    inputText = ""
                    pendingImagePath = null
                    coroutineScope.launch {
                        delay(50)
                        val target = messages.size.coerceAtLeast(0)
                        listState.animateScrollToItem(target, scrollOffset = 10000)
                    }
                }
            },
            onErrorCallback = { errorMsg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(errorMsg)
                }
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.stopSpeaking()
                speechRecognizer.stopListening()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speechRecognizer.destroy()
            viewModel.stopSpeaking()
        }
    }

    LaunchedEffect(isPageActive) {
        if (!isPageActive) {
            viewModel.stopSpeaking()
            speechRecognizer.stopListening()
        }
    }

    LaunchedEffect(showRaayaProfileScreen) {
        if (showRaayaProfileScreen) {
            viewModel.stopSpeaking()
            speechRecognizer.stopListening()
        }
    }

    val isListening by speechRecognizer.isListening.collectAsState()
    val voiceStyle by viewModel.voiceStyle.collectAsState()
    val replyStyle by viewModel.replyStyle.collectAsState()
    val liveSpokenText by speechRecognizer.liveSpokenText.collectAsState()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            speechRecognizer.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    val startVoiceInput = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            speechRecognizer.startListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is ChatUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    LaunchedEffect(widgetAction, isPageActive) {
        if (isPageActive && widgetAction != null) {
            when (widgetAction) {
                "focus_input" -> {
                    delay(200)
                    try {
                        focusRequester.requestFocus()
                    } catch (_: Throwable) {}
                    onWidgetActionHandled()
                }
                "voice" -> {
                    delay(200)
                    startVoiceInput()
                    onWidgetActionHandled()
                }
                "camera" -> {
                    delay(200)
                    showPhotoChoiceDialog = true
                    onWidgetActionHandled()
                }
                else -> {
                    onWidgetActionHandled()
                }
            }
        }
    }

    // Auto-read assistant response aloud if query was spoken via voice
    LaunchedEffect(messages.size) {
        val lastMsg = messages.lastOrNull()
        if (lastMsg != null && !lastMsg.isUser && lastInputWasVoice && lastMsg.text.isNotBlank()) {
            lastInputWasVoice = false
            viewModel.speak(lastMsg.text)
        }
    }

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val lastMessage = messages.lastOrNull()
    val lastMessageId = lastMessage?.id
    val lastMessageTextLength = lastMessage?.text?.length ?: 0
    val totalItemCount = messages.size + if (isGenerating) 1 else 0

    LaunchedEffect(messages.size, lastMessageId, lastMessageTextLength, isGenerating, isImeVisible, isPageActive) {
        if (totalItemCount > 0 && isPageActive) {
            val targetIndex = totalItemCount - 1
            // 1. Immediate scroll towards bottom of latest item
            listState.animateScrollToItem(targetIndex, scrollOffset = 10000)
            // 2. Short delay for Compose layout to measure markdown & cards
            delay(100)
            listState.animateScrollToItem(targetIndex, scrollOffset = 10000)
            // 3. Follow-up pass ensuring bottom is completely reached after async image/subcompose
            delay(200)
            listState.animateScrollToItem(targetIndex, scrollOffset = 10000)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showRaayaProfileScreen) {
            RaayaProfileScreen(
                viewModel = viewModel,
                onBack = { showRaayaProfileScreen = false }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showRaayaProfileScreen = true }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    // Dynamic avatar for Raaya
                                    RaayaAvatarImage(
                                        avatar = raayaAvatar,
                                    contentDescription = raayaName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), CircleShape)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = raayaName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                letterSpacing = 0.2.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_tune),
                                            contentDescription = "AI Profile Settings",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    if (!isToday) {
                                        Text(
                                            text = AppDate.fromIso(selectedChatDate).toDisplayTitle(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "Tap to customize AI",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Normal
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // Three lines menu icon to open Day-wise chat history sidebar
                            IconButton(
                                onClick = { showHistorySidebar = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Chat History",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    // Informational banner when viewing a past day's conversation
                    if (!isToday) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_today),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Viewing chat: ${AppDate.fromIso(selectedChatDate).toDisplayTitle()}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.newTodayChat() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "Today ➔",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Gemini API Key Banner if no key configured
                    if (geminiApiKey.isBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_key),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Gemini API Key Needed",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "1-Tap get free key to chat & log meals",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 10.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Button(
                                    onClick = { showRaayaProfileScreen = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Get Free Key ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            snackbarHost = { ModernFloatingSnackbarHost(snackbarHostState) },
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            thickness = 0.8.dp
                        )

                        // Attached Image Preview Thumbnail (shown above the text box if an image is selected/pasted)
                        pendingImagePath?.let { path ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                                    ) {
                                        AsyncImage(
                                            model = File(path),
                                            contentDescription = "Attached Image",
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Food photo attached",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { pendingImagePath = null },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove photo",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Message Text Input Row (Model selection inside text box on the left edge)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        text = when {
                                            isListening && liveSpokenText.isNotBlank() -> liveSpokenText
                                            isListening && (replyStyle == "minion" || voiceStyle == "minion") -> "🍌 Bello! Minion listening... Speak now"
                                            isListening -> "Listening to you... Speak now 🎙️"
                                            pendingImagePath != null -> "Ask $raayaName about this food photo..."
                                            else -> "Ask $raayaName or describe a meal..."
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        color = if (isListening) {
                                            if (replyStyle == "minion" || voiceStyle == "minion") Color(0xFFFFD215) else MaterialTheme.colorScheme.primary
                                        } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .copilotRotatingBorder(
                                        baseBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        cornerRadius = 26.dp,
                                        borderWidth = 1.3.dp
                                    ),
                                shape = RoundedCornerShape(26.dp),
                                maxLines = 3,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if ((inputText.isNotBlank() || pendingImagePath != null) && !isGenerating) {
                                        viewModel.sendMessage(inputText, pendingImagePath)
                                        inputText = ""
                                        pendingImagePath = null
                                        coroutineScope.launch {
                                            delay(50)
                                            val target = messages.size.coerceAtLeast(0)
                                            listState.animateScrollToItem(target, scrollOffset = 10000)
                                        }
                                    }
                                }),
                                // Left Edge: Small AI Model Icon with Dropdown Menu
                                leadingIcon = {
                                    Box {
                                        IconButton(
                                            onClick = { showModelMenu = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_bolt),
                                                contentDescription = "AI Model Engine",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showModelMenu,
                                            onDismissRequest = { showModelMenu = false },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                        ) {
                                            Text(
                                                text = "SELECT AI MODEL",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                            allChatModels.forEach { option ->
                                                val isSelected = option.id == currentModel
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = option.name,
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                    fontSize = 13.sp
                                                                ),
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (isSelected) {
                                                                Spacer(modifier = Modifier.width(10.dp))
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        viewModel.setGeminiModel(option.id)
                                                        showModelMenu = false
                                                    }
                                                )
                                            }
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Register Model / API Key...",
                                                            fontSize = 12.5.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    showModelMenu = false
                                                    showRaayaProfileScreen = true
                                                }
                                            )
                                        }
                                    }
                                },
                                // Right Edge inside text box: Voice input and photo capture buttons
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val isMinionVoice = replyStyle.equals("minion", ignoreCase = true) || voiceStyle.equals("minion", ignoreCase = true)
                                        val micGlowColor = if (isMinionVoice) Color(0xFFFFD215) else MaterialTheme.colorScheme.primary

                                        val micTransition = rememberInfiniteTransition(label = "mic_glow")
                                        val micPulseScale by micTransition.animateFloat(
                                            initialValue = 1f,
                                            targetValue = 1.34f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(850, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "mic_scale"
                                        )
                                        val micPulseAlpha by micTransition.animateFloat(
                                            initialValue = 0.40f,
                                            targetValue = 0.10f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(850, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "mic_alpha"
                                        )

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            if (isListening) {
                                                // Outer animated pulsing glow halo
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp * micPulseScale)
                                                        .clip(CircleShape)
                                                        .background(micGlowColor.copy(alpha = micPulseAlpha))
                                                )
                                                // Inner solid glowing ring
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(micGlowColor.copy(alpha = 0.22f))
                                                        .border(1.8.dp, micGlowColor, CircleShape)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    if (isListening) {
                                                        speechRecognizer.stopListening()
                                                    } else {
                                                        startVoiceInput()
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_mic),
                                                    contentDescription = "Voice Input",
                                                    tint = if (isListening) micGlowColor else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                showPhotoChoiceDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_camera),
                                                contentDescription = "Attach or capture food photo",
                                                tint = if (pendingImagePath != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                            )

                            // Elevated theme send button
                            val hasContentToSend = (inputText.isNotBlank() || pendingImagePath != null) && !isGenerating
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasContentToSend) {
                                            primaryColor
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .clickable(enabled = hasContentToSend) {
                                        if (hasContentToSend) {
                                            viewModel.sendMessage(inputText, pendingImagePath)
                                            inputText = ""
                                            pendingImagePath = null
                                            coroutineScope.launch {
                                                delay(50)
                                                val target = messages.size.coerceAtLeast(0)
                                                listState.animateScrollToItem(target, scrollOffset = 10000)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (hasContentToSend) onPrimaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_chat),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No chat history recorded for ${AppDate.fromIso(selectedChatDate).toDisplayTitle()}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.newTodayChat() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go to Today's Chat")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageBubble(
                            message = message,
                            aiName = raayaName,
                            aiAvatar = raayaAvatar,
                            onLogClick = { payload ->
                                viewModel.logMealFromChat(message.id, payload)
                            },
                            onActionClick = { action ->
                                val dispatchResult = JarvisActionDispatcher.dispatch(context, action)
                                coroutineScope.launch {
                                    val feedback = when (dispatchResult) {
                                        is JarvisDispatchResult.Success -> dispatchResult.message
                                        is JarvisDispatchResult.Info -> dispatchResult.message
                                        is JarvisDispatchResult.Error -> dispatchResult.message
                                    }
                                    snackbarHostState.showSnackbar(feedback)
                                }
                            },
                            onSpeakClick = { text ->
                                viewModel.speak(text)
                            }
                        )
                    }

                    if (isGenerating) {
                        item {
                            RaayaTypingIndicator(
                                aiName = raayaName,
                                aiAvatar = raayaAvatar,
                                isWebSearch = raayaWebSearchEnabled
                            )
                        }
                    }
                }
            }
        }

        // Animated Scrim Overlay for Sidebar
        AnimatedVisibility(
            visible = showHistorySidebar,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showHistorySidebar = false }
            )
        }

        // Slide-in Day-wise Chat History Sidebar Drawer (half screen width vertically: fillMaxWidth(0.50f))
        AnimatedVisibility(
            visible = showHistorySidebar,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.52f) // Half of the screen vertically as requested
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {},
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = { showHistorySidebar = false },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Clean, simple + Today Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.newTodayChat()
                                    showHistorySidebar = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Today's Chat",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.8.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Simple, un-highlighted day list (no bulky boxes)
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(chatHistoryDates) { dateIso ->
                                val isSelected = dateIso == selectedChatDate
                                val dateToday = dateIso == AppDate.todayIso()
                                val title = AppDate.fromIso(dateIso).toDisplayTitle()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.selectChatDate(dateIso)
                                            showHistorySidebar = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Simple tiny dot for active selection
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.5.sp
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = dateIso,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    if (!dateToday) {
                                        IconButton(
                                            onClick = { dateToDelete = dateIso },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Simple clear current day chat button
                        TextButton(
                            onClick = { showClearCurrentDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Clear Day",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog to delete a specific date's chat history
    if (dateToDelete != null) {
        val targetDate = dateToDelete!!
        AlertDialog(
            onDismissRequest = { dateToDelete = null },
            title = { Text("Delete Chat?") },
            text = {
                Text("Delete messages for ${AppDate.fromIso(targetDate).toDisplayTitle()}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChatForDate(targetDate)
                        dateToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { dateToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog to clear current chat
    if (showClearCurrentDialog) {
        AlertDialog(
            onDismissRequest = { showClearCurrentDialog = false },
            title = { Text("Clear Day's Chat?") },
            text = {
                Text("Clear all messages for ${AppDate.fromIso(selectedChatDate).toDisplayTitle()}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCurrentChat()
                        showClearCurrentDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCurrentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Photo Choice Dialog (Camera vs Gallery vs Barcode vs Clipboard)
    if (showPhotoChoiceDialog) {
        val hasClipImage = remember(showPhotoChoiceDialog) { getClipboardImageUri(context) != null }
        AlertDialog(
            onDismissRequest = { showPhotoChoiceDialog = false },
            title = { Text("Attach Food Photo or Scan") },
            text = { Text("Choose a photo from your gallery, take a new picture, or scan a barcode.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPhotoChoiceDialog = false
                        requestCameraAndLaunch()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo")
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            showPhotoChoiceDialog = false
                            showBarcodeScannerDialog = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_barcode_scanner),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Barcode")
                    }
                    if (hasClipImage) {
                        Spacer(modifier = Modifier.width(2.dp))
                        TextButton(
                            onClick = {
                                showPhotoChoiceDialog = false
                                val clipUri = getClipboardImageUri(context)
                                if (clipUri != null) {
                                    val saved = copyUriToInternalStorage(context, clipUri)
                                    if (saved != null) {
                                        pendingImagePath = saved
                                    }
                                }
                            }
                        ) {
                            Text("Clipboard")
                        }
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    TextButton(
                        onClick = {
                            showPhotoChoiceDialog = false
                            imagePickerLauncher.launch("image/*")
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_photo_library),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery")
                    }
                }
            }
        )
    }

    if (showBarcodeScannerDialog) {
        val currentCustomBarcodes by viewModel.customBarcodes.collectAsState()
        BarcodeNutritionScannerDialog(
            customBarcodes = currentCustomBarcodes,
            onDismiss = { showBarcodeScannerDialog = false },
            onFoodScanned = { entry ->
                showBarcodeScannerDialog = false
                viewModel.sendMessage("Logged ${entry.foodName}: ${entry.calories} kcal, ${entry.protein}g protein, ${entry.carbs}g carbs, ${entry.fats}g fat", null)
            },
            onAnalyzeLabelPhoto = { photoPath, barcode ->
                showBarcodeScannerDialog = false
                viewModel.sendMessage("Please analyze this nutrition facts label and calculate macros", photoPath, barcodeToSave = barcode)
            },
            onBarcodeUnknown = { barcode ->
                showBarcodeScannerDialog = false
                viewModel.sendMessage("Find nutrition for barcode: $barcode", null)
            }
        )
    }

}
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onLogClick: (ChatFoodPayload) -> Unit,
    modifier: Modifier = Modifier,
    aiName: String = "Raaya",
    aiAvatar: String = "chef",
    onActionClick: ((JarvisActionPayload) -> Unit)? = null,
    onSpeakClick: ((String) -> Unit)? = null
) {
    val isUser = message.isUser

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp, start = 2.dp)
            ) {
                RaayaAvatarImage(
                    avatar = aiAvatar,
                    contentDescription = aiName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = aiName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                if (message.isWebSearch) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "🌐 Web Search",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (message.text.isNotBlank() && onSpeakClick != null) {
                    IconButton(
                        onClick = { onSpeakClick(message.text) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_volume_up),
                            contentDescription = "Read aloud",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // If the message has an attached image, display it inside the message bubble!
                message.imageUri?.let { path ->
                    val file = File(path.removePrefix("file://"))
                    AsyncImage(
                        model = file,
                        contentDescription = "Food photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (message.text.isNotBlank()) {
                    val highlightColor = MaterialTheme.colorScheme.primary
                    val formattedText = remember(message.text, highlightColor) {
                        MarkdownTextFormatter.format(message.text, highlightColor = highlightColor)
                    }
                    Text(
                        text = formattedText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // High-End Interactive Food Logging Card
                message.foodPayload?.let { payload ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = payload.foodName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Category Chip
                                        val catColor = when (payload.category) {
                                            MealCategory.BREAKFAST -> MaterialTheme.colorScheme.primary
                                            MealCategory.LUNCH -> CarbsGreen
                                            MealCategory.DINNER -> Color(0xFFAB47BC)
                                            MealCategory.SNACKS -> FatsCoral
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(catColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = payload.category.displayName,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = catColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = payload.portion,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Interactive Log Button
                                Button(
                                    onClick = { if (!message.isLogged) onLogClick(payload) },
                                    enabled = !message.isLogged,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (message.isLogged) CarbsGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary,
                                        contentColor = if (message.isLogged) CarbsGreen else MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = CarbsGreen.copy(alpha = 0.18f),
                                        disabledContentColor = CarbsGreen
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    if (message.isLogged) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Logged ✓",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_nav_restaurant),
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Log",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Macro Grid Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MacroPillCard(
                                    emoji = "🔥",
                                    label = "Calories",
                                    value = "${payload.calories}",
                                    unit = "kcal",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                MacroPillCard(
                                    emoji = "🥩",
                                    label = "Protein",
                                    value = payload.protein.formatMacroOneDecimal(),
                                    unit = "g",
                                    color = ProteinBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                MacroPillCard(
                                    emoji = "🌾",
                                    label = "Carbs",
                                    value = payload.carbs.formatMacroOneDecimal(),
                                    unit = "g",
                                    color = CarbsGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                MacroPillCard(
                                    emoji = "🥑",
                                    label = "Fats",
                                    value = payload.fats.formatMacroOneDecimal(),
                                    unit = "g",
                                    color = FatsCoral,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Optional micronutrient notes (fiber/sugar/sodium)
                            if (payload.fiber > 0f || payload.sodium > 0f || payload.sugar > 0f) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (payload.fiber > 0f) {
                                        Text(
                                            text = "Fiber: ${payload.fiber.formatMacroOneDecimal()}g",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (payload.sugar > 0f) {
                                        Text(
                                            text = "• Sugar: ${payload.sugar.formatMacroOneDecimal()}g",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (payload.sodium > 0f) {
                                        Text(
                                            text = "• Sodium: ${payload.sodium.formatMacroOneDecimal()}mg",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // High-End Interactive Jarvis Device Automation Card
                message.actionPayload?.let { action ->
                    Spacer(modifier = Modifier.height(12.dp))
                    JarvisActionCard(
                        action = action,
                        onExecute = { onActionClick?.invoke(action) },
                        onSelectCandidate = { candidate ->
                            val directCall = action.copy(
                                contactName = candidate.name,
                                phoneNumber = candidate.number,
                                candidates = null,
                                autoExecute = true
                            )
                            onActionClick?.invoke(directCall)
                        }
                    )
                }

                // Timestamp
                Spacer(modifier = Modifier.height(4.dp))
                val timeStr = remember(message.timestamp) {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
                }
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isUser) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun JarvisActionCard(
    action: JarvisActionPayload,
    onExecute: () -> Unit,
    onSelectCandidate: ((ContactCandidate) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val actionIcon = when (action.actionType) {
        JarvisActionType.CALL -> R.drawable.ic_call
        JarvisActionType.WHATSAPP -> R.drawable.ic_whatsapp
        JarvisActionType.SMS -> R.drawable.ic_sms
        JarvisActionType.ALARM -> R.drawable.ic_alarm
        JarvisActionType.TIMER -> R.drawable.ic_timer
        JarvisActionType.OPEN_APP -> R.drawable.ic_apps
        JarvisActionType.MAPS -> R.drawable.ic_navigation
        JarvisActionType.YOUTUBE -> R.drawable.ic_play_arrow
        JarvisActionType.FLASHLIGHT -> R.drawable.ic_flashlight
        JarvisActionType.BATTERY -> R.drawable.ic_battery
        JarvisActionType.NOTE -> R.drawable.ic_save
    }

    val actionBadge = when (action.actionType) {
        JarvisActionType.CALL -> if (action.hasMultipleCandidates) "SELECT CONTACT" else "PHONE CALL"
        JarvisActionType.WHATSAPP -> if (action.autoExecute) "WHATSAPP OPENED" else "WHATSAPP"
        JarvisActionType.SMS -> if (action.autoExecute) "SMS DRAFTED" else "SMS TEXT"
        JarvisActionType.ALARM -> if (action.autoExecute) "ALARM SET" else "ALARM CLOCK"
        JarvisActionType.TIMER -> if (action.autoExecute) "TIMER RUNNING" else "COUNTDOWN TIMER"
        JarvisActionType.OPEN_APP -> if (action.autoExecute) "APP OPENED" else "LAUNCH APP"
        JarvisActionType.MAPS -> if (action.autoExecute) "MAPS OPENED" else "NAVIGATION"
        JarvisActionType.YOUTUBE -> if (action.autoExecute) "YOUTUBE OPENED" else "YOUTUBE"
        JarvisActionType.FLASHLIGHT -> if (action.autoExecute) "TORCH UPDATED" else "TORCH CONTROL"
        JarvisActionType.BATTERY -> "BATTERY STATUS"
        JarvisActionType.NOTE -> "DEVICE NOTE"
    }

    val buttonLabel = when (action.actionType) {
        JarvisActionType.CALL -> if (action.autoExecute) "Call Again" else "Call Now"
        JarvisActionType.WHATSAPP -> if (action.autoExecute) "Reopen" else "Open Chat"
        JarvisActionType.SMS -> if (action.autoExecute) "Redraft" else "Send Text"
        JarvisActionType.ALARM -> if (action.autoExecute) "Set Again" else "Set Alarm"
        JarvisActionType.TIMER -> if (action.autoExecute) "Start Again" else "Start Timer"
        JarvisActionType.OPEN_APP -> if (action.autoExecute) "Open" else "Launch"
        JarvisActionType.MAPS -> if (action.autoExecute) "Open Maps" else "Navigate"
        JarvisActionType.YOUTUBE -> if (action.autoExecute) "Open" else "Watch"
        JarvisActionType.FLASHLIGHT -> if (action.turnOn != false) "Turn On" else "Turn Off"
        JarvisActionType.BATTERY -> "Check"
        JarvisActionType.NOTE -> "Save / Share"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = actionIcon),
                            contentDescription = actionBadge,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🤖 $actionBadge",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = action.displayTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (action.displaySubtitle.isNotBlank()) {
                            Text(
                                text = action.displaySubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!action.hasMultipleCandidates) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onExecute,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = actionIcon),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buttonLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Disambiguation candidates list for multiple/close contact matches
            if (action.hasMultipleCandidates && !action.candidates.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap to call contact directly:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    action.candidates.forEach { candidate ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_call),
                                            contentDescription = "Call",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = candidate.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = candidate.number,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onSelectCandidate?.invoke(candidate) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_call),
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Call",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroPillCard(
    emoji: String,
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = color
                )
                Text(
                    text = unit,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 1.dp, bottom = 1.dp)
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "$emoji $label",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RaayaTypingIndicator(
    aiName: String = "Raaya",
    aiAvatar: String = "chef",
    isWebSearch: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "dots")
    val dot1Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RaayaAvatarImage(
            avatar = aiAvatar,
            contentDescription = aiName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = dot1Offset.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = dot2Offset.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = dot3Offset.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isWebSearch) "$aiName is searching the web..." else "$aiName is calculating your macros...",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "chat_images")
        if (!dir.exists()) dir.mkdirs()
        val destFile = File(dir, "chat_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

fun copyAvatarUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "raaya_avatars")
        if (!dir.exists()) dir.mkdirs()
        val destFile = File(dir, "raaya_avatar_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

private fun getClipboardImageUri(context: Context): Uri? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount > 0) {
        val item = clip.getItemAt(0)
        val uri = item.uri
        if (uri != null) {
            val mimeType = try { context.contentResolver.getType(uri) } catch (e: Exception) { null }
            if (mimeType?.startsWith("image/") == true) {
                return uri
            }
        }
    }
    return null
}

fun getAvatarDrawable(avatarId: String): Int {
    return when (avatarId) {
        "chef" -> R.drawable.img_raaya_chef
        "eureka" -> R.drawable.img_tom_jerry_eureka
        "think" -> R.drawable.img_tom_jerry_think
        "calc" -> R.drawable.img_tom_jerry_calc
        "work" -> R.drawable.img_tom_jerry_work
        else -> R.drawable.ic_default_profile
    }
}

@Composable
fun RaayaAvatarImage(
    avatar: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (avatar.startsWith("/") || avatar.startsWith("file:") || avatar.startsWith("content:")) {
        val model: Any = if (avatar.startsWith("file://")) {
            File(avatar.removePrefix("file://"))
        } else if (avatar.startsWith("/")) {
            File(avatar)
        } else {
            Uri.parse(avatar)
        }
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = getAvatarDrawable(avatar)),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

data class AvatarOption(
    val id: String,
    val title: String,
    val drawableRes: Int
)

val availableAvatars = listOf(
    AvatarOption("default", "Default Profile", R.drawable.ic_default_profile),
    AvatarOption("chef", "Chef Duo", R.drawable.img_raaya_chef),
    AvatarOption("eureka", "Eureka Idea", R.drawable.img_tom_jerry_eureka),
    AvatarOption("think", "Thinker", R.drawable.img_tom_jerry_think),
    AvatarOption("calc", "Math Calc", R.drawable.img_tom_jerry_calc),
    AvatarOption("work", "Hard Worker", R.drawable.img_tom_jerry_work)
)

/**
 * Dedicated Raaya AI Profile & Settings Full Page Screen
 * Exclusively opens full screen when tapping on Raaya in the Chat Screen.
 * Contains custom photo upload, avatar selection, name editing, model engine selection,
 * per-model request analytics, and full AI chat intelligence settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaayaProfileScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val raayaName by viewModel.raayaName.collectAsState()
    val raayaAvatar by viewModel.raayaAvatar.collectAsState()
    val raayaPersonality by viewModel.raayaPersonality.collectAsState()
    val raayaAutoLog by viewModel.raayaAutoLog.collectAsState()
    val raayaIncludeMicros by viewModel.raayaIncludeMicros.collectAsState()
    val raayaDietaryNotes by viewModel.raayaDietaryNotes.collectAsState()
    val raayaWebSearchEnabled by viewModel.raayaWebSearchEnabled.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val customModels by viewModel.customModels.collectAsState()
    val modelUsageStats by viewModel.modelUsageStats.collectAsState()
    val voiceStyle by viewModel.voiceStyle.collectAsState()
    val replyStyle by viewModel.replyStyle.collectAsState()

    var nameInput by remember(raayaName) { mutableStateOf(raayaName) }
    var dietaryNotesInput by remember(raayaDietaryNotes) { mutableStateOf(raayaDietaryNotes) }
    var apiKeyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var customModelInput by remember { mutableStateOf("") }

    // Custom Photo Upload Gallery Launcher
    val customAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = copyAvatarUriToInternalStorage(context, uri)
            if (savedPath != null) {
                viewModel.updateRaayaAvatar(savedPath)
            }
        }
    }

    val isCustomAvatar = raayaAvatar.startsWith("/") || raayaAvatar.startsWith("file:") || raayaAvatar.startsWith("content:")

    val totalModelRequests = remember(modelUsageStats) {
        modelUsageStats.values.sum()
    }

    val onSaveAndBack = {
        val trimmed = nameInput.trim()
        if (trimmed.isNotBlank() && trimmed != raayaName) {
            viewModel.updateRaayaName(trimmed)
        }
        val trimmedNotes = dietaryNotesInput.trim()
        if (trimmedNotes != raayaDietaryNotes) {
            viewModel.updateRaayaDietaryNotes(trimmedNotes)
        }
        onBack()
    }

    BackHandler(onBack = onSaveAndBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Profile & Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSaveAndBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Chat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. AI Name & Custom Avatar Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AI IDENTITY & PROFILE ICON",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Large Avatar Display with Clickable Upload
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { customAvatarLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        RaayaAvatarImage(
                            avatar = raayaAvatar,
                            contentDescription = "Active AI Profile",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prominent Custom Photo Upload Action
                    Button(
                        onClick = { customAvatarLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCustomAvatar) "Change Custom Photo" else "Upload Custom Photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (isCustomAvatar) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { viewModel.updateRaayaAvatar("chef") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Reset to Default Mascot",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Mascot Presets Option
                    Text(
                        text = "Or Choose Mascot Preset:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        availableAvatars.forEach { avatar ->
                            val isSelected = avatar.id == raayaAvatar
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.updateRaayaAvatar(avatar.id) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Image(
                                        painter = painterResource(id = avatar.drawableRes),
                                        contentDescription = avatar.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = avatar.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name Edit Row
                    Text(
                        text = "AI Name:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.updateRaayaName(nameInput) },
                            enabled = nameInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (nameInput.trim() == raayaName) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                contentColor = if (nameInput.trim() == raayaName) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (nameInput.trim() == raayaName) "Saved ✓" else "Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Google Gemini API Key & Registration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GEMINI API KEY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (geminiApiKey.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (geminiApiKey.isNotBlank()) "Active ✓" else "Not Registered",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (geminiApiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "A Google Gemini API key is required to power intelligent chat, food identification, and macro parsing. Free tier keys have no cost and require no credit card.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1-Tap Free Key Button
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                context.startActivity(intent)
                            } catch (_: Throwable) {
                                Toast.makeText(context, "Visit https://aistudio.google.com/app/apikey", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_key),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Get Free Gemini API Key (1-Tap)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Key Input Field
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Paste API Key (AIzaSy...)") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        trailingIcon = {
                            if (apiKeyInput.isNotBlank()) {
                                IconButton(onClick = { apiKeyInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (geminiApiKey.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveGeminiApiKey("")
                                    apiKeyInput = ""
                                    Toast.makeText(context, "API Key removed", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Remove", fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                val trimmed = apiKeyInput.trim()
                                viewModel.saveGeminiApiKey(trimmed)
                                Toast.makeText(context, if (trimmed.isNotBlank()) "API Key saved successfully!" else "Key cleared", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            enabled = apiKeyInput.trim() != geminiApiKey
                        ) {
                            Text("Save Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Active Model Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "AI MODEL ENGINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select active generative model or register custom models:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val allModels = remember(customModels) {
                        availableChatModels + customModels.map { ChatModelOption(it, "$it (Custom)") }
                    }

                    allModels.forEach { modelOpt ->
                        val isSelected = modelOpt.id == currentModel
                        val isCustom = customModels.contains(modelOpt.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.setGeminiModel(modelOpt.id) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = modelOpt.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (isCustom) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.removeCustomModel(modelOpt.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Model",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Register Custom Model:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customModelInput,
                            onValueChange = { customModelInput = it },
                            placeholder = { Text("e.g. gemini-2.0-flash", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clean = customModelInput.trim()
                                if (clean.isNotBlank()) {
                                    viewModel.registerCustomModel(clean)
                                    customModelInput = ""
                                }
                            },
                            enabled = customModelInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Register", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Per-Model Request Analytics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MODEL USAGE ANALYTICS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$totalModelRequests Total Requests",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Requests processed by each AI model:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val allModelsForStats = remember(customModels) {
                        availableChatModels + customModels.map { ChatModelOption(it, "$it (Custom)") }
                    }

                    allModelsForStats.forEach { modelOpt ->
                        val count = modelUsageStats[modelOpt.id] ?: 0
                        val progress = if (totalModelRequests > 0) count.toFloat() / totalModelRequests else 0f
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = modelOpt.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$count req",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Full AI Chat Related Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "CHAT INTELLIGENCE SETTINGS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Coaching Tone
                    Text(
                        text = "Coaching Persona & Tone:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val tones = listOf(
                        "Friendly & Encouraging",
                        "Strict Fitness Coach",
                        "Detailed Clinical Nutritionist",
                        "Concise & Fast"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        tones.forEach { t ->
                            val isSelected = t == raayaPersonality
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateRaayaPersonality(t) }
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = t,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Web Search Grounding Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Live Web Search Grounding",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "GOOGLE SEARCH",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ground answers with live web search for current movies, news, packaged foods, and nutrition",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = raayaWebSearchEnabled,
                            onCheckedChange = { viewModel.updateRaayaWebSearch(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Detect Food Logs Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto 1-Tap Food Logger",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Generate instant logging cards when meals are mentioned",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = raayaAutoLog,
                            onCheckedChange = { viewModel.updateRaayaAutoLog(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Micronutrient Precision Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include Micronutrients",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Calculate dietary fiber, sugar, and sodium",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = raayaIncludeMicros,
                            onCheckedChange = { viewModel.updateRaayaIncludeMicros(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dietary Context
                    Text(
                        text = "Dietary Context & Preferences:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = dietaryNotesInput,
                        onValueChange = { dietaryNotesInput = it },
                        placeholder = {
                            Text("e.g., Vegetarian, high-protein cutting, lactose intolerant", fontSize = 11.5.sp)
                        },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    if (dietaryNotesInput != raayaDietaryNotes) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = { viewModel.updateRaayaDietaryNotes(dietaryNotesInput) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Save Dietary Context", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 5. Voice Assistant & Minions Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "VOICE & REPLIES PERSONALITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (voiceStyle == "minion" || replyStyle == "minion") "🍌 MINIONS ACTIVE" else "STANDARD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Voice Style Toggle (Normal vs Minion Voice)
                    Text(
                        text = "Voice Output Style:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isNormalVoice = voiceStyle != "minion"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.updateVoiceStyle("normal") },
                            color = if (isNormalVoice) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isNormalVoice) 1.5.dp else 0.8.dp,
                                color = if (isNormalVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🎙️ Normal Voice",
                                    fontSize = 12.sp,
                                    fontWeight = if (isNormalVoice) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isNormalVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Standard speech",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isMinionVoice = voiceStyle == "minion"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.updateVoiceStyle("minion") },
                            color = if (isMinionVoice) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isMinionVoice) 1.5.dp else 0.8.dp,
                                color = if (isMinionVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🍌 Minion Voice",
                                    fontSize = 12.sp,
                                    fontWeight = if (isMinionVoice) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isMinionVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "High pitch & squeaky",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.testVoice(voiceStyle) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_volume_up),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (voiceStyle == "minion") "Play Minion Sample 🍌" else "Play Normal Voice Sample 🎙️",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (voiceStyle == "minion") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "🍌 Bello" to "hello",
                                "🍌 Banana" to "banana",
                                "❤️ Tulaliloo" to "tulaliloo",
                                "😄 Hehehe" to "hehe"
                            ).forEach { (label, key) ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.playMinionSound(key) },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Reply Style Toggle (Normal Replies vs Minion Replies)
                    Text(
                        text = "Reply Language Style:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isNormalReply = replyStyle != "minion"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.updateReplyStyle("normal") },
                            color = if (isNormalReply) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isNormalReply) 1.5.dp else 0.8.dp,
                                color = if (isNormalReply) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "💬 Normal Replies",
                                    fontSize = 12.sp,
                                    fontWeight = if (isNormalReply) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isNormalReply) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Polite & professional",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isMinionReply = replyStyle == "minion"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.updateReplyStyle("minion") },
                            color = if (isMinionReply) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isMinionReply) 1.5.dp else 0.8.dp,
                                color = if (isMinionReply) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🍌 Minion Replies",
                                    fontSize = 12.sp,
                                    fontWeight = if (isMinionReply) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isMinionReply) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Banana language mode",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (replyStyle == "minion")
                            "🍌 Minion replies use hilarious Banana language ('Bello!', 'Tulaliloo ti amo!') while keeping all calorie and macro math 100% accurate!"
                        else
                            "Standard helpful nutrition coaching and answers.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
