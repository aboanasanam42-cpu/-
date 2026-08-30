package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.*
import com.example.data.repository.MedicalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Silent automatic local backup. It never opens a chooser or interrupts the user. */
object AutoBackupHelper {
    private const val BACKUP_DIR = "MedicalAccounting/Backups"

    suspend fun saveAll(context: Context, repository: MedicalRepository) = withContext(Dispatchers.IO) {
        runCatching {
            val records = repository.allMedicalRecords.first()
            val patients = repository.allPatients.first()
            val labs = repository.allLabRecords.first()
            val rads = repository.allRadiologyRecords.first()
            val pharms = repository.allPharmacyRecords.first()
            val expenses = repository.allExpenses.first()
            val messages = repository.allScheduledMessages.first()
            val dir = File(context.getExternalFilesDir(null), BACKUP_DIR).apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            saveJson(File(dir, "نسخة_احتياطية_$stamp.json"), records, patients, labs, rads, pharms, expenses, messages)
            savePdf(File(dir, "نسخة_احتياطية_$stamp.pdf"), records)
            saveXlsx(File(dir, "نسخة_احتياطية_$stamp.xlsx"), records)
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(60)?.forEach { it.delete() }
        }
    }

    fun backupDirectory(context: Context): File = File(context.getExternalFilesDir(null), BACKUP_DIR).apply { mkdirs() }

    private fun saveJson(file: File, records: List<MedicalRecord>, patients: List<Patient>, labs: List<LabRecord>, rads: List<RadiologyRecord>, pharms: List<PharmacyRecord>, expenses: List<ExpenseRecord>, messages: List<ScheduledMessage>) {
        val root = JSONObject().apply {
            put("version", 2)
            put("appName", "المحاسب الطبي الشامل")
            put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("medical_records", JSONArray().apply { records.forEach { r -> put(JSONObject().apply { put("id",r.id); put("patientName",r.patientName); put("patientPhone",r.patientPhone); put("caseType",r.caseType); put("consultationFee",r.consultationFee); put("totalTreatmentFee",r.totalTreatmentFee); put("paidAmount",r.paidAmount); put("remainingAmount",r.remainingAmount); put("diagnosis",r.diagnosis); put("notes",r.notes); put("doctorCommission",r.doctorCommission); put("labCommission",r.labCommission); put("nurseSalaryShare",r.nurseSalaryShare); put("materialExpense",r.materialExpense); put("rentAndUtilities",r.rentAndUtilities); put("otherExpenses",r.otherExpenses); put("department",r.department); put("dateString",r.dateString); put("timestamp",r.timestamp) }) } })
            put("patients", JSONArray().apply { patients.forEach { p -> put(JSONObject().apply { put("id",p.id); put("name",p.name); put("phone",p.phone); put("age",p.age); put("gender",p.gender); put("address",p.address); put("bloodGroup",p.bloodGroup); put("medicalHistory",p.medicalHistory); put("createdAt",p.createdAt) }) } })
            put("lab_records", JSONArray().apply { labs.forEach { l -> put(JSONObject().apply { put("id",l.id); put("patientName",l.patientName); put("patientPhone",l.patientPhone); put("testName",l.testName); put("testCost",l.testCost); put("labCommission",l.labCommission); put("paidAmount",l.paidAmount); put("remainingAmount",l.remainingAmount); put("result",l.result); put("notes",l.notes); put("dateString",l.dateString); put("timestamp",l.timestamp) }) } })
            put("radiology_records", JSONArray().apply { rads.forEach { r -> put(JSONObject().apply { put("id",r.id); put("patientName",r.patientName); put("patientPhone",r.patientPhone); put("deviceName",r.deviceName); put("cost",r.cost); put("paidAmount",r.paidAmount); put("remainingAmount",r.remainingAmount); put("operatorShare",r.operatorShare); put("reportDetails",r.reportDetails); put("notes",r.notes); put("dateString",r.dateString); put("timestamp",r.timestamp) }) } })
            put("pharmacy_records", JSONArray().apply { pharms.forEach { p -> put(JSONObject().apply { put("id",p.id); put("patientName",p.patientName); put("patientPhone",p.patientPhone); put("medicineName",p.medicineName); put("quantity",p.quantity); put("unitPrice",p.unitPrice); put("totalAmount",p.totalAmount); put("paidAmount",p.paidAmount); put("remainingAmount",p.remainingAmount); put("costPrice",p.costPrice); put("notes",p.notes); put("dateString",p.dateString); put("timestamp",p.timestamp) }) } })
            put("expenses", JSONArray().apply { expenses.forEach { e -> put(JSONObject().apply { put("id",e.id); put("category",e.category); put("title",e.title); put("amount",e.amount); put("notes",e.notes); put("dateString",e.dateString); put("timestamp",e.timestamp) }) } })
            put("scheduled_messages", JSONArray().apply { messages.forEach { m -> put(JSONObject().apply { put("id",m.id); put("recipientName",m.recipientName); put("recipientPhone",m.recipientPhone); put("messageText",m.messageText); put("scheduledDate",m.scheduledDate); put("scheduledTime",m.scheduledTime); put("messageType",m.messageType); put("channel",m.channel); put("status",m.status); put("createdAt",m.createdAt) }) } })
        }
        FileOutputStream(file).use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
    }

