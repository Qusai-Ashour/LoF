package com.leapoffaith.app.ui.history
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.leapoffaith.app.data.entities.*
import com.leapoffaith.app.data.entities.CustomEntry
import com.leapoffaith.app.navigation.NavRoutes
import com.leapoffaith.app.ui.prepare.parseColor
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: AppViewModel, editMode: Boolean = false, navController: androidx.navigation.NavController? = null, onBack: () -> Unit) {
    val isDark      by viewModel.isDarkTheme.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context     = LocalContext.current
    val fmt         = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today       = LocalDate.now()
    

    val bg      = if (isDark) NavyBackground else LightBackground
    val cardBg  = if (isDark) NavyCard       else LightCard
    val primary = if (isDark) Gold           else ForestGreen
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary
    val textDis = if (isDark) TextDisabled   else LightTextDisabled
    val doneClr = if (isDark) GreenDone      else LightGreenDone

    var selectedCatIndex by remember { mutableIntStateOf(0) }
    var periodDays       by remember { mutableIntStateOf(30) }
    var isEditMode       by remember { mutableStateOf(editMode) }
    var showPdfPicker    by remember { mutableStateOf(false) }

    val startDate = if (periodDays == 0) "2020-01-01" else today.minusDays(periodDays.toLong()).format(fmt)
    val endDate   = today.minusDays(1).format(fmt)

    val recordCats  by viewModel.repository_cats_record(currentUser).collectAsState(initial = emptyList())
    val prepareCats by viewModel.prepareCategories.collectAsState()
        val tasks      by viewModel.repository_tasks_range(currentUser, startDate, endDate).collectAsState(initial = emptyList())
    val planks     by viewModel.repository_planks_range(currentUser, startDate, endDate).collectAsState(initial = emptyList())
    val buriedEntries by viewModel.buriedEntries.collectAsState()
    var includeBuried by remember { mutableStateOf(true) }
    val allPlanks  by viewModel.repository_planks_range(currentUser, "2020-01-01", today.format(fmt)).collectAsState(initial = emptyList())
    val prayers    by viewModel.repository_prayers_range(currentUser, startDate, endDate).collectAsState(initial = emptyList())
    val customs    by viewModel.repository_customs_range(currentUser, startDate, endDate).collectAsState(initial = emptyList())
    val allCustoms by viewModel.repository_customs_range(currentUser, "2020-01-01", today.format(fmt)).collectAsState(initial = emptyList())

    val selectedCat = recordCats.getOrNull(selectedCatIndex)

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit History" else "History", color = textPri) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                actions = {
                    IconButton(onClick = { navController?.navigate(NavRoutes.DEAD_HABITS) }) {
                        Icon(Icons.Default.FolderOff, null, tint = primary)
                    }
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit, null, tint = primary)
                    }
                    IconButton(onClick = { exportCsv(context, tasks, planks, prayers, customs, recordCats, periodDays) }) {
                        Icon(Icons.Default.TableChart, null, tint = primary)
                    }
                    IconButton(onClick = { showPdfPicker = true }) {
                        Icon(Icons.Default.Print, null, tint = primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Category buttons
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                recordCats.forEachIndexed { i, cat ->
                    val sel    = selectedCatIndex == i
                    val accent = parseColor(cat.color)
                    FilterChip(selected = sel, onClick = { selectedCatIndex = i },
                        label = { Text("${cat.emoji} ${cat.name}", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent, selectedLabelColor = Color.White,
                            containerColor = accent.copy(alpha = 0.12f), labelColor = textSec))
                }
            }

            // Period filter
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(7 to "7 days", 30 to "30 days", 90 to "90 days", 0 to "All time").forEach { (days, label) ->
                    FilterChip(selected = periodDays == days, onClick = { periodDays = days },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primary,
                            selectedLabelColor = if (isDark) NavyBackground else LightCard,
                            containerColor = if (isDark) NavyCardLight else LightCardVariant, labelColor = textSec))
                }
            }

            if (isEditMode && buriedEntries.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Checkbox(checked = includeBuried, onCheckedChange = { includeBuried = it },
                        colors = CheckboxDefaults.colors(checkedColor = primary),
                        modifier = Modifier.size(20.dp))
                    Text("Include buried habits in PDF export", color = textSec,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            if (isEditMode) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp)).background(primary.copy(alpha=0.1f)).padding(8.dp)) {
                    Text("Edit mode: tap entries to toggle", color = primary, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(4.dp))

            when (selectedCat?.builtinType) {
                "TASKS"   -> TaskHistoryList(tasks, isDark, isEditMode, viewModel, cardBg, textPri, textSec, textDis, doneClr)
                "PLANK"   -> PlankHistoryList(allPlanks, isDark, isEditMode, viewModel, cardBg, textPri, textSec, textDis, doneClr, primary)
                "PRAYERS" -> PrayerHistoryList(prayers, isDark, isEditMode, viewModel, cardBg, textPri, textSec, doneClr)
                else      -> if (selectedCat != null) {
                    val prepPartnerHistory = prepareCats.firstOrNull { it.name == selectedCat.name }
                    val catEntries = customs.filter { it.categoryId == selectedCat.id || it.categoryId == prepPartnerHistory?.id }
                    val allCatEntries = allCustoms.filter { it.categoryId == selectedCat.id || it.categoryId == prepPartnerHistory?.id }
                    if (selectedCat.frequency == "ONCE_DAILY" && selectedCat.builtinType.isEmpty()) {
                        val streak = computeCustomStreak(allCatEntries)
                        if (streak > 0 && allCatEntries.isNotEmpty()) {
                            // Show streak card same as Plank
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(cardBg).padding(16.dp),
                                contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$streak", color = primary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                                    Text("day streak", color = textSec)
                                }
                            }
                        }
                    }
                    CustomHistoryList(selectedCat, catEntries,
                        isDark, isEditMode, viewModel, cardBg, textPri, textSec, textDis, doneClr)
                }
            }
        }
    }
    if (showPdfPicker) {
        PdfCategoryPickerDialog(
            isDark = isDark, primary = primary, cats = recordCats,
            onDismiss = { showPdfPicker = false },
            onPick = { picked ->
                if (picked == null) {
                    exportPdf(context, tasks, allPlanks, prayers, customs, recordCats, periodDays,
                        if (includeBuried) buriedEntries else emptyList())
                } else {
                    exportCategoryPdf(context, picked, tasks, allPlanks, prayers,
                        customs.filter { it.categoryId == picked.id ||
                            it.categoryId == prepareCats.firstOrNull { p -> p.name == picked.name }?.id },
                        periodDays, if (includeBuried) buriedEntries else emptyList())
                }
            }
        )
    }
}




