package com.macrobite.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorTitle = intent.getStringExtra("EXTRA_ERROR_TITLE") ?: "Diagnostic Alert"
        val errorDetails = intent.getStringExtra("EXTRA_ERROR_DETAILS") ?: "No details recorded."

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(48, 48, 48, 48)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val headerText = TextView(this).apply {
            text = "MacroBite Recovery Assistant"
            setTextColor(Color.parseColor("#FFC107"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
        }
        rootLayout.addView(headerText)

        val subheaderText = TextView(this).apply {
            text = "An issue occurred during startup/runtime. Diagnostic info:"
            setTextColor(Color.parseColor("#E0E0E0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 12, 0, 16)
        }
        rootLayout.addView(subheaderText)

        val errorTitleView = TextView(this).apply {
            text = errorTitle
            setTextColor(Color.parseColor("#FF5252"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(errorTitleView)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(24, 24, 24, 24)
        }

        val detailsText = TextView(this).apply {
            text = errorDetails
            setTextColor(Color.parseColor("#80D8FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        scrollView.addView(detailsText)
        rootLayout.addView(scrollView)

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 0)
        }

        val copyButton = Button(this).apply {
            text = "Copy Stacktrace"
            setBackgroundColor(Color.parseColor("#333333"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                marginEnd = 16
            }
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("MacroBite Crash Log", "$errorTitle\n\n$errorDetails")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@CrashActivity, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        }
        buttonLayout.addView(copyButton)

        val restartButton = Button(this).apply {
            text = "Restart App"
            setBackgroundColor(Color.parseColor("#FFC107"))
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            setOnClickListener {
                val restartIntent = Intent(this@CrashActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(restartIntent)
                finish()
                Runtime.getRuntime().exit(0)
            }
        }
        buttonLayout.addView(restartButton)

        rootLayout.addView(buttonLayout)
        setContentView(rootLayout)
    }
}
