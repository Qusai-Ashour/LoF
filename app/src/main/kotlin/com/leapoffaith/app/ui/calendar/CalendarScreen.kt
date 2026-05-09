package com.leapoffaith.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leapoffaith.app.data.entities.CategoryDefinition
import com.leapoffaith.app.data.entities.CustomEntry
import com.leapoffaith.app.data.entities.Task
import com.leapoffaith.app.ui.prepare.parseColor
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val isDark      by viewModel.isDarkTheme.collectAsState()
    val accentHex   by viewModel.userAccentHex.collectAsState()
    val recordCats  by viewModel.recordCategories.collectAsState()
    val prepCats    by viewModel.prepareCategories.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val bg      = if (isDark) NavyBackground else LightBackground
    val cardBg  = if (isDark) NavyCard       else LightCard
    val primary = parseColor(accentHex)
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary
    val textDis = if (isDark) TextDisabled   else LightTextDisabled
    val doneClr = if (isDark) GreenDone      else LightGreenDone

    val fmt     = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today   = LocalDate.now()
    var month   by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load entries for the displayed month
    val monthStart = month.format(fmt)
    val monthEnd   = month.with(TemporalAdjusters.lastDayOfMonth()).format(fmt)
    val monthTasks   by viewModel.repository_tasks_range(currentUser, monthStart, monthEnd).collectAsState(initial = emptyList())
    val monthCustoms by viewModel.repository_customs_range(currentUser, monthStart, monthEnd).collectAsState(initial = emptyList())
    // Only calendar-eligible: TASKS builtin + custom categories linked to TODAY or NEXT_DAY prepare
    val calendarCats = recordCats.filter { cat ->
        cat.builtinType == "TASKS" ||
        (cat.builtinType.isEmpty() && run {
            val prepCat = prepCats.firstOrNull { p -> p.name == cat.name }
            prepCat?.prepareFrequency == "TODAY" || prepCat?.prepareFrequency == "NEXT_DAY"
        })
    }

    // Days that have any entries
    val activeDates = remember(monthTasks, monthCustoms) {
        (monthTasks.map { it.date } + monthCustoms.filter { it.notes.isNotEmpty() && calendarCats.any { c -> c.id == it.categoryId } }.map { it.date }).toSet()
    }

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Add to Calendar", color = textPri) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Month navigation
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, null, tint = primary)
                }
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    color = textPri, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, null, tint = primary)
                }
            }

            // Day headers
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                listOf("M","T","W","T","F","S","S").forEach { d ->
                    Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        color = textSec, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(4.dp))

            // Calendar grid
            val firstDay    = month.withDayOfMonth(1)
            val daysInMonth = month.lengthOfMonth()
            val startOffset = firstDay.dayOfWeek.value - 1
            val totalCells  = startOffset + daysInMonth
            val rows        = (totalCells + 6) / 7

            Column(modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(rows) { row ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(7) { col ->
                            val cell = row * 7 + col
                            val day  = cell - startOffset + 1
                            if (cell < startOffset || day > daysInMonth) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val dateStr  = month.withDayOfMonth(day).format(fmt)
                                val isToday  = dateStr == today.format(fmt)
                                val isSelected = selectedDate == dateStr
                                val hasEntry = dateStr in activeDates
                                val isPast   = month.withDayOfMonth(day).isBefore(today)

                                Box(modifier = Modifier.weight(1f).aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(when {
                                        isSelected -> primary
                                        isToday    -> primary.copy(alpha = 0.2f)
                                        else       -> Color.Transparent
                                    })
                                    .then(if (isToday && !isSelected) Modifier.border(1.5.dp, primary, CircleShape) else Modifier)
                                    .clickable { selectedDate = dateStr },
                                    contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center) {
                                        Text(day.toString(),
                                            color = when {
                                                isSelected -> if (isDark) NavyBackground else Color.White
                                                isToday    -> primary
                                                isPast     -> textDis
                                                else       -> textPri
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal)
                                        if (hasEntry) {
                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape)
                                                .background(if (isSelected) (if (isDark) NavyBackground else Color.White) else primary))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Tap a day to view or add tasks",
                color = textDis, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }

    // Day detail sheet
    if (selectedDate != null) {
        val dateStr = selectedDate!!
        val dateLabel = try {
            LocalDate.parse(dateStr, fmt).format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        } catch (e: Exception) { dateStr }

        val dayTasks   = monthTasks.filter { it.date == dateStr }
        // Only show entries belonging to calendar-eligible categories (TODAY/NEXT_DAY)
        val eligibleIds = (recordCats + prepCats).filter { cat ->
            cat.builtinType == "TASKS" ||
            (cat.builtinType.isEmpty() && run {
                val p = prepCats.firstOrNull { pc -> pc.name == cat.name }
                p?.prepareFrequency == "TODAY" || p?.prepareFrequency == "NEXT_DAY"
            })
        }.map { it.id }.toSet()
        val dayCustoms = monthCustoms.filter { it.date == dateStr && it.notes.isNotEmpty() && it.categoryId in eligibleIds }

        ModalBottomSheet(
            onDismissRequest = { selectedDate = null },
            sheetState = sheetState,
            containerColor = cardBg
        ) {
            DayDetailSheet(
                viewModel     = viewModel,
                dateStr       = dateStr,
                dateLabel     = dateLabel,
                dayTasks      = dayTasks,
                dayCustoms    = dayCustoms,
                recordCats    = recordCats,
                prepCats      = prepCats,
                addableCats   = calendarCats,
                isDark        = isDark,
                primary       = primary,
                cardBg        = cardBg,
                textPri       = textPri,
                textSec       = textSec,
                textDis       = textDis,
                doneClr       = doneClr
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    viewModel: AppViewModel,
    dateStr: String,
    dateLabel: String,
    dayTasks: List<Task>,
    dayCustoms: List<CustomEntry>,
    recordCats: List<CategoryDefinition>,
    prepCats: List<CategoryDefinition> = emptyList(),
    addableCats: List<CategoryDefinition>,
    isDark: Boolean,
    primary: Color, cardBg: Color,
    textPri: Color, textSec: Color, textDis: Color, doneClr: Color
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(dateLabel, color = textPri, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                val total = dayTasks.size + dayCustoms.size
                if (total > 0) Text("$total item${if (total > 1) "s" else ""} planned",
                    color = textSec, style = MaterialTheme.typography.labelSmall)
            }
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.size(44.dp),
                containerColor = primary,
                contentColor = if (isDark) NavyBackground else LightCard
            ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp)) }
        }

        HorizontalDivider(color = if (isDark) GreyEmpty else LightGreyEmpty)
        Spacer(Modifier.height(4.dp))
        Text(
            "Entries appear only in their category on the relevant day. You can only add tasks for Today/Tomorrow-type categories.",
            color = textDis, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        if (dayTasks.isEmpty() && dayCustoms.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center) {
                Text("Nothing planned yet. Tap + to add.", color = textDis, textAlign = TextAlign.Center)
            }
        } else {
            // Group by category
            val taskCat = recordCats.firstOrNull { it.builtinType == "TASKS" }
            if (dayTasks.isNotEmpty() && taskCat != null) {
                CategorySection(taskCat, isDark, parseColor(taskCat.color), textSec) {
                    dayTasks.forEach { task ->
                        EntryRow(task.title, task.isCompleted, doneClr, textPri, textDis,
                            onDelete = { viewModel.deleteTask(task) })
                    }
                }
            }
            val byCategory = dayCustoms.groupBy { it.categoryId }
            byCategory.forEach { (catId, entries) ->
                val cat = (recordCats + prepCats).firstOrNull { it.id == catId } ?: return@forEach
                CategorySection(cat, isDark, parseColor(cat.color), textSec) {
                    entries.forEach { entry ->
                        EntryRow(entry.notes, entry.isDone, doneClr, textPri, textDis,
                            onDelete = { viewModel.deletePrepareItem(entry) })
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showAddDialog) {
        AddCalendarEntryDialog(
            isDark = isDark,
            recordCats = addableCats,
            primary = primary,
            onDismiss = { showAddDialog = false },
            onConfirm = { category, text ->
                viewModel.addCalendarEntry(category, dateStr, text)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CategorySection(
    cat: CategoryDefinition, isDark: Boolean, accent: Color, textSec: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp)) {
            Text(cat.emoji, fontSize = 14.sp)
            Text(cat.name, color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
private fun EntryRow(text: String, isDone: Boolean, doneClr: Color, textPri: Color, textDis: Color,
    onDelete: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
            tint = if (isDone) doneClr else textDis, modifier = Modifier.size(14.dp))
        Text(text, color = if (isDone) textDis else textPri,
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = textDis.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCalendarEntryDialog(
    isDark: Boolean,
    recordCats: List<CategoryDefinition>,
    primary: Color,
    onDismiss: () -> Unit,
    onConfirm: (CategoryDefinition, String) -> Unit
) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val cardLt  = if (isDark) NavyCardLight else LightCardVariant

    var selectedCat by remember { mutableStateOf(recordCats.firstOrNull()) }
    var text        by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss, containerColor = cardBg,
        title = { Text("Add to Calendar", color = primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Category / type selector
                Text(
                    "Only categories prepared for Today or Tomorrow are shown here.",
                    color = textDis, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text("Category", color = textSec, style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recordCats.forEach { cat ->
                        val sel    = selectedCat?.id == cat.id
                        val accent = parseColor(cat.color)
                        FilterChip(
                            selected = sel,
                            onClick  = { selectedCat = cat },
                            label = { Text("${cat.emoji} ${cat.name}", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent,
                                selectedLabelColor = Color.White,
                                containerColor = accent.copy(alpha = 0.12f),
                                labelColor = textSec
                            )
                        )
                    }
                }

                // Task text
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Task description", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary, unfocusedBorderColor = textDis,
                        focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank() && selectedCat != null) onConfirm(selectedCat!!, text.trim()) },
                enabled = text.isNotBlank() && selectedCat != null
            ) { Text("Add", color = primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = textSec) } }
    )
}

