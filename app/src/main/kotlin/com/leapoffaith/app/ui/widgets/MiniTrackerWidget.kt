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
import com.leapoffaith.app.data.AppDatabase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class MiniTrackerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs     = context.getSharedPreferences("lof_widget", Context.MODE_PRIVATE)
        val userId    = prefs.getString("widget_user", "qusai") ?: "qusai"
        val isDark    = prefs.getBoolean("is_dark_theme", true)
        val miniCatId = prefs.getLong("mini_tracker_cat_id", -1L)

        val bgColor    = if (isDark) Color(0xFF0D1B2A) else Color(0xFFF0F4F1)
        val cellDone   = Color(0xFF2D6A4F)
        val cellPart   = Color(0xFFD4A843)
        val cellEmpty  = if (isDark) Color(0xFF1E2F3D) else Color(0xFFDDE9E2)
        val textColor  = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1A2A1F)
        val primary    = if (isDark) Color(0xFFD4A843) else Color(0xFF2D6A4F)
        val labelClr   = if (isDark) Color(0xFF8B9EA7) else Color(0xFF5A7A60)

        val db       = AppDatabase.getInstance(context)
        val fmt      = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today    = LocalDate.now()

        // Find the ONE mini tracker category
        val allCats  = db.categoryDao().getRecordCategoriesOnce(userId)
        val miniCat  = allCats.firstOrNull { it.id == miniCatId && miniCatId > 0 }
            ?: allCats.firstOrNull { it.isInMiniTracker }
            ?: allCats.firstOrNull()
        val catLabel = "${miniCat?.emoji ?: ""} ${miniCat?.name ?: "LoF"}".trim()

        // Week Fri→Thu
        val daysFromFri = (today.dayOfWeek.value - DayOfWeek.FRIDAY.value + 7) % 7
        val weekStart   = today.minusDays(daysFromFri.toLong())
        val days        = (0..6).map { weekStart.plusDays(it.toLong()) }

        val dayScores = days.map { day ->
            val ds = day.format(fmt)
            if (miniCat == null) return@map 0f
            when (miniCat.builtinType) {
                "PLANK"   -> if (db.plankDao().getPlankByDateOnce(userId, ds)?.completed == true) 1f else 0f
                "PRAYERS" -> { val p = db.prayerDao().getPrayerByDateOnce(userId, ds)
                    (p?.let { listOf(it.fajr,it.dhuhr,it.asr,it.maghrib,it.isha).count{x->x} }?:0)/5f }
                "TASKS"   -> { val t = db.taskDao().getTasksByDateOnce(userId, ds)
                    if(t.isEmpty()) 0f else t.count{it.isCompleted}.toFloat()/t.size }
                else -> if (db.customEntryDao().getEntryOnce(userId, miniCat.id, ds)?.isDone == true) 1f else 0f
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            Box(modifier = GlanceModifier.fillMaxWidth().wrapContentHeight()
                .background(ColorProvider(bgColor))
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(openIntent))
                .padding(10.dp)) {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    // Header: tracking label
                    Row(modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                        Text("Tracking: $catLabel",
                            style = TextStyle(color = ColorProvider(primary),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    // Day bars
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        days.forEachIndexed { i, day ->
                            val isToday = day == today
                            val score   = dayScores[i]
                            val color   = when {
                                score >= 0.8f -> cellDone
                                score >= 0.4f -> cellPart
                                else          -> cellEmpty
                            }
                            val shortName = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2)
                            Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                                horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                                Text(shortName, style = TextStyle(
                                    color = ColorProvider(if (isToday) primary else labelClr),
                                    fontSize = 8.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal))
                                Spacer(GlanceModifier.height(3.dp))
                                Box(modifier = GlanceModifier.fillMaxWidth().height(28.dp)
                                    .background(ColorProvider(color)).cornerRadius(6.dp)) {}
                            }
                        }
                    }
                    Spacer(GlanceModifier.height(3.dp))
                    // Percentage summary
                    val doneCount = dayScores.count { it >= 0.8f }
                    Text("$doneCount/7 days",
                        style = TextStyle(color = ColorProvider(labelClr), fontSize = 9.sp))
                }
            }
        }
    }
}

class MiniTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MiniTrackerWidget()
}
