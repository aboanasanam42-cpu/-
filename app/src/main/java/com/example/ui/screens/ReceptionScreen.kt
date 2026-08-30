package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MedicalRecord
import com.example.ui.components.AppHeaderBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel
import com.example.util.ExcelExporter
import com.example.util.PdfExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceptionScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val records by viewModel.allMedicalRecords.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecords = remember(records, searchQuery) {
        if (searchQuery.isBlank()) records
        else records.filter {
            it.patientName.contains(searchQuery, ignoreCase = true) ||
            it.patientPhone.contains(searchQuery) ||
            it.caseType.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "قسم الاستقبال والمحاسبة",
                showBack = true,
                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MedicalNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_reception_record")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة قيد طبي جديد")
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
            // Search and Export Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في سجل الاستقبال...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Export PDF Button
                Button(
                    onClick = {
                        PdfExporter.exportComprehensiveMedicalTablePdf(context, filteredRecords, "سجل_الاستقبال_المحاسبي")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_export_pdf_reception")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Export Excel Button
                Button(
                    onClick = {
                        ExcelExporter.exportMedicalRecordsToExcel(context, filteredRecords, "سجل_الاستقبال_المحاسبي")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_export_excel_reception")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = "Excel", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Summary Bar
            val totalRev = filteredRecords.sumOf { it.consultationFee + it.totalTreatmentFee }
            val totalPaid = filteredRecords.sumOf { it.paidAmount }
            val totalRemaining = filteredRecords.sumOf { it.remainingAmount }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "العدد: ${filteredRecords.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedicalNavy
                    )
                    Text(
                        text = "الإجمالي: ${totalRev.toInt()} ر.ي",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "الواصل: ${totalPaid.toInt()} ر.ي",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                    Text(
                        text = "الباقي: ${totalRemaining.toInt()} ر.ي",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }

            // Horizontal scrolling PDF style table for thorough bookkeeping
            val horizontalScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    // Header Row matching PDF exactly
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF0F2B5C), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableHeadCell("م", 35.dp)
                        TableHeadCell("اسم المريض", 130.dp)
                        TableHeadCell("نوع الحالة", 100.dp)
                        TableHeadCell("مبلغ المعاينة", 85.dp)
                        TableHeadCell("إجمالي المعالجة", 95.dp)
                        TableHeadCell("الواصل", 80.dp)
                        TableHeadCell("الباقي", 80.dp)
                        TableHeadCell("التشخيص / المعالجة", 140.dp)
                        TableHeadCell("رقم الهاتف", 100.dp)
                        TableHeadCell("نسبة الطبيب", 85.dp)
                        TableHeadCell("نسبة المخبري", 85.dp)
                        TableHeadCell("راتب الممرضة", 85.dp)
                        TableHeadCell("خ مواد/إيجار", 90.dp)
                        TableHeadCell("خرجيات أخرى", 85.dp)
                        TableHeadCell("إجراءات", 75.dp)
                    }

                    if (filteredRecords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد قيود مسجلة حالياً", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(filteredRecords) { index, record ->
                                val rowBg = if (index % 2 == 0) Color(0xFFF8FAFC) else Color.White
                                Row(
                                    modifier = Modifier
                                        .background(rowBg)
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TableCell((index + 1).toString(), 35.dp, isBold = true)
                                    TableCell(record.patientName, 130.dp, isBold = true, color = MedicalNavy)
                                    TableCell(record.caseType, 100.dp)
                                    TableCell("${record.consultationFee.toInt()}", 85.dp)
                                    TableCell("${record.totalTreatmentFee.toInt()}", 95.dp)
                                    TableCell("${record.paidAmount.toInt()}", 80.dp, color = Color(0xFF059669), isBold = true)
                                    TableCell("${record.remainingAmount.toInt()}", 80.dp, color = if (record.remainingAmount > 0) Color(0xFFDC2626) else TextSecondary, isBold = record.remainingAmount > 0)
                                    TableCell(record.diagnosis.ifEmpty { "-" }, 140.dp)
                                    TableCell(record.patientPhone.ifEmpty { "-" }, 100.dp)
                                    TableCell("${record.doctorCommission.toInt()}", 85.dp)
                                    TableCell("${record.labCommission.toInt()}", 85.dp)
                                    TableCell("${record.nurseSalaryShare.toInt()}", 85.dp)
                                    TableCell("${(record.materialExpense + record.rentAndUtilities).toInt()}", 90.dp)
                                    TableCell("${record.otherExpenses.toInt()}", 85.dp)
                                    // Action delete
                                    Box(modifier = Modifier.width(75.dp), contentAlignment = Alignment.Center) {
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteMedicalRecord(record)
                                                Toast.makeText(context, "تم حذف القيد", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReceptionRecordDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newRecord ->
                viewModel.addMedicalRecord(newRecord)
                showAddDialog = false
                Toast.makeText(context, "تم حفظ القيد بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun TableHeadCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width)
    )
}

@Composable
fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = TextPrimary,
    isBold: Boolean = false
) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width),
        maxLines = 1
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceptionRecordDialog(
    onDismiss: () -> Unit,
    onSave: (MedicalRecord) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var patientPhone by remember { mutableStateOf("") }
    var caseType by remember { mutableStateOf("معاينة عامة") }
    var consultationFee by remember { mutableStateOf("") }
    var totalTreatmentFee by remember { mutableStateOf("") }
    var paidAmount by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var doctorCommission by remember { mutableStateOf("") }
    var labCommission by remember { mutableStateOf("") }
    var nurseSalaryShare by remember { mutableStateOf("") }
    var materialExpense by remember { mutableStateOf("") }
    var rentAndUtilities by remember { mutableStateOf("") }
    var otherExpenses by remember { mutableStateOf("") }

    // Calculated balance
    val consult = consultationFee.toDoubleOrNull() ?: 0.0
    val treatment = totalTreatmentFee.toDoubleOrNull() ?: 0.0
    val paid = paidAmount.toDoubleOrNull() ?: 0.0
    val calculatedRemaining = (consult + treatment - paid).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("إضافة قيد محاسبي / طبي جديد", fontWeight = FontWeight.Bold, color = MedicalNavy, fontSize = 16.sp)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
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
                        label = { Text("رقم هاتف المريض") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = caseType,
                        onValueChange = { caseType = it },
                        label = { Text("نوع الحالة / القسم") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = consultationFee,
                            onValueChange = { consultationFee = it },
                            label = { Text("مبلغ المعاينة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = totalTreatmentFee,
                            onValueChange = { totalTreatmentFee = it },
                            label = { Text("إجمالي المعالجة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = paidAmount,
                            onValueChange = { paidAmount = it },
                            label = { Text("الواصل (المدفوع)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("الباقي (المتبقي)", fontSize = 10.sp, color = TextSecondary)
                                Text("${calculatedRemaining.toInt()} ر.ي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = diagnosis,
                        onValueChange = { diagnosis = it },
                        label = { Text("التشخيص / المعالجة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = doctorCommission,
                            onValueChange = { doctorCommission = it },
                            label = { Text("نسبة الطبيب") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = labCommission,
                            onValueChange = { labCommission = it },
                            label = { Text("نسبة المخبري") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nurseSalaryShare,
                            onValueChange = { nurseSalaryShare = it },
                            label = { Text("راتب الممرضة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = materialExpense,
                            onValueChange = { materialExpense = it },
                            label = { Text("خ المواد") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rentAndUtilities,
                            onValueChange = { rentAndUtilities = it },
                            label = { Text("الإيجار والنثريات") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = otherExpenses,
                            onValueChange = { otherExpenses = it },
                            label = { Text("خرجيات أخرى") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (patientName.isNotBlank()) {
                        val record = MedicalRecord(
                            patientName = patientName.trim(),
                            patientPhone = patientPhone.trim(),
                            caseType = caseType.trim(),
                            consultationFee = consult,
                            totalTreatmentFee = treatment,
                            paidAmount = paid,
                            remainingAmount = calculatedRemaining,
                            diagnosis = diagnosis.trim(),
                            notes = notes.trim(),
                            doctorCommission = doctorCommission.toDoubleOrNull() ?: 0.0,
                            labCommission = labCommission.toDoubleOrNull() ?: 0.0,
                            nurseSalaryShare = nurseSalaryShare.toDoubleOrNull() ?: 0.0,
                            materialExpense = materialExpense.toDoubleOrNull() ?: 0.0,
                            rentAndUtilities = rentAndUtilities.toDoubleOrNull() ?: 0.0,
                            otherExpenses = otherExpenses.toDoubleOrNull() ?: 0.0,
                            department = "الاستقبال",
                            dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )
                        onSave(record)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedicalNavy)
            ) {
                Text("حفظ في السجل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}
