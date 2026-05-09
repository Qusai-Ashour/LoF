package com.leapoffaith.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "qusai",
    val title: String,
    val date: String,           // "yyyy-MM-dd"
    val hourOfDay: Int = -1,    // -1 = untimed
    val isCompleted: Boolean = false,
    val insertOrder: Long = System.currentTimeMillis()
)
