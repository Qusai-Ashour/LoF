package com.leapoffaith.app.ui.record
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leapoffaith.app.data.entities.CustomEntry
import com.leapoffaith.app.data.entities.CategoryDefinition
import com.leapoffaith.app.ui.prepare.parseColor
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordCustomCategoryScreen(viewModel: AppViewModel, categoryId: Long, onBack: () -> Unit) {
    val isDark  by viewModel.isDarkTheme.collectAsState()
    var catName  by remember { mutableStateOf("") }
    var catEmoji by remember { mutableStateOf("") }
    var catColor by remember { mutableStateOf("#D4A843") }
    var freq     by remember { mutableStateOf("ONCE_DAILY") }
    var subItems by remember { mutableStateOf(listOf<String>()) }
    var linkedPrepareCat by remember { mutableStateOf<CategoryDefinition?>(null) }

    val todayStr   = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val todayLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    val accent     by viewModel.userAccentHex.collectAsState()

    LaunchedEffect(categoryId) {
        val cat = viewModel.repository.getCategoryById(categoryId) ?: return@LaunchedEffect
        if (cat.type == "PREPARE") { onBack(); return@LaunchedEffect }
        catName  = cat.name
        catEmoji = cat.emoji
        catColor = cat.color
        freq     = cat.frequency
        // Find prepare partner
        val partner = viewModel.findPrepareCategoryByName(cat.name)
        linkedPrepareCat = partner
        // subItems: only used for MULTIPLE_DAILY — read from prepare partner first
        if (freq == "MULTIPLE_DAILY" || cat.frequency == "MULTIPLE_DAILY") {
            val src = partner?.subItems ?: cat.subItems
            subItems = if (!src.isNullOrBlank()) {
                try { val a = JSONArray(src); (0 until a.length()).map { a.getString(it) } }
                catch (_: Exception) { emptyList() }
            } else emptyList()
        }
    }

    val prepCatId = linkedPrepareCat?.id ?: 0L
    val prepFreq  = linkedPrepareCat?.prepareFrequency ?: ""
    val isFixed   = linkedPrepareCat?.isFixed ?: true

    val allPrepItems by viewModel.getPrepareItems(if (prepCatId > 0) prepCatId else categoryId)
        .collectAsState(initial = emptyList())
    val todayPrepItems = allPrepItems.filter { it.date == todayStr }

    val primary = parseColor(accent)
    val bg      = if (isDark) NavyBackground else LightBackground
    val textPri = if (isDark) TextPrimary    else LightTextPrimary

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("$catEmoji  $catName", color = textPri) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        val builtinT = linkedPrepareCat?.builtinType ?: ""
        when {
            // Built-in weekly plans always go to weekly view
            (builtinT == "WEEK_MEALS" || builtinT == "GYM_SPLIT" || prepFreq == "NEXT_WEEK") && prepCatId > 0 ->
                WeeklyPrepareRecordContent(viewModel, prepCatId, isDark, padding)
            // Fixed MULTIPLE_DAILY: show sub-item checklist
            freq == "MULTIPLE_DAILY" && subItems.isNotEmpty() ->
                MultipleTracker(viewModel, categoryId, prepCatId, catColor, subItems, isDark, padding, todayLabel)
            // Non-fixed TODAY/NEXT_DAY: show prepared items for today
            !isFixed && todayPrepItems.isNotEmpty() ->
                PrepareLinkedContent(viewModel, todayPrepItems, catColor, isDark, padding, todayLabel)
            // Non-fixed but nothing prepared
            !isFixed && prepCatId > 0 && todayPrepItems.isEmpty() ->
                NothingPreparedYet(isDark, padding,
                    if (prepFreq == "NEXT_DAY") "No tasks prepared for today.\nAdd them yesterday in Prepare."
                    else "No tasks prepared for today.\nAdd them in Prepare.")
            // Default: ONCE_DAILY habit tracker
            else ->
                OnceDailyTracker(viewModel, categoryId, catEmoji, catName, catColor, isDark, padding, todayLabel)
        }
    }
}

