package com.leapoffaith.app.ui.affirmations

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leapoffaith.app.ui.prepare.parseColor
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AffirmationsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val isDark  by viewModel.isDarkTheme.collectAsState()
    val accent  by viewModel.userAccentHex.collectAsState()
    val items   by viewModel.affirmations.collectAsState()
    val bg      = if (isDark) NavyBackground else LightBackground
    val primary = parseColor(accent)
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary
    val textDis = if (isDark) TextDisabled   else LightTextDisabled

    var currentIndex by remember(items.size) { mutableIntStateOf(0) }
    var showAdd      by remember { mutableStateOf(false) }
    var newText      by remember { mutableStateOf("") }
    var dragOffset   by remember { mutableFloatStateOf(0f) }
    var dir          by remember { mutableIntStateOf(1) }

    Scaffold(containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Daily Affirmations", color = textPri) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = primary) } },
                actions = {
                    IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null, tint = primary) }
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deleteAffirmation(currentIndex); if (currentIndex > 0) currentIndex-- }) {
                            Icon(Icons.Default.DeleteOutline, null, tint = if (isDark) RedMissed else LightRedMissed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally) {

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)) {
                        Text("No affirmations yet", color = textSec,
                            style = MaterialTheme.typography.titleMedium)
                        Text("Tap + to add your first affirmation",
                            color = textDis, textAlign = TextAlign.Center)
                        Button(onClick = { showAdd = true },
                            colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                            Text("Add Affirmation")
                        }
                    }
                }
            } else {
                // Counter
                Text("${currentIndex + 1} / ${items.size}", color = textSec,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp))

                // Slide
                Box(modifier = Modifier.weight(1f).fillMaxWidth()
                    .clickable {
                            if (currentIndex < items.size - 1) { dir = 1; currentIndex++ }
                            else currentIndex = 0 // wrap around
                        }.pointerInput(items.size) {
                            detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffset < -80 && currentIndex < items.size - 1) {
                                    dir = 1; currentIndex++
                                } else if (dragOffset > 80 && currentIndex > 0) {
                                    dir = -1; currentIndex--
                                }
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, d -> dragOffset += d }
                        )
                    },
                    contentAlignment = Alignment.Center) {
                    AnimatedContent(targetState = currentIndex,
                        transitionSpec = {
                            if (targetState > initialState)
                                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                            else
                                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }, label = "aff") { idx ->
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                Text(items.getOrElse(idx) { "" },
                                    color = textPri,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 32.sp)
                            }
                        }
                    }
                }

                // Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 16.dp)) {
                    items.indices.forEach { i ->
                        Box(modifier = Modifier
                            .size(if (i == currentIndex) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i == currentIndex) primary else textDis.copy(alpha=0.4f)))
                    }
                }
                }
            }
        }


    if (showAdd) {
        AlertDialog(onDismissRequest = { showAdd = false; newText = "" },
            containerColor = if (isDark) NavyCard else LightCard,
            title = { Text("New Affirmation", color = primary) },
            text = {
                OutlinedTextField(value = newText, onValueChange = { newText = it },
                    label = { Text("I am...", color = textSec) },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary,
                        unfocusedBorderColor = textDis, focusedTextColor = textPri, unfocusedTextColor = textPri),
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newText.isNotBlank()) {
                        viewModel.addAffirmation(newText.trim())
                        currentIndex = (viewModel.affirmations.value.size - 1).coerceAtLeast(0)
                        newText = ""; showAdd = false
                    }
                }, enabled = newText.isNotBlank()) { Text("Add", color = primary) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false; newText = "" }) { Text("Cancel") } }
        )
    }
}
