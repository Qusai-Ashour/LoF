package com.leapoffaith.app.ui.hub

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.leapoffaith.app.navigation.NavRoutes
import com.leapoffaith.app.ui.prepare.parseColor
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel

private val ACCENT_OPTIONS = listOf(
    "#D4A843" to "Gold", "#22C55E" to "Green", "#60A5FA" to "Blue",
    "#818CF8" to "Purple", "#EF4444" to "Red", "#FF9500" to "Orange",
    "#34D399" to "Teal", "#E879A0" to "Pink", "#F97316" to "Amber",
    "#A78BFA" to "Lavender"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(viewModel: AppViewModel, navController: NavController) {
    val currentUser        by viewModel.currentUser.collectAsState()
    val handler            = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val isDark             by viewModel.isDarkTheme.collectAsState()
    val widgetUser         by viewModel.widgetUser.collectAsState()
    val accentHex          by viewModel.userAccentHex.collectAsState()
    val todayTasks         by viewModel.todayTasks.collectAsState()
    val plankToday         by viewModel.plankToday.collectAsState()
    val prayerToday        by viewModel.prayerToday.collectAsState()
    val recordCats         by viewModel.recordCategories.collectAsState()
    val todayCustomEntries by viewModel.todayCustomEntries.collectAsState()
    val showSnapshot       by viewModel.showSnapshot.collectAsState()
    val showAffirmations   by viewModel.showAffirmations.collectAsState()

    val displayName = if (currentUser == "qusai") "Qusai" else "Lina"
    val accentColor = parseColor(accentHex)
    val bg      = if (isDark) NavyBackground else LightBackground
    val cardBg  = if (isDark) NavyCard       else LightCard
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary
    val textDis = if (isDark) TextDisabled   else LightTextDisabled

    var showMenu         by remember { mutableStateOf(false) }
    var showWidgetDialog by remember { mutableStateOf(false) }
    var showColorPicker  by remember { mutableStateOf(false) }
    var snapshotExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = showMenu) { showMenu = false }

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = bg,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Welcome back,", color = textSec,
                                style = MaterialTheme.typography.labelSmall)
                            Text(displayName, color = accentColor,
                                style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Menu, null, tint = accentColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showAffirmations) {
                    Row(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)).background(cardBg)
                        .clickable { navController.navigate(NavRoutes.AFFIRMATIONS) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, null, tint = accentColor,
                            modifier = Modifier.size(20.dp))
                        Text("Daily Affirmations", color = textPri,
                            fontWeight = FontWeight.SemiBold)
                    }
                }

                if (showSnapshot) {
                    Column(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)).background(cardBg)) {
                        Row(modifier = Modifier.fillMaxWidth()
                            .clickable { snapshotExpanded = !snapshotExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Today, null, tint = accentColor,
                                    modifier = Modifier.size(20.dp))
                                Text("Today's Snapshot", color = textPri,
                                    fontWeight = FontWeight.SemiBold)
                            }
                            Icon(if (snapshotExpanded) Icons.Default.ExpandLess
                                 else Icons.Default.ExpandMore, null, tint = textSec)
                        }
                        AnimatedVisibility(visible = snapshotExpanded,
                            enter = expandVertically(), exit = shrinkVertically()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)
                                .padding(bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HorizontalDivider(color = if (isDark) GreyEmpty else LightGreyEmpty)
                                Spacer(Modifier.height(4.dp))
                                if (recordCats.isEmpty()) {
                                    Text("No record categories yet", color = textDis,
                                        style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    recordCats.forEach { cat ->
                                        val value = when (cat.builtinType) {
                                            "TASKS"   -> "${todayTasks.count { it.isCompleted }}/${todayTasks.size} done"
                                            "PLANK"   -> if (plankToday?.completed == true) "Done" else "Not yet"
                                            "PRAYERS" -> "${prayerToday?.let { listOf(it.fajr, it.dhuhr, it.asr, it.maghrib, it.isha).count { p -> p } } ?: 0}/5"
                                            else -> {
                                                val entries = todayCustomEntries.filter { it.categoryId == cat.id }
                                                val done = if (cat.frequency == "MULTIPLE_DAILY")
                                                    entries.isNotEmpty() && entries.all { it.isDone }
                                                else entries.any { it.isDone }
                                                if (done) "Done" else "No"
                                            }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Text("${cat.emoji} ${cat.name}", color = textSec,
                                                style = MaterialTheme.typography.bodyMedium)
                                            Text(value, color = parseColor(cat.color),
                                                fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(cardBg, accentColor.copy(alpha = 0.2f))))
                    .clickable { navController.navigate(NavRoutes.VISUAL_TRACKER) },
                    contentAlignment = Alignment.CenterStart) {
                    Row(modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Default.CalendarMonth, null, tint = accentColor,
                            modifier = Modifier.size(32.dp))
                        Column {
                            Text("Visual Tracker", color = textPri,
                                fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Overview", color = textSec,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = textDis,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
                }

                Spacer(Modifier.height(4.dp))
                Text("What do you want to do?", color = textSec,
                    style = MaterialTheme.typography.labelLarge)

                BigActionButton("PREPARE", "Plan tomorrow, week meals, more",
                    Icons.Default.EditNote, accentColor, isDark) {
                    navController.navigate(NavRoutes.PREPARE)
                }
                BigActionButton("RECORD PROGRESS", "Tasks, Plank, Prayers, custom",
                    Icons.Default.CheckBox, accentColor.copy(alpha = 0.7f), isDark) {
                    navController.navigate(NavRoutes.RECORD)
                }

                TextButton(onClick = { navController.navigate(NavRoutes.HISTORY) },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.EditCalendar, null,
                        modifier = Modifier.size(16.dp), tint = textSec)
                    Spacer(Modifier.width(6.dp))
                    Text("Forgot to add an entry?", color = textSec,
                        style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Slide-in overlay menu — no Popup, no DrawerState, no blank screen ──
        AnimatedVisibility(
            visible = showMenu,
            enter = slideInHorizontally { -it },
            exit  = slideOutHorizontally { -it },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        showMenu = false
                    })
                Column(modifier = Modifier
                    .fillMaxWidth(0.72f).fillMaxHeight()
                    .background(if (isDark) NavyCard else LightCard)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 48.dp, bottom = 16.dp)
                ) {
                    Text("Menu", color = accentColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                    MenuRow("Add to Calendar", Icons.Default.CalendarToday, textPri, textSec) {
                        showMenu = false
                        handler.postDelayed({ navController.navigate(NavRoutes.CALENDAR) { launchSingleTop = true } }, 200)
                    }
                    MenuRow("History", Icons.Default.History, textPri, textSec) {
                        showMenu = false
                        handler.postDelayed({ navController.navigate(NavRoutes.HISTORY) { launchSingleTop = true } }, 200)
                    }
                    MenuRow("Widget: ${if (widgetUser == "qusai") "Qusai" else "Lina"}",
                        Icons.Default.Widgets, textPri, textSec) {
                        showMenu = false; handler.postDelayed({ showWidgetDialog = true }, 250)
                    }
                    MenuRow("Theme Color", Icons.Default.Palette, textPri, textSec) {
                        showMenu = false; handler.postDelayed({ showColorPicker = true }, 250)
                    }
                    MenuRow(if (isDark) "Light Theme" else "Dark Theme",
                        if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        textPri, textSec) {
                        showMenu = false; viewModel.toggleTheme()
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Home Preferences", color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    PrefToggleRow("Daily Affirmations", Icons.Default.AutoAwesome,
                        showAffirmations, accentColor, textPri, isDark) {
                        viewModel.toggleShowAffirmations()
                    }
                    PrefToggleRow("Today's Snapshot", Icons.Default.Today,
                        showSnapshot, accentColor, textPri, isDark) {
                        viewModel.toggleShowSnapshot()
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    MenuRow("Switch User", Icons.Default.SwitchAccount, textPri, textSec) {
                        showMenu = false
                        viewModel.selectUser("")
                        handler.postDelayed({
                            navController.navigate(NavRoutes.WHO_ARE_YOU) {
                                popUpTo(NavRoutes.HOME) { inclusive = true }
                            }
                        }, 250)
                    }
                }
            }
        }
    }

    if (showWidgetDialog) {
        AlertDialog(onDismissRequest = { showWidgetDialog = false },
            containerColor = if (isDark) NavyCard else LightCard,
            title = { Text("Widgets show data for...", color = accentColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("qusai" to "Qusai", "lina" to "Lina").forEach { (id, label) ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (widgetUser == id) accentColor.copy(alpha = 0.15f)
                                else if (isDark) NavyCardLight else LightCardVariant)
                            .clickable { viewModel.setWidgetUser(id); showWidgetDialog = false }
                            .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = if (widgetUser == id) accentColor else textPri)
                            if (widgetUser == id)
                                Icon(Icons.Default.Check, null, tint = accentColor,
                                    modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }, confirmButton = {})
    }

    if (showColorPicker) {
        AlertDialog(onDismissRequest = { showColorPicker = false },
            containerColor = if (isDark) NavyCard else LightCard,
            title = { Text("$displayName's Theme Color", color = accentColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose your accent color", color = textSec,
                        style = MaterialTheme.typography.bodyMedium)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ACCENT_OPTIONS.forEach { (hex, name) ->
                            val c = parseColor(hex)
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(c)
                                    .clickable { viewModel.setUserAccent(hex); showColorPicker = false }
                                    .then(if (accentHex == hex) Modifier.border(3.dp, textPri, CircleShape) else Modifier))
                                Text(name, color = textDis, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorPicker = false }) { Text("Done", color = accentColor) } }
        )
    }
}

@Composable
private fun MenuRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    textPri: Color, textSec: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }
        .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = textSec, modifier = Modifier.size(20.dp))
        Text(label, color = textPri, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PrefToggleRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean, accentColor: Color, textPri: Color, isDark: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }
        .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            Text(label, color = textPri, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedTrackColor = accentColor,
                checkedThumbColor = if (isDark) NavyBackground else LightCard))
    }
}

@Composable
private fun BigActionButton(title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color, isDark: Boolean, onClick: () -> Unit) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val textSec = if (isDark) TextSecondary else LightTextSecondary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    Box(modifier = Modifier.fillMaxWidth().height(90.dp)
        .clip(RoundedCornerShape(16.dp)).background(cardBg).clickable { onClick() }) {
        Box(modifier = Modifier.width(4.dp).fillMaxHeight()
            .background(color).align(Alignment.CenterStart))
        Row(modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = color, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, letterSpacing = 1.sp)
                Text(subtitle, color = textSec, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = textDis)
        }
    }
}
