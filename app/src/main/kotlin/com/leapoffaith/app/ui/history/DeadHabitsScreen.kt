package com.leapoffaith.app.ui.history

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leapoffaith.app.ui.prepare.parseColor
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadHabitsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val isDark        by viewModel.isDarkTheme.collectAsState()
    val accent        by viewModel.userAccentHex.collectAsState()
    val buriedEntries by viewModel.buriedEntries.collectAsState()
    val bg      = if (isDark) NavyBackground else LightBackground
    val cardBg  = if (isDark) NavyCard       else LightCard
    val primary = parseColor(accent)
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary
    val textDis = if (isDark) TextDisabled   else LightTextDisabled

    val buriedGroups = remember(buriedEntries) {
        buriedEntries
            .filter { it.notes.isNotBlank() || it.subItemKey.isNotBlank() }
            .groupBy { e -> e.notes.ifEmpty { e.subItemKey } }
            .map { (label, entries) -> label to entries.sortedByDescending { it.date } }
            .sortedByDescending { (_, v) -> v.size }
    }

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("No Longer Tracked", color = textPri) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        if (buriedGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = textDis, modifier = Modifier.size(56.dp))
                    Text("No buried habits found", color = textSec)
                    Text("All your tracked habits are still active", color = textDis,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text("${buriedGroups.size} habit${if (buriedGroups.size != 1) "s" else ""} no longer tracked",
                    color = textSec, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(16.dp))
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(buriedGroups) { (label, entries) ->
                        var confirmDelete by remember { mutableStateOf(false) }
                        Row(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)).background(cardBg).padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, color = textPri, fontWeight = FontWeight.SemiBold)
                                Text("${entries.size} completions · Last: ${entries.firstOrNull()?.date ?: "-"}",
                                    color = textDis, style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.DeleteOutline, null,
                                    tint = if (isDark) RedMissed.copy(alpha=0.7f) else LightRedMissed.copy(alpha=0.7f),
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                        if (confirmDelete) {
                            AlertDialog(onDismissRequest = { confirmDelete = false },
                                containerColor = if (isDark) NavyCard else LightCard,
                                title = { Text("Delete \"$label\"?", color = primary) },
                                text = { Text("Removes all records for this buried habit permanently.",
                                    color = textSec) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteBuriedHabit(entries)
                                        confirmDelete = false
                                    }) { Text("Delete", color = if (isDark) RedMissed else LightRedMissed) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}