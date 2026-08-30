package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.ExpenseRecord
import com.example.data.local.MedicalRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    /**
     * Exports full comprehensive medical table to CSV / Excel format
     */
    fun exportMedicalRecordsToExcel(
        context: Context,
        records: List<MedicalRecord>,
        fileNamePrefix: String = "المحاسب_الطبي_الشامل"
    ): File? {
        return try {
            val fileName = "${fileNamePrefix}_${getTimestamp()}.csv"
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)

            // UTF-8 BOM for Arabic support in Excel
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = "م,اسم المريض,نوع الحالة,مبلغ المعاينة,إجمالي مبلغ المعالجة,الواصل,الباقي,التشخيص والمعالجة,رقم الهاتف,ملاحظات,نسبة الطبيب,نسبة المخبري,راتب الممرضة,خ المواد,الإيجار والنثريات,خرجيات أخرى,التاريخ\n"
            fos.write(header.toByteArray(Charsets.UTF_8))

            records.forEachIndexed { index, r ->
                val line = "${index + 1},\"${escapeCsv(r.patientName)}\",\"${escapeCsv(r.caseType)}\",${r.consultationFee.toInt()},${r.totalTreatmentFee.toInt()},${r.paidAmount.toInt()},${r.remainingAmount.toInt()},\"${escapeCsv(r.diagnosis)}\",\"${escapeCsv(r.patientPhone)}\",\"${escapeCsv(r.notes)}\",${r.doctorCommission.toInt()},${r.labCommission.toInt()},${r.nurseSalaryShare.toInt()},${r.materialExpense.toInt()},${r.rentAndUtilities.toInt()},${r.otherExpenses.toInt()},\"${r.dateString}\"\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }

            // Totals Row
            val totalConsultation = records.sumOf { it.consultationFee }
            val totalTreatment = records.sumOf { it.totalTreatmentFee }
            val totalPaid = records.sumOf { it.paidAmount }
            val totalRemaining = records.sumOf { it.remainingAmount }
            val totalDoc = records.sumOf { it.doctorCommission }
            val totalLab = records.sumOf { it.labCommission }
            val totalNurse = records.sumOf { it.nurseSalaryShare }
            val totalMat = records.sumOf { it.materialExpense }
            val totalRent = records.sumOf { it.rentAndUtilities }
            val totalOther = records.sumOf { it.otherExpenses }

            val totalLine = "الإجمالي الكلي,-,-,${totalConsultation.toInt()},${totalTreatment.toInt()},${totalPaid.toInt()},${totalRemaining.toInt()},-,-,-,${totalDoc.toInt()},${totalLab.toInt()},${totalNurse.toInt()},${totalMat.toInt()},${totalRent.toInt()},${totalOther.toInt()},-\n"
            fos.write(totalLine.toByteArray(Charsets.UTF_8))

            fos.flush()
            fos.close()

            shareFile(context, file, "text/csv", "مشاركة السجل المحاسبي الطبي (Excel)")
            file
        } catch (e: Exception) {
            Toast.makeText(context, "فشل تصدير ملف Excel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Exports Expenses list to Excel CSV
     */
    fun exportExpensesToExcel(
        context: Context,
        expenses: List<ExpenseRecord>,
        fileNamePrefix: String = "سجل_الخرجيات"
    ): File? {
        return try {
            val fileName = "${fileNamePrefix}_${getTimestamp()}.csv"
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)

            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = "م,نوع الخرجية,بيان المصروف,المبلغ (ر.ي),ملاحظات,التاريخ\n"
            fos.write(header.toByteArray(Charsets.UTF_8))

            expenses.forEachIndexed { index, exp ->
                val line = "${index + 1},\"${escapeCsv(exp.category)}\",\"${escapeCsv(exp.title)}\",${exp.amount.toInt()},\"${escapeCsv(exp.notes)}\",\"${exp.dateString}\"\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }

            val total = expenses.sumOf { it.amount }
            val totalLine = "الإجمالي,-,-,${total.toInt()},-,- \n"
            fos.write(totalLine.toByteArray(Charsets.UTF_8))

            fos.flush()
            fos.close()

            shareFile(context, file, "text/csv", "مشاركة سجل المصروفات (Excel)")
            file
        } catch (e: Exception) {
            Toast.makeText(context, "فشل تصدير المصروفات: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Exports Financial Summary Report to Excel CSV
     */
    fun exportFinancialSummaryExcel(
        context: Context,
        periodTitle: String,
        totalRevenue: Double,
        totalPaid: Double,
        totalRemaining: Double,
        totalExpenses: Double,
        netProfit: Double,
        records: List<MedicalRecord>,
        expenses: List<ExpenseRecord>
    ): File? {
        return try {
            val fileName = "تقرير_مالي_${periodTitle.replace(" ", "_")}_${getTimestamp()}.csv"
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)

            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = "التقرير المحاسبي والمالي: $periodTitle\n"
            fos.write(header.toByteArray(Charsets.UTF_8))
            fos.write("البيان,المبلغ (ر.ي)\n".toByteArray(Charsets.UTF_8))
            fos.write("إجمالي الإيرادات الكلية,${totalRevenue.toInt()}\n".toByteArray(Charsets.UTF_8))
            fos.write("إجمالي المبلغ المحصل (الواصل),${totalPaid.toInt()}\n".toByteArray(Charsets.UTF_8))
            fos.write("إجمالي المتبقي (ديون المرضى),${totalRemaining.toInt()}\n".toByteArray(Charsets.UTF_8))
            fos.write("إجمالي المصروفات والخرجيات,${totalExpenses.toInt()}\n".toByteArray(Charsets.UTF_8))
            fos.write("صافي الأرباح,${netProfit.toInt()}\n\n".toByteArray(Charsets.UTF_8))

            fos.write("--- تفاصيل القيود والحالات الطبية ---\n".toByteArray(Charsets.UTF_8))
            val recHeader = "م,اسم المريض,نوع الحالة,المعاينة,المعالجة,الواصل,الباقي,نسبة د/مخ\n"
            fos.write(recHeader.toByteArray(Charsets.UTF_8))
            records.forEachIndexed { i, r ->
                val line = "${i + 1},\"${escapeCsv(r.patientName)}\",\"${escapeCsv(r.caseType)}\",${r.consultationFee.toInt()},${r.totalTreatmentFee.toInt()},${r.paidAmount.toInt()},${r.remainingAmount.toInt()},\"${r.doctorCommission.toInt()}/${r.labCommission.toInt()}\"\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }

            fos.flush()
            fos.close()

            shareFile(context, file, "text/csv", "مشاركة التقرير المالي (Excel)")
            file
        } catch (e: Exception) {
            Toast.makeText(context, "فشل تصدير التقرير المالي: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, chooserTitle)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
