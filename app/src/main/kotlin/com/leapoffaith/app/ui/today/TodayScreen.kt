package com.leapoffaith.app.ui.today

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.leapoffaith.app.data.entities.Task
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val todayTasks by viewModel.todayTasks.collectAsState()
    val isDark     by viewModel.isDarkTheme.collectAsState()
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    val bg       = if (isDark) NavyBackground  else LightBackground
    val cardBg   = if (isDark) NavyCard        else LightCard
    val cardLt   = if (isDark) NavyCardLight   else LightCardVariant
    val primary  = if (isDark) Gold            else ForestGreen
    val textPri  = if (isDark) TextPrimary     else LightTextPrimary
    val textSec  = if (isDark) TextSecondary   else LightTextSecondary
    val textDis  = if (isDark) TextDisabled    else LightTextDisabled
    val doneClr  = if (isDark) GreenDone       else LightGreenDone

    val pending   = remember(todayTasks) { todayTasks.filter { !it.isCompleted } }
    val completed = remember(todayTasks) { todayTasks.filter { it.isCompleted } }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Today", color = textPri)
                        Text(today, style = MaterialTheme.typography.labelSmall, color = textSec)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        if (todayTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircleOutline, null, tint = textDis, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No tasks for today", color = textSec)
                    Text("Add them in Tomorrow's Plan the night before", color = textDis, style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (pending.isNotEmpty()) {
                    item { Text("Pending (${pending.size})", style = MaterialTheme.typography.labelLarge, color = textSec, modifier = Modifier.padding(bottom = 4.dp)) }
                    items(pending, key = { it.id }) { task ->
                        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically(), exit = fadeOut(animationSpec = tween(300)) + slideOutVertically()) {
                            TaskItem(task, isDark, cardBg, textPri, textSec, doneClr, primary) { viewModel.toggleTask(task) }
                        }
                    }
                }
                if (completed.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)) }
                    item { Text("Done (${completed.size})", style = MaterialTheme.typography.labelLarge, color = textDis, modifier = Modifier.padding(bottom = 4.dp)) }
                    items(completed, key = { it.id }) { task ->
                        TaskItem(task, isDark, cardLt.copy(alpha = 0.5f), textDis, textDis, doneClr, primary, dimmed = true) { viewModel.toggleTask(task) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItem(task: Task, isDark: Boolean,
    bg: androidx.compose.ui.graphics.Color, textPri: androidx.compose.ui.graphics.Color,
    textSec: androidx.compose.ui.graphics.Color, doneClr: androidx.compose.ui.graphics.Color,
    primary: androidx.compose.ui.graphics.Color, dimmed: Boolean = false, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = doneClr, uncheckedColor = textSec,
                checkmarkColor = if (isDark) NavyBackground else LightCard))
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, color = textPri, style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (dimmed) TextDecoration.LineThrough else TextDecoration.None)
            if (task.hourOfDay >= 0)
                Text("%02d:00".format(task.hourOfDay), color = primary.copy(alpha = if (dimmed) 0.4f else 0.8f),
                    style = MaterialTheme.typography.labelSmall)
        }
        if (task.isCompleted)
            Icon(Icons.Default.CheckCircle, null, tint = doneClr.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
    }
}
