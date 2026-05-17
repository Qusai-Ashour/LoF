package com.leapoffaith.app.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.leapoffaith.app.data.dao.*
import com.leapoffaith.app.data.entities.*

@Database(
    entities = [Task::class, PlankEntry::class, MealEntry::class,
                PrayerEntry::class, CategoryDefinition::class, CustomEntry::class],
    version = 7, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun plankDao(): PlankDao
    abstract fun mealDao(): MealDao
    abstract fun prayerDao(): PrayerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun customEntryDao(): CustomEntryDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(ctx: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "leap_of_faith_db")
                    .fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
