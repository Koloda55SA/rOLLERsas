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

/**
 * Формирование PDF-отчёта смены средствами android.graphics.pdf.
 * Надёжно работает даже при пустых данных.
 */
object ReportPdf {

    private fun timeFmt(ms: Long): String =
        if (ms <= 0) "—" else SimpleDateFormat("HH:mm", Locale.US).format(Date(ms))

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
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 @72dpi
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val title = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val h = Paint().apply { textSize = 14f; isFakeBoldText = true }
        val p = Paint().apply { textSize = 12f }

        var y = 40f
        val x = 40f
        fun line(text: String, paint: Paint = p, dy: Float = 20f) {
            canvas.drawText(text, x, y, paint); y += dy
        }

        line("Отчёт смены — Роллердром", title, 30f)
        line("Дата: $dateKey", p)
        line("Открытие смены: ${timeFmt(shift.openTime)}    Закрытие: ${timeFmt(shift.closeTime)}", p, 28f)

        line("Итоги дня", h)
        line("Количество клиентов: ${analytics.clientsCount}", p)
        line("Суммарные часы проката: %.1f ч".format(analytics.totalHours), p)
        line("Общая выручка: ${analytics.totalRevenue} сом", p)
        line("Прощено доплат: ${analytics.forgivenExtra} сом", p, 28f)

        line("Расходы", h)
        line("Зарплата (сотрудниц: $staffCount): $salary сом", p)
        line("Прочие расходы: $otherExpenses сом", p, 28f)

        line("ЧИСТАЯ ПРИБЫЛЬ: ${analytics.netProfit} сом", h, 30f)

        line("Износ роликов (по частоте)", h)
        analytics.rollerUsage.take(25).forEach { (rollerId, count) ->
            if (y > 800f) return@forEach
            line("Ролик #$rollerId  —  $count выдач", p, 16f)
        }

        doc.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val file = File(dir, "smena_$dateKey.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Поделиться сформированным PDF. */
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
