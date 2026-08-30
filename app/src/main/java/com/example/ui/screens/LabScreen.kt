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
import com.example.data.local.LabRecord
import com.example.ui.components.AppHeaderBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val labRecords by viewModel.allLabRecords.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLab = remember(labRecords, searchQuery) {
        if (searchQuery.isBlank()) labRecords
        else labRecords.filter { it.patientName.contains(searchQuery, ignoreCase = true) || it.testName.contains(searchQuery, ignoreCase = true) }
    }

    val totalLabRevenue = filteredLab.sumOf { it.testCost }
    val totalLabCommission = filteredLab.sumOf { it.labCommission }

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "قسم المختبرات والتحاليل الطبية",
                showBack = true,
                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF0284C7),
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_lab_test")
            ) {
                Icon(Icons.Default.Science, contentDescription = "إضافة فحص مخبري")
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
            // Lab Stats
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .shadow(3.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0369A1), Color(0xFF0284C7))
                            )
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إيرادات المختبر الإجمالية", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("${totalLabRevenue.toInt()} ر.ي", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("مستحقات فني المختبر", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("${totalLabCommission.toInt()} ر.ي", color = Color(0xFFFFD54F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المريض أو الفحص المخبري...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Lab tests list
            if (filteredLab.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد فحوصات مخبرية مسجلة حالياً", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLab) { item ->
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
                                            viewModel.deleteLabRecord(item)
                                            Toast.makeText(context, "تم حذف الفحص", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "نوع الفحص: ${item.testName}",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0369A1)
                                )

                                if (item.result.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "النتيجة: ${item.result}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF047857)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("التكلفة: ${item.testCost.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("نسبة المخبري: ${item.labCommission.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
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
        AddLabTestDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newLab ->
                viewModel.addLabRecord(newLab)
                showAddDialog = false
                Toast.makeText(context, "تم إضافة الفحص المخبري بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddLabTestDialog(
    onDismiss: () -> Unit,
    onSave: (LabRecord) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var patientPhone by remember { mutableStateOf("") }
    var testName by remember { mutableStateOf("فحص دم عام CBC") }
    var testCost by remember { mutableStateOf("3000") }
    var labCommission by remember { mutableStateOf("1200") }
    var result by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تسجيل فحص مخبري جديد", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("اسم المريض *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = patientPhone,
                    onValueChange = { patientPhone = it },
                    label = { Text("رقم الهاتف") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = testName,
                    onValueChange = { testName = it },
                    label = { Text("اسم الفحص المخبري *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = testCost,
                        onValueChange = { testCost = it },
                        label = { Text("قيمة الفحص") },
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
                OutlinedTextField(
                    value = result,
                    onValueChange = { result = it },
                    label = { Text("نتيجة الفحص (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (patientName.isNotBlank() && testName.isNotBlank()) {
                        val cost = testCost.toDoubleOrNull() ?: 0.0
                        val comm = labCommission.toDoubleOrNull() ?: 0.0
                        val rec = LabRecord(
                            patientName = patientName.trim(),
                            patientPhone = patientPhone.trim(),
                            testName = testName.trim(),
                            testCost = cost,
                            labCommission = comm,
                            paidAmount = cost,
                            remainingAmount = 0.0,
                            result = result.trim(),
                            notes = notes.trim(),
                            dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )
                        onSave(rec)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
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
