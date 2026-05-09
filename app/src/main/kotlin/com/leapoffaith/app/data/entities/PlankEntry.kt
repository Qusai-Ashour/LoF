package com.leapoffaith.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plank_entries")
data class PlankEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "qusai",
    val date: String,       // "yyyy-MM-dd"
    val completed: Boolean
)
