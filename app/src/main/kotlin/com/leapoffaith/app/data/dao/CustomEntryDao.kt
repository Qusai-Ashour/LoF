package com.leapoffaith.app.data.dao
import androidx.room.*
import com.leapoffaith.app.data.entities.CustomEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomEntryDao {
    @Query("SELECT * FROM custom_entries WHERE categoryId=:categoryId ORDER BY date DESC")
    fun getEntriesForCategory(categoryId: Long): Flow<List<CustomEntry>>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND categoryId=:categoryId AND date=:date")
    fun getEntriesForCategoryDate(userId: String, categoryId: Long, date: String): Flow<List<CustomEntry>>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND date=:date")
    fun getEntriesForDate(userId: String, date: String): Flow<List<CustomEntry>>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND date=:date")
    suspend fun getEntriesForDateOnce(userId: String, date: String): List<CustomEntry>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND categoryId=:categoryId AND date=:date AND subItemKey=:subItemKey LIMIT 1")
    fun getEntry(userId: String, categoryId: Long, date: String, subItemKey: String = ""): Flow<CustomEntry?>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND categoryId=:categoryId AND date=:date AND subItemKey=:subItemKey LIMIT 1")
    suspend fun getEntryOnce(userId: String, categoryId: Long, date: String, subItemKey: String = ""): CustomEntry?

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND date>=:startDate AND date<=:endDate ORDER BY date ASC")
    fun getEntriesByDateRange(userId: String, startDate: String, endDate: String): Flow<List<CustomEntry>>

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND date>=:startDate AND date<=:endDate ORDER BY date ASC")
    suspend fun getEntriesByDateRangeOnce(userId: String, startDate: String, endDate: String): List<CustomEntry>

    @Query("DELETE FROM custom_entries WHERE userId=:userId AND categoryId IN (:categoryIds) AND date<=:date AND isDone=0")
    suspend fun deleteUndoneEntriesBefore(userId: String, categoryIds: List<Long>, date: String)

    @Query("DELETE FROM custom_entries WHERE categoryId=:catId AND subItemKey=:key AND isDone=0 AND date>=:fromDate")
    suspend fun deleteUndoneSubItemFrom(catId: Long, key: String, fromDate: String)

    @Query("SELECT * FROM custom_entries WHERE userId=:userId AND isDone=1 AND categoryId NOT IN (SELECT id FROM categories WHERE userId=:userId) ORDER BY date DESC")
    fun getBuriedEntries(userId: String): Flow<List<CustomEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CustomEntry): Long

    @Update
    suspend fun updateEntry(entry: CustomEntry)

    @Delete
    suspend fun deleteEntry(entry: CustomEntry)

    @Query("DELETE FROM custom_entries WHERE id=:id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM custom_entries WHERE categoryId=:categoryId")
    suspend fun deleteAllForCategory(categoryId: Long)
}
