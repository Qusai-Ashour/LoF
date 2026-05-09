package com.leapoffaith.app.data.dao

import androidx.room.*
import com.leapoffaith.app.data.entities.PlankEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PlankDao {

    @Query("SELECT * FROM plank_entries WHERE userId = :userId AND date = :date LIMIT 1")
    fun getPlankByDate(userId: String, date: String): Flow<PlankEntry?>

    @Query("SELECT * FROM plank_entries WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getPlankByDateOnce(userId: String, date: String): PlankEntry?

    @Query("SELECT * FROM plank_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getPlanksByDateRange(userId: String, startDate: String, endDate: String): Flow<List<PlankEntry>>

    @Query("SELECT * FROM plank_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getPlanksByDateRangeOnce(userId: String, startDate: String, endDate: String): List<PlankEntry>

    @Query("SELECT * FROM plank_entries ORDER BY date DESC")
    fun getAllPlanks(): Flow<List<PlankEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlank(plankEntry: PlankEntry): Long

    @Update
    suspend fun updatePlank(plankEntry: PlankEntry)

    @Delete
    suspend fun deletePlank(plankEntry: PlankEntry)

    @Query("DELETE FROM plank_entries WHERE userId = :userId AND date = :date")
    suspend fun deletePlankByDate(userId: String, date: String)
}
