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
import org.json.JSONArray
import com.leapoffaith.app.navigation.NavRoutes
import com.leapoffaith.app.ui.prepare.parseColor
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordProgressScreen(viewModel: AppViewModel, navController: NavController, onBack: () -> Unit) {
    val categories  by viewModel.recordCategories.collectAsState()
    val prepCatsRecord by viewModel.prepareCategories.collectAsState()
    val todayTasks  by viewModel.todayTasks.collectAsState()
    val plankToday  by viewModel.plankToday.collectAsState()
    val prayerToday by viewModel.prayerToday.collectAsState()
    val isDark              by viewModel.isDarkTheme.collectAsState()
    val todayCustomEntries by viewModel.todayCustomEntries.collectAsState()
    val accent      by viewModel.userAccentHex.collectAsState()
    val todayLabel  = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    val bg      = if (isDark) NavyBackground else LightBackground
    val primary = parseColor(accent)
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary

    var editMode          by remember { mutableStateOf(false) }
    var showAddDialog     by remember { mutableStateOf(false) }
    var editingCat        by remember { mutableStateOf<CategoryDefinition?>(null) }
    var confirmDeleteCat  by remember { mutableStateOf<CategoryDefinition?>(null) }

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
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null, tint = primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (editMode) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp)).background(primary.copy(alpha=0.1f)).padding(8.dp)) {
                    Text("Edit mode: pencil to edit, trash to delete",
                        color = primary, style = MaterialTheme.typography.labelSmall)
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
                items(categories, key = { it.id }) { cat ->
                    // Parse sub-items for MULTIPLE_DAILY
                    val subItemsList: List<String> = if (cat.frequency == "MULTIPLE_DAILY" && !cat.subItems.isNullOrBlank()) {
                        try { val a = JSONArray(cat.subItems); (0 until a.length()).map { a.getString(it) } }
                        catch (e: Exception) { emptyList() }
                    } else emptyList()

                    // Entries may be stored under linked PREPARE category ID — check both
                    val prepPartner = prepCatsRecord.firstOrNull { it.name == cat.name }
                    val checkIds = setOfNotNull(cat.id, prepPartner?.id)
                    val catEntries = todayCustomEntries.filter { it.categoryId in checkIds }
                    val doneSubCount = subItemsList.count { key ->
                        catEntries.any { it.subItemKey == key && it.isDone }
                    }

                    val statusText = when (cat.builtinType) {
                        "TASKS"   -> "${todayTasks.count{it.isCompleted}}/${todayTasks.size} done"
                        "PLANK"   -> if (plankToday?.completed == true) "Done" else "Not yet"
                        "PRAYERS" -> "${prayerToday?.let{listOf(it.fajr,it.dhuhr,it.asr,it.maghrib,it.isha).count{p->p}}?:0}/5 prayed"
                        else      -> if (cat.frequency == "MULTIPLE_DAILY" && subItemsList.isNotEmpty())
                            "$doneSubCount/${subItemsList.size} done"
                        else if (catEntries.any { it.isDone && it.subItemKey.isEmpty() }) "Done"
                        else "Tap to record"
                    }
                    val isDone = when (cat.builtinType) {
                        "PLANK"   -> plankToday?.completed == true
                        "PRAYERS" -> (prayerToday?.let{listOf(it.fajr,it.dhuhr,it.asr,it.maghrib,it.isha).count{p->p}}?:0) == 5
                        "TASKS"   -> todayTasks.isNotEmpty() && todayTasks.all { it.isCompleted }
                        else      -> if (cat.frequency == "MULTIPLE_DAILY" && subItemsList.isNotEmpty())
                            doneSubCount == subItemsList.size
                        else catEntries.any { it.isDone && it.subItemKey.isEmpty() }
                    }
                    RecordCategoryCard(cat, statusText, isDone, isDark, editMode, primary,
                        hasPreparePair = prepCatsRecord.any { it.name == cat.name },
                        onClick = {
                            if (!editMode) when (cat.builtinType) {
                                "PLANK"   -> navController.navigate(NavRoutes.PLANK)
                                "PRAYERS" -> navController.navigate(NavRoutes.PRAYERS)
                                "TASKS"   -> navController.navigate(NavRoutes.TODAY_TASKS)
                                else      -> navController.navigate(NavRoutes.recordCustomRoute(cat.id))
                            }
                        },
                        onEdit   = { editingCat = cat },
                        onDelete = { confirmDeleteCat = cat }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecordCategoryDialog(isDark = isDark, accent = accent,
            onDismiss = { showAddDialog = false }) { name, emoji, color, freq, sub, inWidget, inMini ->
            viewModel.addCategory(name, emoji, "RECORD", color, frequency = freq, subItems = sub,
                showInWidget = inWidget, isInMiniTracker = inMini)
            showAddDialog = false
        }
    }

    editingCat?.let { cat ->
        EditRecordCategoryDialog(cat, isDark, accent,
            onDismiss = { editingCat = null },
            onConfirm = { updated -> viewModel.updateCategoryDirect(updated); editingCat = null }
        )
    }

    confirmDeleteCat?.let { cat ->
        AlertDialog(
            onDismissRequest = { confirmDeleteCat = null },
            containerColor = if (isDark) NavyCard else LightCard,
            title = { Text("Delete category?", color = if (isDark) TextPrimary else LightTextPrimary) },
            text = { Text("\"${cat.name}\" will be removed. Historical data is preserved.",
                color = if (isDark) TextSecondary else LightTextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCategory(cat); confirmDeleteCat = null }) {
                    Text("Delete", color = if (isDark) RedMissed else LightRedMissed)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteCat = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RecordCategoryCard(
    cat: CategoryDefinition, statusText: String, isDone: Boolean,
    isDark: Boolean, editMode: Boolean,
    primary: androidx.compose.ui.graphics.Color,
    hasPreparePair: Boolean = false,
    onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
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
            if (cat.isInMiniTracker) Text("Mini Tracker", color = parseColor("#818CF8").copy(alpha=0.8f),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            if (editMode && hasPreparePair) Text("Delete from Prepare to remove",
                color = if (isDark) TextDisabled else LightTextDisabled,
                style = MaterialTheme.typography.labelSmall)
        }
        if (editMode) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, null, tint = primary, modifier = Modifier.size(18.dp))
            }
            if (hasPreparePair) {
                IconButton(onClick = {}, modifier = Modifier.size(32.dp), enabled = false) {
                    Icon(Icons.Default.LinkOff, null,
                        tint = if (isDark) TextDisabled else LightTextDisabled,
                        modifier = Modifier.size(18.dp))
                }
            } else {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, null,
                        tint = if (isDark) RedMissed.copy(alpha=0.7f) else LightRedMissed.copy(alpha=0.7f),
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
fun AddRecordCategoryDialog(isDark: Boolean, accent: String = "#D4A843", onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, color: String, freq: String, sub: String, inWidget: Boolean, inMini: Boolean) -> Unit) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val pr      = parseColor(accent)
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val cardLt  = if (isDark) NavyCardLight else LightCardVariant
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    var selColor by remember { mutableStateOf("#D4A843") }
    var frequency by remember { mutableStateOf("ONCE_DAILY") }
    var subItemsRaw by remember { mutableStateOf("") }
    var inWidget by remember { mutableStateOf(false) }
    var inMini by remember { mutableStateOf(false) }
    val colors = listOf("#D4A843","#22C55E","#818CF8","#EF4444","#FF9500","#34D399","#E879A0","#60A5FA")
    AlertDialog(onDismissRequest = onDismiss, containerColor = cardBg,
        title = { Text("Add Record Category", color = pr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr, unfocusedBorderColor = textDis,
                        focusedTextColor = textPri, unfocusedTextColor = textPri), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { v -> if (v.isEmpty() || (v.length <= 2 && v.all { c -> c.code > 127 })) emoji = v },
                    label = { Text("Emoji (optional)", color = textSec) },
                    supportingText = { Text("Paste one emoji or leave empty", color = textDis, style = MaterialTheme.typography.labelSmall) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr, unfocusedBorderColor = textDis,
                        focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = frequency == "ONCE_DAILY", onClick = { frequency = "ONCE_DAILY" },
                        label = { Text("Once a day") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pr,
                            selectedLabelColor = if (isDark) NavyBackground else LightCard, containerColor = cardLt, labelColor = textSec))
                    FilterChip(selected = frequency == "MULTIPLE_DAILY", onClick = { frequency = "MULTIPLE_DAILY" },
                        label = { Text("Multiple") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pr,
                            selectedLabelColor = if (isDark) NavyBackground else LightCard, containerColor = cardLt, labelColor = textSec))
                }
                if (frequency == "MULTIPLE_DAILY") {
                    OutlinedTextField(value = subItemsRaw, onValueChange = { subItemsRaw = it },
                        label = { Text("Sub-items (comma separated)", color = textSec) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr, unfocusedBorderColor = textDis,
                            focusedTextColor = textPri, unfocusedTextColor = textPri), modifier = Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { hex -> ColorDot(hex, selColor == hex) { selColor = hex } }
                }
                ToggleRow("Show in Snapshot widget (max 8)", inWidget, isDark, pr) { inWidget = it }
                ToggleRow("Use in Mini Tracker widget", inMini, isDark, pr) { v -> inMini = v; if (v) inWidget = true }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val sub = if (frequency == "MULTIPLE_DAILY" && subItemsRaw.isNotBlank())
                        "[" + subItemsRaw.split(",").joinToString(",") { "\"${it.trim()}\"" } + "]" else ""
                    onConfirm(name.trim(), emoji.trim(), selColor, frequency, sub, inWidget, inMini)
                }
            }, enabled = name.isNotBlank()) { Text("Add", color = pr) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = textSec) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordCategoryDialog(cat: CategoryDefinition, isDark: Boolean, accent: String = "#D4A843",
    onDismiss: () -> Unit, onConfirm: (CategoryDefinition) -> Unit) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val pr      = parseColor(accent)
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    var name     by remember { mutableStateOf(cat.name) }
    var emoji    by remember { mutableStateOf(cat.emoji) }
    var selColor by remember { mutableStateOf(cat.color) }
    var inWidget by remember { mutableStateOf(cat.showInWidget) }
    var inMini   by remember { mutableStateOf(cat.isInMiniTracker) }
    val colors = listOf("#D4A843","#22C55E","#818CF8","#EF4444","#FF9500","#34D399","#E879A0","#60A5FA")
    AlertDialog(onDismissRequest = onDismiss, containerColor = cardBg,
        title = { Text("Edit Category", color = pr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr, unfocusedBorderColor = textDis,
                        focusedTextColor = textPri, unfocusedTextColor = textPri), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { v -> if (v.isEmpty() || (v.length <= 2 && v.all { c -> c.code > 127 })) emoji = v },
                    label = { Text("Emoji (optional)", color = textSec) },
                    supportingText = { Text("Paste one emoji or leave empty", color = textDis, style = MaterialTheme.typography.labelSmall) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr, unfocusedBorderColor = textDis,
                        focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { hex -> ColorDot(hex, selColor == hex) { selColor = hex } }
                }
                ToggleRow("Show in Snapshot widget (max 8)", inWidget, isDark, pr) { inWidget = it }
                ToggleRow("Use in Mini Tracker widget", inMini, isDark, pr) { inMini = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(cat.copy(name=name.trim(), emoji=emoji.trim(), color=selColor, showInWidget=inWidget, isInMiniTracker=inMini))
            }, enabled = name.isNotBlank()) { Text("Save", color = pr) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = textSec) } }
    )
}

@Composable
fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
        .background(parseColor(hex)).clickable { onClick() },
        contentAlignment = Alignment.Center) {
        if (selected) Icon(Icons.Default.Check, null,
            tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, isDark: Boolean,
    primary: androidx.compose.ui.graphics.Color, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = if (isDark) NavyBackground else LightCard, checkedTrackColor = primary))
        Text(label, color = if (isDark) TextSecondary else LightTextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
