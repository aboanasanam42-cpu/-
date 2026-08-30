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
import com.example.data.local.RadiologyRecord
import com.example.ui.components.AppHeaderBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadiologyScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val radiologyRecords by viewModel.allRadiologyRecords.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecords = remember(radiologyRecords, searchQuery) {
        if (searchQuery.isBlank()) radiologyRecords
        else radiologyRecords.filter {
            it.patientName.contains(searchQuery, ignoreCase = true) ||
            it.deviceName.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalRadiologyRevenue = filteredRecords.sumOf { it.cost }

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "قسم الأشعة والأجهزة التشخيصية",
                showBack = true,
                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFD97706),
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_radiology")
            ) {
                Icon(Icons.Default.Sensors, contentDescription = "إضافة فحص أشعة / جهاز")
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
            // Stats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .shadow(3.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFB45309), Color(0xFFD97706))
                            )
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي إيرادات الأشعة والأجهزة", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("${totalRadiologyRevenue.toInt()} ر.ي", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("عدد الفحوصات المنفذة", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("${filteredRecords.size} فحص", color = Color(0xFFFFE082), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المريض أو نوع الجهاز...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد فحوصات أشعة مسجلة حالياً", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRecords) { item ->
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
                                        text = item.patientName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedicalNavy
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteRadiologyRecord(item)
                                            Toast.makeText(context, "تم حذف سجل الأشعة", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الجهاز / نوع الأشعة: ${item.deviceName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB45309)
                                )

                                if (item.reportDetails.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "التقرير الطبي: ${item.reportDetails}",
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("القيمة: ${item.cost.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("نسبة المشغل: ${item.operatorShare.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                    Text("التاريخ: ${item.dateString}", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRadiologyDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newRec ->
                viewModel.addRadiologyRecord(newRec)
                showAddDialog = false
                Toast.makeText(context, "تم إضافة فحص الأشعة بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddRadiologyDialog(
    onDismiss: () -> Unit,
    onSave: (RadiologyRecord) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var patientPhone by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("جهاز السونار (Ultrasound)") }
    var cost by remember { mutableStateOf("6000") }
    var operatorShare by remember { mutableStateOf("2000") }
    var reportDetails by remember { mutableStateOf("") }

    val commonDevices = listOf(
        "جهاز السونار (Ultrasound)",
        "أشعة سينية (X-Ray)",
        "أشعة مقطعية (CT-Scan)",
        "تخطيط القلب (ECG)",
        "رنين مغناطيسي (MRI)",
        "بانوراما الأسنان (Dental Panorama)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تسجيل فحص جهاز / أشعة جديد", fontWeight = FontWeight.Bold, color = Color(0xFFD97706), fontSize = 16.sp)
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
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("نوع الجهاز أو الأشعة *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cost,
                            onValueChange = { cost = it },
                            label = { Text("القيمة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = operatorShare,
                            onValueChange = { operatorShare = it },
                            label = { Text("نسبة المشغل") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        label = { Text("تقرير الفحص والنتيجة التشخيصية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (patientName.isNotBlank() && deviceName.isNotBlank()) {
                        val c = cost.toDoubleOrNull() ?: 0.0
                        val op = operatorShare.toDoubleOrNull() ?: 0.0
                        val rec = RadiologyRecord(
                            patientName = patientName.trim(),
                            patientPhone = patientPhone.trim(),
                            deviceName = deviceName.trim(),
                            cost = c,
                            paidAmount = c,
                            remainingAmount = 0.0,
                            operatorShare = op,
                            reportDetails = reportDetails.trim(),
                            dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )
                        onSave(rec)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("حفظ الفحص")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}
