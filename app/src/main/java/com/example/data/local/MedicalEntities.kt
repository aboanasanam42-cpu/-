package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Main Medical & Accounting Record mapped directly to the PDF Table:
 * [م, اسم المريض, نوع الحالة, مبلغ المعاينة, إجمالي مبلغ المعالجة, الواصل, الباقي, التشخيص / المعالجة,
 *  رقم هاتف المريض, ملاحظات, نسبة الطبيب, نسبة المخبري, راتب الممرضة, خ المواد/الإيجار/النثريات, خرجيات أخرى]
 */
@Entity(tableName = "medical_records")
data class MedicalRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String,
    val patientPhone: String = "",
    val caseType: String = "معاينة عامة", // نوع الحالة
    val consultationFee: Double = 0.0,    // مبلغ المعاينة
    val totalTreatmentFee: Double = 0.0,  // إجمالي مبلغ المعالجة
    val paidAmount: Double = 0.0,         // الواصل
    val remainingAmount: Double = 0.0,     // الباقي
    val diagnosis: String = "",           // التشخيص / المعالجة
    val notes: String = "",               // ملاحظات
    val doctorCommission: Double = 0.0,   // نسبة الطبيب
    val labCommission: Double = 0.0,      // نسبة المخبري
    val nurseSalaryShare: Double = 0.0,   // راتب الممرضة
    val materialExpense: Double = 0.0,    // خ المواد
    val rentAndUtilities: Double = 0.0,   // الإيجار والنثريات
    val otherExpenses: Double = 0.0,      // خرجيات أخرى
    val department: String = "الاستقبال", // الاستقبال, الطبيب, المختبرات, الأشعة, الصيدلية
    val dateString: String = "",          // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Patient Master Record
 */
@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val age: String = "",
    val gender: String = "ذكر",
    val address: String = "",
    val bloodGroup: String = "",
    val medicalHistory: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Laboratory Record (المختبرات)
 */
@Entity(tableName = "lab_records")
data class LabRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String,
    val patientPhone: String = "",
    val testName: String,               // اسم الفحص المخبري
    val testCost: Double = 0.0,         // تكلفة الفحص
    val labCommission: Double = 0.0,    // نسبة المخبري
    val paidAmount: Double = 0.0,       // الواصل
    val remainingAmount: Double = 0.0,  // الباقي
    val result: String = "",            // نتيجة الفحص
    val notes: String = "",
    val dateString: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Radiology & Devices Record (الأشعة والأجهزة الأخرى)
 */
@Entity(tableName = "radiology_records")
data class RadiologyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String,
    val patientPhone: String = "",
    val deviceName: String,             // نوع الجهاز / الأشعة (X-Ray, Ultrasound, CT, ECG, etc.)
    val cost: Double = 0.0,             // القيمة
    val paidAmount: Double = 0.0,       // الواصل
    val remainingAmount: Double = 0.0,  // الباقي
    val operatorShare: Double = 0.0,    // نسبة المشغل / الفني
    val reportDetails: String = "",     // التقرير الفني
    val notes: String = "",
    val dateString: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Pharmacy Record (الصيدلية)
 */
@Entity(tableName = "pharmacy_records")
data class PharmacyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String,
    val patientPhone: String = "",
    val medicineName: String,           // اسم الدواء أو الصنف
    val quantity: Int = 1,              // الكمية
    val unitPrice: Double = 0.0,        // سعر الوحدة
    val totalAmount: Double = 0.0,      // الإجمالي
    val paidAmount: Double = 0.0,       // الواصل
    val remainingAmount: Double = 0.0,  // الباقي
    val costPrice: Double = 0.0,        // سعر التكلفة للعيادة
    val notes: String = "",
    val dateString: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * General Expenses Record (الخرجيات العامة)
 */
@Entity(tableName = "expenses")
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,               // خ المواد, الإيجار, النثريات, راتب الممرضة, خرجيات أخرى
    val title: String,                  // البيان / التوضيح
    val amount: Double = 0.0,           // المبلغ
    val notes: String = "",
    val dateString: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Automated Messages for Patients & Staff (الرسائل التلقائية)
 */
@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipientName: String,
    val recipientPhone: String,
    val messageText: String,
    val scheduledDate: String,          // YYYY-MM-DD
    val scheduledTime: String,          // HH:mm
    val messageType: String = "تذكير موعد", // تذكير موعد, متابعة علاج, كشف حساب ومطالبة, تهنئة/عامة
    val channel: String = "واتساب",      // واتساب, SMS, كلاهما
    val status: String = "مجدولة",       // مجدولة, تم الإرسال, ملغاة
    val createdAt: Long = System.currentTimeMillis()
)
