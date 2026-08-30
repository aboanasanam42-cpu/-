package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.MedicalRepository
import com.example.util.BackupHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppScreen {
    DASHBOARD,
    RECEPTION,
    DOCTOR,
    LAB,
    RADIOLOGY,
    PHARMACY,
    EXPENSES,
    QUICK_SEARCH,
    INVOICES_REPORTS,
    BACKUP,
    AUTO_MESSAGES,
    REPORTS
}

enum class ReportPeriod(val title: String) {
    DAILY("التقرير اليومي"),
    WEEKLY("التقرير الأسبوعي"),
    MONTHLY("التقرير الشهري"),
    ANNUAL("التقرير السنوي")
}

data class GlobalSearchResult(
    val patients: List<Patient> = emptyList(),
    val medicalRecords: List<MedicalRecord> = emptyList(),
    val labRecords: List<LabRecord> = emptyList(),
    val radiologyRecords: List<RadiologyRecord> = emptyList(),
    val pharmacyRecords: List<PharmacyRecord> = emptyList()
)

data class Tuple4<A, B, C, D>(
    val a: A,
    val b: B,
    val c: C,
    val d: D
)

class MedicalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicalRepository

    init {
        val db = MedicalDatabase.getDatabase(application)
        repository = MedicalRepository(db.medicalDao())
        viewModelScope.launch {
            repository.allMedicalRecords.firstOrNull()?.let { list ->
                if (list.isEmpty()) {
                    BackupHelper.seedInitialDataIfEmpty(repository)
                }
            }
        }
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Data Flows
    val allMedicalRecords = repository.allMedicalRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPatients = repository.allPatients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLabRecords = repository.allLabRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRadiologyRecords = repository.allRadiologyRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPharmacyRecords = repository.allPharmacyRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScheduledMessages = repository.allScheduledMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val globalSearchResult: StateFlow<GlobalSearchResult> = combine(
        _searchQuery,
        combine(allPatients, allMedicalRecords, allLabRecords, allRadiologyRecords) { pats, meds, labs, rads ->
            Tuple4(pats, meds, labs, rads)
        },
        allPharmacyRecords
    ) { query, tuple, pharms ->
        val pats = tuple.a
        val meds = tuple.b
        val labs = tuple.c
        val rads = tuple.d
        val q = query.trim().lowercase(Locale.getDefault())

        if (q.isEmpty()) {
            GlobalSearchResult(pats, meds, labs, rads, pharms)
        } else {
            GlobalSearchResult(
                patients = pats.filter { it.name.lowercase(Locale.getDefault()).contains(q) || it.phone.contains(q) },
                medicalRecords = meds.filter { it.patientName.lowercase(Locale.getDefault()).contains(q) || it.patientPhone.contains(q) || it.diagnosis.lowercase(Locale.getDefault()).contains(q) || it.caseType.lowercase(Locale.getDefault()).contains(q) },
                labRecords = labs.filter { it.patientName.lowercase(Locale.getDefault()).contains(q) || it.testName.lowercase(Locale.getDefault()).contains(q) },
                radiologyRecords = rads.filter { it.patientName.lowercase(Locale.getDefault()).contains(q) || it.deviceName.lowercase(Locale.getDefault()).contains(q) },
                pharmacyRecords = pharms.filter { it.patientName.lowercase(Locale.getDefault()).contains(q) || it.medicineName.lowercase(Locale.getDefault()).contains(q) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlobalSearchResult())

    // Selected Patient for Invoices / Statements
    private val _selectedPatientName = MutableStateFlow<String?>(null)
    val selectedPatientName: StateFlow<String?> = _selectedPatientName.asStateFlow()

    fun selectPatientForInvoice(name: String?) {
        _selectedPatientName.value = name
    }

    // Reports Period
    private val _reportPeriod = MutableStateFlow(ReportPeriod.DAILY)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    // Filtered Records based on Report Period
    val filteredReportData = combine(
        _reportPeriod,
        allMedicalRecords,
        allExpenses
    ) { period, records, expenses ->
        val calendar = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val filteredRecords = when (period) {
            ReportPeriod.DAILY -> records.filter { it.dateString == todayStr }
            ReportPeriod.WEEKLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.timeInMillis
                records.filter { it.timestamp >= weekAgo }
            }
            ReportPeriod.MONTHLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val monthAgo = calendar.timeInMillis
                records.filter { it.timestamp >= monthAgo }
            }
            ReportPeriod.ANNUAL -> {
                calendar.add(Calendar.DAY_OF_YEAR, -365)
                val yearAgo = calendar.timeInMillis
                records.filter { it.timestamp >= yearAgo }
            }
        }

        val filteredExpenses = when (period) {
            ReportPeriod.DAILY -> expenses.filter { it.dateString == todayStr }
            ReportPeriod.WEEKLY -> {
                val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                expenses.filter { it.timestamp >= cal2.timeInMillis }
            }
            ReportPeriod.MONTHLY -> {
                val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
                expenses.filter { it.timestamp >= cal2.timeInMillis }
            }
            ReportPeriod.ANNUAL -> {
                val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -365) }
                expenses.filter { it.timestamp >= cal2.timeInMillis }
            }
        }

        Pair(filteredRecords, filteredExpenses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(emptyList(), emptyList()))

    // Operations
    fun addMedicalRecord(record: MedicalRecord) {
        viewModelScope.launch {
            repository.insertMedicalRecord(record)
        }
    }

    fun updateMedicalRecord(record: MedicalRecord) {
        viewModelScope.launch {
            repository.updateMedicalRecord(record)
        }
    }

    fun deleteMedicalRecord(record: MedicalRecord) {
        viewModelScope.launch {
            repository.deleteMedicalRecord(record)
        }
    }

    fun addLabRecord(record: LabRecord) {
        viewModelScope.launch {
            repository.insertLabRecord(record)
        }
    }

    fun deleteLabRecord(record: LabRecord) {
        viewModelScope.launch {
            repository.deleteLabRecord(record)
        }
    }

    fun addRadiologyRecord(record: RadiologyRecord) {
        viewModelScope.launch {
            repository.insertRadiologyRecord(record)
        }
    }

    fun deleteRadiologyRecord(record: RadiologyRecord) {
        viewModelScope.launch {
            repository.deleteRadiologyRecord(record)
        }
    }

    fun addPharmacyRecord(record: PharmacyRecord) {
        viewModelScope.launch {
            repository.insertPharmacyRecord(record)
        }
    }

    fun deletePharmacyRecord(record: PharmacyRecord) {
        viewModelScope.launch {
            repository.deletePharmacyRecord(record)
        }
    }

    fun addExpense(expense: ExpenseRecord) {
        viewModelScope.launch {
            repository.insertExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseRecord) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun addScheduledMessage(msg: ScheduledMessage) {
        viewModelScope.launch {
            repository.insertScheduledMessage(msg)
        }
    }

    fun deleteScheduledMessage(msg: ScheduledMessage) {
        viewModelScope.launch {
            repository.deleteScheduledMessage(msg)
        }
    }

    fun updateScheduledMessageStatus(msg: ScheduledMessage, newStatus: String) {
        viewModelScope.launch {
            repository.updateScheduledMessage(msg.copy(status = newStatus))
        }
    }

    fun addPatient(patient: Patient) {
        viewModelScope.launch {
            repository.insertPatient(patient)
        }
    }

    fun clearAllDatabase() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }
}
