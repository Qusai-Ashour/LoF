package com.leapoffaith.app.data.dao

import androidx.room.*
import com.leapoffaith.app.data.entities.MealEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Query("SELECT * FROM meal_entries WHERE userId = :userId AND date = :date AND entryType = :entryType ORDER BY mealType ASC")
    fun getMealsByDateAndType(userId: String, date: String, entryType: String): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries WHERE userId = :userId AND date = :date AND entryType = :entryType ORDER BY mealType ASC")
    suspend fun getMealsByDateAndTypeOnce(userId: String, date: String, entryType: String): List<MealEntry>

    @Query("SELECT * FROM meal_entries WHERE userId = :userId AND weekStartDate = :weekStart AND entryType = 'WEEK_PLAN' ORDER BY dayOfWeek ASC, mealType ASC")
    fun getMealsByWeekStart(userId: String, weekStart: String): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries WHERE userId = :userId AND weekStartDate = :weekStart AND entryType = 'WEEK_PLAN' ORDER BY dayOfWeek ASC, mealType ASC")
    suspend fun getMealsByWeekStartOnce(userId: String, weekStart: String): List<MealEntry>

    @Query("SELECT * FROM meal_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getMealsByDateRange(userId: String, startDate: String, endDate: String): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getMealsByDateRangeOnce(userId: String, startDate: String, endDate: String): List<MealEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(mealEntry: MealEntry): Long

    @Update
    suspend fun updateMeal(mealEntry: MealEntry)

    @Delete
    suspend fun deleteMeal(mealEntry: MealEntry)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteMealById(id: Long)

    @Query("UPDATE meal_entries SET isCompleted = :completed WHERE id = :id")
    suspend fun setMealCompleted(id: Long, completed: Boolean)
}
