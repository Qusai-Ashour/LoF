package com.leapoffaith.app.data.dao

import androidx.room.*
import com.leapoffaith.app.data.entities.PrayerEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {

    @Query("SELECT * FROM prayer_entries WHERE userId = :userId AND date = :date LIMIT 1")
    fun getPrayerByDate(userId: String, date: String): Flow<PrayerEntry?>

    @Query("SELECT * FROM prayer_entries WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getPrayerByDateOnce(userId: String, date: String): PrayerEntry?

    @Query("SELECT * FROM prayer_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getPrayersByDateRange(userId: String, startDate: String, endDate: String): Flow<List<PrayerEntry>>

    @Query("SELECT * FROM prayer_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getPrayersByDateRangeOnce(userId: String, startDate: String, endDate: String): List<PrayerEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayer(prayerEntry: PrayerEntry): Long

    @Update
    suspend fun updatePrayer(prayerEntry: PrayerEntry)

    @Delete
    suspend fun deletePrayer(prayerEntry: PrayerEntry)
}
