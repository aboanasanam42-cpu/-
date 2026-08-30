package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppFooterBanner
import com.example.ui.components.AppHeaderBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel

@Composable
fun MainDashboardScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE9EDF2))
    ) {
        // App Top Header Pill
        AppHeaderBanner(title = "التطبيق المحاسبي الطبي الشامل")

        // Dashboard Grid in a smooth LazyColumn to fit all screen sizes and foldables nicely
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: الاستقبال | الطبيب | المختبرات (Reversed for RTL layout)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Right in RTL: الاستقبال
                    DashboardTileTop(
                        title = "الاستقبال",
                        icon = Icons.Default.SupportAgent,
                        bottomStripeColor = Color(0xFF1E3A8A), // Navy Blue
                        iconColor = Color(0xFF1E3A8A),
                        tag = "tile_reception",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.RECEPTION) }
                    )

                    // Middle: الطبيب
                    DashboardTileTop(
                        title = "الطبيب",
                        icon = Icons.Default.MedicalServices,
                        bottomStripeColor = Color(0xFF10B981), // Green
                        iconColor = Color(0xFF059669),
                        tag = "tile_doctor",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.DOCTOR) }
                    )

                    // Left: المختبرات
                    DashboardTileTop(
                        title = "المختبرات",
                        icon = Icons.Default.Science,
                        bottomStripeColor = Color(0xFF0284C7), // Blue/Cyan
                        iconColor = Color(0xFF0284C7),
                        tag = "tile_lab",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.LAB) }
                    )
                }
            }

            // Row 2: الاشعة والاجهزة الاخرى | الصيدلية | الخرجيات العامة
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Right: الاشعة والاجهزة الأخرى
                    DashboardTileTop(
                        title = "الاشعة والاجهزة الأخرى",
                        icon = Icons.Default.Sensors,
                        bottomStripeColor = Color(0xFFD97706), // Amber/Brown
                        iconColor = Color(0xFFD97706),
                        tag = "tile_radiology",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.RADIOLOGY) }
                    )

                    // Middle: الصيدلية
                    DashboardTileTop(
                        title = "الصيدلية",
                        icon = Icons.Default.Medication,
                        bottomStripeColor = Color(0xFF0D9488), // Teal
                        iconColor = Color(0xFF0D9488),
                        tag = "tile_pharmacy",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.PHARMACY) }
                    )

                    // Left: الخرجيات العامة
                    DashboardTileTop(
                        title = "الخرجيات العامة",
                        icon = Icons.Default.PointOfSale,
                        bottomStripeColor = Color(0xFF7C3AED), // Purple
                        iconColor = Color(0xFF7C3AED),
                        tag = "tile_expenses",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.EXPENSES) }
                    )
                }
            }

            // Row 3: البحث السريع | الفواتير / التقارير / كشف حساب المرضى | الحفظ / النسخ الاحتياطية
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Right: البحث السريع
                    DashboardTileSideAccent(
                        title = "البحث السريع",
                        icon = Icons.Default.Search,
                        accentColor = Color(0xFF0284C7),
                        tag = "tile_quick_search",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.QUICK_SEARCH) }
                    )

                    // Middle: الفواتير / التقارير / كشف حساب المرضى
                    DashboardTileSideAccent(
                        title = "الفواتير / التقارير / كشف حساب المرضى",
                        icon = Icons.Default.Calculate,
                        accentColor = Color(0xFF059669),
                        tag = "tile_invoices_statements",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.INVOICES_REPORTS) }
                    )

                    // Left: الحفظ / النسخ الاحتياطية
                    DashboardTileSideAccent(
                        title = "الحفظ / النسخ الاحتياطية",
                        icon = Icons.Default.CloudSync,
                        accentColor = Color(0xFF0284C7),
                        tag = "tile_backup",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.BACKUP) }
                    )
                }
            }

            // Row 4: الرسائل التلقائية للأمراض وللمرتبطين بمكان العمل | طباعة الفواتير | التقارير
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Right: الرسائل التلقائية للأمراض وللمرتبطين بمكان العمل
                    DashboardTileSideAccent(
                        title = "الرسائل التلقائية للأمراض وللمرتبطين بمكان العمل",
                        icon = Icons.Default.MarkEmailRead,
                        accentColor = Color(0xFFEA580C),
                        tag = "tile_auto_messages",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.AUTO_MESSAGES) }
                    )

                    // Middle: طباعة الفواتير
                    DashboardTileSideAccent(
                        title = "طباعة الفواتير",
                        icon = Icons.Default.Print,
                        accentColor = Color(0xFF10B981),
                        tag = "tile_print_invoices",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.INVOICES_REPORTS) }
                    )

                    // Left: التقارير
                    DashboardTileSideAccent(
                        title = "التقارير",
                        icon = Icons.Default.PieChart,
                        accentColor = Color(0xFF2563EB),
                        tag = "tile_reports",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.REPORTS) }
                    )
                }
            }

            // Footer credits banner
            item {
                AppFooterBanner()
            }
        }
    }
}

/**
 * Top 6 Department Tiles with solid bottom colored accent stripe
 */
@Composable
fun DashboardTileTop(
    title: String,
    icon: ImageVector,
    bottomStripeColor: Color,
    iconColor: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp, start = 6.dp, end = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }

            // Bottom Colored Stripe matching original UI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(bottomStripeColor)
            )
        }
    }
}

/**
 * Bottom 6 Service Tiles with stylish curved side/corner accent
 */
@Composable
fun DashboardTileSideAccent(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(125.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Diagonal/Corner Accent Ribbon
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(accentColor)
                    .align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
