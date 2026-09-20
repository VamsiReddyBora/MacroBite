package com.macrobite.app

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.macrobite.app.domain.repository.UserPreferencesRepository
import com.macrobite.app.ui.chat.ChatScreen
import com.macrobite.app.ui.dashboard.DashboardScreen
import com.macrobite.app.ui.history.HistoryScreen
import com.macrobite.app.ui.navigation.BottomNavBar
import com.macrobite.app.ui.navigation.Screen
import com.macrobite.app.ui.settings.SettingsScreen
import com.macrobite.app.ui.theme.MacroBiteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val _targetPageFlow = MutableStateFlow<Int?>(null)
    val targetPageFlow: StateFlow<Int?> = _targetPageFlow.asStateFlow()

    private val _widgetActionFlow = MutableStateFlow<String?>(null)
    val widgetActionFlow: StateFlow<String?> = _widgetActionFlow.asStateFlow()

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = intent.getIntExtra("target_page", -1)
        if (target in 0..3) {
            _targetPageFlow.value = target
        }
        val action = intent.getStringExtra("extra_action")
        if (!action.isNullOrBlank()) {
            _widgetActionFlow.value = action
        }
    }

    override fun onResume() {
        super.onResume()
        com.macrobite.app.notification.ChatVisibilityTracker.setAppForeground(true)
        // Ensure home screen widget shows accurate theme & assistant name
        com.macrobite.app.widget.MacroBiteChatWidgetProvider.updateAllWidgets(this)
    }

    override fun onPause() {
        super.onPause()
        com.macrobite.app.notification.ChatVisibilityTracker.setAppForeground(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if opened with target page extra from notification or widget
        val target = intent?.getIntExtra("target_page", -1) ?: -1
        if (target in 0..3) {
            _targetPageFlow.value = target
        }
        val action = intent?.getStringExtra("extra_action")
        if (!action.isNullOrBlank()) {
            _widgetActionFlow.value = action
        }

        // Proactively request permissions on startup so voice actions and direct phone calls run seamlessly without prompts
        val neededPermissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                neededPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            neededPermissions.add(android.Manifest.permission.CALL_PHONE)
        }
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            neededPermissions.add(android.Manifest.permission.READ_CONTACTS)
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            neededPermissions.add(android.Manifest.permission.RECORD_AUDIO)
        }
        if (neededPermissions.isNotEmpty()) {
            requestPermissions(neededPermissions.toTypedArray(), 1010)
        }

        // Ensure pure AMOLED black window right from the first frame
        window.setBackgroundDrawableResource(android.R.color.black)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        // Force edge-to-edge full screen and accommodate curved/notch cutouts
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        try {
            setContent {
                val darkModeFlow = remember {
                    try {
                        userPreferencesRepository.getDarkModePreference()
                    } catch (e: Throwable) {
                        Log.e("MainActivity", "Fallback to dark mode flow", e)
                        flowOf("dark")
                    }
                }
                val darkModePref by darkModeFlow.collectAsState(initial = "dark")

                val themeColorFlow = remember {
                    try {
                        userPreferencesRepository.getThemeColor()
                    } catch (e: Throwable) {
                        Log.e("MainActivity", "Fallback to theme color flow", e)
                        flowOf("amber")
                    }
                }
                val themeColorPref by themeColorFlow.collectAsState(initial = "amber")
                val activeThemePreset = remember(themeColorPref) {
                    com.macrobite.app.ui.theme.AppThemePreset.fromId(themeColorPref)
                }

                LaunchedEffect(themeColorPref) {
                    com.macrobite.app.notification.NotificationHelper.updateThemeColor(themeColorPref)
                }

                val isDark = when (darkModePref) {
                    "light" -> false
                    "system" -> isSystemInDarkTheme()
                    else -> true // "dark" default per specification
                }

                var appReady by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(60)
                    appReady = true
                }
                val contentAlpha by animateFloatAsState(
                    targetValue = if (appReady) 1f else 0f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    label = "app_startup_fade"
                )

                MacroBiteTheme(
                    darkTheme = isDark,
                    themeId = themeColorPref
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .graphicsLayer { alpha = contentAlpha }
                    ) {
                        MacroBiteAppMain(
                            targetPageFlow = targetPageFlow,
                            widgetActionFlow = widgetActionFlow,
                            onClearWidgetAction = { _widgetActionFlow.value = null }
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("MainActivity", "Uncaught error during setContent", t)
            throw t
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MacroBiteAppMain(
    targetPageFlow: StateFlow<Int?> = MutableStateFlow(null),
    widgetActionFlow: StateFlow<String?> = MutableStateFlow(null),
    onClearWidgetAction: () -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val requestedPage by targetPageFlow.collectAsState()
    val widgetAction by widgetActionFlow.collectAsState()
    LaunchedEffect(requestedPage) {
        requestedPage?.let { page ->
            if (page in 0..3) {
                pagerState.animateScrollToPage(page)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        com.macrobite.app.notification.ChatVisibilityTracker.setChatSelected(pagerState.currentPage == 1)
    }

    val currentRoute = when (pagerState.currentPage) {
        0 -> Screen.Dashboard.route
        1 -> Screen.Chat.route
        2 -> Screen.History.route
        3 -> Screen.Settings.route
        else -> Screen.Dashboard.route
    }

    // Pressing back on Chat, History, or Settings smoothly returns to Dashboard (page 0)
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Safe drawing horizontal insets protect content on curved waterfall edge screens
        // Top inset is handled edge-to-edge by each screen's TopAppBar
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal
        ),
        bottomBar = {
            val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            if (!isImeVisible) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        val targetPage = when (route) {
                            Screen.Dashboard.route -> 0
                            Screen.Chat.route -> 1
                            Screen.History.route -> 2
                            Screen.Settings.route -> 3
                            else -> 0
                        }
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            beyondBoundsPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> DashboardScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToSettings = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(3)
                        }
                    }
                )
                1 -> ChatScreen(
                    viewModel = hiltViewModel(),
                    isPageActive = pagerState.currentPage == 1,
                    widgetAction = widgetAction,
                    onWidgetActionHandled = onClearWidgetAction
                )
                2 -> HistoryScreen(
                    viewModel = hiltViewModel(),
                    isPageActive = pagerState.currentPage == 2
                )
                3 -> SettingsScreen(
                    viewModel = hiltViewModel(),
                    isScreenActive = pagerState.currentPage == 3
                )
            }
        }
    }
}
