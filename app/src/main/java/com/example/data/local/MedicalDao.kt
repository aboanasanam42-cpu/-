package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalDao {

    // ================== MAIN MEDICAL RECORDS ==================
    @Query("SELECT * FROM medical_records ORDER BY id DESC")
    fun getAllMedicalRecords(): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE department = :dept ORDER BY id DESC")
    fun getRecordsByDepartment(dept: String): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE dateString = :date ORDER BY id DESC")
    fun getRecordsByDate(date: String): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE patientName LIKE '%' || :query || '%' OR patientPhone LIKE '%' || :query || '%' OR diagnosis LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchMedicalRecords(query: String): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE patientName = :patientName ORDER BY id DESC")
    fun getRecordsForPatient(patientName: String): Flow<List<MedicalRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalRecord(record: MedicalRecord): Long

    @Update
    suspend fun updateMedicalRecord(record: MedicalRecord)

    @Delete
    suspend fun deleteMedicalRecord(record: MedicalRecord)

    @Query("DELETE FROM medical_records WHERE id = :id")
    suspend fun deleteMedicalRecordById(id: Long)

    // ================== PATIENTS ==================
    @Query("SELECT * FROM patients ORDER BY name ASC")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchPatients(query: String): Flow<List<Patient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient): Long

    @Update
    suspend fun updatePatient(patient: Patient)

    @Delete
    suspend fun deletePatient(patient: Patient)

    // ================== LAB RECORDS ==================
    @Query("SELECT * FROM lab_records ORDER BY id DESC")
    fun getAllLabRecords(): Flow<List<LabRecord>>

    @Query("SELECT * FROM lab_records WHERE patientName LIKE '%' || :query || '%' OR testName LIKE '%' || :query || '%'")
    fun searchLabRecords(query: String): Flow<List<LabRecord>>

    @Query("SELECT * FROM lab_records WHERE patientName = :patientName ORDER BY id DESC")
    fun getLabRecordsForPatient(patientName: String): Flow<List<LabRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabRecord(record: LabRecord): Long

    @Update
    suspend fun updateLabRecord(record: LabRecord)

    @Delete
    suspend fun deleteLabRecord(record: LabRecord)

    // ================== RADIOLOGY RECORDS ==================
    @Query("SELECT * FROM radiology_records ORDER BY id DESC")
    fun getAllRadiologyRecords(): Flow<List<RadiologyRecord>>

    @Query("SELECT * FROM radiology_records WHERE patientName LIKE '%' || :query || '%' OR deviceName LIKE '%' || :query || '%'")
    fun searchRadiologyRecords(query: String): Flow<List<RadiologyRecord>>

    @Query("SELECT * FROM radiology_records WHERE patientName = :patientName ORDER BY id DESC")
    fun getRadiologyRecordsForPatient(patientName: String): Flow<List<RadiologyRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadiologyRecord(record: RadiologyRecord): Long

    @Update
    suspend fun updateRadiologyRecord(record: RadiologyRecord)

    @Delete
    suspend fun deleteRadiologyRecord(record: RadiologyRecord)

    // ================== PHARMACY RECORDS ==================
    @Query("SELECT * FROM pharmacy_records ORDER BY id DESC")
    fun getAllPharmacyRecords(): Flow<List<PharmacyRecord>>

    @Query("SELECT * FROM pharmacy_records WHERE patientName LIKE '%' || :query || '%' OR medicineName LIKE '%' || :query || '%'")
    fun searchPharmacyRecords(query: String): Flow<List<PharmacyRecord>>

    @Query("SELECT * FROM pharmacy_records WHERE patientName = :patientName ORDER BY id DESC")
    fun getPharmacyRecordsForPatient(patientName: String): Flow<List<PharmacyRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPharmacyRecord(record: PharmacyRecord): Long

    @Update
    suspend fun updatePharmacyRecord(record: PharmacyRecord)

    @Delete
    suspend fun deletePharmacyRecord(record: PharmacyRecord)

    // ================== EXPENSES ==================
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseRecord>>

    @Query("SELECT * FROM expenses WHERE category = :cat ORDER BY id DESC")
    fun getExpensesByCategory(cat: String): Flow<List<ExpenseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(record: ExpenseRecord): Long

    @Update
    suspend fun updateExpense(record: ExpenseRecord)

    @Delete
    suspend fun deleteExpense(record: ExpenseRecord)

    // ================== SCHEDULED MESSAGES ==================
    @Query("SELECT * FROM scheduled_messages ORDER BY id DESC")
    fun getAllScheduledMessages(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE status = :status ORDER BY scheduledDate ASC, scheduledTime ASC")
    fun getMessagesByStatus(status: String): Flow<List<ScheduledMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledMessage(msg: ScheduledMessage): Long

    @Update
    suspend fun updateScheduledMessage(msg: ScheduledMessage)

    @Delete
    suspend fun deleteScheduledMessage(msg: ScheduledMessage)

    // ================== BULK / BACKUP ==================
    @Query("DELETE FROM medical_records")
    suspend fun clearAllMedicalRecords()

    @Query("DELETE FROM patients")
    suspend fun clearAllPatients()

    @Query("DELETE FROM lab_records")
    suspend fun clearAllLabRecords()

    @Query("DELETE FROM radiology_records")
    suspend fun clearAllRadiologyRecords()

    @Query("DELETE FROM pharmacy_records")
    suspend fun clearAllPharmacyRecords()

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("DELETE FROM scheduled_messages")
    suspend fun clearAllScheduledMessages()
}