@Composable
private fun NothingPreparedYet(isDark: Boolean, padding: PaddingValues, message: String) {
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.EventAvailable, null, tint = textDis, modifier = Modifier.size(56.dp))
            Text(message, color = textSec, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PrepareLinkedContent(
    viewModel: AppViewModel, items: List<CustomEntry>,
    catColor: String, isDark: Boolean, padding: PaddingValues, todayLabel: String
) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val doneClr = if (isDark) GreenDone     else LightGreenDone
    val accent  = parseColor(catColor)
    val done    = items.count { it.isDone }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)
            .clip(RoundedCornerShape(12.dp)).background(cardBg).padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(todayLabel, color = textSec, style = MaterialTheme.typography.labelLarge)
                Text("$done / ${items.size}", color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)) {
            items(items, key = { it.id }) { entry ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (entry.isDone) doneClr.copy(alpha=0.1f) else cardBg)
                    .clickable { viewModel.togglePrepareItem(entry) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Checkbox(checked = entry.isDone,
                        onCheckedChange = { viewModel.togglePrepareItem(entry) },
                        colors = CheckboxDefaults.colors(checkedColor = doneClr, uncheckedColor = textSec,
                            checkmarkColor = if (isDark) NavyBackground else LightCard))
                    Text(entry.notes, color = if (entry.isDone) textDis else textPri,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (entry.isDone) TextDecoration.LineThrough else TextDecoration.None,
                        style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyPrepareRecordContent(
    viewModel: AppViewModel, prepCatId: Long, isDark: Boolean, padding: PaddingValues
) {
    val fmt    = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val friday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
    val allItems by viewModel.getPrepareItems(prepCatId).collectAsState(initial = emptyList())
    val cardBg  = if (isDark) NavyCard      else LightCard
    val primary = if (isDark) Gold          else ForestGreen
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val doneClr = if (isDark) GreenDone     else LightGreenDone
    val today   = LocalDate.now()
    var expandedDay by remember { mutableIntStateOf(
        (0..6).indexOfFirst { friday.plusDays(it.toLong()) == today }.coerceAtLeast(0)
    ) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Text("This Week — tap a day to record", color = textSec,
            style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp)) }
        items(7) { i ->
            val dayDate  = friday.plusDays(i.toLong())
            val dayStr   = dayDate.format(fmt)
            val dayName  = dayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val isToday  = dayDate == today
            val dayItems = allItems.filter { it.date == dayStr }
            val doneCount = dayItems.count { it.isDone }
            val isExpanded = expandedDay == i
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (isToday) doneClr.copy(alpha=0.08f) else cardBg)) {
                Row(modifier = Modifier.fillMaxWidth()
                    .clickable { expandedDay = if (expandedDay == i) -1 else i }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(dayName, color = if (isToday) primary else textPri,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                        Text(dayDate.format(DateTimeFormatter.ofPattern("MMM d")),
                            color = textSec, style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (dayItems.isNotEmpty()) Text("$doneCount/${dayItems.size}",
                            color = if (doneCount == dayItems.size) doneClr else textDis,
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        else Text("No tasks", color = textDis, style = MaterialTheme.typography.labelSmall)
                        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = textSec)
                    }
                }
                AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HorizontalDivider(color = if (isDark) GreyEmpty else LightGreyEmpty)
                        Spacer(Modifier.height(4.dp))
                        if (dayItems.isEmpty()) Text("Nothing planned", color = textDis,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp))
                        else dayItems.forEach { entry ->
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(if (entry.isDone) doneClr.copy(alpha=0.08f) else if (isDark) NavyCardLight else LightCardVariant)
                                .clickable { viewModel.togglePrepareItem(entry) }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Checkbox(checked = entry.isDone, onCheckedChange = { viewModel.togglePrepareItem(entry) },
                                    colors = CheckboxDefaults.colors(checkedColor = doneClr, uncheckedColor = textSec,
                                        checkmarkColor = if (isDark) NavyBackground else LightCard))
                                Text(entry.notes, color = if (entry.isDone) textDis else textPri,
                                    textDecoration = if (entry.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnceDailyTracker(
    viewModel: AppViewModel, categoryId: Long,
    catEmoji: String, catName: String, catColor: String, isDark: Boolean,
    padding: PaddingValues, todayLabel: String
) {
    val entry  by viewModel.getCustomEntryFlow(categoryId).collectAsState(
        initial = null as com.leapoffaith.app.data.entities.CustomEntry?)
    val isDone  = entry?.isDone == true
    val primary = parseColor(catColor)
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val doneClr = if (isDark) GreenDone     else LightGreenDone
    val pulse   = rememberInfiniteTransition(label = "p")
    val scale  by pulse.animateFloat(1f, 1.06f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s")

    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)) {
            Text(catEmoji, fontSize = 64.sp)
            Text(todayLabel, color = textSec, style = MaterialTheme.typography.bodyMedium)
            if (isDone) {
                Box(modifier = Modifier.size(100.dp).clip(CircleShape)
                    .background(doneClr.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = doneClr, modifier = Modifier.size(56.dp))
                }
                Text("Done!", color = doneClr, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { viewModel.toggleCustomEntry(categoryId) }) {
                    Icon(Icons.Default.Undo, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text("Undo")
                }
            } else {
                Text("Did you $catName today?", color = textPri, fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Button(onClick = { viewModel.toggleCustomEntry(categoryId) },
                    modifier = Modifier.scale(scale).fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary,
                        contentColor = if (isDark) NavyBackground else LightCard)) {
                    Text("YES", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MultipleTracker(
    viewModel: AppViewModel, categoryId: Long, prepCatId: Long, catColor: String,
    subItems: List<String>, isDark: Boolean, padding: PaddingValues, todayLabel: String
) {
    // Check entries under both record and prepare IDs
    val entriesRec  by viewModel.getCustomEntriesForCategoryDate(categoryId).collectAsState(initial = emptyList())
    val entriesPrep by viewModel.getCustomEntriesForCategoryDate(if (prepCatId > 0) prepCatId else categoryId)
        .collectAsState(initial = emptyList())
    val entries = (entriesRec + entriesPrep).distinctBy { it.id }

    val cardBg  = if (isDark) NavyCard      else LightCard
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val doneClr = if (isDark) GreenDone     else LightGreenDone
    val accent  = parseColor(catColor)
    val done    = subItems.count { key -> entries.any { e -> e.subItemKey == key && e.isDone } }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)) {
        item {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(cardBg).padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(todayLabel, color = textSec, style = MaterialTheme.typography.labelLarge)
                    Text("$done / ${subItems.size}", color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        items(subItems) { key ->
            val isDone = entries.any { e -> e.subItemKey == key && e.isDone }
            // Toggle under the record category ID (or prepare ID if no record)
            val toggleId = if (categoryId > 0) categoryId else prepCatId
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (isDone) doneClr.copy(alpha=0.12f) else cardBg)
                .clickable { viewModel.toggleCustomEntry(toggleId, key) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(if (isDone) doneClr.copy(alpha=0.2f) else if (isDark) NavyCardLight else LightCardVariant),
                        contentAlignment = Alignment.Center) {
                        if (isDone) Icon(Icons.Default.Check, null, tint = doneClr, modifier = Modifier.size(18.dp))
                        else Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(textDis.copy(alpha=0.3f)))
                    }
                    Text(key, color = if (isDone) doneClr else textPri,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Text(if (isDone) "Done" else "Tap to mark",
                    color = if (isDone) doneClr else textDis, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
