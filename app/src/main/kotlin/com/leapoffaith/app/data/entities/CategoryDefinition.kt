package com.leapoffaith.app.data.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryDefinition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "",
    val name: String = "",
    val emoji: String = "",
    val type: String = "RECORD",
    val builtinType: String = "",
    val color: String = "#D4A843",
    val orderIndex: Int = 0,
    val frequency: String = "ONCE_DAILY",
    val subItems: String? = null,
    val prepareFrequency: String = "",
    val showInWidget: Boolean = false,
    val isInMiniTracker: Boolean = false,
    val isFixed: Boolean = true   // true = repeats every day; false = per-day tasks expire at EOD
)
