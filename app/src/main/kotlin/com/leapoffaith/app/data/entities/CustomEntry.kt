package com.leapoffaith.app.data.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_entries")
data class CustomEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val categoryId: Long,
    val date: String,
    val isDone: Boolean = false,
    val notes: String = "",
    val subItemKey: String = "",
    val hourOfDay: Int = -1
)
