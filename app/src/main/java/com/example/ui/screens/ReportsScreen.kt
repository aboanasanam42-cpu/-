package com.example.ui.screens

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
import com.example.ui.components.AppHeaderBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel
import com.example.ui.viewmodel.ReportPeriod
import com.example.util.ExcelExporter
import com.example.util.PdfExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPeriod by viewModel.reportPeriod.collectAsStateWithLifecycle()
    val (filteredRecords, filteredExpenses) = viewModel.filteredReportData.collectAsStateWithLifecycle().value

    val totalConsultation = filteredRecords.sumOf { it.consultationFee }
    val totalTreatment = filteredRecords.sumOf { it.totalTreatmentFee }
    val totalRevenue = totalConsultation + totalTreatment
    val totalPaid = filteredRecords.sumOf { it.paidAmount }
    val totalRemaining = filteredRecords.sumOf { it.remainingAmount }

    val doctorShare = filteredRecords.sumOf { it.doctorCommission }
    val labShare = filteredRecords.sumOf { it.labCommission }
    val nurseShare = filteredRecords.sumOf { it.nurseSalaryShare }
    val materialsExp = filteredRecords.sumOf { it.materialExpense }
    val rentExp = filteredRecords.sumOf { it.rentAndUtilities }
    val otherExp = filteredRecords.sumOf { it.otherExpenses }
    val directExpenses = filteredExpenses.sumOf { it.amount }

    val grandTotalExpenses = doctorShare + labShare + nurseShare + materialsExp + rentExp + otherExp + directExpenses
    val netProfit = totalPaid - grandTotalExpenses

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "التقارير المحاسبية والمالية الشاملة",
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
            // Period Tabs (اليومي | الأسبوعي | الشهري | السنوي)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReportPeriod.values().forEach { period ->
                    FilterChip(
                        selected = currentPeriod == period,
                        onClick = { viewModel.setReportPeriod(period) },
                        label = { Text(period.title, fontSize = 11.5.sp, fontWeight = if (currentPeriod == period) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MedicalNavy,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Export Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        PdfExporter.exportComprehensiveMedicalTablePdf(context, filteredRecords, "تقرير_${currentPeriod.title}")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصدير PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        ExcelExporter.exportFinancialSummaryExcel(
                            context,
                            currentPeriod.title,
                            totalRevenue,
                            totalPaid,
                            totalRemaining,
                            grandTotalExpenses,
                            netProfit,
                            filteredRecords,
                            filteredExpenses
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = "Excel", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصدير Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Net Profit Hero Card
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
                                        colors = if (netProfit >= 0) listOf(Color(0xFF065F46), Color(0xFF059669)) else listOf(Color(0xFF991B1B), Color(0xFFDC2626))
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "صافي الأرباح (${currentPeriod.title})",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${netProfit.toInt()} ر.ي",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي المحصل: ${totalPaid.toInt()} ر.ي", fontSize = 12.sp, color = Color.White)
                                Text("إجمالي المصروفات: ${grandTotalExpenses.toInt()} ر.ي", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }
                }

                // Financial Breakdown Grid
                item {
                    Text("التفصيل المحاسبي والمالي:", fontWeight = FontWeight.Bold, color = MedicalNavy, fontSize = 14.sp)
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportMetricCard(
                            title = "إجمالي الإيرادات",
                            amount = totalRevenue,
                            color = MedicalNavy,
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricCard(
                            title = "المحصل (الواصل)",
                            amount = totalPaid,
                            color = Color(0xFF059669),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportMetricCard(
                            title = "المتبقي (ديون المرضى)",
                            amount = totalRemaining,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricCard(
                            title = "إجمالي المصروفات",
                            amount = grandTotalExpenses,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("توزيع التكاليف والنسب:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            ExpenseRowItem("نسب الأطباء المستحقة", doctorShare)
                            ExpenseRowItem("نسب المختبرات والتحاليل", labShare)
                            ExpenseRowItem("رواتب التمريض والمساعدين", nurseShare)
                            ExpenseRowItem("تكاليف المواد والمستلزمات الطبية", materialsExp)
                            ExpenseRowItem("الإيجارات والنثريات والخدمات", rentExp)
                            ExpenseRowItem("خرجيات ومصروفات مباشرة أخرى", directExpenses + otherExp)
                        }
                    }
                }

                // Recent Transactions in period
                item {
                    Text("حالات وسجلات الفترة (${filteredRecords.size} حالة):", fontWeight = FontWeight.Bold, color = MedicalNavy, fontSize = 14.sp)
                }

                items(filteredRecords) { r ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
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
                                Text(r.patientName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MedicalNavy)
                                Text("${r.caseType} | ${r.dateString}", fontSize = 11.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("الواصل: ${r.paidAmount.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                if (r.remainingAmount > 0) {
                                    Text("باقي: ${r.remainingAmount.toInt()} ر.ي", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ReportMetricCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.5.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${amount.toInt()} ر.ي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ExpenseRowItem(title: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 12.sp, color = TextSecondary)
        Text("${amount.toInt()} ر.ي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
