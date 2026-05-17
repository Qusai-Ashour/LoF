package com.leapoffaith.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.leapoffaith.app.data.entities.CategoryDefinition
import com.leapoffaith.app.navigation.NavRoutes
import com.leapoffaith.app.ui.prepare.parseColor
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordProgressScreen(viewModel: AppViewModel, navController: NavController, onBack: () -> Unit) {
    val categories         by viewModel.recordCategories.collectAsState()
    val prepCatsRecord     by viewModel.prepareCategories.collectAsState()
    val todayTasks         by viewModel.todayTasks.collectAsState()
    val plankToday         by viewModel.plankToday.collectAsState()
    val prayerToday        by viewModel.prayerToday.collectAsState()
    val todayCustomEntries by viewModel.todayCustomEntries.collectAsState()
    val isDark             by viewModel.isDarkTheme.collectAsState()
    val accent             by viewModel.userAccentHex.collectAsState()
    val todayLabel         = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    val bg      = if (isDark) NavyBackground else LightBackground
    val primary = parseColor(accent)
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary

    var editMode         by remember { mutableStateOf(false) }
    var editingWidgetCat    by remember { mutableStateOf<CategoryDefinition?>(null) }
    var confirmDeleteCat  by remember { mutableStateOf<CategoryDefinition?>(null) }

    // Compute isDone for sorting
    fun isDoneFor(cat: CategoryDefinition): Boolean {
        return when (cat.builtinType) {
            "PLANK"   -> plankToday?.completed == true
            "PRAYERS" -> (prayerToday?.let { listOf(it.fajr,it.dhuhr,it.asr,it.maghrib,it.isha).count{p->p} }?:0) == 5
            "TASKS"   -> todayTasks.isNotEmpty() && todayTasks.all { it.isCompleted }
            else -> {
                val prepPartner = prepCatsRecord.firstOrNull { it.name == cat.name }
                val checkIds = setOfNotNull(cat.id, prepPartner?.id)
                val entries = todayCustomEntries.filter { it.categoryId in checkIds }
                val subItemsList = parseSubItemsList(prepPartner?.subItems ?: cat.subItems ?: "")
                if (subItemsList.isNotEmpty()) {
                    val done = subItemsList.count { key -> entries.any { it.subItemKey == key && it.isDone } }
                    done == subItemsList.size && subItemsList.isNotEmpty()
                } else entries.any { it.isDone && it.subItemKey.isEmpty() }
            }
        }
    }

    val sortedCategories = remember(categories, plankToday, prayerToday, todayTasks, todayCustomEntries, prepCatsRecord) {
        categories.sortedBy { if (isDoneFor(it)) 1 else 0 }
    }

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("Record Progress", color = textPri)
                    Text(todayLabel, color = textSec, style = MaterialTheme.typography.labelSmall)
                } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                actions = {
                    IconButton(onClick = { editMode = !editMode }) {
                        Icon(if (editMode) Icons.Default.EditOff else Icons.Default.Edit, null, tint = primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Check for missing RECORD built-ins
        val recordBuiltinTypes = categories.map { it.builtinType }.toSet()
        val missingRecordBuiltins = listOf("TASKS" to "Today's Tasks", "PLANK" to "Plank", "PRAYERS" to "Daily Prayers")
            .filter { (type, _) -> type !in recordBuiltinTypes }

        if (editMode) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=4.dp)
                .clip(RoundedCornerShape(8.dp)).background(primary.copy(alpha=0.1f)).padding(8.dp)) {
                Text("Tap pencil to adjust widget settings", color = primary,
                    style = MaterialTheme.typography.labelSmall)
                }
                missingRecordBuiltins.forEach { (type, label) ->
                    TextButton(onClick = { viewModel.restoreBuiltin(type) },
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restore: $label", color = primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal=16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top=8.dp, bottom=16.dp)) {
                items(categories.sortedBy { cat -> if (isDoneFor(cat)) 1 else 0 }, key = { it.id }) { cat ->
                    val prepPartner = prepCatsRecord.firstOrNull { it.name == cat.name }
                    val checkIds    = setOfNotNull(cat.id, prepPartner?.id)
                    val catEntries  = todayCustomEntries.filter { it.categoryId in checkIds }
                    val subItemsList = parseSubItemsList(prepPartner?.subItems ?: cat.subItems ?: "")
                    val doneSubCount = subItemsList.count { key -> catEntries.any { it.subItemKey == key && it.isDone } }

                    val isWeekly = prepPartner?.prepareFrequency == "NEXT_WEEK" ||
                        cat.builtinType == "WEEK_MEALS" || cat.builtinType == "GYM_SPLIT"
                    val statusText = when (cat.builtinType) {
                        "TASKS"   -> "${todayTasks.count{it.isCompleted}}/${todayTasks.size} done"
                        "PLANK"   -> if (plankToday?.completed == true) "Done" else "Not yet"
                        "PRAYERS" -> "${prayerToday?.let{listOf(it.fajr,it.dhuhr,it.asr,it.maghrib,it.isha).count{p->p}}?:0}/5 prayed"
                        else -> if (isWeekly) "Tap to view week"
                            else if (subItemsList.isNotEmpty()) "$doneSubCount/${subItemsList.size} done"
                            else if (catEntries.any { it.isDone && it.subItemKey.isEmpty() }) "Done"
                            else "Tap to record"
                    }
                    val isDone = isDoneFor(cat)

                    RecordCategoryCard(cat, statusText, isDone, isDark, editMode, primary,
                        hasPreparePair = prepCatsRecord.any { it.name == cat.name },
                        onDelete = { confirmDeleteCat = cat },
                        onClick = {
                            if (!editMode) when (cat.builtinType) {
                                "PLANK"   -> navController.navigate(NavRoutes.PLANK)
                                "PRAYERS" -> navController.navigate(NavRoutes.PRAYERS)
                                "TASKS"   -> navController.navigate(NavRoutes.TODAY_TASKS)
                                else      -> navController.navigate(NavRoutes.recordCustomRoute(cat.id))
                            }
                        },
                        onEdit = { editingWidgetCat = cat }
                    )
                }
            }
        }
    }

    // Widget options editor (only thing editable from Record Progress)
    confirmDeleteCat?.let { cat ->
        AlertDialog(onDismissRequest = { confirmDeleteCat = null },
            containerColor = if (isDark) NavyCard else LightCard,
            title = { Text("Delete \"${cat.name}\"?", color = if (isDark) TextPrimary else LightTextPrimary) },
            text = { Text("Historical data preserved.", color = if (isDark) TextSecondary else LightTextSecondary) },
            confirmButton = { TextButton(onClick = { viewModel.deleteCategory(cat); confirmDeleteCat = null }) {
                Text("Delete", color = if (isDark) RedMissed else LightRedMissed) } },
            dismissButton = { TextButton(onClick = { confirmDeleteCat = null }) { Text("Cancel") } }
        )
    }

    editingWidgetCat?.let { cat ->
        WidgetOptionsDialog(cat, isDark, accent,
            onDismiss = { editingWidgetCat = null },
            onConfirm = { updated -> viewModel.updateCategoryDirect(updated); editingWidgetCat = null }
        )
    }
}

