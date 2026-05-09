package com.leapoffaith.app.ui.prayers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Prayer(val key: String, val arabicName: String, val time: String)

val PRAYERS = listOf(
    Prayer("fajr",    "الفجر",  "Fajr"),
    Prayer("dhuhr",   "الظهر",  "Dhuhr"),
    Prayer("asr",     "العصر",  "Asr"),
    Prayer("maghrib", "المغرب", "Maghrib"),
    Prayer("isha",    "العشاء", "Isha")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayersScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val prayerToday by viewModel.prayerToday.collectAsState()
    val isDark      by viewModel.isDarkTheme.collectAsState()
    val todayLabel  = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    val bg      = if (isDark) NavyBackground  else LightBackground
    val cardBg  = if (isDark) NavyCard        else LightCard
    val primary = if (isDark) Gold            else ForestGreen
    val textPri = if (isDark) TextPrimary     else LightTextPrimary
    val textSec = if (isDark) TextSecondary   else LightTextSecondary
    val doneClr = if (isDark) GreenDone       else LightGreenDone

    val done = prayerToday?.let {
        listOf(it.fajr, it.dhuhr, it.asr, it.maghrib, it.isha).count { p -> p }
    } ?: 0

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Daily Prayers", color = textPri)
                        Text(todayLabel, color = textSec,
                            style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                // Progress bar
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("☽  $done / 5 prayers", color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (done == 5)
                            Text("All done ✓", color = doneClr,
                                fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            items(PRAYERS) { prayer ->
                val isDone = when (prayer.key) {
                    "fajr"    -> prayerToday?.fajr    == true
                    "dhuhr"   -> prayerToday?.dhuhr   == true
                    "asr"     -> prayerToday?.asr     == true
                    "maghrib" -> prayerToday?.maghrib == true
                    "isha"    -> prayerToday?.isha    == true
                    else -> false
                }
                PrayerRow(
                    prayer = prayer,
                    isDone = isDone,
                    isDark = isDark,
                    onToggle = { viewModel.togglePrayer(prayer.key) }
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(
    prayer: Prayer,
    isDone: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit
) {
    val cardBg  = if (isDark) NavyCard      else LightCard
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textDis = if (isDark) TextDisabled  else LightTextDisabled
    val doneClr = if (isDark) GreenDone     else LightGreenDone

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDone) doneClr.copy(alpha = 0.12f) else cardBg)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(if (isDone) doneClr.copy(alpha = 0.2f)
                                else if (isDark) NavyCardLight else LightCardVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isDone)
                    Icon(Icons.Default.Check, null, tint = doneClr,
                        modifier = Modifier.size(20.dp))
                else
                    Text("☽", fontSize = 18.sp)
            }
            Column {
                Text(prayer.time, color = if (isDone) doneClr else textPri,
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(prayer.arabicName, color = textDis,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(
            text = if (isDone) "Prayed ✓" else "Tap to mark",
            color = if (isDone) doneClr else textDis,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
