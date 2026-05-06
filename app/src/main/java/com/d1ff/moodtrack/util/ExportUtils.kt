package com.d1ff.moodtrack.util

import android.content.Context
import android.content.Intent
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ExportUtils {
    suspend fun generatePdf(context: Context, entries: List<DailyEntry>, start: LocalDate, end: LocalDate): File? = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            // Landscape A4: 842 x 595
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            
            val paint = Paint().apply { textSize = 9f }
            val boldPaint = Paint().apply { 
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val titlePaint = Paint().apply {
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerPaint = Paint().apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.5f
            }

            var y = 40f
            canvas.drawText(context.getString(R.string.pdf_report_title), 40f, y, titlePaint); y += 25f
            
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            canvas.drawText(context.getString(R.string.pdf_period, start.format(formatter), end.format(formatter)), 40f, y, paint); y += 15f
            canvas.drawText(context.getString(R.string.pdf_created_at, LocalDate.now().format(formatter)), 40f, y, paint); y += 25f

            // Summary
            canvas.drawText(context.getString(R.string.pdf_summary), 40f, y, headerPaint); y += 20f
            
            val filledDays = entries.size
            val avgAnxiety = entries.map { it.anxiety }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgSleep = entries.map { it.sleepHours }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgMood = entries.map { it.mood }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgApathy = entries.map { it.apathy }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgFatigue = entries.map { it.fatigue }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgHopelessness = entries.map { it.hopelessness }.average().takeIf { !it.isNaN() } ?: 0.0
            val selfHarmDays = entries.count { it.selfHarm }
            val suicidalDays = entries.count { it.suicidalThoughts > 0 }

            val stats = listOf(
                context.getString(R.string.pdf_summary_filled_days, filledDays),
                context.getString(R.string.pdf_avg_mood, avgMood),
                context.getString(R.string.pdf_avg_sleep, avgSleep),
                context.getString(R.string.pdf_avg_anxiety, avgAnxiety),
                context.getString(R.string.pdf_avg_apathy, avgApathy),
                context.getString(R.string.pdf_avg_fatigue, avgFatigue),
                context.getString(R.string.pdf_avg_hopelessness, avgHopelessness),
                context.getString(R.string.pdf_self_harm_days, selfHarmDays),
                context.getString(R.string.pdf_suicidal_days, suicidalDays)
            )

            var statX = 50f
            var statY = y
            for ((index, stat) in stats.withIndex()) {
                canvas.drawText(stat, statX, statY, paint)
                statY += 15f
                if ((index + 1) % 3 == 0) {
                    statX += 200f
                    statY = y
                }
            }
            y += 50f
            
            canvas.drawText(context.getString(R.string.pdf_entries), 40f, y, headerPaint); y += 20f

            // Table Headers
            val cols = listOf(
                context.getString(R.string.pdf_col_date) to 40f,
                context.getString(R.string.pdf_col_sleep) to 100f,
                context.getString(R.string.pdf_col_ease) to 135f,
                context.getString(R.string.pdf_col_mood) to 170f,
                context.getString(R.string.pdf_col_anxiety) to 205f,
                context.getString(R.string.pdf_col_irritability) to 240f,
                context.getString(R.string.pdf_col_impulsivity) to 275f,
                context.getString(R.string.pdf_col_racing) to 310f,
                context.getString(R.string.pdf_col_apathy) to 345f,
                context.getString(R.string.pdf_col_fatigue) to 380f,
                context.getString(R.string.pdf_col_interest) to 415f,
                context.getString(R.string.pdf_col_hopelessness) to 450f,
                context.getString(R.string.pdf_col_risks) to 485f,
                context.getString(R.string.pdf_col_note) to 530f
            )

            fun drawHeaders(c: android.graphics.Canvas, yPos: Float) {
                cols.forEach { (text, x) -> c.drawText(text, x, yPos, boldPaint) }
                c.drawLine(40f, yPos + 5f, 800f, yPos + 5f, linePaint)
            }

            drawHeaders(canvas, y)
            y += 20f

            for (entry in entries.sortedByDescending { it.date }) {
                if (y > 560) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f
                    drawHeaders(canvas, y)
                    y += 20f
                }
                
                canvas.drawText(entry.date, 40f, y, paint)
                canvas.drawText(String.format("%.1f", entry.sleepHours), 100f, y, paint)
                canvas.drawText(entry.sleepEase.toString(), 135f, y, paint)
                canvas.drawText(entry.mood.toString(), 170f, y, paint)
                canvas.drawText(entry.anxiety.toString(), 205f, y, paint)
                canvas.drawText(entry.irritability.toString(), 240f, y, paint)
                canvas.drawText(entry.impulsivity.toString(), 275f, y, paint)
                canvas.drawText(entry.racingThoughts.toString(), 310f, y, paint)
                canvas.drawText(entry.apathy.toString(), 345f, y, paint)
                canvas.drawText(entry.fatigue.toString(), 380f, y, paint)
                canvas.drawText(entry.lossOfInterest.toString(), 415f, y, paint)
                canvas.drawText(entry.hopelessness.toString(), 450f, y, paint)
                
                val risk = if (entry.selfHarm) "SH!" else if (entry.suicidalThoughts > 0) "S${entry.suicidalThoughts}" else "-"
                canvas.drawText(risk, 485f, y, paint)
                
                // Note wrapping
                if (entry.note.isNotEmpty()) {
                    val noteLines = wrapText(entry.note, paint, 270f)
                    for (line in noteLines) {
                        if (y > 560) {
                            document.finishPage(page)
                            page = document.startPage(pageInfo)
                            canvas = page.canvas
                            y = 40f
                            drawHeaders(canvas, y)
                            y += 20f
                        }
                        canvas.drawText(line, 530f, y, paint)
                        y += 12f
                    }
                    // Subtract last increment to keep spacing consistent if we didn't add a line
                    if (noteLines.isNotEmpty()) y -= 12f
                }
                
                y += 5f
                canvas.drawLine(40f, y, 800f, y, linePaint)
                y += 15f
            }

            document.finishPage(page)

            val dir = File(context.cacheDir, "pdfs")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "moodtrack_report_${LocalDate.now()}.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
                // If a single word is too long, force wrap it
                while (paint.measureText(currentLine) > maxWidth) {
                    var chars = currentLine.length
                    while (chars > 0 && paint.measureText(currentLine.substring(0, chars)) > maxWidth) {
                        chars--
                    }
                    if (chars > 0) {
                        lines.add(currentLine.substring(0, chars))
                        currentLine = currentLine.substring(chars)
                    } else {
                        // Word is extremely long, just add it and move on
                        lines.add(currentLine)
                        currentLine = ""
                        break
                    }
                }
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    fun sharePdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }
}
