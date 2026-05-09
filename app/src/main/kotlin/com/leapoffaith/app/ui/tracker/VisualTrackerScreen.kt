package com.leapoffaith.app.ui.tracker
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leapoffaith.app.data.entities.CategoryDefinition
import com.leapoffaith.app.ui.prepare.parseColor
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import com.leapoffaith.app.viewmodel.DayTrackerData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualTrackerScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val trackerMonth by viewModel.trackerMonth.collectAsState()
    val trackerData  by viewModel.trackerData.collectAsState()
    val recordCats   by viewModel.recordCategories.collectAsState()
    val isDark       by viewModel.isDarkTheme.collectAsState()
    val context      = LocalContext.current
    val fmt          = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val bg      = if (isDark) NavyBackground else LightBackground
    val cardBg  = if (isDark) NavyCard       else LightCard
    val primary = if (isDark) Gold           else ForestGreen
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary
    val textDis = if (isDark) TextDisabled   else LightTextDisabled
    val doneClr = if (isDark) GreenDone      else LightGreenDone

    var selectedCatIndex by remember { mutableIntStateOf(0) }
    var viewMode         by remember { mutableStateOf("MONTH") }
    var selectedDay      by remember { mutableStateOf(LocalDate.now()) }
    var showPrintDialog  by remember { mutableStateOf(false) }
    var showDaySheet     by remember { mutableStateOf<String?>(null) }
    val sheetState       = rememberModalBottomSheetState()
    val selectedCat      = recordCats.getOrNull(selectedCatIndex)

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Visual Tracker", color = textPri) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                actions = { IconButton(onClick = { showPrintDialog = true }) { Icon(Icons.Default.Print, null, tint = primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Category buttons
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
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

            // View mode
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("DAY","WEEK","MONTH").forEach { mode ->
                    val sel = viewMode == mode
                    OutlinedButton(onClick = { viewMode = mode }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (sel) primary else Color.Transparent,
                            contentColor = if (sel) (if (isDark) NavyBackground else LightCard) else primary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primary)) {
                        Text(mode, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            when (viewMode) {
                "DAY"  -> DayView(trackerData, recordCats, selectedDay, isDark, cardBg, primary, textPri, textSec, textDis, doneClr,
                    onPrev = { selectedDay = selectedDay.minusDays(1) },
                    onNext = { if (selectedDay < LocalDate.now()) selectedDay = selectedDay.plusDays(1) })
                "WEEK" -> WeekView(trackerData, recordCats, selectedCatIndex, isDark, primary, textPri, textSec, doneClr,
                    onDayClick = { day -> selectedDay = LocalDate.parse(day, fmt); viewMode = "DAY" })
                else   -> MonthView(trackerMonth, trackerData, selectedCat, isDark, primary, textPri, textSec, doneClr,
                    onPrev = { viewModel.prevMonth() }, onNext = { viewModel.nextMonth() },
                    onDayClick = { showDaySheet = it })
            }
        }
    }

    if (showDaySheet != null) {
        ModalBottomSheet(onDismissRequest = { showDaySheet = null }, sheetState = sheetState,
            containerColor = if (isDark) NavyCard else LightCard) {
            val d = trackerData[showDaySheet!!]
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(showDaySheet!!, style = MaterialTheme.typography.titleLarge, color = primary)
                Spacer(Modifier.height(8.dp))
                if (d != null) {
                    if (recordCats.any { it.builtinType == "TASKS" })
                        SummaryRow("Tasks", "${d.taskDone}/${d.taskTotal}", textPri, textSec)
                    if (recordCats.any { it.builtinType == "PLANK" })
                        SummaryRow("Plank", if (d.plankDone) "Done" else "Not logged", textPri, textSec)
                    if (recordCats.any { it.builtinType == "PRAYERS" })
                        SummaryRow("Prayers", "${d.prayersDone}/5", textPri, textSec)
                    recordCats.filter { it.builtinType.isEmpty() }.forEach { cat ->
                        SummaryRow(cat.name, if (d.customEntries[cat.id] == true) "Done" else "Not done", textPri, textSec)
                    }
                } else Text("No data for this day.", color = textSec)
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showPrintDialog) {
        var printAll by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showPrintDialog = false },
            containerColor = if (isDark) NavyCard else LightCard,
            title = { Text("Print", color = primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Categories:", color = textSec, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !printAll, onClick = { printAll = false },
                            label = { Text(selectedCat?.name ?: "Current") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primary,
                                selectedLabelColor = if (isDark) NavyBackground else LightCard,
                                containerColor = if (isDark) NavyCardLight else LightCardVariant, labelColor = textSec))
                        FilterChip(selected = printAll, onClick = { printAll = true },
                            label = { Text("All categories") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primary,
                                selectedLabelColor = if (isDark) NavyBackground else LightCard,
                                containerColor = if (isDark) NavyCardLight else LightCardVariant, labelColor = textSec))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cats = if (printAll) recordCats else listOfNotNull(selectedCat)
                    printReport(context, trackerMonth, trackerData, cats, viewMode)
                    showPrintDialog = false
                }) { Text("Print", color = primary) }
            },
            dismissButton = { TextButton(onClick = { showPrintDialog = false }) { Text("Cancel", color = textSec) } }
        )
    }
}

