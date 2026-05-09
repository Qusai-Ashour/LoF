package com.leapoffaith.app.data.dao

import androidx.room.*
import com.leapoffaith.app.data.entities.CustomEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomEntryDao {
    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND categoryId=:categoryId AND date=:date AND subItemKey=:subItemKey LIMIT 1")
    fun getEntry(userId: String, categoryId: Long, date: String, subItemKey: String = ""): Flow<CustomEntry?>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND categoryId=:categoryId AND date=:date AND subItemKey=:subItemKey LIMIT 1")
    suspend fun getEntryOnce(userId: String, categoryId: Long, date: String, subItemKey: String = ""): CustomEntry?

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND categoryId=:categoryId AND date=:date ORDER BY subItemKey ASC")
    fun getEntriesForCategoryDate(userId: String, categoryId: Long, date: String): Flow<List<CustomEntry>>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND date=:date")
    fun getEntriesForDate(userId: String, date: String): Flow<List<CustomEntry>>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND date=:date")
    suspend fun getEntriesForDateOnce(userId: String, date: String): List<CustomEntry>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND date>=:startDate AND date<=:endDate ORDER BY date ASC")
    fun getEntriesByDateRange(userId: String, startDate: String, endDate: String): Flow<List<CustomEntry>>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND date>=:startDate AND date<=:endDate ORDER BY date ASC")
    suspend fun getEntriesByDateRangeOnce(userId: String, startDate: String, endDate: String): List<CustomEntry>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND categoryId=:categoryId ORDER BY date DESC")
    fun getEntriesForCategory(userId: String, categoryId: Long): Flow<List<CustomEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CustomEntry): Long

    @Update
    suspend fun updateEntry(entry: CustomEntry)

    @Delete
    suspend fun deleteEntry(entry: CustomEntry)

    @Query("DELETE FROM custom_entries WHERE categoryId=:categoryId")
    suspend fun deleteEntriesForCategory(categoryId: Long)

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND categoryId=:categoryId AND notes!='' ORDER BY date DESC")
    fun getPrepareItems(userId: String, categoryId: Long): Flow<List<CustomEntry>>
}