@Composable
private fun PdfCategoryPickerDialog(
    isDark: Boolean, primary: Color, cats: List<CategoryDefinition>,
    onDismiss: () -> Unit, onPick: (CategoryDefinition?) -> Unit
) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    AlertDialog(onDismissRequest = onDismiss, containerColor = cardBg,
        title = { Text("Export PDF", color = primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { onPick(null); onDismiss() },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.SelectAll, null, tint = primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("All Categories", color = primary,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                cats.forEach { cat ->
                    TextButton(onClick = { onPick(cat); onDismiss() },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("${cat.emoji} ${cat.name}", color = textPri,
                            modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = textSec) } }
    )
}

@Composable
private fun TaskHistoryList(tasks: List<Task>, isDark: Boolean, editMode: Boolean,
    viewModel: AppViewModel, cardBg: Color, textPri: Color, textSec: Color, textDis: Color, doneClr: Color) {
    val byDate = tasks.groupBy { it.date }.entries.sortedByDescending { it.key }
    if (byDate.isEmpty()) { EmptyState(textSec, textDis); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(byDate) { (date, dayTasks) ->
            val done = dayTasks.count { it.isCompleted }
            val pct  = if (dayTasks.isEmpty()) 0f else done.toFloat() / dayTasks.size
            val c    = when { pct >= 0.8f -> doneClr; pct >= 0.4f -> YellowPartial; else -> if (isDark) RedMissed else LightRedMissed }
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(fmtDate(date), color = textPri, fontWeight = FontWeight.SemiBold)
                    Text("$done/${dayTasks.size}", color = c, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                dayTasks.forEach { task ->
                    Row(modifier = Modifier.fillMaxWidth()
                        .then(if (editMode) Modifier.clickable { viewModel.toggleTask(task) } else Modifier),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
                            tint = if (task.isCompleted) doneClr else textDis, modifier = Modifier.size(14.dp))
                        Text(task.title, color = if (task.isCompleted) textSec else textDis, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlankHistoryList(planks: List<PlankEntry>, isDark: Boolean, editMode: Boolean,
    viewModel: AppViewModel, cardBg: Color, textPri: Color, textSec: Color, textDis: Color, doneClr: Color, primary: Color) {
    val sorted = planks.sortedByDescending { it.date }
    if (sorted.isEmpty()) { EmptyState(textSec, textDis); return }
    val streak = computeStreak(sorted)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(16.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("...", fontSize = 28.sp)
                    Text("$streak", color = primary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text("day streak", color = textSec)
                }
            }
        }
        items(sorted) { entry ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(cardBg)
                .then(if (editMode) Modifier.clickable { viewModel.togglePlankEntry(entry) } else Modifier).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(fmtDate(entry.date), color = textPri)
                Icon(if (entry.completed) Icons.Default.CheckCircle else Icons.Default.Cancel, null,
                    tint = if (entry.completed) doneClr else if (isDark) RedMissed else LightRedMissed,
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PrayerHistoryList(prayers: List<PrayerEntry>, isDark: Boolean, editMode: Boolean,
    viewModel: AppViewModel, cardBg: Color, textPri: Color, textSec: Color, doneClr: Color) {
    if (editMode) {
        val accent by viewModel.userAccentHex.collectAsState()
        val primary = com.leapoffaith.app.ui.prepare.parseColor(accent)
        AddMissingDateRow(primary, "+ Add a missing date") { date ->
            viewModel.addMissingPrayerDay(date)
        }
    }
    val sorted = prayers.sortedByDescending { it.date }
    val names  = listOf("Fajr","Dhuhr","Asr","Maghrib","Isha")
    if (sorted.isEmpty()) { EmptyState(textSec, if (isDark) TextDisabled else LightTextDisabled); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sorted) { entry ->
            val statuses = listOf(entry.fajr, entry.dhuhr, entry.asr, entry.maghrib, entry.isha)
            val done     = statuses.count { it }
            val c        = if (done == 5) doneClr else if (done >= 3) YellowPartial else if (isDark) RedMissed else LightRedMissed
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(fmtDate(entry.date), color = textPri, fontWeight = FontWeight.SemiBold)
                    Text("$done/5", color = c, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    names.forEachIndexed { i, name ->
                        val pDone = statuses[i]
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (pDone) doneClr.copy(alpha=0.2f) else (if (isDark) RedMissed else LightRedMissed).copy(alpha=0.15f))
                            .then(if (editMode) Modifier.clickable { viewModel.togglePrayerForHistory(entry, name.lowercase()) } else Modifier)
                            .padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Text(name.take(3), color = if (pDone) doneClr else if (isDark) RedMissed else LightRedMissed,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomHistoryList(cat: CategoryDefinition, entries: List<CustomEntry>,
    isDark: Boolean, editMode: Boolean, viewModel: AppViewModel,
    cardBg: Color, textPri: Color, textSec: Color, textDis: Color, doneClr: Color) {
    if (editMode && cat.frequency == "ONCE_DAILY") {
        val accent by viewModel.userAccentHex.collectAsState()
        val primary = com.leapoffaith.app.ui.prepare.parseColor(accent)
        AddMissingDateRow(primary, "+ Add a missing date") { date ->
            viewModel.addMissingCustomDay(cat.id, date)
        }
    }
    val byDate = entries.groupBy { it.date }.entries.sortedByDescending { it.key }
    if (byDate.isEmpty()) { EmptyState(textSec, textDis); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(byDate) { (date, dayEntries) ->
            val allDone = dayEntries.isNotEmpty() && dayEntries.all { it.isDone }
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(cardBg)
                .then(if (editMode && dayEntries.size == 1) Modifier.clickable { viewModel.toggleCustomEntryById(dayEntries.first()) } else Modifier)
                .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(fmtDate(date), color = textPri)
                    if (dayEntries.any { it.subItemKey.isNotEmpty() || it.notes.isNotEmpty() }) {
                        val done = dayEntries.count { it.isDone }
                        Text("$done/${dayEntries.size} done", color = parseColor(cat.color),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                Icon(if (allDone) Icons.Default.CheckCircle else Icons.Default.Cancel, null,
                    tint = if (allDone) doneClr else if (isDark) RedMissed else LightRedMissed,
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(textSec: Color, textDis: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.History, null, tint = textDis, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text("No history in this period", color = textSec)
        }
    }
}

private fun fmtDate(d: String) = try {
    LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        .format(DateTimeFormatter.ofPattern("EEE, MMM d"))
} catch (e: Exception) { d }

private fun computeCustomStreak(entries: List<CustomEntry>): Int {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val sorted = entries.filter { it.isDone && it.subItemKey.isEmpty() }.sortedByDescending { it.date }
    val today = LocalDate.now()
    var expected = if (sorted.any { it.date == today.format(fmt) }) today else today.minusDays(1)
    var streak = 0
    for (e in sorted) {
        val d = try { LocalDate.parse(e.date, fmt) } catch (_: Exception) { continue }
        if (d == expected) { streak++; expected = expected.minusDays(1) } else if (d.isBefore(expected)) break
    }
    return streak
}

private fun computeStreak(sorted: List<PlankEntry>): Int {
    var streak = 0
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    // Allow streak to start from today OR yesterday (in case today not yet done)
    var expected = if (sorted.any { it.date == today.format(fmt) && it.completed }) today
                   else today.minusDays(1)
    for (e in sorted) {
        if (!e.completed) continue
        val d = LocalDate.parse(e.date, fmt)
        if (d == expected) { streak++; expected = expected.minusDays(1) }
        else if (d.isBefore(expected)) break
    }
    return streak
}

private fun exportCategoryPdf(context: android.content.Context,
    cat: com.leapoffaith.app.data.entities.CategoryDefinition?,
    tasks: List<Task>, planks: List<PlankEntry>, prayers: List<PrayerEntry>,
    catEntries: List<CustomEntry>, periodDays: Int, buriedEntries: List<CustomEntry> = emptyList()) {
    if (cat == null) return
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val label = if (periodDays == 0) "All Time" else "Last $periodDays Days"
    val start = if (periodDays == 0) LocalDate.of(2020,1,1) else today.minusDays(periodDays.toLong())
    val rows  = StringBuilder()
    var d = today.minusDays(1)
    while (!d.isBefore(start)) {
        val ds = d.format(fmt)
        val display = d.format(DateTimeFormatter.ofPattern("EEE MMM d"))
        when (cat.builtinType) {
            "TASKS" -> {
                val dt = tasks.filter { it.date == ds }
                if (dt.isNotEmpty()) rows.append("<tr><td>$display</td><td>${dt.count{it.isCompleted}}/${dt.size}</td></tr>")
            }
            "PLANK" -> {
                val pl = planks.firstOrNull { it.date == ds }
                if (pl != null) rows.append("<tr><td>$display</td><td>${if(pl.completed)"Done" else "Missed"}</td></tr>")
            }
            "PRAYERS" -> {
                val pr = prayers.firstOrNull { it.date == ds }
                if (pr != null) {
                    val c = listOf(pr.fajr,pr.dhuhr,pr.asr,pr.maghrib,pr.isha).count{it}
                    rows.append("<tr><td>$display</td><td>$c/5</td></tr>")
                }
            }
            else -> {
                val e = catEntries.filter { it.date == ds && it.isDone }
                if (e.isNotEmpty()) rows.append("<tr><td>$display</td><td>${e.size} done</td></tr>")
            }
        }
        d = d.minusDays(1)
    }
    val html = """<html><head><style>
        body{font-family:sans-serif;padding:16px}h2{color:#2D6A4F}
        table{width:100%;border-collapse:collapse;font-size:12px}
        th{background:#E8F5E9;color:#2D6A4F;padding:8px;text-align:left}
        td{padding:6px;border-bottom:1px solid #E8F5E9}
    </style></head><body>
    <h2>${cat.emoji} ${cat.name} — $label</h2>
    <table><tr><th>Date</th><th>Result</th></tr>${rows}</table>
    ${if (buriedEntries.isNotEmpty()) """
    <h3 style='color:#888;margin-top:20px'>No Longer Tracked</h3>
    <table><tr><th>Habit</th><th>Times Done</th><th>Last Done</th></tr>
    ${buriedEntries.groupBy { e -> e.subItemKey.ifEmpty { e.notes.ifEmpty { "Entry" } } }
        .entries.joinToString("") { (label, entries) ->
        "<tr><td>$label</td><td>${entries.size}</td><td>${entries.sortedByDescending{it.date}.firstOrNull()?.date?:"-"}</td></tr>" }}
    </table>""" else ""}
    </body></html>"""
    val wv = android.webkit.WebView(context)
    wv.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(v: android.webkit.WebView, url: String) {
            val pm = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            pm.print("LoF_${cat.name}_$label", v.createPrintDocumentAdapter("LoF"),
                android.print.PrintAttributes.Builder()
                    .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4).build())
        }
    }
    wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

private fun exportCsv(context: Context, tasks: List<Task>, planks: List<PlankEntry>,
    prayers: List<PrayerEntry>, customs: List<CustomEntry>, cats: List<CategoryDefinition>, periodDays: Int) {
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val start = if (periodDays == 0) LocalDate.of(2020,1,1) else today.minusDays(periodDays.toLong())
    val customCats    = cats.filter { it.builtinType.isEmpty() }
    val sb            = StringBuilder()
    val tasksByDate   = tasks.groupBy { it.date }
    val plankByDate   = planks.associateBy { it.date }
    val prayerByDate  = prayers.associateBy { it.date }
    val customsByDate = customs.groupBy { it.date }
    sb.append("Date,Tasks Done,Tasks Total,Plank,Prayers Done")
    customCats.forEach { sb.append(",${it.name}") }
    sb.append("\n")
    var d = today
    while (!d.isBefore(start)) {
        val ds = d.format(fmt)
        val dt = tasksByDate[ds] ?: emptyList()
        val pl = plankByDate[ds]
        val pr = prayerByDate[ds]
        val cu = customsByDate[ds] ?: emptyList()
        val pd = pr?.let { listOf(it.fajr,it.dhuhr,it.asr,it.maghrib,it.isha).count{p->p} } ?: 0
        sb.append("$ds,${dt.count{it.isCompleted}},${dt.size},${if(pl?.completed==true)"Yes" else "No"},$pd")
        customCats.forEach { cat -> sb.append(",${if(cu.any{it.categoryId==cat.id&&it.isDone})"Yes" else "No"}") }
        sb.append("\n")
        d = d.minusDays(1)
    }
    try {
        val file = File(context.getExternalFilesDir(null), "LoF_${today.format(fmt)}.csv")
        file.writeText(sb.toString())
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Export CSV"))
    } catch (_: Exception) {}
}

private fun exportPdf(context: Context, tasks: List<Task>, planks: List<PlankEntry>,
    prayers: List<PrayerEntry>, customs: List<CustomEntry>, cats: List<CategoryDefinition>, periodDays: Int,
    buriedEntries: List<CustomEntry> = emptyList()) {
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val label = if (periodDays == 0) "All Time" else "Last $periodDays Days"
    val start = if (periodDays == 0) LocalDate.of(2020,1,1) else today.minusDays(periodDays.toLong())
    val customCats    = cats.filter { it.builtinType.isEmpty() }
    val tasksByDate   = tasks.groupBy { it.date }
    val plankByDate   = planks.associateBy { it.date }
    val prayerByDate  = prayers.associateBy { it.date }
    val customsByDate = customs.groupBy { it.date }
    val headers = customCats.joinToString("") { "<th>${it.name}</th>" }
    val rows    = StringBuilder()
    var d = today
    while (!d.isBefore(start)) {
        val ds = d.format(fmt)
        val dt = tasksByDate[ds] ?: emptyList()
        val pl = plankByDate[ds]
        val pr = prayerByDate[ds]
        val cu = customsByDate[ds] ?: emptyList()
        val pd = pr?.let { listOf(it.fajr,it.dhuhr,it.asr,it.maghrib,it.isha).count{p->p} } ?: 0
        rows.append("<tr><td>${d.format(DateTimeFormatter.ofPattern("EEE MMM d"))}</td>")
        rows.append("<td>${dt.count{it.isCompleted}}/${dt.size}</td>")
        rows.append("<td style='color:${if(pl?.completed==true)"#2D6A4F" else "#D62839"}'>${if(pl?.completed==true)"Y" else "N"}</td>")
        rows.append("<td>$pd/5</td>")
        customCats.forEach { cat -> rows.append("<td style='color:${if(cu.any{it.categoryId==cat.id&&it.isDone})"#2D6A4F" else "#D62839"}'>${if(cu.any{it.categoryId==cat.id&&it.isDone})"Y" else "N"}</td>") }
        rows.append("</tr>")
        d = d.minusDays(1)
    }
    val html = "<html><head><style>@page{size:A4 landscape;margin:8mm}" +
        "body{font-family:sans-serif;background:#fff;color:#111;padding:8px}" +
        "h2{color:#2D6A4F;font-size:16px}table{width:100%;border-collapse:collapse;font-size:11px}" +
        "th{background:#E8F5E9;color:#2D6A4F;padding:6px;border-bottom:2px solid #95D5B2}" +
        "td{padding:5px;border-bottom:1px solid #E8F5E9}</style></head><body>" +
        "<h2>LoF History - $label</h2><table><tr><th>Date</th><th>Tasks</th><th>Plank</th><th>Prayers</th>$headers</tr>$rows</table></body></html>"
    val wv = WebView(context)
    wv.webViewClient = object : WebViewClient() {
        override fun onPageFinished(v: WebView, url: String) {
            val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            pm.print("LoF_History_$label", v.createPrintDocumentAdapter("LoF"),
                PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape()).build())
        }
    }
    wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddMissingDateRow(primary: Color, label: String, onPick: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    androidx.compose.material3.TextButton(
        onClick = { show = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.material3.Icon(
            androidx.compose.material.icons.Icons.Default.AddCircleOutline, null,
            tint = primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = primary, style = MaterialTheme.typography.labelMedium)
    }
    if (show) {
        val state = androidx.compose.material3.rememberDatePickerState()
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = java.time.Instant.ofEpochMilli(ms)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        onPick(d)
                    }
                    show = false
                }) { Text("Add", color = primary) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { show = false }) { Text("Cancel") }
            }
        ) {
            androidx.compose.material3.DatePicker(state = state)
        }
    }
}

