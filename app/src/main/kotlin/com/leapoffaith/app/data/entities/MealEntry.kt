package com.leapoffaith.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "qusai",
    val date: String,            // "yyyy-MM-dd"
    val weekStartDate: String,   // Monday of that week
    val dayOfWeek: Int = -1,     // -1 for TODAY_GOAL entries
    val description: String,
    val mealType: String,        // Breakfast / Lunch / Dinner / Snack
    val isCompleted: Boolean = false,
    val entryType: String        // "TODAY_GOAL" or "WEEK_PLAN"
)