    private fun savePdf(file: File, records: List<MedicalRecord>) {
        val doc = PdfDocument()
        try {
            val pageW = 842; val pageH = 595
            var pageNo = 1
            var index = 0
            while (index < records.size || (records.isEmpty() && pageNo == 1)) {
                val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo++).create())
                val c = page.canvas
                val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 1f }
                val header = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15,43,92); style = Paint.Style.FILL }
                c.drawRect(20f,20f,(pageW-20).toFloat(),65f,header)
                val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize=18f; typeface=Typeface.DEFAULT_BOLD; textAlign=Paint.Align.CENTER }
                c.drawText("المحاسب الطبي الشامل - نسخة احتياطية", pageW/2f,49f,text)
                val cols = floatArrayOf(35f,120f,100f,80f,80f,70f,70f,160f)
                val heads = listOf("م","اسم المريض","نوع الحالة","المعاينة","المعالجة","الواصل","الباقي","التشخيص / المعالجة")
                var y=82f; var x=20f
                val th=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.rgb(232,240,254); style=Paint.Style.FILL }
                c.drawRect(20f,y,822f,y+28f,th); c.drawRect(20f,y,822f,y+28f,border)
                heads.forEachIndexed { i,h -> c.drawLine(x+cols[i],y,x+cols[i],y+28f,border); text.color=Color.rgb(15,43,92); text.textSize=9f; text.typeface=Typeface.DEFAULT_BOLD; c.drawText(h,x+cols[i]/2f,y+19f,text); x+=cols[i] }
                y+=28f
                val body=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.BLACK; textSize=8.5f; textAlign=Paint.Align.CENTER }
                val maxRows=13
                repeat(maxRows) {
                    if (index >= records.size) return@repeat
                    val r=records[index++]
                    val vals=listOf((index).toString(),r.patientName,r.caseType,r.consultationFee.toInt().toString(),r.totalTreatmentFee.toInt().toString(),r.paidAmount.toInt().toString(),r.remainingAmount.toInt().toString(),r.diagnosis.ifBlank{"-"})
                    x=20f; c.drawRect(20f,y,822f,y+30f,border)
                    vals.forEachIndexed { i,v -> c.drawLine(x+cols[i],y,x+cols[i],y+30f,border); c.drawText(if(v.length>24)v.take(22)+".." else v,x+cols[i]/2f,y+19f,body); x+=cols[i] }
                    y+=30f
                }
                doc.finishPage(page)
                if (index >= records.size) break
            }
            FileOutputStream(file).use { doc.writeTo(it) }
        } finally { doc.close() }
    }

    private fun saveXlsx(file: File, records: List<MedicalRecord>) {
        val rows = mutableListOf<List<String>>()
        rows += listOf("م","اسم المريض","نوع الحالة","مبلغ المعاينة","إجمالي مبلغ المعالجة","الواصل","الباقي","التشخيص / المعالجة","رقم الهاتف","ملاحظات","نسبة الطبيب","نسبة المخبري","راتب الممرضة","خ المواد","الإيجار والنثريات","خرجيات أخرى","التاريخ")
        records.forEachIndexed { i,r -> rows += listOf((i+1).toString(),r.patientName,r.caseType,r.consultationFee.toString(),r.totalTreatmentFee.toString(),r.paidAmount.toString(),r.remainingAmount.toString(),r.diagnosis,r.patientPhone,r.notes,r.doctorCommission.toString(),r.labCommission.toString(),r.nurseSalaryShare.toString(),r.materialExpense.toString(),r.rentAndUtilities.toString(),r.otherExpenses.toString(),r.dateString) }
        fun colName(n0:Int):String { var n=n0; val b=StringBuilder(); while(n>0){ val r=(n-1)%26; b.insert(0,('A'.code+r).toChar()); n=(n-1)/26 }; return b.toString() }
        fun esc(v:String)=v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
        val data=rows.mapIndexed { ri,row -> val r=ri+1; "<row r=\"$r\">"+row.mapIndexed{ci,v->"<c r=\"${colName(ci+1)}$r\" t=\"inlineStr\"><is><t>${esc(v)}</t></is></c>"}.joinToString("")+"</row>" }.joinToString("")
        val sheet="""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><dimension ref="A1:${colName(rows.first().size)}${rows.size}"/><sheetData>$data</sheetData></worksheet>"""
        val workbook="""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="المحاسب الطبي الشامل" sheetId="1" r:id="rId1"/></sheets></workbook>"""
        val rootRels="""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
        val bookRels="""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>"""
        val types="""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>"""
        java.util.zip.ZipOutputStream(FileOutputStream(file)).use { z -> fun put(p:String,s:String){z.putNextEntry(java.util.zip.ZipEntry(p));z.write(s.toByteArray(Charsets.UTF_8));z.closeEntry()}; put("[Content_Types].xml",types);put("_rels/.rels",rootRels);put("xl/workbook.xml",workbook);put("xl/_rels/workbook.xml.rels",bookRels);put("xl/worksheets/sheet1.xml",sheet) }
    }
}
