package com.leapoffaith.app.ui.tomorrow

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.leapoffaith.app.data.entities.Task
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TomorrowPlanScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val tomorrowTasks by viewModel.tomorrowTasks.collectAsState()
    val isDark        by viewModel.isDarkTheme.collectAsState()
    val tomorrowLabel = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    val bg      = if (isDark) NavyBackground else LightBackground
    val cardBg  = if (isDark) NavyCard       else LightCard
    val primary = if (isDark) Gold           else ForestGreen
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary
    val textDis = if (isDark) TextDisabled   else LightTextDisabled

    val sheetState = rememberModalBottomSheetState()
    var showSheet  by remember { mutableStateOf(false) }

    val timedTasks   = remember(tomorrowTasks) { tomorrowTasks.filter { it.hourOfDay >= 0 }.sortedBy { it.hourOfDay } }
    val untimedTasks = remember(tomorrowTasks) { tomorrowTasks.filter { it.hourOfDay < 0 }.sortedBy { it.insertOrder } }
    val displayTasks = timedTasks + untimedTasks

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("Tomorrow\'s Plan", color = textPri)
                    Text(tomorrowLabel, style = MaterialTheme.typography.labelSmall, color = textSec)
                } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                actions = { IconButton(onClick = { showSheet = true }) { Icon(Icons.Default.Add, null, tint = primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        if (displayTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lightbulb, null, tint = textDis, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No tasks planned yet", color = textSec)
                    Text("What do you want to do tomorrow?", color = textDis, style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(displayTasks, key = { it.id }) { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(if (task.hourOfDay >= 0) Icons.Default.Schedule else Icons.Default.RadioButtonUnchecked,
                            null, tint = if (task.hourOfDay >= 0) primary else textDis, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, color = textPri, style = MaterialTheme.typography.bodyLarge)
                            if (task.hourOfDay >= 0)
                                Text("%02d:00".format(task.hourOfDay), color = primary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { viewModel.deleteTask(task) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = if (isDark) RedMissed.copy(alpha=0.6f) else LightRedMissed.copy(alpha=0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState, containerColor = cardBg) {
            AddTaskSheet(isDark = isDark, onDismiss = { showSheet = false }) { title, hour ->
                viewModel.addTomorrowTask(title, hour)
                showSheet = false
            }
        }
    }
}

@Composable
private fun AddTaskSheet(isDark: Boolean, onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var title       by remember { mutableStateOf("") }
    var useTime     by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableIntStateOf(9) }

    val primary = if (isDark) Gold else ForestGreen
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled else LightTextDisabled
    val textPri = if (isDark) TextPrimary else LightTextPrimary

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Add Task for Tomorrow", style = MaterialTheme.typography.titleLarge, color = primary)
        OutlinedTextField(value = title, onValueChange = { title = it },
            label = { Text("Task title", color = textSec) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary, unfocusedBorderColor = textDis,
                focusedTextColor = textPri, unfocusedTextColor = textPri),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = useTime, onCheckedChange = { useTime = it },
                colors = SwitchDefaults.colors(checkedThumbColor = if (isDark) NavyBackground else LightCard, checkedTrackColor = primary))
            Text("Set a time", color = textSec)
        }
        if (useTime) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Hour:", color = textSec)
                Slider(value = selectedHour.toFloat(), onValueChange = { selectedHour = it.toInt() },
                    valueRange = 0f..23f, steps = 22,
                    colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                    modifier = Modifier.weight(1f))
                Text("%02d:00".format(selectedHour), color = primary, style = MaterialTheme.typography.titleMedium)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) { Text("Cancel", color = textSec) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (title.isNotBlank()) onConfirm(title.trim(), if (useTime) selectedHour else -1) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = if (isDark) NavyBackground else LightCard)) {
                Text("Add Task")
            }
        }
    }
}
