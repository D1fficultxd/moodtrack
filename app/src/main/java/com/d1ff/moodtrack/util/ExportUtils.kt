package com.d1ff.moodtrack.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.data.DailyEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

object ExportUtils {
    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
    private const val PAGE_BOTTOM = PAGE_HEIGHT - MARGIN
    private const val TABLE_PADDING = 4f
    private const val TABLE_LINE_HEIGHT = 11f
    private const val BODY_LINE_HEIGHT = 13.5f
    private const val GUIDE_LINE_HEIGHT = 13f

    suspend fun generatePdf(
        context: Context,
        entries: List<DailyEntry>,
        start: LocalDate,
        end: LocalDate,
        includeRatingGuide: Boolean = false
    ): File? = withContext(Dispatchers.IO) {
        createPdf(context, "moodtrack_report_${fileTimestamp()}.pdf") { writer ->
            drawReport(writer, entries.sortedBy { it.parsedDate() }, start, end)
            if (includeRatingGuide) {
                writer.startNewPage()
                drawRatingGuide(writer)
            }
        }
    }

    suspend fun generateRatingGuidePdf(context: Context): File? = withContext(Dispatchers.IO) {
        createPdf(context, "moodtrack_rating_guide_${fileTimestamp()}.pdf") { writer ->
            drawRatingGuide(writer)
        }
    }

    suspend fun generateDoctorPdf(
        context: Context,
        entries: List<DailyEntry>,
        start: LocalDate,
        end: LocalDate
    ): File? = withContext(Dispatchers.IO) {
        createPdf(context, "moodtrack_doctor_report_${fileTimestamp()}.pdf") { writer ->
            drawReport(
                writer = writer,
                entries = entries.sortedBy { it.parsedDate() },
                start = start,
                end = end,
                includeKeyObservations = true
            )
            writer.startNewPage()
            drawRatingGuide(writer)
        }
    }

