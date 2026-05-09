package com.leapoffaith.app.navigation
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.*
import androidx.navigation.compose.*
import com.leapoffaith.app.ui.calendar.CalendarScreen
import com.leapoffaith.app.ui.history.HistoryScreen
import com.leapoffaith.app.ui.hub.MainHubScreen
import com.leapoffaith.app.ui.mealplan.WeekMealPlanScreen
import com.leapoffaith.app.ui.plank.PlankScreen
import com.leapoffaith.app.ui.prayers.PrayersScreen
import com.leapoffaith.app.ui.prepare.CustomPrepareScreen
import com.leapoffaith.app.ui.prepare.PrepareScreen
import com.leapoffaith.app.ui.record.RecordCustomCategoryScreen
import com.leapoffaith.app.ui.record.RecordProgressScreen
import com.leapoffaith.app.ui.today.TodayScreen
import com.leapoffaith.app.ui.tomorrow.TomorrowPlanScreen
import com.leapoffaith.app.ui.tracker.VisualTrackerScreen
import com.leapoffaith.app.ui.whoareyou.WhoAreYouScreen
import com.leapoffaith.app.ui.theme.*
import com.leapoffaith.app.viewmodel.AppViewModel

@Composable
fun NavGraph(navController: NavHostController, viewModel: AppViewModel) {
    val isLoaded    by viewModel.isLoaded.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDark      by viewModel.isDarkTheme.collectAsState()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (isDark)
                android.graphics.Color.parseColor("#0D1B2A")
            else
                android.graphics.Color.parseColor("#1A5C38")
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    if (!isLoaded) {
        Box(modifier = Modifier.fillMaxSize().background(if (isDark) NavyBackground else LightBackground),
            contentAlignment = Alignment.Center) {
            Text("LoF", color = if (isDark) Gold else ForestGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        return
    }

    NavHost(navController = navController,
        startDestination = if (currentUser.isEmpty()) NavRoutes.WHO_ARE_YOU else NavRoutes.HOME) {
        composable(NavRoutes.WHO_ARE_YOU) {
            WhoAreYouScreen(viewModel = viewModel, onUserSelected = { uid ->
                viewModel.selectUser(uid)
                navController.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.WHO_ARE_YOU) { inclusive = true } }
            })
        }
        composable(NavRoutes.HOME)           { MainHubScreen(viewModel, navController) }
        composable(NavRoutes.VISUAL_TRACKER) { VisualTrackerScreen(viewModel) { navController.popBackStack() } }
        composable(NavRoutes.PREPARE)        { PrepareScreen(viewModel, navController) { navController.popBackStack() } }
        composable(NavRoutes.RECORD)         { RecordProgressScreen(viewModel, navController) { navController.popBackStack() } }
        composable(NavRoutes.PLANK)          { PlankScreen(viewModel) { navController.popBackStack() } }
        composable(NavRoutes.PRAYERS)        { PrayersScreen(viewModel) { navController.popBackStack() } }
        composable(NavRoutes.TODAY_TASKS)    { TodayScreen(viewModel) { navController.popBackStack() } }
        composable(NavRoutes.HISTORY)        { HistoryScreen(viewModel, editMode = false) { navController.popBackStack() } }
        composable(NavRoutes.HISTORY_EDIT)   { HistoryScreen(viewModel, editMode = true) { navController.popBackStack() } }
        composable(NavRoutes.CALENDAR)       { CalendarScreen(viewModel) { navController.popBackStack() } }
        composable(NavRoutes.TOMORROW_PLAN)  { TomorrowPlanScreen(viewModel) { navController.popBackStack() } }
        composable(NavRoutes.WEEK_MEAL_PLAN) { WeekMealPlanScreen(viewModel) { navController.popBackStack() } }
        composable(NavRoutes.RECORD_CUSTOM,
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
        ) { bs -> RecordCustomCategoryScreen(viewModel, bs.arguments?.getLong("categoryId") ?: 0L) { navController.popBackStack() } }
        composable(NavRoutes.PREPARE_CUSTOM,
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
        ) { bs -> CustomPrepareScreen(viewModel, bs.arguments?.getLong("categoryId") ?: 0L) { navController.popBackStack() } }
    }
}
