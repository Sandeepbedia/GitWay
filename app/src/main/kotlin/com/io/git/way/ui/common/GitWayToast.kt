package com.io.git.way.ui.common

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

/** Theme-aware app toast. Keeps success/info feedback readable in both light and dark mode. */
fun showGitWayToast(context: Context, message: String, isError: Boolean = false) {
    val isDark = (context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES

    val bgColor = if (isError) AndroidColor.rgb(127, 29, 29)
    else if (isDark) AndroidColor.rgb(36, 50, 71)
    else AndroidColor.rgb(255, 255, 255)
    val foreground = if (isError || isDark) AndroidColor.WHITE else AndroidColor.rgb(23, 32, 51)

    val textView = TextView(context).apply {
        text = message
        setTextColor(foreground)
        textSize = 14f
        setPadding(18, 11, 18, 11)
        background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 100f
            setStroke(1, if (isError) AndroidColor.rgb(239, 68, 68)
            else if (isDark) AndroidColor.rgb(71, 85, 105)
            else AndroidColor.rgb(203, 213, 225))
        }
        elevation = 8f
    }

    Toast(context).apply {
        duration = Toast.LENGTH_SHORT
        view = textView
        setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 96)
        show()
    }
}
