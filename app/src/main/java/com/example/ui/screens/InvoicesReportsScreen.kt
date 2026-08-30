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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppHeaderBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel
import com.example.util.ExcelExporter
import com.example.util.MessageHelper
import com.example.util.PdfExporter
import com.example.util.PrintHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesReportsScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allRecords by viewModel.allMedicalRecords.collectAsStateWithLifecycle()
    val patients by viewModel.allPatients.collectAsStateWithLifecycle()

    var selectedPatient by remember { mutableStateOf("") }
    var expandedPatientDropdown by remember { mutableStateOf(false) }

    // Distinct patient names from records and patient entities
    val patientNames = remember(allRecords, patients) {
        (allRecords.map { it.patientName } + patients.map { it.name }).distinct().filter { it.isNotBlank() }
    }

    LaunchedEffect(patientNames) {
        if (selectedPatient.isBlank() && patientNames.isNotEmpty()) {
            selectedPatient = patientNames.first()
        }
    }

    val patientRecords = remember(selectedPatient, allRecords) {
        allRecords.filter { it.patientName.equals(selectedPatient, ignoreCase = true) }
    }

    val patientPhone = remember(selectedPatient, patients, patientRecords) {
        patients.find { it.name == selectedPatient }?.phone
            ?: patientRecords.firstOrNull()?.patientPhone ?: ""
    }

    val totalCost = patientRecords.sumOf { it.consultationFee + it.totalTreatmentFee }
    val totalPaid = patientRecords.sumOf { it.paidAmount }
    val totalRemaining = patientRecords.sumOf { it.remainingAmount }

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "الفواتير وسندات كشف حساب المرضى",
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
            // Patient selector
            ExposedDropdownMenuBox(
                expanded = expandedPatientDropdown,
                onExpandedChange = { expandedPatientDropdown = !expandedPatientDropdown },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = selectedPatient.ifEmpty { "اختر مريضاً لإنشاء الفاتورة" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("المريض المحدد للفاتورة") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPatientDropdown) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MedicalNavy) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expandedPatientDropdown,
                    onDismissRequest = { expandedPatientDropdown = false }
                ) {
                    patientNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                selectedPatient = name
                                expandedPatientDropdown = false
                            }
                        )
                    }
                }
            }

            // Quick Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Print Button
                Button(
                    onClick = {
                        if (patientRecords.isNotEmpty()) {
                            PrintHelper.printInvoice(context, selectedPatient, patientPhone, patientRecords)
                        } else {
                            Toast.makeText(context, "لا توجد سجلات طبية لهذا المريض", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = "طباعة", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("طباعة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // PDF Button
                Button(
                    onClick = {
                        if (patientRecords.isNotEmpty()) {
                            PdfExporter.exportPatientInvoicePdf(context, selectedPatient, patientPhone, patientRecords)
                        } else {
                            Toast.makeText(context, "لا توجد سجلات", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // Excel Button
                Button(
                    onClick = {
                        if (patientRecords.isNotEmpty()) {
                            ExcelExporter.exportMedicalRecordsToExcel(context, patientRecords, "كشف_حساب_${selectedPatient}")
                        } else {
                            Toast.makeText(context, "لا توجد سجلات", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = "Excel", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Excel", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // WhatsApp Send Invoice
                Button(
                    onClick = {
                        if (patientPhone.isNotEmpty()) {
                            val msg = "عزيزنا المريض $selectedPatient، إليكم ملخص كشف الحساب:\nإجمالي التكلفة: ${totalCost.toInt()} ر.ي\nالمسدد: ${totalPaid.toInt()} ر.ي\nالمتبقي: ${totalRemaining.toInt()} ر.ي\nشكراً لزيارتكم - التطبيق المحاسبي الطبي الشامل."
                            MessageHelper.sendWhatsAppMessage(context, patientPhone, msg)
                        } else {
                            Toast.makeText(context, "يرجى تسجيل رقم هاتف المريض أولاً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "واتساب", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("واتساب", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Real-time Preview Invoice Sheet
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Invoice Document Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("التطبيق المحاسبي الطبي الشامل", fontWeight = FontWeight.Bold, color = MedicalNavy, fontSize = 14.sp)
                            Text("سند وفاتورة علاجية", fontSize = 11.5.sp, color = TextSecondary)
                        }
                        Badge(containerColor = Color(0xFFE0E7FF), contentColor = MedicalNavy) {
                            Text("رقم الفاتورة: INV-${(selectedPatient.hashCode() and 0xFFFF)}", modifier = Modifier.padding(4.dp), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Patient Details Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("اسم المريض: $selectedPatient", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("الهاتف: ${if (patientPhone.isNotEmpty()) patientPhone else "غير مسجل"}", fontSize = 12.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Items table header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F2B5C), RoundedCornerShape(6.dp))
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الخدمة / المعاينة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("التكلفة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("الواصل", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("الباقي", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }

                    // Items list
                    if (patientRecords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد خدمات مسجلة لهذا المريض", color = TextMuted)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(patientRecords) { item ->
                                val total = item.consultationFee + item.totalTreatmentFee
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(item.caseType, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        if (item.diagnosis.isNotBlank()) {
                                            Text(item.diagnosis, fontSize = 10.5.sp, color = TextSecondary, maxLines = 1)
                                        }
                                    }
                                    Text("${total.toInt()}", fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                                    Text("${item.paidAmount.toInt()}", fontSize = 11.5.sp, color = Color(0xFF059669), modifier = Modifier.weight(1f))
                                    Text("${item.remainingAmount.toInt()}", fontSize = 11.5.sp, color = if (item.remainingAmount > 0) Color(0xFFDC2626) else TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }

                    // Totals summary card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الإجمالي: ${totalCost.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                            Text("الواصل: ${totalPaid.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            Text("الباقي: ${totalRemaining.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }
    }
}
