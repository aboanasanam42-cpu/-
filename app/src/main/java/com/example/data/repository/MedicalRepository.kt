package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.flow.Flow

class MedicalRepository(private val dao: MedicalDao) {

    // Medical Records
    val allMedicalRecords: Flow<List<MedicalRecord>> = dao.getAllMedicalRecords()
    fun getRecordsByDepartment(dept: String): Flow<List<MedicalRecord>> = dao.getRecordsByDepartment(dept)
    fun searchMedicalRecords(query: String): Flow<List<MedicalRecord>> = dao.searchMedicalRecords(query)
    fun getRecordsForPatient(patientName: String): Flow<List<MedicalRecord>> = dao.getRecordsForPatient(patientName)
    suspend fun insertMedicalRecord(record: MedicalRecord): Long = dao.insertMedicalRecord(record)
    suspend fun updateMedicalRecord(record: MedicalRecord) = dao.updateMedicalRecord(record)
    suspend fun deleteMedicalRecord(record: MedicalRecord) = dao.deleteMedicalRecord(record)
    suspend fun deleteMedicalRecordById(id: Long) = dao.deleteMedicalRecordById(id)

    // Patients
    val allPatients: Flow<List<Patient>> = dao.getAllPatients()
    fun searchPatients(query: String): Flow<List<Patient>> = dao.searchPatients(query)
    suspend fun insertPatient(patient: Patient): Long = dao.insertPatient(patient)
    suspend fun updatePatient(patient: Patient) = dao.updatePatient(patient)
    suspend fun deletePatient(patient: Patient) = dao.deletePatient(patient)

    // Lab Records
    val allLabRecords: Flow<List<LabRecord>> = dao.getAllLabRecords()
    fun searchLabRecords(query: String): Flow<List<LabRecord>> = dao.searchLabRecords(query)
    fun getLabRecordsForPatient(patientName: String): Flow<List<LabRecord>> = dao.getLabRecordsForPatient(patientName)
    suspend fun insertLabRecord(record: LabRecord): Long = dao.insertLabRecord(record)
    suspend fun updateLabRecord(record: LabRecord) = dao.updateLabRecord(record)
    suspend fun deleteLabRecord(record: LabRecord) = dao.deleteLabRecord(record)

    // Radiology Records
    val allRadiologyRecords: Flow<List<RadiologyRecord>> = dao.getAllRadiologyRecords()
    fun searchRadiologyRecords(query: String): Flow<List<RadiologyRecord>> = dao.searchRadiologyRecords(query)
    fun getRadiologyRecordsForPatient(patientName: String): Flow<List<RadiologyRecord>> = dao.getRadiologyRecordsForPatient(patientName)
    suspend fun insertRadiologyRecord(record: RadiologyRecord): Long = dao.insertRadiologyRecord(record)
    suspend fun updateRadiologyRecord(record: RadiologyRecord) = dao.updateRadiologyRecord(record)
    suspend fun deleteRadiologyRecord(record: RadiologyRecord) = dao.deleteRadiologyRecord(record)

    // Pharmacy Records
    val allPharmacyRecords: Flow<List<PharmacyRecord>> = dao.getAllPharmacyRecords()
    fun searchPharmacyRecords(query: String): Flow<List<PharmacyRecord>> = dao.searchPharmacyRecords(query)
    fun getPharmacyRecordsForPatient(patientName: String): Flow<List<PharmacyRecord>> = dao.getPharmacyRecordsForPatient(patientName)
    suspend fun insertPharmacyRecord(record: PharmacyRecord): Long = dao.insertPharmacyRecord(record)
    suspend fun updatePharmacyRecord(record: PharmacyRecord) = dao.updatePharmacyRecord(record)
    suspend fun deletePharmacyRecord(record: PharmacyRecord) = dao.deletePharmacyRecord(record)

    // Expenses
    val allExpenses: Flow<List<ExpenseRecord>> = dao.getAllExpenses()
    fun getExpensesByCategory(cat: String): Flow<List<ExpenseRecord>> = dao.getExpensesByCategory(cat)
    suspend fun insertExpense(record: ExpenseRecord): Long = dao.insertExpense(record)
    suspend fun updateExpense(record: ExpenseRecord) = dao.updateExpense(record)
    suspend fun deleteExpense(record: ExpenseRecord) = dao.deleteExpense(record)

    // Scheduled Messages
    val allScheduledMessages: Flow<List<ScheduledMessage>> = dao.getAllScheduledMessages()
    suspend fun insertScheduledMessage(msg: ScheduledMessage): Long = dao.insertScheduledMessage(msg)
    suspend fun updateScheduledMessage(msg: ScheduledMessage) = dao.updateScheduledMessage(msg)
    suspend fun deleteScheduledMessage(msg: ScheduledMessage) = dao.deleteScheduledMessage(msg)

    // Database Reset
    suspend fun clearAllData() {
        dao.clearAllMedicalRecords()
        dao.clearAllPatients()
        dao.clearAllLabRecords()
        dao.clearAllRadiologyRecords()
        dao.clearAllPharmacyRecords()
        dao.clearAllExpenses()
        dao.clearAllScheduledMessages()
    }
}
