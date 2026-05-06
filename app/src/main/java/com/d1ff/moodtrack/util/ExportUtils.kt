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
            // Using Landscape A4 for more columns: 842 x 595
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            
            val paint = Paint().apply { textSize = 10f }
            val boldPaint = Paint().apply { 
                textSize = 10f
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
                strokeWidth = 1f
            }

            var y = 40f
            canvas.drawText(context.getString(R.string.pdf_report_title), 40f, y, titlePaint); y += 25f
            
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            canvas.drawText(context.getString(R.string.pdf_period, start.format(formatter), end.format(formatter)), 40f, y, paint); y += 15f
            canvas.drawText(context.getString(R.string.pdf_created_at, LocalDate.now().format(formatter)), 40f, y, paint); y += 25f

            // Summary
            canvas.drawText(context.getString(R.string.pdf_summary), 40f, y, headerPaint); y += 20f
            
            val avgAnxiety = entries.map { it.anxiety }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgSleep = entries.map { it.sleepHours }.average().takeIf { !it.isNaN() } ?: 0.0
            val avgMood = entries.map { it.mood }.average().takeIf { !it.isNaN() } ?: 0.0
            val selfHarmDays = entries.count { it.selfHarm }
            val suicidalDays = entries.count { it.suicidalThoughts > 0 }

            val stats = listOf(
                context.getString(R.string.pdf_avg_anxiety, avgAnxiety),
                context.getString(R.string.pdf_avg_mood, avgMood),
                context.getString(R.string.pdf_avg_sleep, avgSleep),
                context.getString(R.string.pdf_self_harm_days, selfHarmDays),
                context.getString(R.string.pdf_suicidal_days, suicidalDays)
            )

            for (stat in stats) {
                canvas.drawText(stat, 50f, y, paint)
                y += 15f
            }
            
            y += 20f
            canvas.drawText(context.getString(R.string.pdf_entries), 40f, y, headerPaint); y += 20f

            // Table Headers
            val cols = listOf(
                context.getString(R.string.pdf_col_date) to 40f,
                context.getString(R.string.pdf_col_sleep) to 110f,
                "Easy" to 150f,
                context.getString(R.string.pdf_col_anxiety) to 180f,
                "Irr" to 210f,
                "Imp" to 240f,
                "Race" to 270f,
                context.getString(R.string.pdf_col_mood) to 300f,
                "Apa" to 330f,
                "Fat" to 360f,
                "Int" to 390f,
                "Hop" to 420f,
                context.getString(R.string.pdf_col_risks) to 450f,
                context.getString(R.string.section_note) to 520f
            )

            cols.forEach { (text, x) -> canvas.drawText(text, x, y, boldPaint) }
            y += 5f
            canvas.drawLine(40f, y, 800f, y, linePaint)
            y += 15f

            for (entry in entries.sortedByDescending { it.date }) {
                if (y > 550) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f
                    // Repeat headers on new page
                    cols.forEach { (text, x) -> canvas.drawText(text, x, y, boldPaint) }
                    y += 5f
                    canvas.drawLine(40f, y, 800f, y, linePaint)
                    y += 15f
                }
                
                canvas.drawText(entry.date, 40f, y, paint)
                canvas.drawText(entry.sleepHours.toString(), 110f, y, paint)
                canvas.drawText(entry.sleepEase.toString(), 150f, y, paint)
                canvas.drawText(entry.anxiety.toString(), 180f, y, paint)
                canvas.drawText(entry.irritability.toString(), 210f, y, paint)
                canvas.drawText(entry.impulsivity.toString(), 240f, y, paint)
                canvas.drawText(entry.racingThoughts.toString(), 270f, y, paint)
                canvas.drawText(entry.mood.toString(), 300f, y, paint)
                canvas.drawText(entry.apathy.toString(), 330f, y, paint)
                canvas.drawText(entry.fatigue.toString(), 360f, y, paint)
                canvas.drawText(entry.lossOfInterest.toString(), 390f, y, paint)
                canvas.drawText(entry.hopelessness.toString(), 420f, y, paint)
                
                val risk = if (entry.selfHarm) "SH!" else if (entry.suicidalThoughts > 0) "S${entry.suicidalThoughts}" else "-"
                canvas.drawText(risk, 450f, y, paint)
                
                val noteSnippet = if (entry.note.length > 50) entry.note.take(47) + "..." else entry.note
                canvas.drawText(noteSnippet, 520f, y, paint)
                
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
