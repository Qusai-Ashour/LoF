package com.leapoffaith.app.ui.gym

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leapoffaith.app.ui.prepare.parseColor
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
fun GymSplitScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val isDark  by viewModel.isDarkTheme.collectAsState()
    val accent  by viewModel.userAccentHex.collectAsState()
    val bg      = if (isDark) NavyBackground else LightBackground
    val primary = parseColor(accent)
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary
    val textDis = if (isDark) TextDisabled   else LightTextDisabled
    val cardBg  = if (isDark) NavyCard       else LightCard

    val fmt    = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val friday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))

    // Use a fixed gymSplit categoryId (builtinType = "GYM_SPLIT")
    val gymCatId = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val uid = viewModel.currentUser.value
        val all = viewModel.repository.getAllCategoriesOnce(uid)
        gymCatId.longValue = all.firstOrNull { it.builtinType == "GYM_SPLIT" }?.id ?: 0L
    }

    val allItems by viewModel.getPrepareItems(gymCatId.longValue).collectAsState(initial = emptyList())

    var expandedDay by remember { mutableIntStateOf(-1) }
    val sheetState  = rememberModalBottomSheetState()
    var showAddDay  by remember { mutableIntStateOf(-1) }
    var newExercise by remember { mutableStateOf("") }

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("Gym Split", color = textPri)
                    Text("${friday.format(DateTimeFormatter.ofPattern("MMM d"))} — ${friday.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d"))}",
                        color = textSec, style = MaterialTheme.typography.labelSmall)
                } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)) {
            items(7) { i ->
                val dayDate  = friday.plusDays(i.toLong())
                val dayStr   = dayDate.format(fmt)
                val dayName  = dayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                val isToday  = dayDate == LocalDate.now()
                val dayItems = allItems.filter { it.date == dayStr }
                val isExpanded = expandedDay == i

                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg)) {
                    Row(modifier = Modifier.fillMaxWidth()
                        .clickable { expandedDay = if (expandedDay == i) -1 else i }
                        .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(dayName,
                                color = if (isToday) primary else textPri,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                            Text(dayDate.format(DateTimeFormatter.ofPattern("MMM d")),
                                color = textSec, style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (dayItems.isNotEmpty()) Text("${dayItems.size} exercises",
                                color = primary, style = MaterialTheme.typography.labelSmall)
                            else Text("Rest / add exercises", color = textDis,
                                style = MaterialTheme.typography.labelSmall)
                            Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, tint = textSec)
                        }
                    }
                    AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HorizontalDivider(color = if (isDark) GreyEmpty else LightGreyEmpty)
                            Spacer(Modifier.height(4.dp))
                            dayItems.forEach { entry ->
                                Row(modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("🏋️ ${entry.notes}", color = textPri, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.deletePrepareItem(entry) },
                                        modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, null, tint = textDis,
                                            modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            if (showAddDay == i) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = newExercise,
                                        onValueChange = { newExercise = it },
                                        modifier = Modifier.weight(1f), singleLine = true,
                                        label = { Text("Exercise name", color = textSec) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primary, unfocusedBorderColor = textDis,
                                            focusedTextColor = textPri, unfocusedTextColor = textPri))
                                    IconButton(onClick = {
                                        if (newExercise.isNotBlank() && gymCatId.longValue > 0) {
                                            viewModel.addPrepareItemWithTime(gymCatId.longValue, dayStr, newExercise.trim(), -1)
                                            newExercise = ""
                                            showAddDay = -1
                                        }
                                    }) { Icon(Icons.Default.Check, null, tint = primary) }
                                }
                            }
                            TextButton(onClick = { showAddDay = if (showAddDay == i) -1 else i; newExercise = "" },
                                colors = ButtonDefaults.textButtonColors(contentColor = primary)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add exercise")
                            }
                        }
                    }
                }
            }
        }
    }
}
