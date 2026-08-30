package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.MedicalRepository
import com.example.util.AutoBackupHelper
import com.example.util.BackupHelper
import com.example.util.MessageScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AppScreen {
    DASHBOARD, RECEPTION, DOCTOR, LAB, RADIOLOGY, PHARMACY, EXPENSES, QUICK_SEARCH, INVOICES_REPORTS, BACKUP, AUTO_MESSAGES, REPORTS
}

enum class ReportPeriod(val title: String) {
    DAILY("التقرير اليومي"), WEEKLY("التقرير الأسبوعي"), MONTHLY("التقرير الشهري"), ANNUAL("التقرير السنوي")
}

data class GlobalSearchResult(
    val patients: List<Patient> = emptyList(),
    val medicalRecords: List<MedicalRecord> = emptyList(),
    val labRecords: List<LabRecord> = emptyList(),
    val radiologyRecords: List<RadiologyRecord> = emptyList(),
    val pharmacyRecords: List<PharmacyRecord> = emptyList()
)

data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

class MedicalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MedicalRepository

    init {
        val db = MedicalDatabase.getDatabase(application)
        repository = MedicalRepository(db.medicalDao())
        viewModelScope.launch {
            repository.allMedicalRecords.firstOrNull()?.let { list ->
                if (list.isEmpty()) BackupHelper.seedInitialDataIfEmpty(repository)
            }
        }
    }

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()
    fun navigateTo(screen: AppScreen) { _currentScreen.value = screen }

    val allMedicalRecords = repository.allMedicalRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPatients = repository.allPatients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allLabRecords = repository.allLabRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allRadiologyRecords = repository.allRadiologyRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPharmacyRecords = repository.allPharmacyRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allExpenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allScheduledMessages = repository.allScheduledMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    val globalSearchResult: StateFlow<GlobalSearchResult> = combine(
        _searchQuery,
        combine(allPatients, allMedicalRecords, allLabRecords, allRadiologyRecords) { pats, meds, labs, rads -> Tuple4(pats, meds, labs, rads) },
        allPharmacyRecords
    ) { query, tuple, pharms ->
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) GlobalSearchResult(tuple.a, tuple.b, tuple.c, tuple.d, pharms)
        else GlobalSearchResult(
            patients = tuple.a.filter { it.name.lowercase(Locale.getDefault()).contains(q) || it.phone.contains(q) },
            medicalRecords = tuple.b.filter { it.patientName.lowercase(Locale.getDefault()).contains(q) || it.patientPhone.contains(q) || it.diagnosis.lowercase(Locale.getDefault()).contains(q) || it.caseType.lowercase(Locale.getDefault()).contains(q) },
            labRecords = tuple.c.filter { it.patientName.lowercase(Locale.getDefault()).contains(q) || it.testName.lowercase(Locale.getDefault()).contains(q) },
            radiologyRecords = tuple.d.filter { it.patientName.lowercase(Locale.getDefault()).contains(q) || it.deviceName.lowercase(Locale.getDefault()).contains(q) },
            pharmacyRecords = pharms.filter { it.patientName.lowercase(Locale.getDefault()).contains(q) || it.medicineName.lowercase(Locale.getDefault()).contains(q) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlobalSearchResult())

    private val _selectedPatientName = MutableStateFlow<String?>(null)
    val selectedPatientName: StateFlow<String?> = _selectedPatientName.asStateFlow()
    fun selectPatientForInvoice(name: String?) { _selectedPatientName.value = name }

    private val _reportPeriod = MutableStateFlow(ReportPeriod.DAILY)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()
    fun setReportPeriod(period: ReportPeriod) { _reportPeriod.value = period }

    val filteredReportData = combine(_reportPeriod, allMedicalRecords, allExpenses) { period, records, expenses ->
        val calendar = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val filteredRecords = when (period) {
            ReportPeriod.DAILY -> records.filter { it.dateString == todayStr }
            ReportPeriod.WEEKLY -> { calendar.add(Calendar.DAY_OF_YEAR, -7); records.filter { it.timestamp >= calendar.timeInMillis } }
            ReportPeriod.MONTHLY -> { calendar.add(Calendar.DAY_OF_YEAR, -30); records.filter { it.timestamp >= calendar.timeInMillis } }
            ReportPeriod.ANNUAL -> { calendar.add(Calendar.DAY_OF_YEAR, -365); records.filter { it.timestamp >= calendar.timeInMillis } }
        }
        val filteredExpenses = when (period) {
            ReportPeriod.DAILY -> expenses.filter { it.dateString == todayStr }
            ReportPeriod.WEEKLY -> expenses.filter { it.timestamp >= Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis }
            ReportPeriod.MONTHLY -> expenses.filter { it.timestamp >= Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis }
            ReportPeriod.ANNUAL -> expenses.filter { it.timestamp >= Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -365) }.timeInMillis }
        }
        Pair(filteredRecords, filteredExpenses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(emptyList(), emptyList()))

    private fun triggerAutomaticBackup() {
        viewModelScope.launch { AutoBackupHelper.saveAll(getApplication(), repository) }
    }

    fun addMedicalRecord(record: MedicalRecord) { viewModelScope.launch { repository.insertMedicalRecord(record); triggerAutomaticBackup() } }
    fun updateMedicalRecord(record: MedicalRecord) { viewModelScope.launch { repository.updateMedicalRecord(record); triggerAutomaticBackup() } }
    fun deleteMedicalRecord(record: MedicalRecord) { viewModelScope.launch { repository.deleteMedicalRecord(record); triggerAutomaticBackup() } }
    fun addLabRecord(record: LabRecord) { viewModelScope.launch { repository.insertLabRecord(record); triggerAutomaticBackup() } }
    fun deleteLabRecord(record: LabRecord) { viewModelScope.launch { repository.deleteLabRecord(record); triggerAutomaticBackup() } }
    fun addRadiologyRecord(record: RadiologyRecord) { viewModelScope.launch { repository.insertRadiologyRecord(record); triggerAutomaticBackup() } }
    fun deleteRadiologyRecord(record: RadiologyRecord) { viewModelScope.launch { repository.deleteRadiologyRecord(record); triggerAutomaticBackup() } }
    fun addPharmacyRecord(record: PharmacyRecord) { viewModelScope.launch { repository.insertPharmacyRecord(record); triggerAutomaticBackup() } }
    fun deletePharmacyRecord(record: PharmacyRecord) { viewModelScope.launch { repository.deletePharmacyRecord(record); triggerAutomaticBackup() } }
    fun addExpense(expense: ExpenseRecord) { viewModelScope.launch { repository.insertExpense(expense); triggerAutomaticBackup() } }
    fun deleteExpense(expense: ExpenseRecord) { viewModelScope.launch { repository.deleteExpense(expense); triggerAutomaticBackup() } }

    fun addScheduledMessage(msg: ScheduledMessage) {
        viewModelScope.launch {
            val id = repository.insertScheduledMessage(msg)
            MessageScheduler.schedule(getApplication(), msg.copy(id = id))
            triggerAutomaticBackup()
        }
    }

    fun deleteScheduledMessage(msg: ScheduledMessage) {
        viewModelScope.launch {
            MessageScheduler.cancel(getApplication(), msg)
            repository.deleteScheduledMessage(msg)
            triggerAutomaticBackup()
        }
    }

    fun updateScheduledMessageStatus(msg: ScheduledMessage, newStatus: String) {
        viewModelScope.launch {
            val updated = msg.copy(status = newStatus)
            repository.updateScheduledMessage(updated)
            if (newStatus == "مجدولة") MessageScheduler.schedule(getApplication(), updated)
            triggerAutomaticBackup()
        }
    }

    fun addPatient(patient: Patient) { viewModelScope.launch { repository.insertPatient(patient); triggerAutomaticBackup() } }
    fun clearAllDatabase() { viewModelScope.launch { repository.clearAllData(); triggerAutomaticBackup() } }
}
