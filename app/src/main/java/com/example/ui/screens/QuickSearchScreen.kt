package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.*
import com.example.ui.components.AppHeaderBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel
import com.example.util.MessageHelper
import com.example.util.PdfExporter
import com.example.util.PrintHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSearchScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.globalSearchResult.collectAsStateWithLifecycle()
    val allRecords by viewModel.allMedicalRecords.collectAsStateWithLifecycle()

    var selectedPatientName by remember { mutableStateOf<String?>(null) }

    // Patient Dossier Data
    val patientMedicalRecords = remember(selectedPatientName, allRecords) {
        if (selectedPatientName == null) emptyList()
        else allRecords.filter { it.patientName.equals(selectedPatientName, ignoreCase = true) }
    }

    val patientLabs = remember(selectedPatientName, searchResults.labRecords) {
        if (selectedPatientName == null) emptyList()
        else searchResults.labRecords.filter { it.patientName.equals(selectedPatientName, ignoreCase = true) }
    }

    val patientRadiology = remember(selectedPatientName, searchResults.radiologyRecords) {
        if (selectedPatientName == null) emptyList()
        else searchResults.radiologyRecords.filter { it.patientName.equals(selectedPatientName, ignoreCase = true) }
    }

    val patientPharmacy = remember(selectedPatientName, searchResults.pharmacyRecords) {
        if (selectedPatientName == null) emptyList()
        else searchResults.pharmacyRecords.filter { it.patientName.equals(selectedPatientName, ignoreCase = true) }
    }

    // Auto-select first patient if available
    LaunchedEffect(searchResults.patients) {
        if (selectedPatientName == null && searchResults.patients.isNotEmpty()) {
            selectedPatientName = searchResults.patients.first().name
        }
    }

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "البحث السريع وملفات المرضى الشاملة",
                showBack = true,
                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceBackground)
                .padding(horizontal = 12.dp)
        ) {
            // Search Input Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("ابحث باسم المريض، رقم الهاتف، التشخيص، الفحص، الدواء...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = MedicalBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("input_quick_search"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MedicalBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                singleLine = true
            )

            // Patient Selection Chips
            if (searchResults.patients.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(searchResults.patients) { patient ->
                        FilterChip(
                            selected = selectedPatientName == patient.name,
                            onClick = { selectedPatientName = patient.name },
                            label = { Text(patient.name, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedPatientName == patient.name) Color.White else MedicalBlue
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MedicalNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // If a patient is selected, show their full dossier
            if (selectedPatientName != null) {
                val currentPatient = searchResults.patients.find { it.name == selectedPatientName }
                val phone = currentPatient?.phone ?: patientMedicalRecords.firstOrNull()?.patientPhone ?: ""

                // Calculations
                val totalConsultation = patientMedicalRecords.sumOf { it.consultationFee }
                val totalTreatment = patientMedicalRecords.sumOf { it.totalTreatmentFee }
                val totalLabsCost = patientLabs.sumOf { it.testCost }
                val totalRadCost = patientRadiology.sumOf { it.cost }
                val totalPharmCost = patientPharmacy.sumOf { it.totalAmount }

                val grandTotalCost = totalConsultation + totalTreatment + totalLabsCost + totalRadCost + totalPharmCost
                val grandTotalPaid = patientMedicalRecords.sumOf { it.paidAmount } + patientLabs.sumOf { it.paidAmount } + patientRadiology.sumOf { it.paidAmount } + patientPharmacy.sumOf { it.paidAmount }
                val grandTotalDebt = (grandTotalCost - grandTotalPaid).coerceAtLeast(0.0)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Patient Header Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(MedicalNavy, Color(0xFF1E3A8A))
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = selectedPatientName ?: "",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "الهاتف: ${if (phone.isNotEmpty()) phone else "غير مسجل"}",
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }

                                    // Quick Communication Icons
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (phone.isNotEmpty()) {
                                            IconButton(
                                                onClick = { MessageHelper.makePhoneCall(context, phone) },
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(
                                                onClick = { MessageHelper.sendWhatsAppMessage(context, phone, "السلام عليكم $selectedPatientName:") },
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = Color(0xFF4ADE80), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Financial Status summary
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("إجمالي الحساب", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                        Text("${grandTotalCost.toInt()} ر.ي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("المسدد (الواصل)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                        Text("${grandTotalPaid.toInt()} ر.ي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("المتبقي (الدين)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                        Text("${grandTotalDebt.toInt()} ر.ي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (grandTotalDebt > 0) Color(0xFFF87171) else Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action Buttons: Print Invoice, PDF Statement
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (patientMedicalRecords.isNotEmpty()) {
                                                PrintHelper.printInvoice(context, selectedPatientName ?: "", phone, patientMedicalRecords)
                                            } else {
                                                Toast.makeText(context, "لا توجد سجلات طبية لطباعتها", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = "طباعة", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("طباعة فاتورة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            if (patientMedicalRecords.isNotEmpty()) {
                                                PdfExporter.exportPatientInvoicePdf(context, selectedPatientName ?: "", phone, patientMedicalRecords)
                                            } else {
                                                Toast.makeText(context, "لا توجد سجلات لتصديرها", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("كشف PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Section 1: Clinical Diagnoses & Visits
                    item {
                        SectionHeader(title = "1. الزيارات والتشخيصات والمعاينات السريرية", count = patientMedicalRecords.size)
                    }
                    if (patientMedicalRecords.isEmpty()) {
                        item { EmptySectionCard("لا توجد زيارات مسجلة لهذا المريض") }
                    } else {
                        items(patientMedicalRecords) { r ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(r.caseType, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MedicalNavy)
                                        Text(r.dateString, fontSize = 11.sp, color = TextMuted)
                                    }
                                    if (r.diagnosis.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("التشخيص: ${r.diagnosis}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF047857))
                                    }
                                    if (r.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("الملاحظات: ${r.notes}", fontSize = 11.5.sp, color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("المعاينة: ${r.consultationFee.toInt()} | المعالجة: ${r.totalTreatmentFee.toInt()}", fontSize = 11.sp)
                                        Text("الواصل: ${r.paidAmount.toInt()} | الباقي: ${r.remainingAmount.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (r.remainingAmount > 0) Color(0xFFDC2626) else Color(0xFF059669))
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Laboratory Tests
                    item {
                        SectionHeader(title = "2. الفحوصات المخبرية ونتائج التحاليل", count = patientLabs.size)
                    }
                    if (patientLabs.isEmpty()) {
                        item { EmptySectionCard("لا توجد فحوصات مخبرية مسجلة") }
                    } else {
                        items(patientLabs) { lab ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(lab.testName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0284C7))
                                        Text("التكلفة: ${lab.testCost.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (lab.result.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("النتيجة: ${lab.result}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Radiology & Scans
                    item {
                        SectionHeader(title = "3. فحوصات الأشعة والأجهزة", count = patientRadiology.size)
                    }
                    if (patientRadiology.isEmpty()) {
                        item { EmptySectionCard("لا توجد فحوصات أشعة مسجلة") }
                    } else {
                        items(patientRadiology) { rad ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(rad.deviceName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFFD97706))
                                        Text("القيمة: ${rad.cost.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (rad.reportDetails.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("التقرير: ${rad.reportDetails}", fontSize = 12.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Section 4: Pharmacy Purchases
                    item {
                        SectionHeader(title = "4. الأدوية المصروفة من الصيدلية", count = patientPharmacy.size)
                    }
                    if (patientPharmacy.isEmpty()) {
                        item { EmptySectionCard("لا توجد أدوية مصروفة مسجلة") }
                    } else {
                        items(patientPharmacy) { pharm ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${pharm.medicineName} (${pharm.quantity} عبوة)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0D9488))
                                        if (pharm.notes.isNotBlank()) {
                                            Text(pharm.notes, fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                    Text("${pharm.totalAmount.toInt()} ر.ي", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("اكتب اسم المريض أو رقم الهاتف للبحث الفوري عن ملفه الشامل", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = MedicalNavy
        )
        Badge(containerColor = Color(0xFFE2E8F0), contentColor = MedicalNavy) {
            Text("$count", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.5.sp)
        }
    }
}

@Composable
fun EmptySectionCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
            Text(message, fontSize = 12.sp, color = TextMuted)
        }
    }
}
