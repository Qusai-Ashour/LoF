package com.leapoffaith.app.repository

import com.leapoffaith.app.data.dao.*
import com.leapoffaith.app.data.entities.*
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val taskDao: TaskDao,
    private val plankDao: PlankDao,
    private val mealDao: MealDao,
    private val prayerDao: PrayerDao,
    private val categoryDao: CategoryDao,
    private val customEntryDao: CustomEntryDao
) {
    // Tasks
    fun getTasksByDate(uid: String, date: String)                     = taskDao.getTasksByDate(uid, date)
    fun getTasksByDateRange(uid: String, s: String, e: String)        = taskDao.getTasksByDateRange(uid, s, e)
    suspend fun insertTask(t: Task)                                   = taskDao.insertTask(t)
    suspend fun updateTask(t: Task)                                   = taskDao.updateTask(t)
    suspend fun deleteTask(t: Task)                                   = taskDao.deleteTask(t)
    suspend fun deleteTaskById(id: Long)                              = taskDao.deleteTaskById(id)
    suspend fun setTaskCompleted(id: Long, c: Boolean)               = taskDao.setTaskCompleted(id, c)

    // Plank
    fun getPlankByDate(uid: String, date: String)                     = plankDao.getPlankByDate(uid, date)
    suspend fun getPlankByDateOnce(uid: String, date: String)         = plankDao.getPlankByDateOnce(uid, date)
    fun getPlanksByDateRange(uid: String, s: String, e: String)       = plankDao.getPlanksByDateRange(uid, s, e)
    suspend fun getPlanksByDateRangeOnce(uid: String, s: String, e: String) = plankDao.getPlanksByDateRangeOnce(uid, s, e)
    suspend fun insertPlank(p: PlankEntry)                            = plankDao.insertPlank(p)
    suspend fun updatePlank(p: PlankEntry)                            = plankDao.updatePlank(p)
    suspend fun deletePlankByDate(uid: String, date: String)          = plankDao.deletePlankByDate(uid, date)

    // Meals
    fun getMealsByDateAndType(uid: String, date: String, t: String)   = mealDao.getMealsByDateAndType(uid, date, t)
    fun getMealsByWeekStart(uid: String, ws: String)                  = mealDao.getMealsByWeekStart(uid, ws)
    suspend fun getMealsByWeekStartOnce(uid: String, ws: String)      = mealDao.getMealsByWeekStartOnce(uid, ws)
    fun getMealsByDateRange(uid: String, s: String, e: String)        = mealDao.getMealsByDateRange(uid, s, e)
    suspend fun insertMeal(m: MealEntry)                              = mealDao.insertMeal(m)
    suspend fun setMealCompleted(id: Long, c: Boolean)               = mealDao.setMealCompleted(id, c)
    suspend fun deleteMealById(id: Long)                              = mealDao.deleteMealById(id)

    // Prayers
    fun getPrayerByDate(uid: String, date: String)                    = prayerDao.getPrayerByDate(uid, date)
    suspend fun getPrayerByDateOnce(uid: String, date: String)        = prayerDao.getPrayerByDateOnce(uid, date)
    fun getPrayersByDateRange(uid: String, s: String, e: String)      = prayerDao.getPrayersByDateRange(uid, s, e)
    suspend fun getPrayersByDateRangeOnce(uid: String, s: String, e: String) = prayerDao.getPrayersByDateRangeOnce(uid, s, e)
    suspend fun insertPrayer(p: PrayerEntry)                          = prayerDao.insertPrayer(p)
    suspend fun updatePrayer(p: PrayerEntry)                          = prayerDao.updatePrayer(p)

    // Categories
    fun getCategoriesByType(uid: String, type: String)                = categoryDao.getCategoriesByType(uid, type)
    fun getAllCategories(uid: String)                                  = categoryDao.getAllCategories(uid)
    suspend fun getAllCategoriesOnce(uid: String)                      = categoryDao.getAllCategoriesOnce(uid)
    suspend fun getCategoryById(id: Long)                             = categoryDao.getCategoryById(id)
    suspend fun insertCategory(c: CategoryDefinition)                 = categoryDao.insertCategory(c)
    suspend fun updateCategory(c: CategoryDefinition)                 = categoryDao.updateCategory(c)
    suspend fun deleteCategoryById(id: Long)                          = categoryDao.deleteCategoryById(id)

    // Custom Entries
    fun getCustomEntryFlow(uid: String, catId: Long, date: String, sub: String = "") =
        customEntryDao.getEntry(uid, catId, date, sub)

    suspend fun getCustomEntry(uid: String, catId: Long, date: String, sub: String = "") =
        customEntryDao.getEntryOnce(uid, catId, date, sub)
    suspend fun getCustomEntryOnce(uid: String, catId: Long, date: String, sub: String = "") =
        customEntryDao.getEntryOnce(uid, catId, date, sub)
    fun getPrepareItems(userId: String, catId: Long) =
        customEntryDao.getEntriesForCategory(catId)

    fun getCustomEntriesForCategory(uid: String, catId: Long, date: String) =
        customEntryDao.getEntriesForCategoryDate(uid, catId, date)

    fun getCustomEntriesForCategoryOnce(uid: String, catId: Long, date: String) =
        customEntryDao.getEntriesForCategoryDate(uid, catId, date)
    suspend fun customEntryDao_getByDateOnce(uid: String, catId: Long, date: String) =
        customEntryDao.getEntriesForCategoryDate(uid, catId, date).let {
            customEntryDao.getEntriesForDateOnce(uid, date).filter { e -> e.categoryId == catId }
        }

    suspend fun getCustomEntriesForDateOnce(uid: String, date: String) =
        customEntryDao.getEntriesForDateOnce(uid, date)
    suspend fun deleteUndoneEntriesBefore(uid: String, categoryIds: List<Long>, date: String) =
        customEntryDao.deleteUndoneEntriesBefore(uid, categoryIds, date)

    fun getBuriedEntries(uid: String) = customEntryDao.getBuriedEntries(uid)

    fun getCustomEntriesForDate(uid: String, date: String) =
        customEntryDao.getEntriesForDate(uid, date)

    fun getCustomEntriesByDateRange(uid: String, s: String, e: String) =
        customEntryDao.getEntriesByDateRange(uid, s, e)
    suspend fun getCustomEntriesByDateRangeOnce(uid: String, s: String, e: String) =
        customEntryDao.getEntriesByDateRangeOnce(uid, s, e)
    fun getAllEntriesForCategory(uid: String, catId: Long) =
        customEntryDao.getEntriesForCategory(catId)
    suspend fun insertCustomEntry(e: CustomEntry)                     = customEntryDao.insertEntry(e)
    suspend fun updateCustomEntry(e: CustomEntry)                     = customEntryDao.updateEntry(e)
    suspend fun deleteCustomEntry(e: CustomEntry)                     = customEntryDao.deleteEntry(e)
    suspend fun deleteEntriesForCategory(catId: Long)                 = customEntryDao.deleteAllForCategory(catId)
}
