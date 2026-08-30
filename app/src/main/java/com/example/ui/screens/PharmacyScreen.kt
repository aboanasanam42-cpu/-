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
import com.example.data.local.PharmacyRecord
import com.example.ui.components.AppHeaderBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pharmacyRecords by viewModel.allPharmacyRecords.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecords = remember(pharmacyRecords, searchQuery) {
        if (searchQuery.isBlank()) pharmacyRecords
        else pharmacyRecords.filter {
            it.patientName.contains(searchQuery, ignoreCase = true) ||
            it.medicineName.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalPharmacyRevenue = filteredRecords.sumOf { it.totalAmount }
    val totalPharmacyProfit = filteredRecords.sumOf { it.totalAmount - it.costPrice }

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "قسم الصيدلية وصرف الأدوية",
                showBack = true,
                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF0D9488),
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_pharmacy_sale")
            ) {
                Icon(Icons.Default.Medication, contentDescription = "صرف دواء جديد")
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
                                colors = listOf(Color(0xFF115E59), Color(0xFF0D9488))
                            )
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي مبيعات الصيدلية", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("${totalPharmacyRevenue.toInt()} ر.ي", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("صافي ربح المبيعات", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("${totalPharmacyProfit.toInt()} ر.ي", color = Color(0xFFFFE082), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المريض أو اسم الدواء...") },
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
                    Text("لا توجد مبيعات أدوية مسجلة حالياً", color = TextSecondary)
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
                                            viewModel.deletePharmacyRecord(item)
                                            Toast.makeText(context, "تم حذف سجل الدواء", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الدواء: ${item.medicineName} (${item.quantity} عبوة)",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F766E)
                                )

                                if (item.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "الجرعة والملاحظات: ${item.notes}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المبلغ: ${item.totalAmount.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("الواصل: ${item.paidAmount.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
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
        AddPharmacySaleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newRec ->
                viewModel.addPharmacyRecord(newRec)
                showAddDialog = false
                Toast.makeText(context, "تم إضافة قيد الصيدلية بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddPharmacySaleDialog(
    onDismiss: () -> Unit,
    onSave: (PharmacyRecord) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var medicineName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("2500") }
    var costPrice by remember { mutableStateOf("1800") }
    var paidAmount by remember { mutableStateOf("2500") }
    var notes by remember { mutableStateOf("") }

    val qty = quantity.toIntOrNull() ?: 1
    val price = unitPrice.toDoubleOrNull() ?: 0.0
    val total = qty * price
    val cost = costPrice.toDoubleOrNull() ?: (price * 0.7)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("صرف علاج / فاتورة صيدلية", fontWeight = FontWeight.Bold, color = Color(0xFF0D9488), fontSize = 16.sp)
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
                        value = medicineName,
                        onValueChange = { medicineName = it },
                        label = { Text("اسم الدواء / الصنف *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("الكمية") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unitPrice,
                            onValueChange = { unitPrice = it },
                            label = { Text("سعر البيع") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            label = { Text("سعر التكلفة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = paidAmount,
                            onValueChange = { paidAmount = it },
                            label = { Text("المبلغ الواصل") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("تعليمات الاستخدام والملاحظات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (patientName.isNotBlank() && medicineName.isNotBlank()) {
                        val paid = paidAmount.toDoubleOrNull() ?: total
                        val rec = PharmacyRecord(
                            patientName = patientName.trim(),
                            medicineName = medicineName.trim(),
                            quantity = qty,
                            unitPrice = price,
                            totalAmount = total,
                            paidAmount = paid,
                            remainingAmount = (total - paid).coerceAtLeast(0.0),
                            costPrice = cost,
                            notes = notes.trim(),
                            dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )
                        onSave(rec)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
            ) {
                Text("حفظ البيع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}
