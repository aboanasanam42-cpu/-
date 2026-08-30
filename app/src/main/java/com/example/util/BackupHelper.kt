package com.example.util

import android.content.Context
import android.widget.Toast
import com.example.data.local.*
import com.example.data.repository.MedicalRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    fun exportBackup(
        context: Context,
        records: List<MedicalRecord>,
        patients: List<Patient>,
        labRecords: List<LabRecord>,
        radiologyRecords: List<RadiologyRecord>,
        pharmacyRecords: List<PharmacyRecord>,
        expenses: List<ExpenseRecord>,
        scheduledMessages: List<ScheduledMessage>
    ): File? {
        return try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("appName", "المحاسب الطبي الشامل")
            root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            // Medical Records Array
            val recArray = JSONArray()
            records.forEach { r ->
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("patientName", r.patientName)
                    put("patientPhone", r.patientPhone)
                    put("caseType", r.caseType)
                    put("consultationFee", r.consultationFee)
                    put("totalTreatmentFee", r.totalTreatmentFee)
                    put("paidAmount", r.paidAmount)
                    put("remainingAmount", r.remainingAmount)
                    put("diagnosis", r.diagnosis)
                    put("notes", r.notes)
                    put("doctorCommission", r.doctorCommission)
                    put("labCommission", r.labCommission)
                    put("nurseSalaryShare", r.nurseSalaryShare)
                    put("materialExpense", r.materialExpense)
                    put("rentAndUtilities", r.rentAndUtilities)
                    put("otherExpenses", r.otherExpenses)
                    put("department", r.department)
                    put("dateString", r.dateString)
                    put("timestamp", r.timestamp)
                }
                recArray.put(obj)
            }
            root.put("medical_records", recArray)

            // Patients Array
            val patArray = JSONArray()
            patients.forEach { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("phone", p.phone)
                    put("age", p.age)
                    put("gender", p.gender)
                    put("address", p.address)
                    put("bloodGroup", p.bloodGroup)
                    put("medicalHistory", p.medicalHistory)
                    put("createdAt", p.createdAt)
                }
                patArray.put(obj)
            }
            root.put("patients", patArray)

            // Lab Array
            val labArray = JSONArray()
            labRecords.forEach { l ->
                val obj = JSONObject().apply {
                    put("id", l.id)
                    put("patientName", l.patientName)
                    put("patientPhone", l.patientPhone)
                    put("testName", l.testName)
                    put("testCost", l.testCost)
                    put("labCommission", l.labCommission)
                    put("paidAmount", l.paidAmount)
                    put("remainingAmount", l.remainingAmount)
                    put("result", l.result)
                    put("notes", l.notes)
                    put("dateString", l.dateString)
                }
                labArray.put(obj)
            }
            root.put("lab_records", labArray)

            // Radiology Array
            val radArray = JSONArray()
            radiologyRecords.forEach { rad ->
                val obj = JSONObject().apply {
                    put("id", rad.id)
                    put("patientName", rad.patientName)
                    put("patientPhone", rad.patientPhone)
                    put("deviceName", rad.deviceName)
                    put("cost", rad.cost)
                    put("paidAmount", rad.paidAmount)
                    put("remainingAmount", rad.remainingAmount)
                    put("operatorShare", rad.operatorShare)
                    put("reportDetails", rad.reportDetails)
                    put("notes", rad.notes)
                    put("dateString", rad.dateString)
                }
                radArray.put(obj)
            }
            root.put("radiology_records", radArray)

            // Pharmacy Array
            val pharmArray = JSONArray()
            pharmacyRecords.forEach { ph ->
                val obj = JSONObject().apply {
                    put("id", ph.id)
                    put("patientName", ph.patientName)
                    put("patientPhone", ph.patientPhone)
                    put("medicineName", ph.medicineName)
                    put("quantity", ph.quantity)
                    put("unitPrice", ph.unitPrice)
                    put("totalAmount", ph.totalAmount)
                    put("paidAmount", ph.paidAmount)
                    put("remainingAmount", ph.remainingAmount)
                    put("costPrice", ph.costPrice)
                    put("notes", ph.notes)
                    put("dateString", ph.dateString)
                }
                pharmArray.put(obj)
            }
            root.put("pharmacy_records", pharmArray)

            // Expenses Array
            val expArray = JSONArray()
            expenses.forEach { e ->
                val obj = JSONObject().apply {
                    put("id", e.id)
                    put("category", e.category)
                    put("title", e.title)
                    put("amount", e.amount)
                    put("notes", e.notes)
                    put("dateString", e.dateString)
                }
                expArray.put(obj)
            }
            root.put("expenses", expArray)

            // Scheduled Messages Array
            val msgArray = JSONArray()
            scheduledMessages.forEach { m ->
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("recipientName", m.recipientName)
                    put("recipientPhone", m.recipientPhone)
                    put("messageText", m.messageText)
                    put("scheduledDate", m.scheduledDate)
                    put("scheduledTime", m.scheduledTime)
                    put("messageType", m.messageType)
                    put("channel", m.channel)
                    put("status", m.status)
                }
                msgArray.put(obj)
            }
            root.put("scheduled_messages", msgArray)

            val fileName = "نسخة_احتياطية_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)
            fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.close()

            ExcelExporter.shareFile(context, file, "application/json", "حفظ ومشاركة النسخة الاحتياطية")
            file
        } catch (e: Exception) {
            Toast.makeText(context, "فشل إنشاء النسخة الاحتياطية: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    suspend fun seedInitialDataIfEmpty(repository: MedicalRepository) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val samplePatients = listOf(
            Patient(name = "أحمد علي المحمدي", phone = "771234567", age = "34", gender = "ذكر", address = "صنعاء - التحرير"),
            Patient(name = "فاطمة صالح القحطاني", phone = "777891234", age = "28", gender = "أنثى", address = "تعز - الحوبان"),
            Patient(name = "صالح ناصر اليافعي", phone = "733456789", age = "45", gender = "ذكر", address = "عدن - المعلا"),
            Patient(name = "مريم عبدالله الريمي", phone = "711987654", age = "22", gender = "أنثى", address = "إب - الدائري")
        )

        samplePatients.forEach { repository.insertPatient(it) }

        val sampleRecords = listOf(
            MedicalRecord(
                patientName = "أحمد علي المحمدي",
                patientPhone = "771234567",
                caseType = "كشف باطنية",
                consultationFee = 3000.0,
                totalTreatmentFee = 12000.0,
                paidAmount = 15000.0,
                remainingAmount = 0.0,
                diagnosis = "التهاب المعدة الحاد وتشنج القولون",
                notes = "صرف علاج وتحديد موعد مراجعة بعد أسبوع",
                doctorCommission = 4500.0,
                labCommission = 2000.0,
                nurseSalaryShare = 500.0,
                materialExpense = 1000.0,
                rentAndUtilities = 1500.0,
                otherExpenses = 500.0,
                department = "الاستقبال",
                dateString = today
            ),
            MedicalRecord(
                patientName = "فاطمة صالح القحطاني",
                patientPhone = "777891234",
                caseType = "معاينة نساء وولادة",
                consultationFee = 4000.0,
                totalTreatmentFee = 16000.0,
                paidAmount = 14000.0,
                remainingAmount = 6000.0,
                diagnosis = "فحص سونار ومتابعة حمل بالشهر الرابع",
                notes = "متبقي 6,000 ريال تسدد في الزيارة القادمة",
                doctorCommission = 6000.0,
                labCommission = 2500.0,
                nurseSalaryShare = 800.0,
                materialExpense = 1200.0,
                rentAndUtilities = 2000.0,
                otherExpenses = 0.0,
                department = "الطبيب",
                dateString = today
            ),
            MedicalRecord(
                patientName = "صالح ناصر اليافعي",
                patientPhone = "733456789",
                caseType = "فحص دم وسكر شامل",
                consultationFee = 2000.0,
                totalTreatmentFee = 8500.0,
                paidAmount = 10500.0,
                remainingAmount = 0.0,
                diagnosis = "فحص سكر تراكمي HbA1c ودهون كاملة",
                notes = "النتائج سليمة ومتابعة دورية",
                doctorCommission = 2000.0,
                labCommission = 3500.0,
                nurseSalaryShare = 500.0,
                materialExpense = 1500.0,
                rentAndUtilities = 1000.0,
                otherExpenses = 300.0,
                department = "المختبرات",
                dateString = today
            )
        )

        sampleRecords.forEach { repository.insertMedicalRecord(it) }

        val sampleLabs = listOf(
            LabRecord(
                patientName = "أحمد علي المحمدي",
                patientPhone = "771234567",
                testName = "فحص جرثومة المعدة H.Pylori",
                testCost = 4000.0,
                labCommission = 1600.0,
                paidAmount = 4000.0,
                remainingAmount = 0.0,
                result = "Positive (+)",
                notes = "تحتاج كورس علاج ثلاثي",
                dateString = today
            ),
            LabRecord(
                patientName = "صالح ناصر اليافعي",
                patientPhone = "733456789",
                testName = "فحص السكر التراكمي HbA1c",
                testCost = 4500.0,
                labCommission = 1800.0,
                paidAmount = 4500.0,
                remainingAmount = 0.0,
                result = "5.8 % (طبيعي)",
                notes = "حالة جيدة",
                dateString = today
            )
        )
        sampleLabs.forEach { repository.insertLabRecord(it) }

        val sampleRadiology = listOf(
            RadiologyRecord(
                patientName = "فاطمة صالح القحطاني",
                patientPhone = "777891234",
                deviceName = "جهاز السونار والموجات فوق الصوتية (Ultrasound)",
                cost = 6000.0,
                paidAmount = 6000.0,
                remainingAmount = 0.0,
                operatorShare = 2000.0,
                reportDetails = "فحص نمو الجنين سليم ووضع المشيمة طبيعي",
                dateString = today
            ),
            RadiologyRecord(
                patientName = "مريم عبدالله الريمي",
                patientPhone = "711987654",
                deviceName = "أشعة سينية للصدر (Chest X-Ray)",
                cost = 5000.0,
                paidAmount = 5000.0,
                remainingAmount = 0.0,
                operatorShare = 1500.0,
                reportDetails = "لا توجد التهابات رئوية أو ارتشاح",
                dateString = today
            )
        )
        sampleRadiology.forEach { repository.insertRadiologyRecord(it) }

        val samplePharmacy = listOf(
            PharmacyRecord(
                patientName = "أحمد علي المحمدي",
                patientPhone = "771234567",
                medicineName = "Nexium 40mg + Amoxicillin 1g",
                quantity = 2,
                unitPrice = 4500.0,
                totalAmount = 9000.0,
                paidAmount = 9000.0,
                remainingAmount = 0.0,
                costPrice = 7000.0,
                notes = "تناول قبل الطعام بنصف ساعة",
                dateString = today
            )
        )
        samplePharmacy.forEach { repository.insertPharmacyRecord(it) }

        val sampleExpenses = listOf(
            ExpenseRecord(category = "خ المواد", title = "شراء مستلزمات طبية وشاش وقفازات", amount = 8500.0, dateString = today),
            ExpenseRecord(category = "الإيجار", title = "دفعة إيجار مقر العيادة", amount = 30000.0, dateString = today),
            ExpenseRecord(category = "النثريات", title = "فاتورة كهرباء وإنترنت ونظافة", amount = 6000.0, dateString = today),
            ExpenseRecord(category = "راتب الممرضة", title = "سلفة راتب تمريض أسبوعي", amount = 15000.0, dateString = today),
            ExpenseRecord(category = "خرجيات أخرى", title = "صيانة طابعة وجهاز قياس الضغط", amount = 3500.0, dateString = today)
        )
        sampleExpenses.forEach { repository.insertExpense(it) }

        val sampleMessages = listOf(
            ScheduledMessage(
                recipientName = "فاطمة صالح القحطاني",
                recipientPhone = "777891234",
                messageText = "عزيزتنا الأخت فاطمة، نود تذكيركم بموعد مراجعة السونار ومتابعة الحمل غداً الساعة 4:30 مساءً. - العيادة",
                scheduledDate = today,
                scheduledTime = "16:30",
                messageType = "تذكير موعد",
                channel = "واتساب",
                status = "مجدولة"
            )
        )
        sampleMessages.forEach { repository.insertScheduledMessage(it) }
    }
}
