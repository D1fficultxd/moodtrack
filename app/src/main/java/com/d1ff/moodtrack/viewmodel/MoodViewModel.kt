package com.d1ff.moodtrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d1ff.moodtrack.data.AppDatabase
import com.d1ff.moodtrack.data.DailyEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MoodViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).dailyEntryDao()

    val allEntries = dao.getAllEntries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _todayEntry = MutableStateFlow<DailyEntry?>(null)
    val todayEntry: StateFlow<DailyEntry?> = _todayEntry.asStateFlow()

    init {
        loadTodayEntry()
    }

    fun loadTodayEntry() {
        viewModelScope.launch {
            val dateStr = LocalDate.now().toString()
            _todayEntry.value = dao.getEntryByDate(dateStr)
        }
    }

    fun saveEntry(entry: DailyEntry) {
        viewModelScope.launch {
            val existing = dao.getEntryByDate(entry.date)
            if (existing != null) {
                dao.update(entry.copy(id = existing.id, createdAt = existing.createdAt, updatedAt = System.currentTimeMillis()))
            } else {
                dao.insert(entry)
            }
            if (entry.date == LocalDate.now().toString()) {
                _todayEntry.value = entry
            }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
            loadTodayEntry()
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            dao.deleteAll()
            loadTodayEntry()
        }
    }
}
