package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MedicalRecord
import com.example.ui.components.AppHeaderBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val records by viewModel.allMedicalRecords.collectAsStateWithLifecycle()
    var showConsultationDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val doctorRecords = remember(records, searchQuery) {
        val base = records.filter { it.department == "الطبيب" || it.doctorCommission > 0 || it.diagnosis.isNotEmpty() }
        if (searchQuery.isBlank()) base
        else base.filter { it.patientName.contains(searchQuery, ignoreCase = true) || it.diagnosis.contains(searchQuery, ignoreCase = true) }
    }

    val totalDoctorEarnings = doctorRecords.sumOf { it.doctorCommission }
    val totalPatientsCount = doctorRecords.size

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "قسم الطبيب والمعاينات السريرية",
                showBack = true,
                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showConsultationDialog = true },
                containerColor = Color(0xFF059669),
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_doctor_consultation")
            ) {
                Icon(Icons.Default.MedicalServices, contentDescription = "كشف طبي جديد")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceBackground)
                .padding(horizontal = 12.dp)
        ) {
            // Doctor Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .shadow(3.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF065F46), Color(0xFF059669))
                            )
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي مستحقات وأرباح الطبيب", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("${totalDoctorEarnings.toInt()} ر.ي", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("عدد الحالات المعاينة", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("$totalPatientsCount حالة", color = Color(0xFFFFD54F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث في سجلات الطبيب والتشخيصات...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Patient Examination Cards List
            if (doctorRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد معاينات مسجلة حالياً", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(doctorRecords) { record ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = record.patientName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedicalNavy
                                    )
                                    Badge(
                                        containerColor = Color(0xFFE0F2FE),
                                        contentColor = Color(0xFF0369A1)
                                    ) {
                                        Text(record.caseType, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp)
                                    }
                                }

                                if (record.diagnosis.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "التشخيص: ${record.diagnosis}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF047857)
                                    )
                                }

                                if (record.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "الخطة العلاجية / ملاحظات: ${record.notes}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "نسبة الطبيب: ${record.doctorCommission.toInt()} ر.ي",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                    Text(
                                        text = "التاريخ: ${record.dateString}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConsultationDialog) {
        AddDoctorConsultationDialog(
            onDismiss = { showConsultationDialog = false },
            onSave = { newRecord ->
                viewModel.addMedicalRecord(newRecord)
                showConsultationDialog = false
                Toast.makeText(context, "تم حفظ المعاينة الطبية بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddDoctorConsultationDialog(
    onDismiss: () -> Unit,
    onSave: (MedicalRecord) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var patientPhone by remember { mutableStateOf("") }
    var caseType by remember { mutableStateOf("استشارة باطنية") }
    var diagnosis by remember { mutableStateOf("") }
    var prescriptionAndNotes by remember { mutableStateOf("") }
    var consultationFee by remember { mutableStateOf("3000") }
    var treatmentFee by remember { mutableStateOf("5000") }
    var doctorCommission by remember { mutableStateOf("2500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تسجيل معاينة وتشخيص طبي", fontWeight = FontWeight.Bold, color = Color(0xFF059669), fontSize = 16.sp)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("اسم المريض *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = patientPhone,
                        onValueChange = { patientPhone = it },
                        label = { Text("رقم الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = caseType,
                        onValueChange = { caseType = it },
                        label = { Text("التخصص / نوع المعاينة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = diagnosis,
                        onValueChange = { diagnosis = it },
                        label = { Text("التشخيص السريري *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = prescriptionAndNotes,
                        onValueChange = { prescriptionAndNotes = it },
                        label = { Text("العلاج الموصوف والملاحظات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = consultationFee,
                            onValueChange = { consultationFee = it },
                            label = { Text("المعاينة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = doctorCommission,
                            onValueChange = { doctorCommission = it },
                            label = { Text("نسبة الطبيب") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (patientName.isNotBlank()) {
                        val consult = consultationFee.toDoubleOrNull() ?: 0.0
                        val treat = treatmentFee.toDoubleOrNull() ?: 0.0
                        val docComm = doctorCommission.toDoubleOrNull() ?: 0.0
                        val rec = MedicalRecord(
                            patientName = patientName.trim(),
                            patientPhone = patientPhone.trim(),
                            caseType = caseType.trim(),
                            consultationFee = consult,
                            totalTreatmentFee = treat,
                            paidAmount = consult + treat,
                            remainingAmount = 0.0,
                            diagnosis = diagnosis.trim(),
                            notes = prescriptionAndNotes.trim(),
                            doctorCommission = docComm,
                            department = "الطبيب",
                            dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )
                        onSave(rec)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Text("حفظ المعاينة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}
