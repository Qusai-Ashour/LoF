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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SnapshotWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs  = context.getSharedPreferences("lof_widget", Context.MODE_PRIVATE)
        val userId = prefs.getString("widget_user", "qusai") ?: "qusai"
        val isDark = prefs.getBoolean("is_dark_theme", true)

        val bgColor    = if (isDark) Color(0xFF0D1B2A) else Color(0xFFF0F4F1)
        val textColor  = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1A2A1F)
        val labelColor = if (isDark) Color(0xFF8B9EA7) else Color(0xFF5A7A60)
        val primary    = if (isDark) Color(0xFFD4A843) else Color(0xFF2D6A4F)
        val cellDone   = if (isDark) Color(0xFF2D6A4F) else Color(0xFFB7E4C7)
        val cellEmpty  = if (isDark) Color(0xFF1E2F3D) else Color(0xFFDDE9E2)

        val db    = AppDatabase.getInstance(context)
        val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now().format(fmt)
        val dayLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d"))

        // Only show categories marked showInWidget
        val allCats = db.categoryDao().getRecordCategoriesOnce(userId)
        val widgetCats = allCats.filter { it.showInWidget }
        val displayCats = if (widgetCats.isEmpty()) allCats.take(8) else widgetCats.take(8)

        data class SnapItem(val emoji: String, val name: String, val value: String, val done: Boolean)
        val snapItems = mutableListOf<SnapItem>()
        displayCats.forEach { cat ->
            when (cat.builtinType) {
                "TASKS" -> {
                    val tasks = db.taskDao().getTasksByDateOnce(userId, today)
                    val done  = tasks.count { it.isCompleted }
                    snapItems.add(SnapItem(cat.emoji, cat.name.take(7), "$done/${tasks.size}", done == tasks.size && tasks.isNotEmpty()))
                }
                "PLANK" -> {
                    val pl = db.plankDao().getPlankByDateOnce(userId, today)
                    snapItems.add(SnapItem(cat.emoji, "Plank", if (pl?.completed==true) "Done" else "No", pl?.completed==true))
                }
                "PRAYERS" -> {
                    val pr = db.prayerDao().getPrayerByDateOnce(userId, today)
                    val d  = pr?.let { listOf(it.fajr,it.dhuhr,it.asr,it.maghrib,it.isha).count{p->p} } ?: 0
                    snapItems.add(SnapItem(cat.emoji, "Prayers", "$d/5", d==5))
                }
                else -> {
                    // Check both RECORD and linked PREPARE category IDs
                    val allPrep = db.categoryDao().getPrepareCategoriesOnce(userId)
                    val prepPartner = allPrep.firstOrNull { it.name == cat.name }
                    val checkIds = listOfNotNull(cat.id, prepPartner?.id)
                    val entries = checkIds.flatMap {
                        db.customEntryDao().getEntriesForDateOnce(userId, today)
                            .filter { e -> e.categoryId == it }
                    }
                    val isDone = if (cat.frequency == "MULTIPLE_DAILY") {
                        entries.filter { it.subItemKey.isNotEmpty() }.let { subs ->
                            subs.isNotEmpty() && subs.all { it.isDone }
                        }
                    } else {
                        entries.any { it.isDone && it.subItemKey.isEmpty() }
                    }
                    val value = if (isDone) "Done" else "No"
                    snapItems.add(SnapItem(cat.emoji, cat.name.take(7), value, isDone))
                }
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
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Text("LoF", style = TextStyle(color = ColorProvider(primary),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        Spacer(GlanceModifier.defaultWeight())
                        Text(dayLabel, style = TextStyle(color = ColorProvider(labelColor), fontSize = 9.sp))
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    // Row 1: up to 4 items
                    if (snapItems.isNotEmpty()) {
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            snapItems.take(4).forEach { item ->
                                Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                                    Text(item.emoji, style = TextStyle(fontSize = 14.sp))
                                    Text(item.value, style = TextStyle(
                                        color = ColorProvider(if (item.done) cellDone else textColor),
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                    Text(item.name, style = TextStyle(color = ColorProvider(labelColor), fontSize = 8.sp))
                                }
                            }
                        }
                    }
                    // Row 2: items 5-8
                    if (snapItems.size > 4) {
                        Spacer(GlanceModifier.height(6.dp))
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            snapItems.drop(4).take(4).forEach { item ->
                                Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                                    Text(item.emoji, style = TextStyle(fontSize = 14.sp))
                                    Text(item.value, style = TextStyle(
                                        color = ColorProvider(if (item.done) cellDone else textColor),
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                    Text(item.name, style = TextStyle(color = ColorProvider(labelColor), fontSize = 8.sp))
                                }
                            }
                            // Pad remaining slots
                            repeat(4 - (snapItems.size - 4).coerceAtMost(4)) {
                                Spacer(GlanceModifier.defaultWeight())
                            }
                        }
                    }
                }
            }
        }
    }
}

class SnapshotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SnapshotWidget()
}
