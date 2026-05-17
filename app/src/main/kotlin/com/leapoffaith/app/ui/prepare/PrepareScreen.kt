package com.leapoffaith.app.ui.prepare

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.leapoffaith.app.data.entities.CategoryDefinition
import com.leapoffaith.app.navigation.NavRoutes
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareScreen(viewModel: AppViewModel, navController: NavController, onBack: () -> Unit) {
    val categories by viewModel.prepareCategories.collectAsState()
    val isDark     by viewModel.isDarkTheme.collectAsState()
    val accent     by viewModel.userAccentHex.collectAsState()
    val bg         = if (isDark) NavyBackground else LightBackground
    val primary    = parseColor(accent)
    val textPri    = if (isDark) TextPrimary    else LightTextPrimary
    val textSec    = if (isDark) TextSecondary  else LightTextSecondary
    val textDis    = if (isDark) TextDisabled   else LightTextDisabled

    var editMode         by remember { mutableStateOf(false) }
    var showTypeChooser  by remember { mutableStateOf(false) }
    var showOneTask      by remember { mutableStateOf(false) }
    var showTaskPackage  by remember { mutableStateOf(false) }
    var editingCat       by remember { mutableStateOf<CategoryDefinition?>(null) }
    var confirmDeleteCat by remember { mutableStateOf<CategoryDefinition?>(null) }

    val builtinTypes = categories.map { it.builtinType }.toSet()
    val restorable = listOf(
        "TOMORROW_TASKS" to "Tomorrow's Tasks",
        "WEEK_MEALS"     to "Week Meal Plan",
        "GYM_SPLIT"      to "Gym Split"
    ).filter { (type, _) -> type !in builtinTypes }

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Prepare", color = textPri) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                actions = {
                    // ADD on left of EDIT (left=first in actions row)
                    IconButton(onClick = { showTypeChooser = true }) {
                        Icon(Icons.Default.Add, null, tint = primary)
                    }
                    IconButton(onClick = { editMode = !editMode }) {
                        Icon(if (editMode) Icons.Default.EditOff else Icons.Default.Edit, null, tint = primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                if (editMode) Box(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)).background(primary.copy(alpha=0.1f)).padding(8.dp)) {
                    Text("Edit mode — pencil to edit, trash to delete",
                        color = primary, style = MaterialTheme.typography.labelSmall)
                }
            }
            // Split: plans/packages vs one-time tasks
            val oneTimeBuiltins = setOf("PLANK_PREP", "PRAYERS_PREP")
            val plans = categories.filter {
                (it.builtinType.isNotEmpty() && it.builtinType !in oneTimeBuiltins) ||
                it.frequency == "MULTIPLE_DAILY" || !it.isFixed
            }
            val oneTasks = categories.filter {
                (it.builtinType.isEmpty() || it.builtinType in oneTimeBuiltins) &&
                it.frequency == "ONCE_DAILY" && it.isFixed
            }

            if (plans.isNotEmpty()) {
                item {
                    Text("Plans & Packages", color = primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = androidx.compose.ui.Modifier.padding(top = 8.dp, bottom = 2.dp))
                }
            }
            items(plans, key = { it.id }) { cat ->
                PrepareCategoryCard(cat, isDark, editMode, primary,
                    onClick = {
                        if (!editMode && cat.type == "PREPARE") when (cat.builtinType) {
                            "TOMORROW_TASKS" -> navController.navigate(NavRoutes.TOMORROW_PLAN)
                            "WEEK_MEALS"     -> navController.navigate(NavRoutes.WEEK_MEAL_PLAN)
                            "GYM_SPLIT"      -> navController.navigate(NavRoutes.GYM_SPLIT)
                            "PLANK_PREP"     -> navController.navigate(NavRoutes.PLANK)
                            "PRAYERS_PREP"   -> navController.navigate(NavRoutes.PRAYERS)
                            else -> navController.navigate(NavRoutes.prepareCustomRoute(cat.id))
                        }
                    },
                    onEdit   = { editingCat = cat },
                    onDelete = { confirmDeleteCat = cat }
                )
            }

            if (oneTasks.isNotEmpty()) {
                item {
                    Text("One-Time Tasks", color = primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = androidx.compose.ui.Modifier.padding(top = 12.dp, bottom = 2.dp))
                }
            }
            items(oneTasks, key = { it.id }) { cat ->
                PrepareCategoryCard(cat, isDark, editMode, primary,
                    onClick = { /* One-time tasks: not tappable outside edit mode */ },
                    onEdit   = { editingCat = cat },
                    onDelete = { confirmDeleteCat = cat }
                )
            }
        }
    }

    if (showTypeChooser) {
        AlertDialog(onDismissRequest = { showTypeChooser = false },
            containerColor = if (isDark) NavyCard else LightCard,
            title = { Text("What kind of task?", color = primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TypeChoice("One Task", "A habit — did you do X today?",
                        Icons.Default.RadioButtonChecked, isDark, primary) {
                        showTypeChooser = false; showOneTask = true
                    }
                    TypeChoice("Task Package", "A bundle of sub-tasks to check off",
                        Icons.Default.Checklist, isDark, primary) {
                        showTypeChooser = false; showTaskPackage = true
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
                        color = if (isDark) GreyEmpty else LightGreyEmpty)
                    var reimportExpanded by remember { mutableStateOf(false) }
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .clickable { reimportExpanded = !reimportExpanded }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (reimportExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, tint = primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Re-import", color = primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                    }
                    if (reimportExpanded) {

                    // ── Section A: Fixed habits ────────────────────────────────────
                    val fixedHabits = categories.filter { it.builtinType in
                        setOf("PRAYERS_PREP","TOMORROW_TASKS","WEEK_MEALS","GYM_SPLIT") }
                    if (fixedHabits.isNotEmpty()) {
                        Text("Fixed habits", color = textDis,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp))
                        fixedHabits.forEach { cat ->
                            val periodLabel = when (cat.prepareFrequency) {
                                "NEXT_WEEK" -> "last week"
                                "NEXT_DAY"  -> "yesterday"
                                else        -> "last time"
                            }
                            TextButton(onClick = {
                                viewModel.reimportPreviousPeriod(cat.id, cat.prepareFrequency.ifEmpty { "TODAY" })
                                showTypeChooser = false
                            }, modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${cat.emoji} ${cat.name} — from $periodLabel",
                                    color = primary, style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // ── Section B: Packages (custom categories with sub-items) ─────
                    val packages = categories.filter {
                        it.builtinType.isEmpty() &&
                        it.frequency == "MULTIPLE_DAILY" &&
                        !it.subItems.isNullOrBlank()
                    }
                    if (packages.isNotEmpty()) {
                        Text("Packages", color = textDis,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 6.dp))
                        packages.forEach { cat ->
                            TextButton(onClick = {
                                viewModel.reimportPreviousPeriod(cat.id, cat.prepareFrequency.ifEmpty { "TODAY" })
                                showTypeChooser = false
                            }, modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${cat.emoji} ${cat.name}",
                                    color = primary, style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (restorable.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
                            color = if (isDark) GreyEmpty else LightGreyEmpty)
                        Text("Restore deleted built-ins:", color = textDis,
                            style = MaterialTheme.typography.labelSmall)
                        restorable.forEach { (type, label) ->
                            TextButton(
                                onClick = { viewModel.restoreBuiltin(type); showTypeChooser = false },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("+ Restore: $label", color = primary) }
                        }
                    }
                    } // close reimportExpanded
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTypeChooser = false }) { Text("Cancel") } }
        )
    }

    if (showOneTask) {
        AddOneTaskDialog(isDark = isDark, accent = accent,
            onDismiss = { showOneTask = false },
            onConfirm = { name, emoji, color, freq, sub, prepFreq, isFixed, alsoRecord ->
                // Always create PREPARE entry so it appears in Prepare list for management
                viewModel.addCategory(name, emoji, "PREPARE", color,
                    frequency = freq, subItems = sub,
                    prepareFrequency = if (isFixed) "" else prepFreq,
                    isFixed = isFixed)
                if (alsoRecord) {
                    viewModel.addCategory(name, emoji, "RECORD", color,
                        frequency = freq, subItems = sub, isFixed = isFixed)
                }
                showOneTask = false
            })
    }

    if (showTaskPackage) {
        AddTaskPackageDialog(isDark = isDark, accent = accent,
            onDismiss = { showTaskPackage = false },
            onConfirm = { name, emoji, color, subItemsJson, prepFreq, isFixed, alsoRecord ->
                viewModel.addCategory(name, emoji, "PREPARE", color,
                    frequency = "MULTIPLE_DAILY", subItems = subItemsJson,
                    prepareFrequency = if (isFixed) "" else prepFreq,
                    isFixed = isFixed)
                if (alsoRecord) {
                    viewModel.addCategory(name, emoji, "RECORD", color,
                        frequency = "MULTIPLE_DAILY", subItems = subItemsJson, isFixed = isFixed)
                }
                showTaskPackage = false
            })
    }

    editingCat?.let { cat ->
        EditPrepareCategoryDialog(cat, isDark, accent, viewModel, cat.builtinType,
            onDismiss = { editingCat = null },
            onConfirm = { updated ->
                viewModel.updateCategoryDirect(updated)
                viewModel.syncLinkedCategory(updated)
                editingCat = null
            })
    }

    confirmDeleteCat?.let { cat ->
        AlertDialog(onDismissRequest = { confirmDeleteCat = null },
            containerColor = if (isDark) NavyCard else LightCard,
            title = { Text("Delete \"${cat.name}\"?", color = if (isDark) TextPrimary else LightTextPrimary) },
            text = { Text("Linked Record Progress category will also be removed. Historical data preserved.",
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
private fun TypeChoice(title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDark: Boolean, primary: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val cardLt  = if (isDark) NavyCardLight else LightCardVariant
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(cardLt).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = primary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textPri, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = textSec, style = MaterialTheme.typography.labelSmall)
        }
        Icon(Icons.Default.ChevronRight, null, tint = textSec)
    }
}

@Composable
private fun PrepareCategoryCard(cat: CategoryDefinition, isDark: Boolean, editMode: Boolean,
    primary: androidx.compose.ui.graphics.Color, onClick: () -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val accent  = parseColor(cat.color)
    val badge = buildString {
        if (cat.isFixed) append("Fixed daily")
        else when (cat.prepareFrequency) {
            "NEXT_DAY"  -> append("For tomorrow")
            "NEXT_WEEK" -> append("Fri-Thu")
            "TODAY"     -> append("For today")
        }
        when (cat.builtinType) {
            "TOMORROW_TASKS" -> { clear(); append("Tomorrow task list") }
            "WEEK_MEALS"     -> { clear(); append("Meal plan Fri-Thu") }
            "GYM_SPLIT"      -> { clear(); append("Gym plan Fri-Thu") }
        }
        if (cat.frequency == "MULTIPLE_DAILY" && !cat.subItems.isNullOrBlank()) {
            try { append(" · ${JSONArray(cat.subItems).length()} tasks") } catch (_: Exception) {}
        }
    }
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cardBg)
        .clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
            Text(cat.emoji, fontSize = 24.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(cat.name, color = textPri, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (badge.isNotEmpty()) Text(badge, color = textDis,
                style = MaterialTheme.typography.labelSmall)
        }
        if (editMode) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, null, tint = primary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, null,
                    tint = if (isDark) RedMissed.copy(alpha=0.7f) else LightRedMissed.copy(alpha=0.7f),
                    modifier = Modifier.size(18.dp))
            }
        } else Icon(Icons.Default.ChevronRight, null, tint = textDis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOneTaskDialog(isDark: Boolean, accent: String = "#D4A843", onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, color: String, freq: String, sub: String,
                prepFreq: String, isFixed: Boolean, alsoRecord: Boolean) -> Unit) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val pr      = parseColor(accent)
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val cardLt  = if (isDark) NavyCardLight else LightCardVariant
    var name       by remember { mutableStateOf("") }
    var emoji      by remember { mutableStateOf("") }
    var selColor   by remember { mutableStateOf(accent) }
    var freq       by remember { mutableStateOf("ONCE_DAILY") }
    var subRaw     by remember { mutableStateOf("") }
    var prepFreq   by remember { mutableStateOf("TODAY") }
    var isFixed    by remember { mutableStateOf(true) }
    var alsoRecord by remember { mutableStateOf(true) }
    val colors = listOf("#D4A843","#22C55E","#818CF8","#EF4444","#FF9500","#34D399","#E879A0","#60A5FA")

    AlertDialog(onDismissRequest = onDismiss, containerColor = cardBg,
        title = { Text("Add One Task", color = pr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Task name", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr,
                        unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = emoji,
                    onValueChange = { if (it.length <= 4) emoji = it },
                    label = { Text("Emoji (optional)", color = textSec) },
                    placeholder = { Text("Paste emoji", color = textDis) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr,
                        unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { hex -> ColorDot(hex, selColor == hex) { selColor = hex } }
                }
                Text("Frequency", color = textSec, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = freq == "ONCE_DAILY", onClick = { freq = "ONCE_DAILY" },
                        label = { Text("Once a day") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pr,
                            selectedLabelColor = if (isDark) NavyBackground else LightCard,
                            containerColor = cardLt, labelColor = textSec))
                    FilterChip(selected = freq == "MULTIPLE_DAILY", onClick = { freq = "MULTIPLE_DAILY" },
                        label = { Text("Multiple") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pr,
                            selectedLabelColor = if (isDark) NavyBackground else LightCard,
                            containerColor = cardLt, labelColor = textSec))
                }
                if (freq == "MULTIPLE_DAILY") {
                    OutlinedTextField(value = subRaw, onValueChange = { subRaw = it },
                        label = { Text("Sub-items (comma separated)", color = textSec) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr,
                            unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                        modifier = Modifier.fillMaxWidth())
                }
                FixedToggle(isFixed, isDark, pr) { isFixed = it }
                if (!isFixed) {
                    Text("Prepare for", color = textSec, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("TODAY" to "Today", "NEXT_DAY" to "Tomorrow").forEach { (v, l) ->
                            FilterChip(selected = prepFreq == v, onClick = { prepFreq = v },
                                label = { Text(l) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pr,
                                    selectedLabelColor = if (isDark) NavyBackground else LightCard,
                                    containerColor = cardLt, labelColor = textSec))
                        }
                    }
                }
                ToggleRow("Track in Record Progress", alsoRecord, isDark, pr) { alsoRecord = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val sub = if (freq == "MULTIPLE_DAILY" && subRaw.isNotBlank())
                        "[" + subRaw.split(",").joinToString(",") { "\"${it.trim()}\"" } + "]" else ""
                    onConfirm(name.trim(), emoji.trim(), selColor, freq, sub, prepFreq, isFixed, alsoRecord)
                }
            }, enabled = name.isNotBlank()) { Text("Add", color = pr) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = textSec) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskPackageDialog(isDark: Boolean, accent: String = "#D4A843", onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, color: String, subItemsJson: String,
                prepFreq: String, isFixed: Boolean, alsoRecord: Boolean) -> Unit) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val pr      = parseColor(accent)
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val cardLt  = if (isDark) NavyCardLight else LightCardVariant
    var name     by remember { mutableStateOf("") }
    var emoji    by remember { mutableStateOf("") }
    var selColor by remember { mutableStateOf(accent) }
    var prepFreq by remember { mutableStateOf("NEXT_WEEK") }
    var isFixed  by remember { mutableStateOf(true) }
    var alsoRecord by remember { mutableStateOf(true) }
    var newTask  by remember { mutableStateOf("") }
    var tasks    by remember { mutableStateOf(listOf<String>()) }
    val colors = listOf("#D4A843","#22C55E","#818CF8","#EF4444","#FF9500","#34D399","#E879A0","#60A5FA")

    AlertDialog(onDismissRequest = onDismiss, containerColor = cardBg,
        title = { Text("Add Task Package", color = pr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Package name", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr,
                        unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = emoji,
                    onValueChange = { if (it.length <= 4) emoji = it },
                    label = { Text("Emoji (optional)", color = textSec) },
                    placeholder = { Text("Paste emoji", color = textDis) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr,
                        unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth())
                if (isFixed || prepFreq != "NEXT_WEEK") Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newTask, onValueChange = { newTask = it },
                        label = { Text("Add a task", color = textSec) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr,
                            unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                        modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = {
                        if (newTask.isNotBlank()) { tasks = tasks + newTask.trim(); newTask = "" }
                    }) { Icon(Icons.Default.Add, null, tint = pr) }
                }
                if (tasks.isNotEmpty() && (isFixed || prepFreq != "NEXT_WEEK")) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        tasks.forEachIndexed { i, task ->
                            Row(modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• $task", color = textPri, modifier = Modifier.weight(1f))
                                IconButton(onClick = { tasks = tasks.filterIndexed { idx,_ -> idx != i } },
                                    modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, tint = textDis, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { hex -> ColorDot(hex, selColor == hex) { selColor = hex } }
                }
                FixedToggle(isFixed, isDark, pr) { isFixed = it }
                if (!isFixed) {
                    Text("Prepare for", color = textSec, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("TODAY" to "Today", "NEXT_DAY" to "Tomorrow", "NEXT_WEEK" to "Week").forEach { (v, l) ->
                            FilterChip(selected = prepFreq == v, onClick = { prepFreq = v },
                                label = { Text(l, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pr,
                                    selectedLabelColor = if (isDark) NavyBackground else LightCard,
                                    containerColor = cardLt, labelColor = textSec))
                        }
                    }
                    if (prepFreq == "NEXT_WEEK") {
                        Text("Tasks are added per-day from the Prepare screen.",
                            color = textDis, style = MaterialTheme.typography.labelSmall)
                    }
                }
                ToggleRow("Track in Record Progress", alsoRecord, isDark, pr) { alsoRecord = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val sub = if (tasks.isNotEmpty() && (isFixed || prepFreq != "NEXT_WEEK")) "[" + tasks.joinToString(",") { "\"$it\"" } + "]" else ""
                    onConfirm(name.trim(), emoji.trim(), selColor, sub, if (isFixed) "" else prepFreq, isFixed, alsoRecord)
                }
            }, enabled = name.isNotBlank()) { Text("Create", color = pr) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = textSec) } }
    )
}

@Composable
fun FixedToggle(isFixed: Boolean, isDark: Boolean, primary: androidx.compose.ui.graphics.Color,
    onToggle: (Boolean) -> Unit) {
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = isFixed, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isDark) NavyBackground else LightCard, checkedTrackColor = primary))
        Column {
            Text(if (isFixed) "Fixed — repeats every day" else "Not fixed — expires at end of day",
                color = textSec, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold)
            Text(if (isFixed) "Same tasks shown daily" else "Done tasks stay in history; undone ones are cleared",
                color = textDis, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPrepareCategoryDialog(cat: CategoryDefinition, isDark: Boolean, accent: String = "#D4A843",
    viewModel: AppViewModel, builtinType: String = "", onDismiss: () -> Unit,
    onConfirm: (CategoryDefinition) -> Unit) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val pr      = parseColor(accent)
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val cardLt  = if (isDark) NavyCardLight else LightCardVariant
    var name     by remember { mutableStateOf(cat.name) }
    var emoji    by remember { mutableStateOf(cat.emoji) }
    var selColor by remember { mutableStateOf(cat.color) }
    var prepFreq by remember { mutableStateOf(cat.prepareFrequency.ifEmpty { "NEXT_DAY" }) }
    var isFixed  by remember { mutableStateOf(cat.isFixed) }
    var recExists  by remember { mutableStateOf(false) }
    var alsoRecord by remember { mutableStateOf(false) }
    LaunchedEffect(cat.id) {
        recExists = viewModel.findRecordCategoryByName(cat.name) != null
        alsoRecord = recExists
    }
    val colors = listOf("#D4A843","#22C55E","#818CF8","#EF4444","#FF9500","#34D399","#E879A0","#60A5FA")

    AlertDialog(onDismissRequest = onDismiss, containerColor = cardBg,
        title = { Text("Edit", color = pr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr,
                        unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = emoji,
                    onValueChange = { if (it.length <= 4) emoji = it },
                    label = { Text("Emoji", color = textSec) },
                    placeholder = { Text("Paste emoji", color = textDis) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr,
                        unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { hex -> ColorDot(hex, selColor == hex) { selColor = hex } }
                }
                if (builtinType.isEmpty()) {
                    FixedToggle(isFixed, isDark, pr) { isFixed = it }
                    if (!isFixed) {
                        Text("Prepare for", color = textSec, style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("TODAY" to "Today", "NEXT_DAY" to "Tomorrow", "NEXT_WEEK" to "Week").forEach { (v, l) ->
                                FilterChip(selected = prepFreq == v, onClick = { prepFreq = v },
                                    label = { Text(l, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pr,
                                        selectedLabelColor = if (isDark) NavyBackground else LightCard,
                                        containerColor = cardLt, labelColor = textSec))
                            }
                        }
                    }
                }
                ToggleRow("Track in Record Progress", alsoRecord, isDark, pr) { alsoRecord = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (alsoRecord && !recExists) viewModel.addCategory(cat.name, cat.emoji, "RECORD", cat.color, isFixed = cat.isFixed, showInWidget = false)
                if (!alsoRecord && recExists) viewModel.unlinkRecordCategory(cat.name)
                onConfirm(cat.copy(name=name.trim(), emoji=emoji.trim(), color=selColor,
                    prepareFrequency=if (isFixed) "" else prepFreq, isFixed=isFixed))
            }, enabled = name.isNotBlank()) { Text("Save", color = pr) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = textSec) } }
    )
}

fun parseColor(hex: String): androidx.compose.ui.graphics.Color = try {
    val c = hex.trimStart('#').toLong(16)
    androidx.compose.ui.graphics.Color(((c shr 16) and 0xFF)/255f, ((c shr 8) and 0xFF)/255f, (c and 0xFF)/255f)
} catch (_: Exception) { androidx.compose.ui.graphics.Color(0xFFD4A843) }

@Composable
fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = androidx.compose.ui.Modifier.size(28.dp)
        .clip(RoundedCornerShape(6.dp)).background(parseColor(hex)).clickable { onClick() },
        contentAlignment = Alignment.Center) {
        if (selected) Icon(Icons.Default.Check, null,
            tint = androidx.compose.ui.graphics.Color.White, modifier = androidx.compose.ui.Modifier.size(14.dp))
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, isDark: Boolean,
    primary: androidx.compose.ui.graphics.Color, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isDark) NavyBackground else LightCard, checkedTrackColor = primary))
        Text(label, color = if (isDark) TextSecondary else LightTextSecondary,
            style = MaterialTheme.typography.bodySmall)
    }
}
