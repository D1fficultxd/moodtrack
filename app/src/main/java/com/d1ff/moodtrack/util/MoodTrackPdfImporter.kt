package com.d1ff.moodtrack.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class DailyEntryImportDto(
    val date: String,
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
    val note: String = ""
)

data class PdfImportParseResult(
    val entries: List<DailyEntryImportDto>,
    val skippedRows: Int,
    val recognized: Boolean
)

object MoodTrackPdfImporter {
    private val isoDateRegex = Regex("""^(\d{4}-\d{2}-\d{2})(?:\s+(.*))?$""")
    private val ruDateRegex = Regex("""^(\d{2}\.\d{2}\.\d{4})(?:\s+(.*))?$""")
    private val enDateRegex = Regex(
        """^((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*\.?\s+\d{1,2},\s+\d{4})(?:\s+(.*))?$""",
        RegexOption.IGNORE_CASE
    )
    private val intRegex = Regex("""^-?\d+$""")
    private val decimalRegex = Regex("""^-?\d+(?:[.,]\d+)?$""")
    private val enDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val enFullDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
    private val ruDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    suspend fun parse(context: Context, uri: Uri): PdfImportParseResult = withContext(Dispatchers.IO) {
        val text = extractText(context, uri)
        parseText(text)
    }

    internal fun parseText(text: String): PdfImportParseResult {
        val lines = text
            .replace('\u00A0', ' ')
            .replace("\r", "\n")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val legacyResult = parseLegacy(lines)
        if (legacyResult.entries.isNotEmpty() || legacyResult.recognized) {
            return legacyResult
        }

        return parseNewReport(lines)
    }

    private fun extractText(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open PDF input stream")

        return input.use { stream ->
            PDDocument.load(stream).use { document ->
                PDFTextStripper().getText(document).orEmpty()
            }
        }
    }

    private fun parseLegacy(lines: List<String>): PdfImportParseResult {
        val recognized = lines.any { it.equals("Записи:", ignoreCase = true) || it.equals("Entries:", ignoreCase = true) } &&
            lines.any { it == "Засп" || it.equals("Easy", ignoreCase = true) }

        val entries = mutableListOf<DailyEntryImportDto>()
        var skippedRows = 0
        var index = 0

        while (index < lines.size) {
            val dateMatch = isoDateRegex.matchEntire(lines[index])
            if (dateMatch == null) {
                index++
                continue
            }

            val date = runCatching { LocalDate.parse(dateMatch.groupValues[1]) }.getOrNull()
            if (date == null) {
                skippedRows++
                index++
                continue
            }
            val cells = mutableListOf<String>()
            dateMatch.groupValues.getOrNull(2)
                ?.takeIf { it.isNotBlank() }
                ?.let { cells.addAll(splitTokens(it)) }

            var next = index + 1
            while (next < lines.size && !startsWithKnownDate(lines[next]) && !isSectionBoundary(lines[next])) {
                val line = lines[next]
                if (!isTableHeaderCell(line)) {
                    if (cells.size < 12) {
                        cells.addAll(splitTokens(line))
                    } else {
                        cells.add(line)
                    }
                }
                next++
            }

            val entry = parseLegacyCells(date.toString(), cells)
            if (entry != null) {
                entries.add(entry)
            } else {
                skippedRows++
            }
            index = next
        }

        return PdfImportParseResult(
            entries = entries.distinctByDate(),
            skippedRows = skippedRows,
            recognized = recognized || entries.isNotEmpty()
        )
    }

    private fun parseLegacyCells(date: String, cells: List<String>): DailyEntryImportDto? {
        if (cells.size < 11) return null

        val sleepHours = parseFloat(cells[0]) ?: return null
        val sleepEase = parseInt(cells[1]) ?: return null
        val mood = parseInt(cells[2]) ?: return null
        val anxiety = parseInt(cells[3]) ?: return null
        val irritability = parseInt(cells[4]) ?: return null
        val impulsivity = parseInt(cells[5]) ?: return null
        val racingThoughts = parseInt(cells[6]) ?: return null
        val apathy = parseInt(cells[7]) ?: return null
        val fatigue = parseInt(cells[8]) ?: return null
        val lossOfInterest = parseInt(cells[9]) ?: return null
        val hopelessness = parseInt(cells[10]) ?: return null
        val suicidalThoughts = parseRisk(cells.getOrNull(11))
        val note = normalizeNote(cells.drop(12).joinToString(" "))

        return DailyEntryImportDto(
            date = date,
            sleepHours = sleepHours,
            sleepEase = sleepEase,
            anxiety = anxiety,
            irritability = irritability,
            impulsivity = impulsivity,
            racingThoughts = racingThoughts,
            suicidalThoughts = suicidalThoughts,
            selfHarm = false,
            mood = mood,
            apathy = apathy,
            fatigue = fatigue,
            lossOfInterest = lossOfInterest,
            hopelessness = hopelessness,
            note = note
        )
    }

