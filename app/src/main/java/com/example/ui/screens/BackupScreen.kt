package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.util.BackupHelper
import com.example.util.ExcelExporter
import com.example.util.PdfExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val medicalRecords by viewModel.allMedicalRecords.collectAsStateWithLifecycle()
    val patients by viewModel.allPatients.collectAsStateWithLifecycle()
    val labs by viewModel.allLabRecords.collectAsStateWithLifecycle()
    val rads by viewModel.allRadiologyRecords.collectAsStateWithLifecycle()
    val pharms by viewModel.allPharmacyRecords.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val messages by viewModel.allScheduledMessages.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "الحفظ والنسخ الاحتياطي للبيانات",
                showBack = true,
                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceBackground)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Real-time auto save status card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF86EFAC), Color(0xFF22C55E))))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(28.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الحفظ التلقائي الفوري مُفعل", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF166534))
                            Text("يتم حفظ وتأمين كافة العمليات والبيانات فوراً في قاعدة بيانات الهاتف الداخلية المشفرة بدون الحاجة لإنترنت.", fontSize = 11.5.sp, color = Color(0xFF15803D))
                        }
                    }
                }
            }

            // Database Statistics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إحصائيات قاعدة البيانات المحفوظة:", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MedicalNavy)
                        StatRow("القيود الطبية والمحاسبية", "${medicalRecords.size} قيد")
                        StatRow("ملفات المرضى", "${patients.size} مريض")
                        StatRow("الفحوصات المخبرية", "${labs.size} فحص")
                        StatRow("فحوصات الأشعة والأجهزة", "${rads.size} فحص")
                        StatRow("مبيعات الصيدلية", "${pharms.size} صنف")
                        StatRow("سجلات المصروفات", "${expenses.size} مصروف")
                        StatRow("الرسائل المجدولة", "${messages.size} رسالة")
                    }
                }
            }

            // Export Actions
            item {
                Text("خيارات التصدير والحفظ الخارجي:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MedicalNavy)
            }

            // 1. JSON Backup
            item {
                BackupActionCard(
                    title = "إنشاء نسخة احتياطية شاملة (JSON Backup)",
                    description = "تصدير نسخة احتياطية لكافة بيانات المنظومة لمشاركتها أو حفظها في Google Drive أو ذاكرة الهاتف.",
                    icon = Icons.Default.Backup,
                    buttonText = "تصدير نسخة احتياطية",
                    buttonColor = MedicalNavy,
                    onClick = {
                        BackupHelper.exportBackup(context, medicalRecords, patients, labs, rads, pharms, expenses, messages)
                    }
                )
            }

            // 2. Comprehensive PDF Ledger Export
            item {
                BackupActionCard(
                    title = "تصدير السجل المحاسبي الشامل (PDF)",
                    description = "استخراج ملف PDF جاهز للطباعة والمراجعة مطابق لنموذج المحاسب الطبي الشامل.",
                    icon = Icons.Default.PictureAsPdf,
                    buttonText = "تصدير PDF",
                    buttonColor = Color(0xFFDC2626),
                    onClick = {
                        PdfExporter.exportComprehensiveMedicalTablePdf(context, medicalRecords, "السجل_المحاسبي_الشامل")
                    }
                )
            }

            // 3. Complete Excel Export
            item {
                BackupActionCard(
                    title = "تصدير كافة السجلات إلى Excel (CSV)",
                    description = "تصدير جداول البيانات والقيود المالية إلى ملفات إكسل متوافقة مع Microsoft Excel.",
                    icon = Icons.Default.TableChart,
                    buttonText = "تصدير Excel",
                    buttonColor = Color(0xFF15803D),
                    onClick = {
                        ExcelExporter.exportMedicalRecordsToExcel(context, medicalRecords, "السجل_الطبي_الشامل")
                    }
                )
            }

            // Clear Database Option
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("إعادة ضبط / تهيئة البيانات", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 13.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("مسح كافة السجلات الحالية وبدء سجل محاسبي جديد (تأكد من أخذ نسخة احتياطية أولاً).", fontSize = 11.5.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مسح وتهيئة السجلات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("تأكيد مسح كافة البيانات", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("هل أنت متأكد من رغبتك في مسح كافة السجلات المحفوظة في التطبيق؟ لا يمكن التراجع عن هذا الإجراء إلا بوجود نسخة احتياطية.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllDatabase()
                        showClearDialog = false
                        Toast.makeText(context, "تم مسح البيانات بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، امسح البيانات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun StatRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedicalNavy)
    }
}

@Composable
fun BackupActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    buttonText: String,
    buttonColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = buttonColor, modifier = Modifier.size(24.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 11.5.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
