package com.macrobite.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import com.macrobite.app.R
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.repository.ChatRepository
import com.macrobite.app.domain.repository.MealRepository
import com.macrobite.app.domain.usecase.ParseFoodUseCase
import com.macrobite.app.ui.chat.ChatFoodPayload
import com.macrobite.app.ui.chat.ChatMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WidgetInputActivity : ComponentActivity() {

    @Inject
    lateinit var parseFoodUseCase: ParseFoodUseCase

    @Inject
    lateinit var mealRepository: MealRepository

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var userPreferencesRepository: com.macrobite.app.domain.repository.UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        val prefs = getSharedPreferences("macrobite_widget_prefs", Context.MODE_PRIVATE)
        val themeId = prefs.getString("theme_color", "amber") ?: "amber"
        val themeColor = MacroBiteChatWidgetProvider.getThemeColorInt(themeId)

        val density = resources.displayMetrics.density
        val dp16 = (16 * density).toInt()
        val dp8 = (8 * density).toInt()
        val dp56 = (56 * density).toInt()

        // Root container - click outside dismisses with zero animation
        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                dismissCleanly()
            }
        }

        // Input pill container matching widget shape & style
        val inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp8, 0, dp8, 0)

            // Styled rounded pill
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28 * density
                setColor(Color.parseColor("#1C1C22"))
                setStroke((1.5f * density).toInt(), themeColor)
            }
            background = bgDrawable
        }

        // Bolt icon
        val boltIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt())
            setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
            setImageResource(R.drawable.ic_bolt)
            setColorFilter(themeColor)
        }
        inputContainer.addView(boltIcon)

        // Text input field
        val editText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            background = null
            setPadding((6 * density).toInt(), 0, (6 * density).toInt(), 0)
            hint = "Type meal to log..."
            setHintTextColor(Color.parseColor("#71717A"))
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            isSingleLine = true
            maxLines = 1
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        inputContainer.addView(editText)

        // Send button icon
        val sendIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt())
            setPadding((7 * density).toInt(), (7 * density).toInt(), (7 * density).toInt(), (7 * density).toInt())
            setImageResource(R.drawable.ic_arrow_upward)
            setColorFilter(themeColor)
            setOnClickListener {
                submitInput(editText.text.toString().trim())
            }
        }
        inputContainer.addView(sendIcon)

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                submitInput(editText.text.toString().trim())
                true
            } else {
                false
            }
        }

        // Position directly over sourceBounds if available from widget tap
        val sourceBounds = intent.sourceBounds
        val containerParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp56
        ).apply {
            leftMargin = dp16
            rightMargin = dp16
            if (sourceBounds != null && sourceBounds.top > 0) {
                topMargin = sourceBounds.top
            } else {
                // Centered upper half fallback
                topMargin = (140 * density).toInt()
            }
        }
        inputContainer.layoutParams = containerParams
        rootLayout.addView(inputContainer)

        setContentView(rootLayout)

        // Auto focus and show soft keyboard immediately
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        editText.postDelayed({
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 80)
    }

    private fun submitInput(text: String) {
        if (text.isBlank()) {
            dismissCleanly()
            return
        }

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }

        // 1. Immediately reflect the typed text into the home screen widget!
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val componentName = ComponentName(applicationContext, MacroBiteChatWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
            val rv = RemoteViews(packageName, R.layout.widget_chat_bar)
            rv.setTextViewText(R.id.widget_placeholder_text, "$text...")
            appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, rv)
        }

        // 2. Finish immediately with zero screen flashes or popups!
        finish()
        overridePendingTransition(0, 0)

        // 3. Process assistant tasks / food logging completely in background!
        val appContext = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            WidgetActionEngine.processInput(
                context = appContext,
                rawInput = text,
                parseFoodUseCase = parseFoodUseCase,
                mealRepository = mealRepository,
                chatRepository = chatRepository,
                userPreferencesRepository = userPreferencesRepository
            )
        }
    }

    private fun dismissCleanly() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        finish()
        overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
