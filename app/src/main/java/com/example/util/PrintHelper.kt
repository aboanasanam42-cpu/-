package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.local.MedicalRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintHelper {

    /**
     * Prints an official Medical Invoice / Receipt via Android PrintManager
     */
    fun printInvoice(
        context: Context,
        patientName: String,
        patientPhone: String,
        records: List<MedicalRecord>
    ) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val invoiceNo = "INV-${System.currentTimeMillis() % 100000}"

        var sumTotal = 0.0
        var sumPaid = 0.0
        var sumRemaining = 0.0

        val itemsHtml = StringBuilder()
        records.forEachIndexed { index, r ->
            val total = r.consultationFee + r.totalTreatmentFee
            sumTotal += total
            sumPaid += r.paidAmount
            sumRemaining += r.remainingAmount

            itemsHtml.append("""
                <tr>
                    <td style="text-align:center;">${index + 1}</td>
                    <td>${r.caseType}</td>
                    <td>${if (r.diagnosis.isNotEmpty()) r.diagnosis else r.notes}</td>
                    <td style="text-align:center;">${total.toInt()}</td>
                    <td style="text-align:center;">${r.paidAmount.toInt()}</td>
                    <td style="text-align:center; color:#b91c1c; font-weight:bold;">${r.remainingAmount.toInt()}</td>
                </tr>
            """.trimIndent())
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        margin: 20px;
                        color: #1e293b;
                        direction: rtl;
                    }
                    .header {
                        text-align: center;
                        border-bottom: 2px solid #0f2b5c;
                        padding-bottom: 12px;
                        margin-bottom: 20px;
                    }
                    .title {
                        font-size: 22px;
                        font-weight: bold;
                        color: #0f2b5c;
                        margin: 0;
                    }
                    .subtitle {
                        font-size: 14px;
                        color: #64748b;
                        margin-top: 4px;
                    }
                    .info-grid {
                        display: flex;
                        justify-content: space-between;
                        background: #f8fafc;
                        border: 1px solid #e2e8f0;
                        border-radius: 6px;
                        padding: 12px;
                        margin-bottom: 20px;
                    }
                    .info-col {
                        font-size: 13px;
                    }
                    .info-col p {
                        margin: 4px 0;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 20px;
                    }
                    th, td {
                        border: 1px solid #cbd5e1;
                        padding: 8px 10px;
                        font-size: 12px;
                    }
                    th {
                        background-color: #0f2b5c;
                        color: white;
                    }
                    tr:nth-child(even) {
                        background-color: #f8fafc;
                    }
                    .totals-box {
                        margin-top: 15px;
                        border: 2px solid #0f2b5c;
                        border-radius: 6px;
                        padding: 12px;
                        background: #fffbeb;
                    }
                    .totals-row {
                        display: flex;
                        justify-content: space-between;
                        font-size: 14px;
                        font-weight: bold;
                        margin: 6px 0;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 30px;
                        font-size: 11px;
                        color: #64748b;
                        border-top: 1px solid #e2e8f0;
                        padding-top: 10px;
                    }
                    .signatures {
                        display: flex;
                        justify-content: space-between;
                        margin-top: 40px;
                        font-size: 13px;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1 class="title">التطبيق المحاسبي الطبي الشامل</h1>
                    <div class="subtitle">فاتورة وسند محاسبي علاجي</div>
                </div>

                <div class="info-grid">
                    <div class="info-col">
                        <p><strong>اسم المريض:</strong> $patientName</p>
                        <p><strong>رقم الهاتف:</strong> ${if (patientPhone.isNotEmpty()) patientPhone else "غير مسجل"}</p>
                    </div>
                    <div class="info-col" style="text-align: left;">
                        <p><strong>رقم الفاتورة:</strong> $invoiceNo</p>
                        <p><strong>التاريخ:</strong> $currentDate</p>
                    </div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th style="width: 30px;">م</th>
                            <th>الخدمة / نوع الحالة</th>
                            <th>التشخيص / الملاحظات</th>
                            <th style="width: 70px;">الإجمالي</th>
                            <th style="width: 70px;">الواصل</th>
                            <th style="width: 70px;">الباقي</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsHtml
                    </tbody>
                </table>

                <div class="totals-box">
                    <div class="totals-row">
                        <span>إجمالي التكلفة الإجمالية:</span>
                        <span>${sumTotal.toInt()} ر.ي</span>
                    </div>
                    <div class="totals-row" style="color: #047857;">
                        <span>إجمالي المبلغ الواصل (المدفوع):</span>
                        <span>${sumPaid.toInt()} ر.ي</span>
                    </div>
                    <div class="totals-row" style="color: #b91c1c;">
                        <span>المبلغ المتبقي (الدين):</span>
                        <span>${sumRemaining.toInt()} ر.ي</span>
                    </div>
                </div>

                <div class="signatures">
                    <div>توقيع وختم العيادة: ____________________</div>
                    <div>توقيع المستلم: ____________________</div>
                </div>

                <div class="footer">
                    مشارك فكرة التطبيق أ/ محمد عبدالقوي سعيد الرميمة | تصميم وبرمجة الدكتور / مالك الرميمة | هاتف: 771134103
                </div>
            </body>
            </html>
        """.trimIndent()

        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val printAdapter = webView.createPrintDocumentAdapter("طباعة_فاتورة_$patientName")
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build()
                        printManager.print("فاتورة_$patientName", printAdapter, printAttributes)
                    } else {
                        Toast.makeText(context, "خدمة الطباعة غير متاحة في هذا الجهاز", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ أثناء الطباعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