@Composable
private fun RecordCategoryCard(cat: CategoryDefinition, statusText: String, isDone: Boolean,
    isDark: Boolean, editMode: Boolean, primary: androidx.compose.ui.graphics.Color,
    hasPreparePair: Boolean = false, onDelete: () -> Unit = {},
    onClick: () -> Unit, onEdit: () -> Unit) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val doneClr = if (isDark) GreenDone     else LightGreenDone
    val accent  = parseColor(cat.color)

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(if (isDone && !editMode) doneClr.copy(alpha=0.1f) else cardBg)
        .clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
            Text(cat.emoji, fontSize = 26.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(cat.name, color = textPri, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(statusText, color = if (isDone && !editMode) doneClr else textSec,
                style = MaterialTheme.typography.labelSmall)
            if (cat.showInWidget) Text("In widget", color = accent.copy(alpha=0.7f),
                style = MaterialTheme.typography.labelSmall)
            if (cat.isInMiniTracker) Text("Mini Tracker",
                color = parseColor("#818CF8").copy(alpha=0.8f),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
        if (editMode) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Widgets, null, tint = primary, modifier = Modifier.size(18.dp))
            }
            if (!hasPreparePair && cat.builtinType.isEmpty()) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, null,
                        tint = if (isDark) RedMissed.copy(alpha=0.8f) else LightRedMissed.copy(alpha=0.8f),
                        modifier = Modifier.size(18.dp))
                }
            }
        } else {
            Icon(if (isDone) Icons.Default.CheckCircle else Icons.Default.ChevronRight, null,
                tint = if (isDone) doneClr else textDis, modifier = Modifier.size(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetOptionsDialog(cat: CategoryDefinition, isDark: Boolean, accent: String,
    onDismiss: () -> Unit, onConfirm: (CategoryDefinition) -> Unit) {
    val cardBg = if (isDark) NavyCard else LightCard
    val pr     = parseColor(accent)
    val textPri = if (isDark) TextPrimary else LightTextPrimary
    var inWidget by remember { mutableStateOf(cat.showInWidget) }
    var inMini   by remember { mutableStateOf(cat.isInMiniTracker) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = cardBg,
        title = { Text("Widget Options — ${cat.name}", color = pr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleRow("Show in Snapshot Widget (max 8)", inWidget, isDark, pr) { inWidget = it }
                ToggleRow("Use as Mini Tracker", inMini, isDark, pr) { v -> inMini = v; if (v) inWidget = true }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(cat.copy(showInWidget=inWidget, isInMiniTracker=inMini)) }) {
                Text("Save", color = pr)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun parseSubItemsList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try { val a = JSONArray(json); (0 until a.length()).map { a.getString(it) } }
    catch (_: Exception) { emptyList() }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, isDark: Boolean,
    primary: androidx.compose.ui.graphics.Color, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = if (isDark) NavyBackground else LightCard, checkedTrackColor = primary))
        Text(label, color = if (isDark) TextSecondary else LightTextSecondary,
            style = MaterialTheme.typography.bodySmall)
    }
}
