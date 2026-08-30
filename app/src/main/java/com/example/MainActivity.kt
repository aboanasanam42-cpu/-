package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Ensure RTL layout for Arabic
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MedicalAppContent()
                    }
                }
            }
        }
    }
}

@Composable
fun MedicalAppContent(
    viewModel: MedicalViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    // Handle system back button to return to dashboard
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "ScreenTransition"
    ) { targetScreen ->
        when (targetScreen) {
            AppScreen.DASHBOARD -> MainDashboardScreen(viewModel = viewModel)
            AppScreen.RECEPTION -> ReceptionScreen(viewModel = viewModel)
            AppScreen.DOCTOR -> DoctorScreen(viewModel = viewModel)
            AppScreen.LAB -> LabScreen(viewModel = viewModel)
            AppScreen.RADIOLOGY -> RadiologyScreen(viewModel = viewModel)
            AppScreen.PHARMACY -> PharmacyScreen(viewModel = viewModel)
            AppScreen.EXPENSES -> ExpensesScreen(viewModel = viewModel)
            AppScreen.QUICK_SEARCH -> QuickSearchScreen(viewModel = viewModel)
            AppScreen.INVOICES_REPORTS -> InvoicesReportsScreen(viewModel = viewModel)
            AppScreen.BACKUP -> BackupScreen(viewModel = viewModel)
            AppScreen.AUTO_MESSAGES -> AutoMessagesScreen(viewModel = viewModel)
            AppScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
        }
    }
}
