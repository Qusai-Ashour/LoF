package com.leapoffaith.app.ui.prepare

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.leapoffaith.app.data.entities.CategoryDefinition
import com.leapoffaith.app.navigation.NavRoutes
import com.leapoffaith.app.ui.record.AddRecordCategoryDialog
import com.leapoffaith.app.ui.record.ColorDot
import com.leapoffaith.app.ui.record.ToggleRow
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel

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

    var editMode        by remember { mutableStateOf(false) }
    var showAddDialog   by remember { mutableStateOf(false) }
    var editingCat      by remember { mutableStateOf<CategoryDefinition?>(null) }
    var confirmDeleteCat by remember { mutableStateOf<CategoryDefinition?>(null) }

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Prepare", color = textPri) },
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
        if (editMode) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp)).background(primary.copy(alpha=0.1f)).padding(8.dp)) {
                Text("Edit mode — tap pencil to edit, trash to delete",
                    color = primary, style = MaterialTheme.typography.labelSmall)
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)) {
            items(categories, key = { it.id }) { cat ->
                PrepareCategoryCard(cat, isDark, editMode, primary,
                    onClick = {
                        if (!editMode) when (cat.builtinType) {
                            "TOMORROW_TASKS" -> navController.navigate(NavRoutes.TOMORROW_PLAN)
                            "WEEK_MEALS"     -> navController.navigate(NavRoutes.WEEK_MEAL_PLAN)
                            else             -> navController.navigate(NavRoutes.prepareCustomRoute(cat.id))
                        }
                    },
                    onEdit   = { editingCat = cat },
                    onDelete = { confirmDeleteCat = cat }
                )
            }
        }
    }

    if (showAddDialog) {
        AddPrepareCategoryDialog(isDark = isDark, accent = accent,
            onDismiss = { showAddDialog = false }) { name, emoji, color, prepFreq, alsoRecord ->
            viewModel.addCategory(name, emoji, "PREPARE", color, prepareFrequency = prepFreq)
            if (alsoRecord) viewModel.addCategory(name, emoji, "RECORD", color)
            showAddDialog = false
        }
    }

    editingCat?.let { cat ->
        EditPrepareCategoryDialog(cat, isDark, accent, viewModel, cat.builtinType,
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
private fun PrepareCategoryCard(cat: CategoryDefinition, isDark: Boolean, editMode: Boolean,
    primary: androidx.compose.ui.graphics.Color, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val accent  = parseColor(cat.color)

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cardBg)
        .clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
            Text(cat.emoji, fontSize = 24.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(cat.name, color = textPri, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            val sub = when (cat.prepareFrequency) {
                "NEXT_DAY"  -> "Plans for tomorrow"
                "NEXT_WEEK" -> "Weekly plan (Fri-Thu)"
                "TODAY"     -> "Plans for today"
                else -> when (cat.builtinType) {
                    "TOMORROW_TASKS" -> "Tomorrow's task list"
                    "WEEK_MEALS"     -> "Meal plan (Fri-Thu)"
                    else -> ""
                }
            }
            if (sub.isNotEmpty()) Text(sub, color = textDis, style = MaterialTheme.typography.labelSmall)
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
        } else { Icon(Icons.Default.ChevronRight, null, tint = textDis) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPrepareCategoryDialog(cat: CategoryDefinition, isDark: Boolean, accent: String = "#D4A843",
    viewModel: AppViewModel, builtinType: String = "", onDismiss: () -> Unit, onConfirm: (CategoryDefinition) -> Unit) {
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
    var recordExists by remember { mutableStateOf(false) }
    var addToRecord  by remember { mutableStateOf(false) }

    LaunchedEffect(cat.name) {
        val rec = viewModel.findRecordCategoryByName(cat.name)
        recordExists = rec != null
    }

    val colors = listOf("#D4A843","#22C55E","#818CF8","#EF4444","#FF9500","#34D399","#E879A0","#60A5FA")

    AlertDialog(onDismissRequest = onDismiss, containerColor = cardBg,
        title = { Text("Edit Prepare Category", color = pr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr, unfocusedBorderColor = textDis,
                        focusedTextColor = textPri, unfocusedTextColor = textPri), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = emoji, onValueChange = { emoji = it },
                    label = { Text("Emoji", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr, unfocusedBorderColor = textDis,
                        focusedTextColor = textPri, unfocusedTextColor = textPri), modifier = Modifier.fillMaxWidth())
                if (builtinType != "WEEK_MEALS") Text("Prepare for", color = textSec, style = MaterialTheme.typography.labelLarge)
                if (builtinType != "WEEK_MEALS") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NEXT_DAY" to "Tomorrow", "NEXT_WEEK" to "Week", "TODAY" to "Today").forEach { (v, l) ->
                        FilterChip(selected = prepFreq == v, onClick = { prepFreq = v },
                            label = { Text(l, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pr,
                                selectedLabelColor = if (isDark) NavyBackground else LightCard, containerColor = cardLt, labelColor = textSec))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { hex -> ColorDot(hex, selColor == hex) { selColor = hex } }
                }
                if (!recordExists && builtinType != "TOMORROW_TASKS") {
                    ToggleRow("Also add to Record Progress", addToRecord, isDark, pr) { addToRecord = it }
                } else {
                    Text("Linked to Record Progress", color = pr.copy(alpha=0.7f),
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updated = cat.copy(name=name.trim(), emoji=emoji.trim(), color=selColor, prepareFrequency=prepFreq)
                onConfirm(updated)
                if (addToRecord && !recordExists) viewModel.addCategory(name.trim(), emoji.trim(), "RECORD", selColor)
            }, enabled = name.isNotBlank()) { Text("Save", color = pr) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = textSec) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrepareCategoryDialog(isDark: Boolean, accent: String = "#D4A843", onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, color: String, prepFreq: String, alsoRecord: Boolean) -> Unit) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val pr      = parseColor(accent)
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val cardLt  = if (isDark) NavyCardLight else LightCardVariant
    var name       by remember { mutableStateOf("") }
    var emoji      by remember { mutableStateOf("") }
    var selColor   by remember { mutableStateOf("#D4A843") }
    var prepFreq   by remember { mutableStateOf("NEXT_DAY") }
    var alsoRecord by remember { mutableStateOf(false) }
    val colors = listOf("#D4A843","#22C55E","#818CF8","#EF4444","#FF9500","#34D399","#E879A0","#60A5FA")
    AlertDialog(onDismissRequest = onDismiss, containerColor = cardBg,
        title = { Text("Add Prepare Category", color = pr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Category name", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr, unfocusedBorderColor = textDis,
                        focusedTextColor = textPri, unfocusedTextColor = textPri), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = emoji, onValueChange = { emoji = it },
                    label = { Text("Emoji", color = textSec) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pr, unfocusedBorderColor = textDis,
                        focusedTextColor = textPri, unfocusedTextColor = textPri), modifier = Modifier.fillMaxWidth())
                Text("Prepare for", color = textSec, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NEXT_DAY" to "Tomorrow", "NEXT_WEEK" to "This Week", "TODAY" to "Today").forEach { (v, l) ->
                        FilterChip(selected = prepFreq == v, onClick = { prepFreq = v },
                            label = { Text(l, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pr,
                                selectedLabelColor = if (isDark) NavyBackground else LightCard, containerColor = cardLt, labelColor = textSec))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { hex -> ColorDot(hex, selColor == hex) { selColor = hex } }
                }
                ToggleRow("Also add to Record Progress", alsoRecord, isDark, pr) { alsoRecord = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim(), emoji.trim(), selColor, prepFreq, alsoRecord) },
                enabled = name.isNotBlank()) { Text("Add", color = pr) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = textSec) } }
    )
}

fun parseColor(hex: String): androidx.compose.ui.graphics.Color {
    return try {
        val c = hex.trimStart('#').toLong(16)
        androidx.compose.ui.graphics.Color(red=((c shr 16) and 0xFF)/255f, green=((c shr 8) and 0xFF)/255f, blue=(c and 0xFF)/255f)
    } catch (e: Exception) { androidx.compose.ui.graphics.Color(0xFFD4A843) }
}

fun AddCategoryDialog(isDark: Boolean, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {}