    private fun createPdf(
        context: Context,
        fileName: String,
        drawContent: (PdfWriter) -> Unit
    ): File? {
        val document = PdfDocument()
        return try {
            val writer = PdfWriter(context, document)
            writer.startNewPage()
            drawContent(writer)
            writer.finish()

            val dir = File(context.cacheDir, "pdfs")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            FileOutputStream(file).use { output -> document.writeTo(output) }
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                document.close()
            } catch (_: Exception) {
            }
            null
        }
    }

    private fun drawReport(
        writer: PdfWriter,
        entries: List<DailyEntry>,
        start: LocalDate,
        end: LocalDate,
        includeKeyObservations: Boolean = false
    ) {
        val context = writer.context
        val style = writer.style
        val locale = context.pdfLocale()
        val dateFormatter = reportDateFormatter(locale)

        writer.canvas.drawText(context.getString(R.string.pdf_report_title), MARGIN, writer.y, style.title)
        writer.y += 24f
        writer.canvas.drawText(context.getString(R.string.pdf_report_subtitle), MARGIN, writer.y, style.subtitle)
        writer.y += 24f

        drawLabelValue(
            writer,
            context.getString(R.string.pdf_report_period_label),
            "${start.format(dateFormatter)} - ${end.format(dateFormatter)}"
        )
        drawLabelValue(
            writer,
            context.getString(R.string.pdf_created_at_label),
            LocalDate.now().format(dateFormatter)
        )
        writer.y += 7f

        drawWrappedParagraph(
            writer = writer,
            text = context.getString(R.string.pdf_disclaimer),
            paint = style.note,
            lineHeight = BODY_LINE_HEIGHT,
            bottomSpacing = 16f
        )

        drawSummary(writer, entries, locale)

        if (includeKeyObservations) {
            drawKeyObservations(writer, entries, locale)
        }

        drawMetricsTable(
            writer = writer,
            title = context.getString(R.string.pdf_main_metrics_title),
            columns = mainMetricColumns(context, locale),
            entries = entries
        )

        drawMetricsTable(
            writer = writer,
            title = context.getString(R.string.pdf_additional_metrics_title),
            columns = additionalMetricColumns(context),
            entries = entries
        )

        drawNotesSection(writer, entries, locale)
    }

    private fun drawSummary(writer: PdfWriter, entries: List<DailyEntry>, locale: Locale) {
        val context = writer.context
        val dash = context.getString(R.string.pdf_empty_value)
        val summary = listOf(
            context.getString(R.string.pdf_summary_filled_days_label) to entries.size.toString(),
            context.getString(R.string.pdf_summary_avg_sleep_label) to entries.averageOrNull { it.sleepHours.toDouble() }.asHours(context, locale),
            context.getString(R.string.pdf_summary_avg_mood_label) to entries.averageOrNull { it.mood.toDouble() }.asScale(locale, dash),
            context.getString(R.string.pdf_summary_avg_anxiety_label) to entries.averageOrNull { it.anxiety.toDouble() }.asScale(locale, dash),
            context.getString(R.string.pdf_summary_avg_apathy_label) to entries.averageOrNull { it.apathy.toDouble() }.asScale(locale, dash),
            context.getString(R.string.pdf_summary_avg_fatigue_label) to entries.averageOrNull { it.fatigue.toDouble() }.asScale(locale, dash),
            context.getString(R.string.pdf_summary_avg_hopelessness_label) to entries.averageOrNull { it.hopelessness.toDouble() }.asScale(locale, dash),
            context.getString(R.string.pdf_summary_suicidal_days_label) to entries.count { it.suicidalThoughts > 0 }.toString(),
            context.getString(R.string.pdf_summary_self_harm_days_label) to entries.count { it.selfHarm }.toString(),
            context.getString(R.string.pdf_summary_min_sleep_label) to entries.minOfOrNull { it.sleepHours.toDouble() }.asHours(context, locale),
            context.getString(R.string.pdf_summary_max_anxiety_label) to entries.maxOfOrNull { it.anxiety.toDouble() }.asScale(locale, dash),
            context.getString(R.string.pdf_summary_max_racing_label) to entries.maxOfOrNull { it.racingThoughts.toDouble() }.asScale(locale, dash),
            context.getString(R.string.pdf_summary_max_impulsivity_label) to entries.maxOfOrNull { it.impulsivity.toDouble() }.asScale(locale, dash)
        )

        drawSectionTitle(writer, context.getString(R.string.pdf_summary_title))

        val columnWidth = (CONTENT_WIDTH - 18f) / 2f
        summary.chunked(2).forEach { row ->
            val lineSets = row.map { (label, value) ->
                wrapText("$label: $value", writer.style.body, columnWidth)
            }
            val rowHeight = lineSets.maxOf { it.size } * BODY_LINE_HEIGHT + 3f
            writer.ensureSpace(rowHeight)
            val top = writer.y

            row.forEachIndexed { index, _ ->
                val x = MARGIN + index * (columnWidth + 18f)
                var baseline = top - writer.style.body.fontMetrics.ascent
                lineSets[index].forEach { line ->
                    writer.canvas.drawText(line, x, baseline, writer.style.body)
                    baseline += BODY_LINE_HEIGHT
                }
            }
            writer.y += rowHeight
        }
        writer.y += 12f
    }

    private fun drawKeyObservations(writer: PdfWriter, entries: List<DailyEntry>, locale: Locale) {
        val context = writer.context
        val dash = context.getString(R.string.pdf_empty_value)
        val observations = if (entries.isEmpty()) {
            listOf(context.getString(R.string.pdf_key_observations_empty))
        } else {
            listOf(
                context.getString(R.string.pdf_key_observations_period_days, entries.size),
                context.getString(
                    R.string.pdf_key_observations_risk_days,
                    entries.count { it.selfHarm || it.suicidalThoughts > 0 }
                ),
                context.getString(
                    R.string.pdf_key_observations_note_days,
                    entries.count { it.note.isNotBlank() }
                ),
                context.getString(
                    R.string.pdf_key_observations_min_sleep,
                    entries.minOfOrNull { it.sleepHours.toDouble() }.asHours(context, locale)
                ),
                context.getString(
                    R.string.pdf_key_observations_max_anxiety,
                    entries.maxOfOrNull { it.anxiety.toDouble() }.asScale(locale, dash)
                )
            )
        }

        drawSectionTitle(writer, context.getString(R.string.pdf_key_observations_title))
        observations.forEach { observation ->
            drawWrappedParagraph(
                writer = writer,
                text = "- $observation",
                paint = writer.style.body,
                x = MARGIN + 8f,
                maxWidth = CONTENT_WIDTH - 8f,
                lineHeight = BODY_LINE_HEIGHT,
                bottomSpacing = 1f
            )
        }
        writer.y += 10f
    }

    private fun drawMetricsTable(
        writer: PdfWriter,
        title: String,
        columns: List<TableColumn>,
        entries: List<DailyEntry>
    ) {
        val rows = entries.map { entry -> columns.map { column -> column.value(entry) } }
        drawTable(
            writer = writer,
            title = title,
            columns = columns,
            rows = rows,
            emptyMessage = writer.context.getString(R.string.pdf_no_entries_for_period)
        )
    }

    private fun drawNotesSection(writer: PdfWriter, entries: List<DailyEntry>, locale: Locale) {
        val context = writer.context
        val rows = entries
            .filter { it.note.isNotBlank() }
            .map { entry ->
                listOf(
                    entry.formattedDate(locale),
                    entry.note.trim()
                )
            }

        if (rows.isEmpty()) {
            drawSectionTitle(writer, context.getString(R.string.pdf_notes_title))
            drawWrappedParagraph(
                writer = writer,
                text = context.getString(R.string.pdf_no_notes_for_period),
                paint = writer.style.body,
                lineHeight = BODY_LINE_HEIGHT,
                bottomSpacing = 8f
            )
            return
        }

        drawTable(
            writer = writer,
            title = context.getString(R.string.pdf_notes_title),
            columns = listOf(
                TableColumn(context.getString(R.string.pdf_col_date_full), 100f) { it.formattedDate(locale) },
                TableColumn(context.getString(R.string.pdf_col_note_full), CONTENT_WIDTH - 100f) { it.note.trim() }
            ),
            rows = rows,
            emptyMessage = context.getString(R.string.pdf_no_notes_for_period)
        )
    }

    private fun mainMetricColumns(context: Context, locale: Locale): List<TableColumn> = listOf(
        TableColumn(context.getString(R.string.pdf_col_date_full), 78f) { it.formattedDate(locale) },
        TableColumn(context.getString(R.string.pdf_col_sleep_hours_full), 80f) { formatNumber(it.sleepHours.toDouble(), locale) },
        TableColumn(context.getString(R.string.pdf_col_mood_full), 85f) { it.mood.toString() },
        TableColumn(context.getString(R.string.pdf_col_anxiety_full), 85f) { it.anxiety.toString() },
        TableColumn(context.getString(R.string.pdf_col_suicidal_full), 240f) { suicidalValue(context, it.suicidalThoughts) },
        TableColumn(context.getString(R.string.pdf_col_self_harm_full), 202f) { yesNo(context, it.selfHarm) }
    )

    private fun additionalMetricColumns(context: Context): List<TableColumn> = listOf(
        TableColumn(context.getString(R.string.pdf_col_date_full), 78f) { entry -> entry.formattedDate(context.pdfLocale()) },
        TableColumn(context.getString(R.string.pdf_col_irritability_full), 108f) { it.irritability.toString() },
        TableColumn(context.getString(R.string.pdf_col_impulsivity_full), 108f) { it.impulsivity.toString() },
        TableColumn(context.getString(R.string.pdf_col_racing_full), 108f) { it.racingThoughts.toString() },
        TableColumn(context.getString(R.string.pdf_col_apathy_full), 86f) { it.apathy.toString() },
        TableColumn(context.getString(R.string.pdf_col_fatigue_full), 86f) { it.fatigue.toString() },
        TableColumn(context.getString(R.string.pdf_col_interest_full), 112f) { it.lossOfInterest.toString() },
        TableColumn(context.getString(R.string.pdf_col_hopelessness_full), 84f) { it.hopelessness.toString() }
    )

    private fun drawRatingGuide(writer: PdfWriter) {
        val context = writer.context
        val style = writer.style

        writer.canvas.drawText(context.getString(R.string.pdf_rating_guide_title), MARGIN, writer.y, style.title)
        writer.y += 28f

        drawGuideSection(
            writer,
            context.getString(R.string.pdf_rating_guide_how_to_read_title),
            listOf(context.getString(R.string.pdf_rating_guide_intro))
        )

        ratingGuideSections(context).forEach { section ->
            drawGuideSection(writer, section.title, section.items)
        }

        drawGuideSection(
            writer,
            context.getString(R.string.pdf_rating_guide_disclaimer_title),
            listOf(context.getString(R.string.pdf_rating_guide_disclaimer))
        )
    }

    private fun ratingGuideSections(context: Context): List<GuideSection> = listOf(
        GuideSection(
            context.getString(R.string.pdf_guide_general_scale_title),
            listOf(
                context.getString(R.string.pdf_guide_scale_0),
                context.getString(R.string.pdf_guide_scale_1_2),
                context.getString(R.string.pdf_guide_scale_3_4),
                context.getString(R.string.pdf_guide_scale_5_6),
                context.getString(R.string.pdf_guide_scale_7_8),
                context.getString(R.string.pdf_guide_scale_9_10)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_mood_title),
            listOf(
                context.getString(R.string.pdf_guide_mood_0_2),
                context.getString(R.string.pdf_guide_mood_3_4),
                context.getString(R.string.pdf_guide_mood_5),
                context.getString(R.string.pdf_guide_mood_6_7),
                context.getString(R.string.pdf_guide_mood_8),
                context.getString(R.string.pdf_guide_mood_9_10)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_sleep_title),
            listOf(
                context.getString(R.string.pdf_guide_sleep_8_10),
                context.getString(R.string.pdf_guide_sleep_7_8),
                context.getString(R.string.pdf_guide_sleep_6_7),
                context.getString(R.string.pdf_guide_sleep_5_6),
                context.getString(R.string.pdf_guide_sleep_lt5),
                context.getString(R.string.pdf_guide_sleep_lt4_no_fatigue)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_anxiety_title),
            listOf(
                context.getString(R.string.pdf_guide_anxiety_0),
                context.getString(R.string.pdf_guide_anxiety_1_2),
                context.getString(R.string.pdf_guide_anxiety_3_4),
                context.getString(R.string.pdf_guide_anxiety_5_6),
                context.getString(R.string.pdf_guide_anxiety_7_8),
                context.getString(R.string.pdf_guide_anxiety_9_10)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_irritability_title),
            listOf(
                context.getString(R.string.pdf_guide_irritability_0),
                context.getString(R.string.pdf_guide_irritability_1_2),
                context.getString(R.string.pdf_guide_irritability_3_4),
                context.getString(R.string.pdf_guide_irritability_5_6),
                context.getString(R.string.pdf_guide_irritability_7_8),
                context.getString(R.string.pdf_guide_irritability_9_10)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_impulsivity_title),
            listOf(
                context.getString(R.string.pdf_guide_impulsivity_0),
                context.getString(R.string.pdf_guide_impulsivity_1_2),
                context.getString(R.string.pdf_guide_impulsivity_3_4),
                context.getString(R.string.pdf_guide_impulsivity_5_6),
                context.getString(R.string.pdf_guide_impulsivity_7_8),
                context.getString(R.string.pdf_guide_impulsivity_9_10)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_racing_title),
            listOf(
                context.getString(R.string.pdf_guide_racing_0),
                context.getString(R.string.pdf_guide_racing_1_2),
                context.getString(R.string.pdf_guide_racing_3_4),
                context.getString(R.string.pdf_guide_racing_5_6),
                context.getString(R.string.pdf_guide_racing_7_8),
                context.getString(R.string.pdf_guide_racing_9_10)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_apathy_title),
            listOf(
                context.getString(R.string.pdf_guide_apathy_0),
                context.getString(R.string.pdf_guide_apathy_1_2),
                context.getString(R.string.pdf_guide_apathy_3_4),
                context.getString(R.string.pdf_guide_apathy_5_6),
                context.getString(R.string.pdf_guide_apathy_7_8),
                context.getString(R.string.pdf_guide_apathy_9_10)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_fatigue_title),
            listOf(
                context.getString(R.string.pdf_guide_fatigue_0),
                context.getString(R.string.pdf_guide_fatigue_1_2),
                context.getString(R.string.pdf_guide_fatigue_3_4),
                context.getString(R.string.pdf_guide_fatigue_5_6),
                context.getString(R.string.pdf_guide_fatigue_7_8),
                context.getString(R.string.pdf_guide_fatigue_9_10)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_interest_title),
            listOf(
                context.getString(R.string.pdf_guide_interest_0),
                context.getString(R.string.pdf_guide_interest_1_2),
                context.getString(R.string.pdf_guide_interest_3_4),
                context.getString(R.string.pdf_guide_interest_5_6),
                context.getString(R.string.pdf_guide_interest_7_8),
                context.getString(R.string.pdf_guide_interest_9_10)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_hopelessness_title),
            listOf(
                context.getString(R.string.pdf_guide_hopelessness_0),
                context.getString(R.string.pdf_guide_hopelessness_1_2),
                context.getString(R.string.pdf_guide_hopelessness_3_4),
                context.getString(R.string.pdf_guide_hopelessness_5_6),
                context.getString(R.string.pdf_guide_hopelessness_7_8),
                context.getString(R.string.pdf_guide_hopelessness_9_10)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_suicidal_title),
            listOf(
                context.getString(R.string.pdf_guide_suicidal_intro),
                context.getString(R.string.pdf_guide_suicidal_0),
                context.getString(R.string.pdf_guide_suicidal_1),
                context.getString(R.string.pdf_guide_suicidal_2),
                context.getString(R.string.pdf_guide_suicidal_3)
            )
        ),
        GuideSection(
            context.getString(R.string.pdf_guide_self_harm_title),
            listOf(context.getString(R.string.pdf_guide_self_harm_desc))
        )
    )

    private fun drawTable(
        writer: PdfWriter,
        title: String,
        columns: List<TableColumn>,
        rows: List<List<String>>,
        emptyMessage: String
    ) {
        drawSectionTitle(writer, title)

        fun repeatHeader() {
            drawTableHeader(writer, columns)
        }

        repeatHeader()
        if (rows.isEmpty()) {
            drawTableEmptyRow(writer, emptyMessage, ::repeatHeader)
        } else {
            rows.forEachIndexed { index, row ->
                drawTableRow(writer, columns, row, index, ::repeatHeader)
            }
        }
        writer.y += 14f
    }

    private fun drawTableHeader(writer: PdfWriter, columns: List<TableColumn>) {
        val style = writer.style
        val lineSets = columns.map { column ->
            wrapText(column.header, style.tableHeader, column.width - TABLE_PADDING * 2)
        }
        val height = (lineSets.maxOf { it.size } * TABLE_LINE_HEIGHT + TABLE_PADDING * 2).coerceAtLeast(28f)

        writer.ensureSpace(height)
        val top = writer.y
        writer.canvas.drawRect(MARGIN, top, MARGIN + CONTENT_WIDTH, top + height, style.tableHeaderBackground)

        var x = MARGIN
        columns.forEachIndexed { index, column ->
            writer.canvas.drawRect(x, top, x + column.width, top + height, style.tableGrid)
            drawCellLines(writer.canvas, lineSets[index], x, top, height, style.tableHeader)
            x += column.width
        }
        writer.y += height
    }

    private fun drawTableRow(
        writer: PdfWriter,
        columns: List<TableColumn>,
        row: List<String>,
        rowIndex: Int,
        repeatHeader: () -> Unit
    ) {
        val style = writer.style
        val allLines = columns.mapIndexed { index, column ->
            val value = row.getOrNull(index).orEmpty().ifBlank {
                writer.context.getString(R.string.pdf_empty_value)
            }
            wrapText(value, style.tableText, column.width - TABLE_PADDING * 2).toMutableList()
        }

        var firstChunk = true
        while (allLines.any { it.isNotEmpty() }) {
            val remainingLineCount = allLines.maxOf { it.size }
            val fullHeight = rowHeight(remainingLineCount)
            val remainingHeight = PAGE_BOTTOM - writer.y
            val maxPageRowHeight = PAGE_HEIGHT - MARGIN * 2 - 42f

            if (firstChunk && fullHeight > remainingHeight && fullHeight <= maxPageRowHeight) {
                writer.startNewPage()
                repeatHeader()
                continue
            }

            if (remainingHeight < rowHeight(1)) {
                writer.startNewPage()
                repeatHeader()
                continue
            }

            val maxLinesOnPage = ((PAGE_BOTTOM - writer.y - TABLE_PADDING * 2) / TABLE_LINE_HEIGHT)
                .toInt()
                .coerceAtLeast(1)
            val chunkLineCount = if (fullHeight <= PAGE_BOTTOM - writer.y) {
                remainingLineCount
            } else {
                min(remainingLineCount, maxLinesOnPage)
            }
            val chunkHeight = rowHeight(chunkLineCount)
            val top = writer.y

            if (rowIndex % 2 == 1) {
                writer.canvas.drawRect(MARGIN, top, MARGIN + CONTENT_WIDTH, top + chunkHeight, style.tableAlternateBackground)
            }

            var x = MARGIN
            columns.forEachIndexed { index, column ->
                val lines = allLines[index].take(chunkLineCount)
                writer.canvas.drawRect(x, top, x + column.width, top + chunkHeight, style.tableGrid)
                drawCellLines(writer.canvas, lines, x, top, chunkHeight, style.tableText)
                repeat(lines.size) {
                    allLines[index].removeFirst()
                }
                x += column.width
            }

            writer.y += chunkHeight
            firstChunk = false
        }
    }

    private fun drawTableEmptyRow(
        writer: PdfWriter,
        text: String,
        repeatHeader: () -> Unit
    ) {
        val style = writer.style
        val lines = wrapText(text, style.tableText, CONTENT_WIDTH - TABLE_PADDING * 2)
        val height = rowHeight(lines.size)
        if (writer.y + height > PAGE_BOTTOM) {
            writer.startNewPage()
            repeatHeader()
        }
        val top = writer.y
        writer.canvas.drawRect(MARGIN, top, MARGIN + CONTENT_WIDTH, top + height, style.tableGrid)
        drawCellLines(writer.canvas, lines, MARGIN, top, height, style.tableText)
        writer.y += height
    }

    private fun drawGuideSection(writer: PdfWriter, title: String, items: List<String>) {
        val style = writer.style
        writer.ensureSpace(30f)
        writer.canvas.drawText(title, MARGIN, writer.y, style.sectionTitle)
        writer.y += 16f

        items.forEach { item ->
            drawWrappedParagraph(
                writer = writer,
                text = item,
                paint = style.body,
                x = MARGIN + 8f,
                maxWidth = CONTENT_WIDTH - 8f,
                lineHeight = GUIDE_LINE_HEIGHT,
                bottomSpacing = 2f
            )
        }
        writer.y += 8f
    }

    private fun drawSectionTitle(writer: PdfWriter, title: String) {
        writer.ensureSpace(28f)
        writer.canvas.drawText(title, MARGIN, writer.y, writer.style.sectionTitle)
        writer.y += 18f
    }

    private fun drawLabelValue(writer: PdfWriter, label: String, value: String) {
        drawWrappedParagraph(
            writer = writer,
            text = "$label: $value",
            paint = writer.style.body,
            lineHeight = BODY_LINE_HEIGHT,
            bottomSpacing = 1f
        )
    }

    private fun drawWrappedParagraph(
        writer: PdfWriter,
        text: String,
        paint: Paint,
        x: Float = MARGIN,
        maxWidth: Float = CONTENT_WIDTH,
        lineHeight: Float,
        bottomSpacing: Float
    ) {
        val lines = wrapText(text, paint, maxWidth)
        lines.forEach { line ->
            writer.ensureSpace(lineHeight)
            writer.canvas.drawText(line, x, writer.y, paint)
            writer.y += lineHeight
        }
        writer.y += bottomSpacing
    }

    private fun drawCellLines(
        canvas: Canvas,
        lines: List<String>,
        left: Float,
        top: Float,
        height: Float,
        paint: Paint
    ) {
        var baseline = top + TABLE_PADDING - paint.fontMetrics.ascent
        val maxBaseline = top + height - TABLE_PADDING
        lines.forEach { line ->
            if (baseline <= maxBaseline) {
                canvas.drawText(line, left + TABLE_PADDING, baseline, paint)
            }
            baseline += TABLE_LINE_HEIGHT
        }
    }

    private fun rowHeight(lineCount: Int): Float =
        (lineCount * TABLE_LINE_HEIGHT + TABLE_PADDING * 2).coerceAtLeast(24f)

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (maxWidth <= 0f) return listOf(text)
        val normalized = text.replace("\r", "")
        val lines = mutableListOf<String>()

        normalized.split("\n").forEach { paragraph ->
            val words = paragraph.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) {
                lines.add("")
                return@forEach
            }

            var currentLine = ""
            words.forEach { rawWord ->
                val wordParts = splitLongWord(rawWord, paint, maxWidth)
                wordParts.forEach { word ->
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(testLine) <= maxWidth) {
                        currentLine = testLine
                    } else {
                        if (currentLine.isNotEmpty()) lines.add(currentLine)
                        currentLine = word
                    }
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine)
        }

        return lines.ifEmpty { listOf("") }
    }

    private fun splitLongWord(word: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(word) <= maxWidth) return listOf(word)

        val result = mutableListOf<String>()
        var remaining = word
        while (remaining.isNotEmpty() && paint.measureText(remaining) > maxWidth) {
            var cut = remaining.length
            while (cut > 1 && paint.measureText(remaining.substring(0, cut)) > maxWidth) {
                cut--
            }
            result.add(remaining.substring(0, cut))
            remaining = remaining.substring(cut)
        }
        if (remaining.isNotEmpty()) result.add(remaining)
        return result
    }

    private fun yesNo(context: Context, value: Boolean): String =
        context.getString(if (value) R.string.pdf_yes else R.string.pdf_no)

    private fun suicidalValue(context: Context, value: Int): String {
        val labelRes = when (value) {
            0 -> R.string.suicidal_none
            1 -> R.string.suicidal_passive
            2 -> R.string.suicidal_frequent
            else -> R.string.suicidal_high
        }
        return "$value (${context.getString(labelRes)})"
    }

    private fun formatNumber(value: Double, locale: Locale): String {
        val formatter = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
        return formatter.format(value)
    }

    private fun Double?.asScale(locale: Locale, dash: String): String =
        this?.let { "${formatNumber(it, locale)} / 10" } ?: dash

    private fun Double?.asHours(context: Context, locale: Locale): String =
        this?.let { "${formatNumber(it, locale)} ${context.getString(R.string.pdf_unit_hours)}" }
            ?: context.getString(R.string.pdf_empty_value)

    private fun List<DailyEntry>.averageOrNull(selector: (DailyEntry) -> Double): Double? =
        if (isEmpty()) null else map(selector).average()

    private fun DailyEntry.parsedDate(): LocalDate =
        runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.MIN)

    private fun DailyEntry.formattedDate(locale: Locale): String =
        runCatching { LocalDate.parse(date).format(reportDateFormatter(locale)) }.getOrDefault(date)

    private fun Context.pdfLocale(): Locale = resources.configuration.locales[0] ?: Locale.getDefault()

    private fun reportDateFormatter(locale: Locale): DateTimeFormatter {
        val pattern = if (locale.language == "ru") "dd.MM.yyyy" else "MMM d, yyyy"
        return DateTimeFormatter.ofPattern(pattern, locale)
    }

    private fun fileTimestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

    fun sharePdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_pdf)))
    }

    private data class TableColumn(
        val header: String,
        val width: Float,
        val value: (DailyEntry) -> String
    )

    private data class GuideSection(val title: String, val items: List<String>)

    private class PdfWriter(val context: Context, private val document: PdfDocument) {
        val style = PdfStyle()
        lateinit var canvas: Canvas
            private set

        var y: Float = MARGIN
        private var page: PdfDocument.Page? = null
        private var pageNumber: Int = 0

        fun startNewPage() {
            page?.let { document.finishPage(it) }
            pageNumber += 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page!!.canvas
            y = MARGIN
        }

        fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > PAGE_BOTTOM) {
                startNewPage()
            }
        }

        fun finish() {
            page?.let { document.finishPage(it) }
            page = null
        }
    }

    private class PdfStyle {
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 27, 31)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(70, 70, 78)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 27, 31)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 27, 31)
            textSize = 10.5f
        }
        val note = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(75, 75, 84)
            textSize = 9.7f
        }
        val tableText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 27, 31)
            textSize = 9.2f
        }
        val tableHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 27, 31)
            textSize = 9.1f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(184, 184, 192)
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
        }
        val tableHeaderBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(236, 236, 242)
            style = Paint.Style.FILL
        }
        val tableAlternateBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(249, 249, 252)
            style = Paint.Style.FILL
        }
    }
}
