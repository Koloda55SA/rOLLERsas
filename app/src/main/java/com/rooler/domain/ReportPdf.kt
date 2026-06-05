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

        line("\u041E\u0442\u0447\u0451\u0442 \u0441\u043C\u0435\u043D\u044B \u2014 \u0420\u043E\u043B\u043B\u0435\u0440\u0434\u0440\u043E\u043C", title, 28f)
        line("\u0414\u0430\u0442\u0430: $dateKey", p)
        if (shift.cashierName.isNotEmpty()) {
            line("\u041A\u0430\u0441\u0441\u0438\u0440: ${shift.cashierName}", p)
        }
        line("\u041E\u0442\u043A\u0440\u044B\u0442\u0438\u0435: ${timeFmt(shift.openTime)}    \u0417\u0430\u043A\u0440\u044B\u0442\u0438\u0435: ${timeFmt(shift.closeTime)}", p, 28f)

        line("\u0418\u0442\u043E\u0433\u0438 \u0434\u043D\u044F", h)
        line("\u041A\u043B\u0438\u0435\u043D\u0442\u043E\u0432: ${analytics.clientsCount}", p)
        line("\u0427\u0430\u0441\u044B \u043F\u0440\u043E\u043A\u0430\u0442\u0430: %.1f \u0447".format(analytics.totalHours), p)
        line("\u0412\u044B\u0440\u0443\u0447\u043A\u0430: ${analytics.totalRevenue} \u0441", p)
        line("\u041F\u0440\u043E\u0449\u0451\u043D\u043E \u0434\u043E\u043F\u043B\u0430\u0442: ${analytics.forgivenExtra} \u0441", p, 28f)

        line("\u0420\u0430\u0441\u0445\u043E\u0434\u044B", h)
        line("\u0417\u0430\u0440\u043F\u043B\u0430\u0442\u0430 ($staffCount \u0441\u043E\u0442\u0440.): $salary \u0441", p)
        line("\u041F\u0440\u043E\u0447\u0438\u0435: $otherExpenses \u0441", p, 28f)

        line("\u0427\u0418\u0421\u0422\u0410\u042F \u041F\u0420\u0418\u0411\u042B\u041B\u042C: ${analytics.netProfit} \u0441", h, 30f)

        line("\u0418\u0437\u043D\u043E\u0441 \u0440\u043E\u043B\u0438\u043A\u043E\u0432", h)
        analytics.rollerUsage.take(25).forEach { (rollerId, count) ->
            if (y > 800f) return@forEach
            line("\u0420\u043E\u043B\u0438\u043A #$rollerId  \u2014  $count \u0432\u044B\u0434\u0430\u0447", p, 16f)
        }

        y = 820f
        line("\u0420\u0430\u0437\u0440\u0430\u0431: \u0420\u0430\u0445\u043C\u0430\u043D\u043E\u0432 \u0421\u044B\u0439\u043C\u044B\u043A\u0431\u0435\u043A | @rahmanov_", small)

        doc.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val file = File(dir, "smena_$dateKey.pdf")
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
        context.startActivity(Intent.createChooser(intent, "\u041E\u0442\u043F\u0440\u0430\u0432\u0438\u0442\u044C \u043E\u0442\u0447\u0451\u0442").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