    private fun parseNewReport(lines: List<String>): PdfImportParseResult {
        val hasNewMarker = lines.any {
            it.equals("Основные ежедневные показатели", ignoreCase = true) ||
                it.equals("Main daily indicators", ignoreCase = true)
        }
        val rowsByDate = linkedMapOf<String, MutableImportRow>()
        var skippedRows = 0

        collectDateRows(sectionLines(lines, "Основные ежедневные показатели", "Main daily indicators")).forEach { row ->
            val parsed = parseNewMainRow(row)
            if (parsed == null) {
                skippedRows++
            } else {
                rowsByDate.getOrPut(parsed.date) { MutableImportRow(parsed.date) }.apply {
                    sleepHours = parsed.sleepHours
                    sleepEase = parsed.sleepEase
                    mood = parsed.mood
                    anxiety = parsed.anxiety
                    suicidalThoughts = parsed.suicidalThoughts
                    selfHarm = parsed.selfHarm
                }
            }
        }

        collectDateRows(sectionLines(lines, "Дополнительные показатели", "Additional indicators")).forEach { row ->
            val parsed = parseNewAdditionalRow(row)
            if (parsed == null) {
                skippedRows++
            } else {
                rowsByDate.getOrPut(parsed.date) { MutableImportRow(parsed.date) }.apply {
                    irritability = parsed.irritability
                    impulsivity = parsed.impulsivity
                    racingThoughts = parsed.racingThoughts
                    apathy = parsed.apathy
                    fatigue = parsed.fatigue
                    lossOfInterest = parsed.lossOfInterest
                    hopelessness = parsed.hopelessness
                }
            }
        }

        collectDateRows(sectionLines(lines, "Заметки", "Notes")).forEach { row ->
            val note = normalizeNote(row.cells.joinToString(" "))
            if (note.isNotEmpty()) {
                rowsByDate.getOrPut(row.date.toString()) { MutableImportRow(row.date.toString()) }.note = note
            }
        }

        val entries = rowsByDate.values.map { it.toDto() }
        return PdfImportParseResult(
            entries = entries.distinctByDate(),
            skippedRows = skippedRows,
            recognized = hasNewMarker || entries.isNotEmpty()
        )
    }

    private fun parseNewMainRow(row: DateRow): NewMainRow? {
        val numbers = numericTokens(row.cells)
        if (numbers.size < 5) return null

        return NewMainRow(
            date = row.date.toString(),
            sleepHours = numbers[0].toFloat(),
            sleepEase = numbers[1].toInt(),
            mood = numbers[2].toInt(),
            anxiety = numbers[3].toInt(),
            suicidalThoughts = numbers[4].toInt().coerceIn(0, 3),
            selfHarm = row.cells.any { it.equals("да", ignoreCase = true) || it.equals("yes", ignoreCase = true) }
        )
    }

    private fun parseNewAdditionalRow(row: DateRow): NewAdditionalRow? {
        val numbers = numericTokens(row.cells)
        if (numbers.size < 7) return null

        return NewAdditionalRow(
            date = row.date.toString(),
            irritability = numbers[0].toInt(),
            impulsivity = numbers[1].toInt(),
            racingThoughts = numbers[2].toInt(),
            apathy = numbers[3].toInt(),
            fatigue = numbers[4].toInt(),
            lossOfInterest = numbers[5].toInt(),
            hopelessness = numbers[6].toInt()
        )
    }

    private fun collectDateRows(lines: List<String>): List<DateRow> {
        val rows = mutableListOf<DateRow>()
        var index = 0

        while (index < lines.size) {
            val start = parseDateStart(lines[index])
            if (start == null) {
                index++
                continue
            }

            val cells = mutableListOf<String>()
            if (start.rest.isNotBlank()) {
                cells.addAll(splitTokens(start.rest))
            }

            var next = index + 1
            while (next < lines.size && parseDateStart(lines[next]) == null && !isSectionBoundary(lines[next])) {
                val line = lines[next]
                if (!isTableHeaderCell(line)) {
                    cells.add(line)
                }
                next++
            }

            rows.add(DateRow(start.date, cells))
            index = next
        }

        return rows
    }

    private fun sectionLines(lines: List<String>, ruTitle: String, enTitle: String): List<String> {
        val start = lines.indexOfFirst { it.equals(ruTitle, ignoreCase = true) || it.equals(enTitle, ignoreCase = true) }
        if (start < 0) return emptyList()

        val end = (start + 1 until lines.size).firstOrNull { isSectionBoundary(lines[it]) } ?: lines.size
        return lines.subList(start + 1, end)
    }

    private fun parseDateStart(line: String): DateStart? {
        isoDateRegex.matchEntire(line)?.let { match ->
            return DateStart(LocalDate.parse(match.groupValues[1]), match.groupValues.getOrNull(2).orEmpty())
        }
        ruDateRegex.matchEntire(line)?.let { match ->
            return DateStart(LocalDate.parse(match.groupValues[1], ruDateFormatter), match.groupValues.getOrNull(2).orEmpty())
        }
        enDateRegex.matchEntire(line)?.let { match ->
            val rawDate = match.groupValues[1].replace("Sept", "Sep")
            val date = parseEnglishDate(rawDate) ?: return null
            return DateStart(date, match.groupValues.getOrNull(2).orEmpty())
        }
        return null
    }

