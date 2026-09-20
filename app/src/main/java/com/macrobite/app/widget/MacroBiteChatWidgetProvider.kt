package com.macrobite.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import com.macrobite.app.MainActivity
import com.macrobite.app.R

/**
 * Minimal Home Screen Chat Widget for MacroBite.
 * Matches the chat text box in shape, length, width, and includes the rotating outer ring
 * themed to the user's active theme color (Amber, Emerald, Cobalt, Crimson, Amethyst, Slate).
 */
class MacroBiteChatWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET_THEME ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, MacroBiteChatWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET_THEME = "com.macrobite.app.ACTION_UPDATE_WIDGET_THEME"
        private const val PREFS_NAME = "macrobite_widget_prefs"
        private const val KEY_THEME = "theme_color"
        private const val KEY_NAME = "assistant_name"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_chat_bar)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val themeId = prefs.getString(KEY_THEME, "amber") ?: "amber"
                val rawName = prefs.getString(KEY_NAME, "AI") ?: "AI"
                val assistantName = if (rawName.isBlank()) "AI" else rawName

                val themeColorInt = getThemeColorInt(themeId)

                // 1. Configure the static frame for the outer ring
                val drawableRes = getFrameDrawableRes(themeId, 0)
                views.setImageViewResource(R.id.widget_static_border, drawableRes)

                // 2. Tint icons with active theme accent
                views.setInt(R.id.widget_bolt_icon, "setColorFilter", themeColorInt)
                views.setInt(R.id.widget_mic_button, "setColorFilter", themeColorInt)
                views.setInt(R.id.widget_camera_button, "setColorFilter", Color.parseColor("#A1A1AA"))

                // 3. Set placeholder text
                views.setTextViewText(
                    R.id.widget_placeholder_text,
                    "Ask $assistantName or describe a meal..."
                )

                // 4. PendingIntent: Tap text box / input area -> Opens WidgetInputActivity
                val chatIntent = Intent(context, WidgetInputActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val chatPendingIntent = PendingIntent.getActivity(
                    context,
                    1001,
                    chatIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_input_box, chatPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_placeholder_text, chatPendingIntent)

                // 5. PendingIntent: Tap Bolt icon -> Opens Chat
                val boltIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("target_page", 1)
                }
                val boltPendingIntent = PendingIntent.getActivity(
                    context,
                    1002,
                    boltIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_bolt_icon, boltPendingIntent)

                // 6. PendingIntent: Tap Mic icon -> Opens WidgetVoiceActivity
                val micIntent = Intent(context, WidgetVoiceActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val micPendingIntent = PendingIntent.getActivity(
                    context,
                    1003,
                    micIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_mic_button, micPendingIntent)

                // 7. PendingIntent: Tap Camera icon -> Opens photo dialog directly
                val cameraIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("target_page", 1)
                    putExtra("extra_action", "camera")
                }
                val cameraPendingIntent = PendingIntent.getActivity(
                    context,
                    1004,
                    cameraIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_camera_button, cameraPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Throwable) {
                Log.e("WidgetProvider", "Failed to update widget $appWidgetId", e)
            }
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, MacroBiteChatWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            } catch (e: Throwable) {
                Log.e("WidgetProvider", "Error updating all widgets", e)
            }
        }

        fun savePreferences(context: Context, themeColor: String? = null, assistantName: String? = null) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                themeColor?.let { editor.putString(KEY_THEME, it.lowercase()) }
                assistantName?.let { editor.putString(KEY_NAME, it.trim()) }
                editor.apply()
                updateAllWidgets(context)
            } catch (e: Throwable) {
                Log.e("WidgetProvider", "Error saving widget preferences", e)
            }
        }

        fun getThemeColorInt(themeId: String): Int {
            return when (themeId.lowercase().trim()) {
                "emerald" -> 0xFF10B981.toInt()
                "cobalt" -> 0xFF3B82F6.toInt()
                "crimson" -> 0xFFE11D48.toInt()
                "amethyst" -> 0xFF8B5CF6.toInt()
                "slate" -> 0xFF64748B.toInt()
                else -> 0xFFF59E0B.toInt() // Amber Gold default
            }
        }

        private fun getFrameDrawableRes(themeId: String, angle: Int): Int {
            return when (themeId.lowercase().trim()) {
                "emerald" -> when (angle) {
                    0 -> R.drawable.widget_border_emerald_0
                    45 -> R.drawable.widget_border_emerald_45
                    90 -> R.drawable.widget_border_emerald_90
                    135 -> R.drawable.widget_border_emerald_135
                    180 -> R.drawable.widget_border_emerald_180
                    225 -> R.drawable.widget_border_emerald_225
                    270 -> R.drawable.widget_border_emerald_270
                    else -> R.drawable.widget_border_emerald_315
                }
                "cobalt" -> when (angle) {
                    0 -> R.drawable.widget_border_cobalt_0
                    45 -> R.drawable.widget_border_cobalt_45
                    90 -> R.drawable.widget_border_cobalt_90
                    135 -> R.drawable.widget_border_cobalt_135
                    180 -> R.drawable.widget_border_cobalt_180
                    225 -> R.drawable.widget_border_cobalt_225
                    270 -> R.drawable.widget_border_cobalt_270
                    else -> R.drawable.widget_border_cobalt_315
                }
                "crimson" -> when (angle) {
                    0 -> R.drawable.widget_border_crimson_0
                    45 -> R.drawable.widget_border_crimson_45
                    90 -> R.drawable.widget_border_crimson_90
                    135 -> R.drawable.widget_border_crimson_135
                    180 -> R.drawable.widget_border_crimson_180
                    225 -> R.drawable.widget_border_crimson_225
                    270 -> R.drawable.widget_border_crimson_270
                    else -> R.drawable.widget_border_crimson_315
                }
                "amethyst" -> when (angle) {
                    0 -> R.drawable.widget_border_amethyst_0
                    45 -> R.drawable.widget_border_amethyst_45
                    90 -> R.drawable.widget_border_amethyst_90
                    135 -> R.drawable.widget_border_amethyst_135
                    180 -> R.drawable.widget_border_amethyst_180
                    225 -> R.drawable.widget_border_amethyst_225
                    270 -> R.drawable.widget_border_amethyst_270
                    else -> R.drawable.widget_border_amethyst_315
                }
                "slate" -> when (angle) {
                    0 -> R.drawable.widget_border_slate_0
                    45 -> R.drawable.widget_border_slate_45
                    90 -> R.drawable.widget_border_slate_90
                    135 -> R.drawable.widget_border_slate_135
                    180 -> R.drawable.widget_border_slate_180
                    225 -> R.drawable.widget_border_slate_225
                    270 -> R.drawable.widget_border_slate_270
                    else -> R.drawable.widget_border_slate_315
                }
                else -> when (angle) {
                    0 -> R.drawable.widget_border_amber_0
                    45 -> R.drawable.widget_border_amber_45
                    90 -> R.drawable.widget_border_amber_90
                    135 -> R.drawable.widget_border_amber_135
                    180 -> R.drawable.widget_border_amber_180
                    225 -> R.drawable.widget_border_amber_225
                    270 -> R.drawable.widget_border_amber_270
                    else -> R.drawable.widget_border_amber_315
                }
            }
        }
    }
}
