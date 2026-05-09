package com.leapoffaith.app.ui.whoareyou

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel

// Colors used only on the choose-user screen
private val QusaiBlue = Color(0xFF3B82F6)
private val LinaPink  = Color(0xFFEC4899)

@Composable
fun WhoAreYouScreen(viewModel: AppViewModel, onUserSelected: (String) -> Unit) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bg     = if (isDark) NavyBackground else LightBackground
    val cardBg = if (isDark) NavyCard       else LightCard
    val textPri = if (isDark) TextPrimary   else LightTextPrimary
    val textSec = if (isDark) TextSecondary else LightTextSecondary

    Box(modifier = Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)) {

            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LoF", color = if (isDark) Gold else ForestGreen,
                    fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text("Leap of Faith", color = textSec, style = MaterialTheme.typography.titleMedium)
                Text("Who are you?", color = textPri, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                UserCard(emoji = "🥑", name = "Qusai", color = QusaiBlue, isDark = isDark,
                    cardBg = cardBg, textPri = textPri) { onUserSelected("qusai") }
                UserCard(emoji = "🦋", name = "Lina", color = LinaPink, isDark = isDark,
                    cardBg = cardBg, textPri = textPri) { onUserSelected("lina") }
            }
        }
    }
}

@Composable
private fun UserCard(emoji: String, name: String, color: Color, isDark: Boolean,
    cardBg: Color, textPri: Color, onClick: () -> Unit) {
    Column(modifier = Modifier
        .width(130.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(cardBg)
        .clickable { onClick() }
        .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 36.sp)
        }
        Text(name, color = textPri, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
    }
}
