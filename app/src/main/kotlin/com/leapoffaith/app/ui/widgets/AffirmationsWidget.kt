package com.leapoffaith.app.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.leapoffaith.app.MainActivity
import org.json.JSONArray

class AffirmationsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs  = context.getSharedPreferences("lof_widget", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("is_dark_theme", true)
        val bgColor = if (isDark) Color(0xFF0D1B2A) else Color(0xFFF0F4F1)
        val primary = if (isDark) Color(0xFFD4A843) else Color(0xFF2D6A4F)
        val textClr = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1A2A1F)

        // Get today's affirmation
        val json = context.getSharedPreferences("lof_data", Context.MODE_PRIVATE)
            .getString("affirmations", "[]") ?: "[]"
        val items = try {
            val arr = JSONArray(json); (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() }
        val todayIdx = if (items.isEmpty()) -1 else
            (java.time.LocalDate.now().dayOfYear % items.size)
        val preview = items.getOrNull(todayIdx)?.take(40)?.let { if (items[todayIdx].length > 40) "$it..." else it }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "affirmations")
        }

        provideContent {
            Box(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()
                .background(ColorProvider(bgColor))
                .cornerRadius(12.dp)
                .clickable(actionStartActivity(openIntent))
                .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center) {
                Text("Daily Affirmations",
                    style = TextStyle(color = ColorProvider(primary),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

class AffirmationsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = AffirmationsWidget()
}
