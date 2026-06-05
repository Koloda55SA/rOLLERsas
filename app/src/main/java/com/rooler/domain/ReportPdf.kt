package com.rooler.domain

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.rooler.data.models.Shift
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportPdf {

    private fun timeFmt(ms: Long): String =
        if (ms <= 0) "\u2014" else SimpleDateFormat("HH:mm", Locale.US).format(Date(ms))

    fun generate(
        context: Context,
        dateKey: String,
        shift: Shift,
        analytics: DayAnalytics,
        salary: Int,
        staffCount: Int,
        otherExpenses: Int
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val title = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val h = Paint().apply { textSize = 14f; isFakeBoldText = true }
        val p = Paint().apply { textSize = 12f }
        val small = Paint().apply { textSize = 10f }

        var y = 40f
        val x = 40f
        fun line(text: String, paint: Paint = p, dy: Float = 20f) {
            canvas.drawText(text, x, y, paint); y += dy
        }

        line("Отчёт смены — Роллердром", title, 28f)
        line("Дата: $dateKey", p)
        if (shift.cashierName.isNotEmpty()) {
            line("Кассир: ${shift.cashierName}", p)
        }
        line("Открытие: ${timeFmt(shift.openTime)}    Закрытие: ${timeFmt(shift.closeTime)}", p, 28f)

        line("Итоги дня", h)
        line("Клиентов: ${analytics.clientsCount}", p)
        line("Часы проката: %.1f ч".format(analytics.totalHours), p)
        line("Выручка: ${analytics.totalRevenue} с", p)
        line("Прощёно доплат: ${analytics.forgivenExtra} с", p, 28f)

        line("Расходы", h)
        line("Зарплата ($staffCount сотр.): $salary с", p)
        line("Прочие: $otherExpenses с", p, 28f)

        line("ЧИСТАЯ ПРИБЫЛЬ: ${analytics.netProfit} с", h, 30f)

        line("Износ роликов", h)
        analytics.rollerUsage.take(25).forEach { (rollerId, count) ->
            if (y > 800f) return@forEach
            line("Ролик #$rollerId  —  $count выдач", p, 16f)
        }

        y = 820f
        line("Разраб: Рахманов Сыймыкбек | __rahmanov___", small)

        doc.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val file = File(dir, "smena_$dateKey.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun generateShiftHistory(
        context: Context,
        shifts: List<Pair<String, Shift>>
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val title = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val h = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val p = Paint().apply { textSize = 12f }
        val small = Paint().apply { textSize = 10f }
        val bold = Paint().apply { textSize = 12f; isFakeBoldText = true }

        var y = 40f
        val x = 40f
        fun line(text: String, paint: Paint = p, dy: Float = 18f) {
            canvas.drawText(text, x, y, paint); y += dy
        }

        line("История смен — Роллердром", title, 30f)
        line("Всего смен: ${shifts.size}", h, 24f)

        shifts.forEach { (dateKey, shift) ->
            if (y > 780f) return@forEach
            val name = if (shift.cashierName.isNotEmpty()) shift.cashierName else "—"
            val openT = timeFmt(shift.openTime)
            val closeT = timeFmt(shift.closeTime)
            line("$dateKey  |  Кассир: $name  |  $openT — $closeT", bold, 18f)
        }

        y = 820f
        line("Разраб: Рахманов Сыймыкбек | __rahmanov___", small)

        doc.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val file = File(dir, "shifts_history.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Отправить отчёт").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