    private fun parseEnglishDate(rawDate: String): LocalDate? {
        return try {
            LocalDate.parse(rawDate, enDateFormatter)
        } catch (_: DateTimeParseException) {
            try {
                LocalDate.parse(rawDate, enFullDateFormatter)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun startsWithKnownDate(line: String): Boolean =
        isoDateRegex.matches(line) || ruDateRegex.matches(line) || enDateRegex.matches(line)

    private fun isSectionBoundary(line: String): Boolean {
        return line.equals("Сводка за период", ignoreCase = true) ||
            line.equals("Сводка за период:", ignoreCase = true) ||
            line.equals("Summary for the period", ignoreCase = true) ||
            line.equals("Записи:", ignoreCase = true) ||
            line.equals("Entries:", ignoreCase = true) ||
            line.equals("Основные ежедневные показатели", ignoreCase = true) ||
            line.equals("Main daily indicators", ignoreCase = true) ||
            line.equals("Дополнительные показатели", ignoreCase = true) ||
            line.equals("Additional indicators", ignoreCase = true) ||
            line.equals("Заметки", ignoreCase = true) ||
            line.equals("Notes", ignoreCase = true) ||
            line.equals("Методика оценки MoodTrack", ignoreCase = true) ||
            line.equals("MoodTrack Rating Guide", ignoreCase = true)
    }

    private fun isTableHeaderCell(line: String): Boolean {
        val headers = setOf(
            "Дата", "Date", "Сон", "Sleep", "Сон, ч", "Sleep, h", "Засп", "Easy", "Засыпание",
            "Falling asleep", "Настр", "Настроение", "Mood", "Трев", "Тревога", "Anx", "Anxiety",
            "Раздр", "Раздражительность", "Irr", "Irritability", "Имп", "Импульсивность",
            "Impulsivity", "Разг", "Разгон мыслей", "Race", "Racing thoughts", "Апат", "Апатия",
            "Apathy", "Уст", "Усталость", "Fat", "Fatigue", "Инт", "Потеря интереса",
            "Loss of interest", "Безн", "Безнадежность", "Hop", "Hopelessness", "Риски", "Risks",
            "Суицидальные мысли", "Suicidal thoughts", "Самоповреждение", "Self-harm", "Заметка",
            "Note"
        )
        return headers.any { it.equals(line, ignoreCase = true) }
    }

    private fun splitTokens(text: String): List<String> =
        text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    private fun numericTokens(cells: List<String>): List<Double> {
        return cells
            .flatMap { splitTokens(it) }
            .mapNotNull { parseDouble(it.trim('(', ')', ',', ';')) }
    }

    private fun parseFloat(value: String): Float? = parseDouble(value)?.toFloat()

    private fun parseInt(value: String): Int? = value.trim()
        .takeIf { intRegex.matches(it) }
        ?.toIntOrNull()

    private fun parseDouble(value: String): Double? {
        val normalized = value.trim().replace(',', '.')
        if (!decimalRegex.matches(normalized)) return null
        return normalized.toDoubleOrNull()
    }

    private fun parseRisk(value: String?): Int {
        if (value.isNullOrBlank() || value == "-") return 0

        val normalized = value.uppercase(Locale.ROOT)
        Regex("""S(\d+)""").find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            return it.coerceIn(0, 3)
        }
        return normalized.toIntOrNull()?.coerceIn(0, 3) ?: 0
    }

    private fun normalizeNote(value: String): String {
        val normalized = value.trim()
        return if (normalized == "-" || normalized == "—") "" else normalized
    }

    private fun List<DailyEntryImportDto>.distinctByDate(): List<DailyEntryImportDto> =
        associateBy { it.date }.values.toList()

    private data class DateStart(val date: LocalDate, val rest: String)
    private data class DateRow(val date: LocalDate, val cells: List<String>)

    private data class MutableImportRow(
        val date: String,
        var sleepHours: Float = 8f,
        var sleepEase: Int = 5,
        var anxiety: Int = 0,
        var irritability: Int = 0,
        var impulsivity: Int = 0,
        var racingThoughts: Int = 0,
        var suicidalThoughts: Int = 0,
        var selfHarm: Boolean = false,
        var mood: Int = 5,
        var apathy: Int = 0,
        var fatigue: Int = 0,
        var lossOfInterest: Int = 0,
        var hopelessness: Int = 0,
        var note: String = ""
    ) {
        fun toDto(): DailyEntryImportDto = DailyEntryImportDto(
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
            note = note
        )
    }

    private data class NewMainRow(
        val date: String,
        val sleepHours: Float,
        val sleepEase: Int,
        val mood: Int,
        val anxiety: Int,
        val suicidalThoughts: Int,
        val selfHarm: Boolean
    )

    private data class NewAdditionalRow(
        val date: String,
        val irritability: Int,
        val impulsivity: Int,
        val racingThoughts: Int,
        val apathy: Int,
        val fatigue: Int,
        val lossOfInterest: Int,
        val hopelessness: Int
    )
}
