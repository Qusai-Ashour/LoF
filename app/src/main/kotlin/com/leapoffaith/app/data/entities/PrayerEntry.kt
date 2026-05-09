package com.leapoffaith.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_entries")
data class PrayerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: String,       // "yyyy-MM-dd"
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false
)