@Composable
private fun DayView(
    data: Map<String, DayTrackerData>, cats: List<CategoryDefinition>,
    day: LocalDate, isDark: Boolean, cardBg: Color, primary: Color,
    textPri: Color, textSec: Color, textDis: Color, doneClr: Color,
    onPrev: () -> Unit, onNext: () -> Unit
) {
    val fmt     = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dayStr  = day.format(fmt)
    val dayData = data[dayStr]
    val isToday = day == LocalDate.now()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, null, tint = primary) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.format(DateTimeFormatter.ofPattern("EEEE")), color = textPri, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(day.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), color = textSec, style = MaterialTheme.typography.labelMedium)
                if (isToday) Text("Today", color = primary, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onNext, enabled = !isToday) {
                Icon(Icons.Default.ChevronRight, null, tint = if (isToday) textDis else primary)
            }
        }
        if (dayData == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No data recorded", color = textSec, textAlign = TextAlign.Center)
            }
        } else {
            cats.forEach { cat ->
                val value = when (cat.builtinType) {
                    "TASKS"   -> "${dayData.taskDone}/${dayData.taskTotal} tasks done"
                    "PLANK"   -> if (dayData.plankDone) "Done" else "Not logged"
                    "PRAYERS" -> "${dayData.prayersDone}/5 prayers"
                    else      -> if (dayData.customEntries[cat.id] == true) "Done" else "Not done"
                }
                val isDone = when (cat.builtinType) {
                    "PLANK"   -> dayData.plankDone
                    "PRAYERS" -> dayData.prayersDone == 5
                    "TASKS"   -> dayData.taskTotal > 0 && dayData.taskDone == dayData.taskTotal
                    else      -> dayData.customEntries[cat.id] == true
                }
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (isDone) doneClr.copy(alpha=0.1f) else cardBg).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(cat.emoji, fontSize = 22.sp)
                        Column {
                            Text(cat.name, color = textPri, fontWeight = FontWeight.SemiBold)
                            Text(value, color = if (isDone) doneClr else textSec, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Icon(if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null, tint = if (isDone) doneClr else textDis, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun WeekView(
    data: Map<String, DayTrackerData>, cats: List<CategoryDefinition>, selectedCatIndex: Int,
    isDark: Boolean, primary: Color, textPri: Color, textSec: Color, doneClr: Color,
    onDayClick: (String) -> Unit
) {
    val fmt     = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val friday  = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
    val days    = (0..6).map { friday.plusDays(it.toLong()) }
    val cat     = cats.getOrNull(selectedCatIndex)
    val textDis = if (isDark) TextDisabled else LightTextDisabled
    val greyEmp = if (isDark) GreyEmpty    else LightGreyEmpty

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${friday.format(DateTimeFormatter.ofPattern("MMM d"))} - ${friday.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d"))}",
            color = textSec, style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { day ->
                val ds     = day.format(fmt)
                val d      = data[ds]
                val today  = day == LocalDate.now()
                val score  = when {
                    d == null || cat == null -> 0f
                    cat.builtinType == "PLANK"   -> if (d.plankDone) 1f else 0f
                    cat.builtinType == "PRAYERS" -> d.prayersDone / 5f
                    cat.builtinType == "TASKS"   -> if (d.taskTotal == 0) 0f else d.taskDone.toFloat() / d.taskTotal
                    else -> if (d.customEntries[cat.id] == true) 1f else 0f
                }
                val color  = when {
                    score >= 0.8f -> doneClr
                    score >= 0.4f -> YellowPartial
                    else          -> greyEmp
                }
                Column(modifier = Modifier.weight(1f).clickable { onDayClick(ds) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
                        color = if (today) primary else textSec, fontSize = 11.sp,
                        fontWeight = if (today) FontWeight.Bold else FontWeight.Normal)
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(6.dp)).background(color)
                        .then(if (today) Modifier.border(2.dp, primary, RoundedCornerShape(6.dp)) else Modifier))
                    Text(day.dayOfMonth.toString(), color = if (today) primary else textDis, fontSize = 10.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(doneClr, "Done", textSec)
            LegendDot(YellowPartial, "Partial", textSec)
            LegendDot(greyEmp, "Missing", textSec)
        }
    }
}

@Composable
private fun MonthView(
    month: LocalDate, data: Map<String, DayTrackerData>,
    cat: CategoryDefinition?, isDark: Boolean,
    primary: Color, textPri: Color, textSec: Color, doneClr: Color,
    onPrev: () -> Unit, onNext: () -> Unit, onDayClick: (String) -> Unit
) {
    val fmt         = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val greyEmp     = if (isDark) GreyEmpty else LightGreyEmpty
    val firstDay    = month.withDayOfMonth(1)
    val daysInMonth = month.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value - 1

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, null, tint = primary) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleLarge, color = textPri)
            IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, null, tint = primary) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf("M","T","W","T","F","S","S").forEach { l ->
                Text(l, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    color = textSec, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            LazyVerticalGrid(columns = GridCells.Fixed(7),
                modifier = Modifier.height(52.dp * ((startOffset + daysInMonth + 6) / 7)),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(startOffset) { Box(modifier = Modifier.size(44.dp)) }
                items(daysInMonth) { idx ->
                    val day     = idx + 1
                    val dateStr = month.withDayOfMonth(day).format(fmt)
                    val d       = data[dateStr]
                    val isToday = dateStr == LocalDate.now().format(fmt)
                    val score   = when {
                        d == null || cat == null -> 0f
                        cat.builtinType == "PLANK"   -> if (d.plankDone) 1f else 0f
                        cat.builtinType == "PRAYERS" -> d.prayersDone / 5f
                        cat.builtinType == "TASKS"   -> if (d.taskTotal == 0) 0f else d.taskDone.toFloat() / d.taskTotal
                        else -> if (d.customEntries[cat.id] == true) 1f else 0f
                    }
                    val cellColor = when {
                        score >= 0.8f -> doneClr.copy(alpha=0.7f)
                        score >= 0.4f -> YellowPartial.copy(alpha=0.7f)
                        else          -> greyEmp
                    }
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(cellColor)
                        .then(if (isToday) Modifier.border(2.dp, primary, RoundedCornerShape(8.dp)) else Modifier)
                        .clickable { onDayClick(dateStr) }, contentAlignment = Alignment.Center) {
                        Text(day.toString(), color = if (isToday) primary else textPri,
                            fontSize = 13.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(doneClr, ">=80%", textSec)
            LegendDot(YellowPartial, "40-79%", textSec)
            LegendDot(greyEmp, "<40%", textSec)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LegendDot(color: Color, label: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, color = textColor, fontSize = 11.sp)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, textPri: Color, textSec: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = textSec)
        Text(value, color = textPri, fontWeight = FontWeight.SemiBold)
    }
}

private fun printReport(ctx: Context, month: LocalDate, data: Map<String, DayTrackerData>,
    cats: List<CategoryDefinition>, mode: String) {
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val (start, end, label) = when (mode) {
        "DAY"  -> Triple(today, today, "Today ${today.format(DateTimeFormatter.ofPattern("MMM d"))}")
        "WEEK" -> { val f = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
            Triple(f, f.plusDays(6), "Week ${f.format(DateTimeFormatter.ofPattern("MMM d"))}") }
        else   -> Triple(month.withDayOfMonth(1), month.with(TemporalAdjusters.lastDayOfMonth()),
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
    }
    val headers = cats.joinToString("") { "<th>${it.emoji} ${it.name}</th>" }
    val rows    = StringBuilder()
    var d = start
    while (!d.isAfter(end)) {
        val ds = d.format(fmt)
        val e  = data[ds]
        rows.append("<tr><td>${d.format(DateTimeFormatter.ofPattern("EEE MMM d"))}</td>")
        cats.forEach { cat ->
            val v = when {
                e == null -> "-"
                cat.builtinType == "PLANK"   -> if (e.plankDone) "Y" else "N"
                cat.builtinType == "PRAYERS" -> "${e.prayersDone}/5"
                cat.builtinType == "TASKS"   -> "${e.taskDone}/${e.taskTotal}"
                else -> if (e.customEntries[cat.id] == true) "Y" else "N"
            }
            val c = if (v == "Y" || v.startsWith("5/") || v == e?.taskTotal.toString()) "#2D6A4F" else "#D62839"
            rows.append("<td style='color:$c'>$v</td>")
        }
        rows.append("</tr>")
        d = d.plusDays(1)
    }
    val html = "<html><head><style>@page{size:A4 landscape;margin:8mm}" +
        "body{font-family:sans-serif;background:#fff;color:#111;padding:8px}" +
        "h2{color:#2D6A4F;font-size:16px}table{width:100%;border-collapse:collapse;font-size:11px}" +
        "th{background:#E8F5E9;color:#2D6A4F;padding:6px;border-bottom:2px solid #95D5B2}" +
        "td{padding:5px;border-bottom:1px solid #E8F5E9}</style></head><body>" +
        "<h2>LoF Tracker - $label</h2><table><tr><th>Date</th>$headers</tr>$rows</table></body></html>"
    val wv = WebView(ctx)
    wv.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
            pm.print("LoF_$label", view.createPrintDocumentAdapter("LoF"),
                PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape()).build())
        }
    }
    wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}
