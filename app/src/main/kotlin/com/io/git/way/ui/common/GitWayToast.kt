package com.io.git.way.ui.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

/**
 * Theme-aware app toast.
 *
 * Success/info feedback remains readable in both light and dark mode.
 */
fun showGitWayToast(
    context: Context,
    message: String,
    isError: Boolean = false
) {
    val resources = context.resources
    val density = resources.displayMetrics.density

    val isDark = (resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

    val backgroundColor = when {
        isError -> Color.rgb(127, 29, 29)
        isDark -> Color.rgb(36, 50, 71)
        else -> Color.WHITE
    }

    val foregroundColor = when {
        isError || isDark -> Color.WHITE
        else -> Color.rgb(23, 32, 51)
    }

    val strokeColor = when {
        isError -> Color.rgb(239, 68, 68)
        isDark -> Color.rgb(71, 85, 105)
        else -> Color.rgb(203, 213, 225)
    }

    fun dp(value: Int): Int {
        return (value * density).toInt()
    }

    val textView = TextView(context).apply {
        text = message
        setTextColor(foregroundColor)
        textSize = 14f

        setPadding(
            dp(18),
            dp(11),
            dp(18),
            dp(11)
        )

        background = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = dp(50).toFloat()
            setStroke(dp(1), strokeColor)
        }

        elevation = dp(8).toFloat()
    }

    Toast(context).apply {
        duration = Toast.LENGTH_SHORT
        view = textView

        setGravity(
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            0,
            dp(96)
        )

        show()
    }
}