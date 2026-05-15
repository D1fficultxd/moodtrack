package com.d1ff.moodtrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyEntryDao {
    @Query("SELECT * FROM daily_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<DailyEntry>>

    @Query("SELECT * FROM daily_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date: String): DailyEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DailyEntry)

    @Update
    suspend fun update(entry: DailyEntry)

    @Query("DELETE FROM daily_entries WHERE id = :id")
    suspend fun deleteById(id: Long): Int
    
    @Query("DELETE FROM daily_entries")
    suspend fun deleteAll(): Int
}
