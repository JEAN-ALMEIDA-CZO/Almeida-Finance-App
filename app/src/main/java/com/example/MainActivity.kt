package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.ui.audit.AuditHistoryScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.SecurityHelper
import com.example.ui.banksync.BankSyncDialog
import com.example.ui.budgets.BudgetsScreen
import com.example.ui.components.QuickActionSpeedDial
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.goals.GoalsScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.FinanGestTheme
import com.example.ui.theme.GoldPrimary
import com.example.ui.transactions.AddEditTransactionDialog
import com.example.ui.transactions.BoletoDetailDialog
import com.example.ui.transactions.SmartBoletoReaderDialog
import com.example.ui.transactions.TransactionsScreen
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.workspace.WorkspaceSelectorDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class MainDestination(val title: String, val icon: ImageVector) {
    DASHBOARD("Início", Icons.Default.Home),
    TRANSACTIONS("Extrato", Icons.Default.Receipt),
    REPORTS("Relatórios", Icons.Default.TrendingUp),
    AUDIT("Histórico", Icons.Default.History),
    GOALS("Metas", Icons.Default.Flag),
    SETTINGS("Ajustes", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val securityHelper = remember { SecurityHelper(context) }
            var themeMode by remember { mutableStateOf(securityHelper.darkModePreference) }

            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            FinanGestTheme(darkTheme = isDarkTheme) {
                AppContent(
                    currentThemeMode = themeMode,
                    onThemeModeChanged = { themeMode = it }
                )
            }
        }
    }
}

@Composable
fun AppContent(
    currentThemeMode: String,
    onThemeModeChanged: (String) -> Unit
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            MainFeatureScreen(
                currentUserId = uid,
                currentThemeMode = currentThemeMode,
                onThemeModeChanged = onThemeModeChanged,
                onSignOutComplete = { currentUser = null }
            )
        }
    }
}

@Composable
fun MainFeatureScreen(
    currentUserId: String,
    currentThemeMode: String,
    onThemeModeChanged: (String) -> Unit,
    onSignOutComplete: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(currentUserId) { FinanceRepository(context) }
    val viewModel = remember(currentUserId) { FinanceViewModel(repository, currentUserId) }

    var currentScreen by remember { mutableStateOf(MainDestination.DASHBOARD) }

    var showSpeedDial by remember { mutableStateOf(false) }
    var addTxInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var showSmartBoletoDialog by remember { mutableStateOf(false) }
    var showWorkspaceDialog by remember { mutableStateOf(false) }
    var showBankSyncDialog by remember { mutableStateOf(false) }
    var selectedBoletoTx by remember { mutableStateOf<Transaction?>(null) }

    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearStatusMessage()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                MainDestination.values().forEach { destination ->
                    val selected = currentScreen == destination
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldPrimary,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                MainDestination.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTransactions = { currentScreen = MainDestination.TRANSACTIONS },
                        onNavigateToReports = { currentScreen = MainDestination.REPORTS },
                        onNavigateToAudit = { currentScreen = MainDestination.AUDIT },
                        onOpenAddTransaction = {
                            addTxInitialType = TransactionType.EXPENSE
                            showAddTxDialog = true
                        },
                        onOpenSmartBoletoReader = { showSmartBoletoDialog = true },
                        onOpenWorkspaceSelector = { showWorkspaceDialog = true },
                        onOpenBankSync = { showBankSyncDialog = true },
                        onOpenBoletoDetail = { tx -> selectedBoletoTx = tx }
                    )
                }
                MainDestination.TRANSACTIONS -> {
                    TransactionsScreen(
                        viewModel = viewModel,
                        onOpenAddTransaction = {
                            addTxInitialType = TransactionType.EXPENSE
                            showAddTxDialog = true
                        },
                        onOpenSmartBoletoReader = { showSmartBoletoDialog = true },
                        onOpenBoletoDetail = { tx -> selectedBoletoTx = tx }
                    )
                }
                MainDestination.REPORTS -> {
                    ReportsScreen(
                        viewModel = viewModel
                    )
                }
                MainDestination.AUDIT -> {
                    AuditHistoryScreen(
                        viewModel = viewModel
                    )
                }
                MainDestination.GOALS -> {
                    GoalsScreen(
                        viewModel = viewModel
                    )
                }
                MainDestination.SETTINGS -> {
                    SettingsScreen(
                        currentThemeMode = currentThemeMode,
                        onThemeModeChanged = onThemeModeChanged,
                        onSignOutComplete = onSignOutComplete
                    )
                }
            }

            if (showAddTxDialog) {
                AddEditTransactionDialog(
                    currentUserId = currentUserId,
                    onDismiss = { showAddTxDialog = false },
                    onSave = { newTx ->
                        viewModel.addTransaction(newTx)
                    },
                    initialType = addTxInitialType
                )
            }

            if (showSmartBoletoDialog) {
                SmartBoletoReaderDialog(
                    currentUserId = currentUserId,
                    onDismiss = { showSmartBoletoDialog = false },
                    onSaveInstallments = { installments ->
                        viewModel.addInstallmentTransactions(installments)
                        showSmartBoletoDialog = false
                    }
                )
            }

            if (showWorkspaceDialog) {
                WorkspaceSelectorDialog(
                    viewModel = viewModel,
                    onDismiss = { showWorkspaceDialog = false }
                )
            }

            if (showBankSyncDialog) {
                BankSyncDialog(
                    viewModel = viewModel,
                    onDismiss = { showBankSyncDialog = false }
                )
            }

            if (selectedBoletoTx != null) {
                BoletoDetailDialog(
                    transaction = selectedBoletoTx!!,
                    onDismiss = { selectedBoletoTx = null },
                    onTogglePaid = {
                        viewModel.togglePaidStatus(selectedBoletoTx!!)
                        selectedBoletoTx = null
                    }
                )
            }

            // Menu Rápido Flutuante (Speed Dial) estilo Apple
            QuickActionSpeedDial(
                isOpen = showSpeedDial,
                onToggle = { showSpeedDial = !showSpeedDial },
                onAddIncome = {
                    addTxInitialType = TransactionType.INCOME
                    showAddTxDialog = true
                },
                onAddExpense = {
                    addTxInitialType = TransactionType.EXPENSE
                    showAddTxDialog = true
                },
                onReadBoleto = {
                    showSmartBoletoDialog = true
                },
                onBankSync = {
                    showBankSyncDialog = true
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
