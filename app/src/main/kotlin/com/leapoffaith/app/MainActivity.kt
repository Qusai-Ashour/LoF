package com.leapoffaith.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.leapoffaith.app.data.AppDatabase
import com.leapoffaith.app.navigation.NavGraph
import com.leapoffaith.app.repository.AppRepository
import com.leapoffaith.app.ui.theme.LeapOfFaithTheme
import com.leapoffaith.app.viewmodel.AppViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Store route from widget intent
        val intentRoute = intent?.getStringExtra("route")
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(this)
        val repository = AppRepository(
            db.taskDao(), db.plankDao(), db.mealDao(), db.prayerDao(),
            db.categoryDao(), db.customEntryDao()
        )
        viewModel = ViewModelProvider(
            this, AppViewModel.Factory(repository, applicationContext)
        )[AppViewModel::class.java]

        lifecycleScope.launch {
            viewModel.widgetRefreshTrigger.collect { ts ->
                if (ts > 0L) refreshWidgets()
            }
        }

        setContent {
            val startRoute = intentRoute
            val isDark by viewModel.isDarkTheme.collectAsState()
            LeapOfFaithTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController, viewModel = viewModel, startRoute = startRoute)
                }
            }
        }
    }

    private fun refreshWidgets() {
        lifecycleScope.launch {
            try {
                val mgr = androidx.glance.appwidget.GlanceAppWidgetManager(this@MainActivity)
                mgr.getGlanceIds(com.leapoffaith.app.ui.widgets.SnapshotWidget::class.java)
                    .forEach { com.leapoffaith.app.ui.widgets.SnapshotWidget().update(this@MainActivity, it) }
                mgr.getGlanceIds(com.leapoffaith.app.ui.widgets.MiniTrackerWidget::class.java)
                    .forEach { com.leapoffaith.app.ui.widgets.MiniTrackerWidget().update(this@MainActivity, it) }
            } catch (_: Exception) {}
        }
    }
}
