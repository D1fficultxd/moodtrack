package com.d1ff.moodtrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_entries")
data class DailyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String = LocalDate.now().toString(),
    val sleepHours: Float = 8f,
    val sleepEase: Int = 5,
    val anxiety: Int = 0,
    val irritability: Int = 0,
    val impulsivity: Int = 0,
    val racingThoughts: Int = 0,
    val suicidalThoughts: Int = 0,
    val selfHarm: Boolean = false,
    val mood: Int = 5,
    val apathy: Int = 0,
    val fatigue: Int = 0,
    val lossOfInterest: Int = 0,
    val hopelessness: Int = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
