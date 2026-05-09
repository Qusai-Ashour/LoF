package com.leapoffaith.app.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leapoffaith.app.data.entities.*
import com.leapoffaith.app.repository.AppRepository
import kotlinx.coroutines.flow.*
import androidx.glance.appwidget.updateAll
import com.leapoffaith.app.ui.widgets.MiniTrackerWidget
import com.leapoffaith.app.ui.widgets.SnapshotWidget
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

val Context.dataStore by preferencesDataStore(name = "lof_prefs")
val USER_KEY        = stringPreferencesKey("selected_user")
val WIDGET_USER_KEY = stringPreferencesKey("widget_user")
val DARK_THEME_KEY  = booleanPreferencesKey("dark_theme")

data class DayTrackerData(
    val taskTotal: Int = 0,
    val taskDone: Int = 0,
    val plankDone: Boolean = false,
    val prayersDone: Int = 0,
    val prayersTotal: Int = 5,
    val customEntries: Map<Long, Boolean> = emptyMap()
)

class AppViewModel(
    internal val repository: AppRepository,
    internal val context: Context
) : ViewModel() {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val WIDGET_PREFS = "lof_widget"

    // ── Loading state ─────────────────────────────────────────────────────────
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    // ── User ──────────────────────────────────────────────────────────────────
    private val _currentUser = MutableStateFlow("")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    // ── Theme ─────────────────────────────────────────────────────────────────
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // ── User accent color
    private val _userAccentHex = MutableStateFlow("#D4A843")
    val userAccentHex: StateFlow<String> = _userAccentHex.asStateFlow()
    fun getAccentForUser(uid: String): String =
        context.getSharedPreferences("lof_accents", android.content.Context.MODE_PRIVATE)
            .getString(uid, if (uid == "lina") "#60A5FA" else "#D4A843") ?: "#D4A843"
    fun setUserAccent(hex: String) {
        context.getSharedPreferences("lof_accents", android.content.Context.MODE_PRIVATE)
            .edit().putString(_currentUser.value, hex).apply()
        _userAccentHex.value = hex
    }

    // ── Widget user ───────────────────────────────────────────────────────────
    private val _widgetUser = MutableStateFlow("qusai")
    val widgetUser: StateFlow<String> = _widgetUser.asStateFlow()

    init {
        viewModelScope.launch {
            // Wait for first DataStore emission before marking loaded
            val prefs = context.dataStore.data.first()
            _currentUser.value  = prefs[USER_KEY]        ?: ""
            _isDarkTheme.value  = prefs[DARK_THEME_KEY]  ?: true
            _widgetUser.value   = prefs[WIDGET_USER_KEY] ?: "qusai"
            _userAccentHex.value = getAccentForUser(_currentUser.value)
            _isLoaded.value = true
            // Continue collecting updates
            context.dataStore.data.collect { p ->
                _currentUser.value  = p[USER_KEY]        ?: ""
                _isDarkTheme.value  = p[DARK_THEME_KEY]  ?: true
                _widgetUser.value   = p[WIDGET_USER_KEY] ?: "qusai"
            }
        }
        viewModelScope.launch {
            _currentUser.collect { uid -> if (uid.isNotEmpty()) seedDefaultCategories(uid) }
        }
    }

    fun selectUser(userId: String) = viewModelScope.launch {
        context.dataStore.edit { it[USER_KEY] = userId }
        _userAccentHex.value = getAccentForUser(userId)
        triggerWidgetUpdate()
    }

    fun toggleTheme() = viewModelScope.launch {
        val newVal = !_isDarkTheme.value
        context.dataStore.edit { it[DARK_THEME_KEY] = newVal }
        context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("is_dark_theme", newVal).apply()
        triggerWidgetUpdate()
    }

    fun setWidgetUser(userId: String) = viewModelScope.launch {
        context.dataStore.edit { it[WIDGET_USER_KEY] = userId }
        context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
            .edit().putString("widget_user", userId).apply()
        triggerWidgetUpdate()
    }

    fun getWidgetUserBlocking(): String =
        context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
            .getString("widget_user", "qusai") ?: "qusai"

    // ── Dates ─────────────────────────────────────────────────────────────────
    fun today()    = LocalDate.now().format(fmt)
    fun tomorrow() = LocalDate.now().plusDays(1).format(fmt)
    fun mealWeekStart(d: LocalDate = LocalDate.now()) =
        d.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)).format(fmt)

    fun weekStart(d: LocalDate = LocalDate.now()) =
        d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(fmt)

    // ── Categories ────────────────────────────────────────────────────────────
    val prepareCategories: StateFlow<List<CategoryDefinition>> = _currentUser
        .flatMapLatest { uid -> if (uid.isEmpty()) flowOf(emptyList()) else repository.getCategoriesByType(uid, "PREPARE") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordCategories: StateFlow<List<CategoryDefinition>> = _currentUser
        .flatMapLatest { uid -> if (uid.isEmpty()) flowOf(emptyList()) else repository.getCategoriesByType(uid, "RECORD") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategory(
        name: String, emoji: String, type: String, color: String,
        frequency: String = "ONCE_DAILY", subItems: String = "",
        prepareFrequency: String = "", showInWidget: Boolean = false, isInMiniTracker: Boolean = false
    ) = viewModelScope.launch {
        val existing = repository.getAllCategoriesOnce(_currentUser.value)
        val nextOrder = existing.filter { it.type == type }.size
        if (isInMiniTracker) {
            repository.getAllCategoriesOnce(_currentUser.value).forEach { c ->
                if (c.isInMiniTracker) repository.updateCategory(c.copy(isInMiniTracker = false))
            }
        }
        if (showInWidget) {
            val widgetCount = repository.getAllCategoriesOnce(_currentUser.value).count { it.showInWidget }
            if (widgetCount >= 8) return@launch  // max 8 in snapshot widget
        }
        repository.insertCategory(CategoryDefinition(
            userId = _currentUser.value, name = name, emoji = emoji,
            type = type, color = color, frequency = frequency,
            subItems = subItems, prepareFrequency = prepareFrequency,
            showInWidget = showInWidget, isInMiniTracker = isInMiniTracker,
            orderIndex = nextOrder
        ))
    }

    suspend fun findPrepareCategoryByName(name: String) =
        repository.getAllCategoriesOnce(_currentUser.value)
            .firstOrNull { it.type == "PREPARE" && it.name == name }

    fun updateCategoryDirect(cat: com.leapoffaith.app.data.entities.CategoryDefinition) = viewModelScope.launch {
        if (cat.isInMiniTracker) {
            repository.getAllCategoriesOnce(cat.userId).forEach { c ->
                if (c.id != cat.id && c.isInMiniTracker)
                    repository.updateCategory(c.copy(isInMiniTracker = false))
            }
            context.getSharedPreferences("lof_widget", android.content.Context.MODE_PRIVATE)
                .edit().putLong("mini_tracker_cat_id", cat.id).apply()
        }
        repository.updateCategory(cat)
        // sync mini tracker pref
        if (cat.isInMiniTracker) {
            context.getSharedPreferences("lof_widget", android.content.Context.MODE_PRIVATE)
                .edit().putLong("mini_tracker_cat_id", cat.id).apply()
        }
    }

    suspend fun findRecordCategoryByName(name: String) =
        repository.getAllCategoriesOnce(_currentUser.value)
            .firstOrNull { it.type == "RECORD" && it.name == name }

    fun addCalendarEntry(category: com.leapoffaith.app.data.entities.CategoryDefinition, date: String, text: String) = viewModelScope.launch {
        when (category.builtinType) {
            "TASKS" -> repository.insertTask(
                com.leapoffaith.app.data.entities.Task(
                    userId = _currentUser.value, title = text, date = date, hourOfDay = -1))
            else -> {
                // Store under the PREPARE category so RecordCustomCategoryScreen can find it
                val prepareCat = findPrepareCategoryByName(category.name)
                val targetId = prepareCat?.id ?: category.id
                repository.insertCustomEntry(
                    com.leapoffaith.app.data.entities.CustomEntry(
                        userId = _currentUser.value, categoryId = targetId,
                        date = date, notes = text, isDone = false))
            }
        }
        triggerWidgetUpdate()
    }

    fun deleteCategory(cat: com.leapoffaith.app.data.entities.CategoryDefinition) = viewModelScope.launch {
        val uid = _currentUser.value
        val all = repository.getAllCategoriesOnce(uid)
        // Delete the linked counterpart (PREPARE deletes RECORD partner and vice versa)
        val partner = if (cat.type == "PREPARE")
            all.find { it.type == "RECORD" && it.name == cat.name }
        else
            all.find { it.type == "PREPARE" && it.name == cat.name }
        partner?.let { repository.deleteCategoryById(it.id) }
        repository.deleteCategoryById(cat.id)
        // data entries preserved for history
    }

    private suspend fun seedDefaultCategories(userId: String) {
        val existing = repository.getAllCategoriesOnce(userId)
        if (existing.isNotEmpty()) return
        if (true) {
            repository.insertCategory(CategoryDefinition(userId=userId, name="Tomorrow's Tasks", emoji="💡", type="PREPARE", builtinType="TOMORROW_TASKS", color="#D4A843", orderIndex=0))
            repository.insertCategory(CategoryDefinition(userId=userId, name="Week Meal Plan",    emoji="🍽",  type="PREPARE", builtinType="WEEK_MEALS",     color="#34D399", orderIndex=1, prepareFrequency="NEXT_WEEK"))
            repository.insertCategory(CategoryDefinition(userId=userId, name="Today's Tasks",   emoji="✅",  type="RECORD",  builtinType="TASKS",           color="#22C55E", orderIndex=0))
            repository.insertCategory(CategoryDefinition(userId=userId, name="Plank",             emoji="💪",  type="RECORD",  builtinType="PLANK",           color="#D4A843", orderIndex=1))
            repository.insertCategory(CategoryDefinition(userId=userId, name="Daily Prayers",     emoji="☽",  type="RECORD",  builtinType="PRAYERS",         color="#818CF8", orderIndex=2))
        }
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────
    val todayTasks: StateFlow<List<Task>> = _currentUser
        .flatMapLatest { uid -> if (uid.isEmpty()) flowOf(emptyList()) else repository.getTasksByDate(uid, today()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tomorrowTasks: StateFlow<List<Task>> = _currentUser
        .flatMapLatest { uid -> if (uid.isEmpty()) flowOf(emptyList()) else repository.getTasksByDate(uid, tomorrow()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTomorrowTask(title: String, hourOfDay: Int = -1) = viewModelScope.launch {
        repository.insertTask(Task(userId=_currentUser.value, title=title, date=tomorrow(), hourOfDay=hourOfDay))
    }

    fun toggleTask(task: Task) = viewModelScope.launch { launch { kotlinx.coroutines.delay(500); triggerWidgetUpdate() };
        repository.setTaskCompleted(task.id, !task.isCompleted)
        }

    fun deleteTask(task: Task) = viewModelScope.launch { repository.deleteTask(task) }

    // ── Meals ─────────────────────────────────────────────────────────────────
    val weekMeals: StateFlow<List<MealEntry>> = _currentUser
        .flatMapLatest { uid -> if (uid.isEmpty()) flowOf(emptyList()) else repository.getMealsByWeekStart(uid, mealWeekStart()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWeekMeal(dayIndex: Int, description: String, mealType: String) = viewModelScope.launch {
        val fridayStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
        val mealDate = fridayStart.plusDays(dayIndex.toLong())
        repository.insertMeal(MealEntry(userId=_currentUser.value, date=mealDate.format(fmt),
            weekStartDate=mealWeekStart(), dayOfWeek=mealDate.dayOfWeek.value,
            description=description, mealType=mealType, entryType="WEEK_PLAN"))
        _widgetRefreshTrigger.value = System.currentTimeMillis()
    }

    fun deleteWeekMeal(id: Long) = viewModelScope.launch { repository.deleteMealById(id) }

    // ── Plank ─────────────────────────────────────────────────────────────────
    val todayCustomEntries: StateFlow<List<com.leapoffaith.app.data.entities.CustomEntry>> = _currentUser
        .flatMapLatest { uid ->
            if (uid.isEmpty()) flowOf(emptyList())
            else repository.getCustomEntriesForDate(uid, today())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plankToday: StateFlow<PlankEntry?> = _currentUser
        .flatMapLatest { uid -> if (uid.isEmpty()) flowOf(null) else repository.getPlankByDate(uid, today()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logPlank() = viewModelScope.launch { launch { kotlinx.coroutines.delay(500); triggerWidgetUpdate() };
        val existing = repository.getPlankByDateOnce(_currentUser.value, today())
        if (existing == null) repository.insertPlank(PlankEntry(userId=_currentUser.value, date=today(), completed=true))
        else repository.updatePlank(existing.copy(completed=true))
        _widgetRefreshTrigger.value = System.currentTimeMillis()
    }

    fun undoPlank() = viewModelScope.launch { launch { kotlinx.coroutines.delay(500); triggerWidgetUpdate() };
        repository.deletePlankByDate(_currentUser.value, today())
        _widgetRefreshTrigger.value = System.currentTimeMillis()
    }

    // ── Prayers ───────────────────────────────────────────────────────────────
    val prayerToday: StateFlow<PrayerEntry?> = _currentUser
        .flatMapLatest { uid -> if (uid.isEmpty()) flowOf(null) else repository.getPrayerByDate(uid, today()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun togglePrayer(name: String) = viewModelScope.launch {
        val e = repository.getPrayerByDateOnce(_currentUser.value, today())
            ?: PrayerEntry(userId=_currentUser.value, date=today())
        val u = when (name) {
            "fajr"    -> e.copy(fajr    = !e.fajr)
            "dhuhr"   -> e.copy(dhuhr   = !e.dhuhr)
            "asr"     -> e.copy(asr     = !e.asr)
            "maghrib" -> e.copy(maghrib = !e.maghrib)
            "isha"    -> e.copy(isha    = !e.isha)
            else -> e
        }
        if (e.id == 0L) repository.insertPrayer(u) else repository.updatePrayer(u)
        launch { kotlinx.coroutines.delay(500); triggerWidgetUpdate() }
    }

    // ── Custom entries ────────────────────────────────────────────────────────
    fun getCustomEntryFlow(categoryId: Long, subKey: String = "") =
        repository.getCustomEntry(_currentUser.value, categoryId, today(), subKey)

    fun getCustomEntriesForCategoryDate(categoryId: Long) =
        repository.getCustomEntriesForCategory(_currentUser.value, categoryId, today())

    fun toggleCustomEntry(categoryId: Long, subKey: String = "") = viewModelScope.launch { launch { kotlinx.coroutines.delay(500); triggerWidgetUpdate() };
        val existing = repository.getCustomEntryOnce(_currentUser.value, categoryId, today(), subKey)
        if (existing == null) {
            repository.insertCustomEntry(CustomEntry(userId=_currentUser.value, categoryId=categoryId,
                date=today(), isDone=true, subItemKey=subKey))
        } else {
            repository.updateCustomEntry(existing.copy(isDone = !existing.isDone))
        }
    }

    fun addPrepareItemWithTime(categoryId: Long, date: String, notes: String, hourOfDay: Int = -1) = viewModelScope.launch {
        repository.insertCustomEntry(com.leapoffaith.app.data.entities.CustomEntry(
            userId=_currentUser.value, categoryId=categoryId, date=date, notes=notes, hourOfDay=hourOfDay))
    }
    fun addPrepareItem(categoryId: Long, date: String, notes: String) = viewModelScope.launch {
        repository.insertCustomEntry(CustomEntry(userId=_currentUser.value, categoryId=categoryId,
            date=date, notes=notes, isDone=false))
    }

    fun togglePrepareItem(entry: CustomEntry) = viewModelScope.launch {
        repository.updateCustomEntry(entry.copy(isDone = !entry.isDone))
    }

    fun deletePrepareItem(entry: CustomEntry) = viewModelScope.launch {
        repository.deleteCustomEntry(entry)
    }

    fun getPrepareItems(categoryId: Long) =
        repository.getPrepareItems(_currentUser.value, categoryId)

    // ── Tracker ───────────────────────────────────────────────────────────────
    private val _trackerMonth = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val trackerMonth: StateFlow<LocalDate> = _trackerMonth.asStateFlow()
    fun prevMonth() { _trackerMonth.value = _trackerMonth.value.minusMonths(1) }
    fun nextMonth() { _trackerMonth.value = _trackerMonth.value.plusMonths(1) }

    val trackerData: StateFlow<Map<String, DayTrackerData>> =
        combine(_trackerMonth, _currentUser) { m, u -> Pair(m, u) }
            .flatMapLatest { (month, uid) ->
                if (uid.isEmpty()) flowOf(emptyMap())
                else {
                    val start = month.format(fmt)
                    val end   = month.with(TemporalAdjusters.lastDayOfMonth()).format(fmt)
                    combine(
                        repository.getTasksByDateRange(uid, start, end),
                        repository.getPlanksByDateRange(uid, start, end),
                        repository.getPrayersByDateRange(uid, start, end),
                        repository.getCustomEntriesByDateRange(uid, start, end)
                    ) { tasks, planks, prayers, customs -> buildTrackerMap(tasks, planks, prayers, customs, kotlinx.coroutines.runBlocking { repository.getAllCategoriesOnce(_currentUser.value) }) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun buildTrackerMap(tasks: List<Task>, planks: List<PlankEntry>,
                                 prayers: List<PrayerEntry>, customs: List<CustomEntry>,
                                 allCats: List<com.leapoffaith.app.data.entities.CategoryDefinition> = emptyList()): Map<String, DayTrackerData> {
        val result = mutableMapOf<String, DayTrackerData>()
        val tasksByDate   = tasks.groupBy { it.date }
        val plankByDate   = planks.associateBy { it.date }
        val prayerByDate  = prayers.associateBy { it.date }
        val customsByDate = customs.groupBy { it.date }
        val allDates = (tasksByDate.keys + plankByDate.keys + prayerByDate.keys + customsByDate.keys).toSet()
        for (date in allDates) {
            val dayTasks   = tasksByDate[date]   ?: emptyList()
            val prayer     = prayerByDate[date]
            val dayCustoms = customsByDate[date] ?: emptyList()
            result[date] = DayTrackerData(
                taskTotal    = dayTasks.size,
                taskDone     = dayTasks.count { it.isCompleted },
                plankDone    = plankByDate[date]?.completed == true,
                prayersDone  = prayer?.let { listOf(it.fajr,it.dhuhr,it.asr,it.maghrib,it.isha).count { p->p } } ?: 0,
customEntries = run {
                    val base = dayCustoms.filter { it.subItemKey.isEmpty() }.associate { it.categoryId to it.isDone }
                    val expanded = base.toMutableMap()
                    base.forEach { (catId, isDone) ->
                        val prepCat = allCats.firstOrNull { it.id == catId && it.type == "PREPARE" }
                        if (prepCat != null) {
                            val recCat = allCats.firstOrNull { it.type == "RECORD" && it.name == prepCat.name }
                            if (recCat != null) expanded[recCat.id] = isDone
                        }
                    }
                    expanded
                }
            )
        }
        return result
    }

    suspend fun computePlankStreak(): Int {
        var streak = 0
        var d = LocalDate.now()
        if (repository.getPlankByDateOnce(_currentUser.value, d.format(fmt))?.completed != true) return 0
        while (true) {
            val e = repository.getPlankByDateOnce(_currentUser.value, d.format(fmt))
            if (e?.completed == true) { streak++; d = d.minusDays(1) } else break
        }
        return streak
    }

    // ── History helpers ───────────────────────────────────────────────────────
    fun repository_tasks_range(uid: String, s: String, e: String)   = repository.getTasksByDateRange(uid, s, e)
    fun repository_planks_range(uid: String, s: String, e: String)  = repository.getPlanksByDateRange(uid, s, e)
    fun repository_prayers_range(uid: String, s: String, e: String) = repository.getPrayersByDateRange(uid, s, e)
    fun repository_customs_range(uid: String, s: String, e: String) = repository.getCustomEntriesByDateRange(uid, s, e)
    fun repository_cats_record(uid: String)                         = repository.getCategoriesByType(uid, "RECORD")

    fun togglePlankEntry(entry: com.leapoffaith.app.data.entities.PlankEntry) = viewModelScope.launch { repository.updatePlank(entry.copy(completed = !entry.completed)) }
    fun togglePrayerForHistory(entry: com.leapoffaith.app.data.entities.PrayerEntry, name: String) = viewModelScope.launch {
        val u = when (name) { "fajr"->entry.copy(fajr=!entry.fajr); "dhuhr"->entry.copy(dhuhr=!entry.dhuhr); "asr"->entry.copy(asr=!entry.asr); "maghrib"->entry.copy(maghrib=!entry.maghrib); "isha"->entry.copy(isha=!entry.isha); else->entry }; if (entry.id==0L) repository.insertPrayer(u) else repository.updatePrayer(u) }
    fun toggleCustomEntryById(entry: com.leapoffaith.app.data.entities.CustomEntry) = viewModelScope.launch { repository.updateCustomEntry(entry.copy(isDone = !entry.isDone)) }

    fun triggerWidgetUpdate() {
        try {
            val mgr = android.appwidget.AppWidgetManager.getInstance(context)
            mapOf(
                "MiniTrackerWidgetReceiver" to com.leapoffaith.app.ui.widgets.MiniTrackerWidgetReceiver::class.java,
                "SnapshotWidgetReceiver"    to com.leapoffaith.app.ui.widgets.SnapshotWidgetReceiver::class.java
            ).forEach { (name, cls) ->
                val comp = android.content.ComponentName(context, cls)
                val ids  = mgr.getAppWidgetIds(comp)
                android.util.Log.d("LoF_Widget", "$name: found ${ids.size} widget(s)")
                if (ids.isNotEmpty()) {
                    context.sendBroadcast(
                        android.content.Intent(
                            android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        ).apply {
                            component = comp
                            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
                    )
                    // Force launcher to redraw by notifying options changed
                    ids.forEach { id ->
                        mgr.updateAppWidgetOptions(id, mgr.getAppWidgetOptions(id))
                    }
                }
            }
        // HONOR-specific widget refresh broadcast
            try {
                context.sendBroadcast(android.content.Intent("com.hihonor.android.action.WIDGET_CHANGE"))
                context.sendBroadcast(android.content.Intent("com.hihonor.suggestion.action.UPDATE_WIDGET"))
            } catch (_: Exception) {}
        } catch (e: Exception) {
            android.util.Log.e("LoF_Widget", "triggerWidgetUpdate failed: ${e.message}")
        }
        // Glance backup with stable scope
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                MiniTrackerWidget().updateAll(context.applicationContext)
                SnapshotWidget().updateAll(context.applicationContext)
                android.util.Log.d("LoF_Widget", "Glance updateAll succeeded")
            } catch (e: Exception) {
                android.util.Log.e("LoF_Widget", "Glance failed: ${e.message}")
            }
        }
    }

    // ── Widget refresh ────────────────────────────────────────────────────────
    private val _widgetRefreshTrigger = MutableStateFlow(0L)
    val widgetRefreshTrigger: StateFlow<Long> = _widgetRefreshTrigger.asStateFlow()

    class Factory(private val repo: AppRepository, private val ctx: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(m: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return AppViewModel(repo, ctx) as T
        }
    }
}
