package com.leapoffaith.app.ui.prepare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.leapoffaith.app.data.entities.CustomEntry
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPrepareScreen(viewModel: AppViewModel, categoryId: Long, onBack: () -> Unit) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    var categoryName  by remember { mutableStateOf("") }
    var categoryEmoji by remember { mutableStateOf("") }
    var prepFreq      by remember { mutableStateOf("NEXT_DAY") }

    LaunchedEffect(categoryId) {
        val cat = viewModel.repository.getCategoryById(categoryId) ?: return@LaunchedEffect
        if (cat.type == "RECORD") { onBack(); return@LaunchedEffect }
        categoryName  = cat.name
        categoryEmoji = cat.emoji
        prepFreq      = cat.prepareFrequency.ifEmpty { "NEXT_DAY" }
    }

    val bg      = if (isDark) NavyBackground else LightBackground
    val primary = parseColor(viewModel.userAccentHex.collectAsState().value)
    val textPri = if (isDark) TextPrimary    else LightTextPrimary

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("$categoryEmoji  $categoryName", color = textPri) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        when (prepFreq) {
            "NEXT_WEEK" -> WeeklyPrepareContent(viewModel, categoryId, isDark, padding)
            "TODAY"     -> DailyPrepareContent(viewModel, categoryId, isDark, LocalDate.now(), padding)
            else        -> DailyPrepareContent(viewModel, categoryId, isDark, LocalDate.now().plusDays(1), padding)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyPrepareContent(
    viewModel: AppViewModel, categoryId: Long, isDark: Boolean,
    targetDate: LocalDate, padding: PaddingValues
) {
    val fmt     = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dateStr = targetDate.format(fmt)
    val allItems by viewModel.getPrepareItems(categoryId).collectAsState(initial = emptyList())
    val forDate  = allItems.filter { it.date == dateStr }

    val cardBg  = if (isDark) NavyCard      else LightCard
    val primary = parseColor(viewModel.userAccentHex.collectAsState().value)
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val doneClr = if (isDark) GreenDone     else LightGreenDone

    val sheetState  = rememberModalBottomSheetState()
    var showSheet   by remember { mutableStateOf(false) }
    var newText     by remember { mutableStateOf("") }
    var useTime     by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableIntStateOf(9) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Text("For ${targetDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))}",
            color = textSec, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)) {
            if (forDate.isEmpty()) {
                item { Text("No items yet. Tap + to add.", color = textDis, modifier = Modifier.padding(vertical = 16.dp)) }
            }
            items(forDate, key = { it.id }) { entry ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.RadioButtonUnchecked, null, tint = textDis, modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.notes, color = textPri)
                        if (entry.hourOfDay >= 0)
                            Text("%02d:00".format(entry.hourOfDay), color = primary.copy(alpha=0.8f),
                                style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { viewModel.deletePrepareItem(entry) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, null,
                            tint = if (isDark) RedMissed.copy(alpha=0.5f) else LightRedMissed.copy(alpha=0.5f),
                            modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        val prevLabel = if (targetDate == java.time.LocalDate.now()) "yesterday" else "today"
        TextButton(onClick = { viewModel.reimportPreviousPeriod(categoryId, if (targetDate == java.time.LocalDate.now()) "TODAY" else "NEXT_DAY") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp), tint = textSec)
            Spacer(Modifier.width(4.dp))
            Text("Re-import from $prevLabel", color = textSec, style = MaterialTheme.typography.labelSmall)
        }
        Button(onClick = { showSheet = true }, modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary,
                contentColor = if (isDark) NavyBackground else LightCard)) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp)); Text("Add Item")
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false; newText = ""; useTime = false },
            sheetState = sheetState, containerColor = if (isDark) NavyCard else LightCard) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add Item", style = MaterialTheme.typography.titleMedium, color = primary)
                OutlinedTextField(value = newText, onValueChange = { newText = it },
                    label = { Text("Task", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary,
                        unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = useTime, onCheckedChange = { useTime = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = if (isDark) NavyBackground else LightCard, checkedTrackColor = primary))
                    Text("Set a time", color = textSec)
                }
                if (useTime) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Time:", color = textSec)
                        Slider(value = selectedHour.toFloat(), onValueChange = { selectedHour = it.toInt() },
                            valueRange = 0f..23f, steps = 22,
                            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                            modifier = Modifier.weight(1f))
                        Text("%02d:00".format(selectedHour), color = primary, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Button(onClick = {
                    if (newText.isNotBlank()) {
                        viewModel.addPrepareItemWithTime(categoryId, dateStr, newText.trim(), if (useTime) selectedHour else -1)
                        newText = ""; useTime = false; showSheet = false
                    }
                }, enabled = newText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = if (isDark) NavyBackground else LightCard),
                    modifier = Modifier.fillMaxWidth()) { Text("Add") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyPrepareContent(
    viewModel: AppViewModel, categoryId: Long, isDark: Boolean, padding: PaddingValues
) {
    val fmt    = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    // Week starts FRIDAY
    val friday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
    val allItems by viewModel.getPrepareItems(categoryId).collectAsState(initial = emptyList())

    val cardBg  = if (isDark) NavyCard      else LightCard
    val primary = parseColor(viewModel.userAccentHex.collectAsState().value)
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled

    var expandedDay by remember { mutableIntStateOf(-1) }
    var showAddDay  by remember { mutableIntStateOf(-1) }
    var addText     by remember { mutableStateOf("") }
    var useTime     by remember { mutableStateOf(false) }
    var addHour     by remember { mutableIntStateOf(9) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("${friday.format(DateTimeFormatter.ofPattern("MMM d"))} — ${friday.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                color = textSec, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
        }
        items(7) { i ->
            val dayDate  = friday.plusDays(i.toLong())
            val dayStr   = dayDate.format(fmt)
            val dayName  = dayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val dayItems = allItems.filter { it.date == dayStr }
            val isExpanded = expandedDay == i
            val isToday  = dayDate == LocalDate.now()

            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg)) {
                Row(modifier = Modifier.fillMaxWidth()
                    .clickable { expandedDay = if (expandedDay == i) -1 else i }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(dayName, color = if (isToday) primary else textPri,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isToday) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal)
                        Text(dayDate.format(DateTimeFormatter.ofPattern("MMM d")), color = textSec, style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (dayItems.isNotEmpty()) Text("${dayItems.size}", color = primary, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(4.dp))
                        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = textSec)
                    }
                }
                AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HorizontalDivider(color = if (isDark) GreyEmpty else LightGreyEmpty)
                        Spacer(Modifier.height(4.dp))
                        dayItems.forEach { entry ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("• ${entry.notes}", color = textPri)
                                    if (entry.hourOfDay >= 0)
                                        Text("%02d:00".format(entry.hourOfDay), color = primary.copy(alpha=0.7f),
                                            style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { viewModel.deletePrepareItem(entry) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, tint = textDis, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        if (showAddDay == i) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = addText, onValueChange = { addText = it },
                                        modifier = Modifier.weight(1f), singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary,
                                            unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri))
                                    IconButton(onClick = {
                                        if (addText.isNotBlank()) {
                                            viewModel.addPrepareItemWithTime(categoryId, dayStr, addText.trim(), if (useTime) addHour else -1)
                                            addText = ""; useTime = false; showAddDay = -1
                                        }
                                    }) { Icon(Icons.Default.Check, null, tint = primary) }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Switch(checked = useTime, onCheckedChange = { useTime = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = if (isDark) NavyBackground else LightCard, checkedTrackColor = primary),
                                        modifier = Modifier.size(width = 40.dp, height = 24.dp))
                                    Text("Time", color = textSec, style = MaterialTheme.typography.labelSmall)
                                    if (useTime) {
                                        Slider(value = addHour.toFloat(), onValueChange = { addHour = it.toInt() },
                                            valueRange = 0f..23f, steps = 22,
                                            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                                            modifier = Modifier.weight(1f))
                                        Text("%02d:00".format(addHour), color = primary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        if (i == 0 && dayItems.isEmpty()) {
                            TextButton(onClick = { viewModel.reimportPreviousPeriod(categoryId, "NEXT_WEEK") },
                                colors = ButtonDefaults.textButtonColors(contentColor = primary.copy(alpha=0.7f))) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Re-import last week", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        TextButton(onClick = { showAddDay = if (showAddDay == i) -1 else i; addText = "" },
                            colors = ButtonDefaults.textButtonColors(contentColor = primary)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp)); Text("Add")
                        }
                    }
                }
            }
        }
    }
}
