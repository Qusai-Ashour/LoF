package com.leapoffaith.app.ui.mealplan
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val MEAL_TYPES = listOf("Breakfast","Lunch","Dinner","Snack")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekMealPlanScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val weekMeals by viewModel.weekMeals.collectAsState()
    val isDark    by viewModel.isDarkTheme.collectAsState()
    var isLocked    by remember { mutableStateOf(false) }
    var expandedDay by remember { mutableIntStateOf(-1) }
    var showAddDialog  by remember { mutableStateOf(false) }
    var addingForIndex by remember { mutableIntStateOf(0) }

    val bg      = if (isDark) NavyBackground else LightBackground
    val cardBg  = if (isDark) NavyCard       else LightCard
    val primary = if (isDark) Gold           else ForestGreen
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary
    val textDis = if (isDark) TextDisabled   else LightTextDisabled

    val friday    = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
    val weekLabel = friday.format(DateTimeFormatter.ofPattern("MMM d")) + " - " +
                    friday.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d"))

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("Week Meal Plan", color = textPri)
                    Text(weekLabel, style = MaterialTheme.typography.labelSmall, color = textSec)
                } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (isLocked) {
                        OutlinedButton(onClick = { isLocked = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primary)) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("Edit Plan")
                        }
                    } else if (weekMeals.isNotEmpty()) {
                        Button(onClick = { isLocked = true },
                            colors = ButtonDefaults.buttonColors(containerColor = primary,
                                contentColor = if (isDark) NavyBackground else LightCard)) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("Save Plan")
                        }
                    }
                }
            }
            items(7) { i ->
                val dayDate  = friday.plusDays(i.toLong())
                val isoDay   = dayDate.dayOfWeek.value
                val dayName  = dayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                val dayMeals = weekMeals.filter { it.dayOfWeek == isoDay }
                val isExpanded = expandedDay == i && !isLocked

                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cardBg)) {
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { if (!isLocked) expandedDay = if (expandedDay == i) -1 else i }
                        .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(dayName, color = textPri, style = MaterialTheme.typography.titleMedium)
                            Text(dayDate.format(DateTimeFormatter.ofPattern("MMM d")), color = textSec, style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (dayMeals.isNotEmpty()) Text("${dayMeals.size} meals", color = primary, style = MaterialTheme.typography.labelSmall)
                            if (!isLocked) Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = textSec)
                        }
                    }
                    AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HorizontalDivider(color = if (isDark) GreyEmpty else LightGreyEmpty)
                            Spacer(Modifier.height(4.dp))
                            if (dayMeals.isEmpty()) {
                                Text("No meals added yet", color = textDis, style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp))
                            } else {
                                dayMeals.forEach { meal ->
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(meal.description, color = textPri, style = MaterialTheme.typography.bodyMedium)
                                            Text(meal.mealType, color = primary.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                        }
                                        IconButton(onClick = { viewModel.deleteWeekMeal(meal.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Close, null,
                                                tint = if (isDark) RedMissed.copy(alpha=0.5f) else LightRedMissed.copy(alpha=0.5f),
                                                modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                            TextButton(onClick = { addingForIndex = i; showAddDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = primary)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp)); Text("Add Meal")
                            }
                        }
                    }
                    if (isLocked && dayMeals.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            HorizontalDivider(color = if (isDark) GreyEmpty else LightGreyEmpty)
                            Spacer(Modifier.height(4.dp))
                            dayMeals.forEach { meal ->
                                Text("- ${meal.mealType}: ${meal.description}", color = textSec, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var description  by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("Breakfast") }
        AlertDialog(onDismissRequest = { showAddDialog = false }, containerColor = cardBg,
            title = { Text("Add Meal", color = primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = description, onValueChange = { description = it },
                        label = { Text("Meal description", color = textSec) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary,
                            unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                        modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MEAL_TYPES.forEach { type ->
                            FilterChip(selected = selectedType == type, onClick = { selectedType = type },
                                label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primary,
                                    selectedLabelColor = if (isDark) NavyBackground else LightCard,
                                    containerColor = if (isDark) NavyCardLight else LightCardVariant,
                                    labelColor = textSec))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (description.isNotBlank()) {
                        viewModel.addWeekMeal(addingForIndex, description.trim(), selectedType)
                        showAddDialog = false
                    }
                }, enabled = description.isNotBlank()) { Text("Add", color = primary) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = textSec) } }
        )
    }
}
