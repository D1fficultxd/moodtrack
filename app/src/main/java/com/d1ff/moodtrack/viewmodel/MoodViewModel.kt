package com.d1ff.moodtrack.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d1ff.moodtrack.data.AppDatabase
import com.d1ff.moodtrack.data.DailyEntry
import com.d1ff.moodtrack.util.DailyEntryImportDto
import com.d1ff.moodtrack.util.MoodTrackPdfImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PdfImportResult(
    val addedEntries: Int,
    val updatedEntries: Int,
    val skippedRows: Int
)

sealed class PdfImportState {
    object Idle : PdfImportState()
    object Loading : PdfImportState()
    data class Completed(val result: PdfImportResult) : PdfImportState()
    object FormatNotRecognized : PdfImportState()
    object FileError : PdfImportState()
}

class MoodViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).dailyEntryDao()

    val allEntries = dao.getAllEntries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _todayEntry = MutableStateFlow<DailyEntry?>(null)
    val todayEntry: StateFlow<DailyEntry?> = _todayEntry.asStateFlow()

    private val _pdfImportState = MutableStateFlow<PdfImportState>(PdfImportState.Idle)
    val pdfImportState: StateFlow<PdfImportState> = _pdfImportState.asStateFlow()

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

    fun importEntriesFromPdf(uri: Uri) {
        viewModelScope.launch {
            _pdfImportState.value = PdfImportState.Loading

            runCatching {
                val parseResult = MoodTrackPdfImporter.parse(getApplication(), uri)
                if (!parseResult.recognized) {
                    _pdfImportState.value = PdfImportState.FormatNotRecognized
                    return@launch
                }

                val result = withContext(Dispatchers.IO) {
                    importParsedEntries(parseResult.entries, parseResult.skippedRows)
                }
                loadTodayEntry()
                _pdfImportState.value = PdfImportState.Completed(result)
            }.onFailure {
                _pdfImportState.value = PdfImportState.FileError
            }
        }
    }

    fun clearPdfImportState() {
        _pdfImportState.value = PdfImportState.Idle
    }

    private suspend fun importParsedEntries(
        importedEntries: List<DailyEntryImportDto>,
        skippedRows: Int
    ): PdfImportResult {
        var added = 0
        var updated = 0

        importedEntries.associateBy { it.date }.values.forEach { imported ->
            val existing = dao.getEntryByDate(imported.date)
            val now = System.currentTimeMillis()
            val entry = imported.toDailyEntry(
                id = existing?.id ?: 0,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )

            if (existing == null) {
                dao.insert(entry)
                added++
            } else {
                dao.update(entry)
                updated++
            }
        }

        return PdfImportResult(
            addedEntries = added,
            updatedEntries = updated,
            skippedRows = skippedRows
        )
    }

    private fun DailyEntryImportDto.toDailyEntry(
        id: Long,
        createdAt: Long,
        updatedAt: Long
    ): DailyEntry = DailyEntry(
        id = id,
        date = date,
        sleepHours = sleepHours,
        sleepEase = sleepEase,
        anxiety = anxiety,
        irritability = irritability,
        impulsivity = impulsivity,
        racingThoughts = racingThoughts,
        suicidalThoughts = suicidalThoughts,
        selfHarm = selfHarm,
        mood = mood,
        apathy = apathy,
        fatigue = fatigue,
        lossOfInterest = lossOfInterest,
        hopelessness = hopelessness,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
