package com.example.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.example.data.local.ExpenseRecord
import com.example.data.local.MedicalRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private fun getFormattedTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }

    /**
     * Generates a PDF matching the exact "المحاسب الطبي الشامل" layout
     */
    fun exportComprehensiveMedicalTablePdf(
        context: Context,
        records: List<MedicalRecord>,
        title: String = "المحاسب الطبي الشامل"
    ): File? {
        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width (points)
        val pageHeight = 842 // A4 standard height (points)

        return try {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val borderPaint = Paint().apply {
                color = Color.DKGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#0F2B5C")
                style = Paint.Style.FILL
            }
            val thBgPaint = Paint().apply {
                color = Color.parseColor("#E8F0FE")
                style = Paint.Style.FILL
            }

            // Draw Header Banner
            canvas.drawRect(20f, 20f, (pageWidth - 20).toFloat(), 70f, headerBgPaint)
            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("التطبيق المحاسبي الطبي الشامل", pageWidth / 2f, 50f, paint)

            paint.color = Color.DKGRAY
            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("تاريخ التقرير: ${getCurrentDate()}", (pageWidth - 25).toFloat(), 85f, paint)

            // Table setup
            var currentY = 105f
            val tableLeft = 20f
            val tableRight = (pageWidth - 20).toFloat()
            val colWidths = floatArrayOf(25f, 95f, 65f, 50f, 50f, 45f, 45f, 90f, 55f, 35f)
            // Total width = 555f (595 - 40)
            val headers = arrayOf("م", "اسم المريض", "نوع الحالة", "المعاينة", "المعالجة", "الواصل", "الباقي", "التشخيص/المعالجة", "نسبة د/مخ", "التاريخ")

            // Draw Table Header
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + 24f, thBgPaint)
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + 24f, borderPaint)

            paint.color = Color.parseColor("#0F2B5C")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER

            var currentX = tableLeft
            for (i in headers.indices) {
                val nextX = currentX + colWidths[i]
                canvas.drawLine(nextX, currentY, nextX, currentY + 24f, borderPaint)
                canvas.drawText(headers[i], currentX + (colWidths[i] / 2f), currentY + 16f, paint)
                currentX = nextX
            }

            currentY += 24f

            // Draw Table Rows
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8.5f
            paint.color = Color.BLACK

            var totalConsultation = 0.0
            var totalTreatment = 0.0
            var totalPaid = 0.0
            var totalRemaining = 0.0

            val maxRows = minOf(records.size, 20) // Fit on single page cleanly
            for (idx in 0 until maxRows) {
                val r = records[idx]
                totalConsultation += r.consultationFee
                totalTreatment += r.totalTreatmentFee
                totalPaid += r.paidAmount
                totalRemaining += r.remainingAmount

                val rowHeight = 22f
                // alternate row tint
                if (idx % 2 == 1) {
                    val altPaint = Paint().apply { color = Color.parseColor("#F9FBFD"); style = Paint.Style.FILL }
                    canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, altPaint)
                }

                canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, borderPaint)

                currentX = tableLeft
                val values = arrayOf(
                    (idx + 1).toString(),
                    truncateText(r.patientName, 16),
                    truncateText(r.caseType, 12),
                    r.consultationFee.toInt().toString(),
                    r.totalTreatmentFee.toInt().toString(),
                    r.paidAmount.toInt().toString(),
                    r.remainingAmount.toInt().toString(),
                    truncateText(if (r.diagnosis.isNotEmpty()) r.diagnosis else "-", 16),
                    "${r.doctorCommission.toInt()}/${r.labCommission.toInt()}",
                    truncateText(r.dateString, 8)
                )

                for (i in values.indices) {
                    val nextX = currentX + colWidths[i]
                    canvas.drawLine(nextX, currentY, nextX, currentY + rowHeight, borderPaint)
                    canvas.drawText(values[i], currentX + (colWidths[i] / 2f), currentY + 15f, paint)
                    currentX = nextX
                }

                currentY += rowHeight
            }

            // Totals Row
            val totalRowHeight = 24f
            val totalBgPaint = Paint().apply { color = Color.parseColor("#E2E8F0"); style = Paint.Style.FILL }
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + totalRowHeight, totalBgPaint)
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + totalRowHeight, borderPaint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#0F2B5C")

            canvas.drawText("الإجمالي الكلي", tableLeft + 90f, currentY + 16f, paint)
            canvas.drawText(totalConsultation.toInt().toString(), tableLeft + 25f + 95f + 65f + 25f, currentY + 16f, paint)
            canvas.drawText(totalTreatment.toInt().toString(), tableLeft + 25f + 95f + 65f + 50f + 25f, currentY + 16f, paint)
            canvas.drawText(totalPaid.toInt().toString(), tableLeft + 25f + 95f + 65f + 100f + 22f, currentY + 16f, paint)
            canvas.drawText(totalRemaining.toInt().toString(), tableLeft + 25f + 95f + 65f + 145f + 22f, currentY + 16f, paint)

            currentY += totalRowHeight + 25f

            // Accounting Expense & Net Summary Box
            val summaryPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 90f, 8f, 8f, summaryPaint)
            canvas.drawRoundRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 90f, 8f, 8f, borderPaint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 11f
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.parseColor("#0F2B5C")
            canvas.drawText("ملخص الحسابات والخرجيات:", (pageWidth - 35).toFloat(), currentY + 22f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9.5f
            paint.color = Color.DKGRAY

            val netProfit = totalPaid - (records.sumOf { it.materialExpense + it.rentAndUtilities + it.otherExpenses + it.nurseSalaryShare + it.doctorCommission + it.labCommission })

            canvas.drawText("• إجمالي الإيراد الواصل: ${totalPaid.toInt()} ر.ي", (pageWidth - 35).toFloat(), currentY + 42f, paint)
            canvas.drawText("• إجمالي المتبقي (ديون المرضى): ${totalRemaining.toInt()} ر.ي", (pageWidth - 35).toFloat(), currentY + 60f, paint)
            canvas.drawText("• صافي الأرباح التقديري: ${netProfit.toInt()} ر.ي", (pageWidth - 35).toFloat(), currentY + 78f, paint)

            canvas.drawText("• إجمالي نسب الأطباء: ${records.sumOf { it.doctorCommission }.toInt()} ر.ي", (pageWidth / 2f) + 30f, currentY + 42f, paint)
            canvas.drawText("• إجمالي نسب المختبرات: ${records.sumOf { it.labCommission }.toInt()} ر.ي", (pageWidth / 2f) + 30f, currentY + 60f, paint)
            canvas.drawText("• المصروفات والخرجيات: ${records.sumOf { it.materialExpense + it.rentAndUtilities + it.otherExpenses }.toInt()} ر.ي", (pageWidth / 2f) + 30f, currentY + 78f, paint)

            // Footer credits
            val footerY = (pageHeight - 35).toFloat()
            paint.textSize = 8.5f
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.GRAY
            canvas.drawText("مشارك فكرة التطبيق أ/ محمد عبدالقوي سعيد الرميمة | تصميم وبرمجة الدكتور / مالك الرميمة | هاتف: 771134103", pageWidth / 2f, footerY, paint)

            document.finishPage(page)

            val fileName = "المحاسب_الطبي_الشامل_${getFormattedTimestamp()}.pdf"
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()

            ExcelExporter.shareFile(context, file, "application/pdf", "مشاركة ملف PDF: $title")
            file
        } catch (e: Exception) {
            document.close()
            Toast.makeText(context, "فشل إنشاء ملف PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Generates a Single Patient Detailed Invoice & Account Statement PDF
     */
    fun exportPatientInvoicePdf(
        context: Context,
        patientName: String,
        patientPhone: String,
        records: List<MedicalRecord>
    ): File? {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        return try {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val borderPaint = Paint().apply {
                color = Color.DKGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            // Header Banner
            val headerBg = Paint().apply { color = Color.parseColor("#0F2B5C"); style = Paint.Style.FILL }
            canvas.drawRect(20f, 20f, (pageWidth - 20).toFloat(), 80f, headerBg)

            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("فاتورة وكشف حساب مريض", pageWidth / 2f, 48f, paint)

            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("المحاسب الطبي الشامل", pageWidth / 2f, 68f, paint)

            // Patient Info Box
            val infoBoxY = 95f
            val infoBg = Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL }
            canvas.drawRoundRect(20f, infoBoxY, (pageWidth - 20).toFloat(), infoBoxY + 50f, 6f, 6f, infoBg)
            canvas.drawRoundRect(20f, infoBoxY, (pageWidth - 20).toFloat(), infoBoxY + 50f, 6f, 6f, borderPaint)

            paint.color = Color.parseColor("#0F2B5C")
            paint.textSize = 11f
            paint.textAlign = Paint.Align.RIGHT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("اسم المريض: $patientName", (pageWidth - 35).toFloat(), infoBoxY + 22f, paint)
            canvas.drawText("رقم الهاتف: ${if (patientPhone.isNotEmpty()) patientPhone else "غير مسجل"}", (pageWidth - 35).toFloat(), infoBoxY + 40f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("تاريخ الطباعة: ${getCurrentDate()}", 35f, infoBoxY + 22f, paint)
            canvas.drawText("رقم الفاتورة: INV-${System.currentTimeMillis() % 100000}", 35f, infoBoxY + 40f, paint)

            // Items Table
            var currentY = 160f
            val tableLeft = 20f
            val tableRight = (pageWidth - 20).toFloat()
            val colWidths = floatArrayOf(30f, 130f, 155f, 80f, 80f, 80f)
            val headers = arrayOf("م", "الخدمة / الحالة", "التشخيص / الملاحظات", "المبلغ الكلي", "الواصل", "المتبقي")

            val thBg = Paint().apply { color = Color.parseColor("#E0E7FF"); style = Paint.Style.FILL }
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + 24f, thBg)
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + 24f, borderPaint)

            paint.color = Color.parseColor("#1E1B4B")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER

            var currentX = tableLeft
            for (i in headers.indices) {
                val nextX = currentX + colWidths[i]
                canvas.drawLine(nextX, currentY, nextX, currentY + 24f, borderPaint)
                canvas.drawText(headers[i], currentX + (colWidths[i] / 2f), currentY + 16f, paint)
                currentX = nextX
            }

            currentY += 24f
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9.5f
            paint.color = Color.BLACK

            var sumTotal = 0.0
            var sumPaid = 0.0
            var sumRemaining = 0.0

            records.forEachIndexed { index, r ->
                val totalCost = r.consultationFee + r.totalTreatmentFee
                sumTotal += totalCost
                sumPaid += r.paidAmount
                sumRemaining += r.remainingAmount

                val rowH = 24f
                if (index % 2 == 1) {
                    val rowBg = Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL }
                    canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowH, rowBg)
                }
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowH, borderPaint)

                currentX = tableLeft
                val rowVals = arrayOf(
                    (index + 1).toString(),
                    truncateText(r.caseType, 20),
                    truncateText(if (r.diagnosis.isNotEmpty()) r.diagnosis else r.notes, 24),
                    totalCost.toInt().toString(),
                    r.paidAmount.toInt().toString(),
                    r.remainingAmount.toInt().toString()
                )

                for (i in rowVals.indices) {
                    val nextX = currentX + colWidths[i]
                    canvas.drawLine(nextX, currentY, nextX, currentY + rowH, borderPaint)
                    canvas.drawText(rowVals[i], currentX + (colWidths[i] / 2f), currentY + 16f, paint)
                    currentX = nextX
                }
                currentY += rowH
            }

            // Summary row
            val summaryRowH = 26f
            val sumBg = Paint().apply { color = Color.parseColor("#FEF3C7"); style = Paint.Style.FILL }
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + summaryRowH, sumBg)
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + summaryRowH, borderPaint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10.5f
            paint.color = Color.parseColor("#78350F")

            canvas.drawText("الإجمالي المستحق", tableLeft + 150f, currentY + 17f, paint)
            canvas.drawText(sumTotal.toInt().toString(), tableLeft + 30f + 130f + 155f + 40f, currentY + 17f, paint)
            canvas.drawText(sumPaid.toInt().toString(), tableLeft + 30f + 130f + 155f + 80f + 40f, currentY + 17f, paint)
            canvas.drawText(sumRemaining.toInt().toString(), tableLeft + 30f + 130f + 155f + 160f + 40f, currentY + 17f, paint)

            currentY += summaryRowH + 30f

            // Stamps and signatures
            paint.textSize = 10f
            paint.color = Color.DKGRAY
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("توقيع المحاسب / الطبيب: ..............................", (pageWidth - 35).toFloat(), currentY, paint)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("الختم الرسمي: ..............................", 35f, currentY, paint)

            // Footer
            val footerY = (pageHeight - 35).toFloat()
            paint.textSize = 8.5f
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.GRAY
            canvas.drawText("مشارك فكرة التطبيق أ/ محمد عبدالقوي سعيد الرميمة | برمجة د/ مالك الرميمة | هاتف: 771134103", pageWidth / 2f, footerY, paint)

            document.finishPage(page)

            val fileName = "فاتورة_${patientName.replace(" ", "_")}_${getFormattedTimestamp()}.pdf"
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()

            ExcelExporter.shareFile(context, file, "application/pdf", "مشاركة فاتورة: $patientName")
            file
        } catch (e: Exception) {
            document.close()
            Toast.makeText(context, "فشل إنشاء الفاتورة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun truncateText(text: String, maxChars: Int): String {
        return if (text.length > maxChars) text.take(maxChars - 2) + ".." else text
    }
}
