package com.leapoffaith.app.ui.plank

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlankScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val plankToday by viewModel.plankToday.collectAsState()
    val isDark     by viewModel.isDarkTheme.collectAsState()
    val planked    = plankToday?.completed == true
    val todayLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    val bg      = if (isDark) NavyBackground else LightBackground
    val primary = if (isDark) Gold           else ForestGreen
    val textPri = if (isDark) TextPrimary    else LightTextPrimary
    val textSec = if (isDark) TextSecondary  else LightTextSecondary

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Plank", color = textPri) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                if (planked) {
                    Box(
                        modifier = Modifier.size(120.dp).clip(CircleShape)
                            .background(Brush.radialGradient(
                                listOf(primary.copy(alpha = 0.3f),
                                    if (isDark) NavyCard else LightCard)
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = primary, modifier = Modifier.size(64.dp))
                    }
                    Text("Planked ✓", fontSize = 32.sp,
                        fontWeight = FontWeight.Bold, color = primary)
                    Text(todayLabel, color = textSec,
                        style = MaterialTheme.typography.bodyLarge)
                    Text("You showed up. Keep the streak alive.",
                        color = textSec, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { viewModel.undoPlank() }) {
                        Icon(Icons.Default.Undo, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Undo")
                    }
                } else {
                    Icon(Icons.Default.FitnessCenter, null,
                        tint = if (isDark) TextDisabled else LightTextDisabled,
                        modifier = Modifier.size(80.dp))
                    Text("Did you plank today?", fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold, color = textPri,
                        textAlign = TextAlign.Center)
                    Text(todayLabel, color = textSec,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.logPlank() },
                        modifier = Modifier.scale(scale).fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = if (isDark) NavyBackground else LightCard
                        )
                    ) {
                        Text("YES", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
