package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.InteractiveExpensePieChart
import com.example.ui.components.IosBalanceDashboardCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.FixedExpensePurple
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.VariableExpenseAmber
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToAudit: () -> Unit,
    onOpenAddTransaction: () -> Unit,
    onOpenSmartBoletoReader: () -> Unit,
    onOpenWorkspaceSelector: () -> Unit,
    onOpenBankSync: () -> Unit,
    onOpenBoletoDetail: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val transactionsUiState by viewModel.transactionsState.collectAsState()
    val isBankSyncing by viewModel.isBankSyncing.collectAsState()
    val activeWorkspace by viewModel.selectedWorkspace.collectAsState()

    var hideBalance by remember { mutableStateOf(false) }

    val transactions = (transactionsUiState as? UiState.Success)?.data ?: emptyList()

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val currentMonth = remember { DateTimeUtils.getCurrentYearMonth() }
    val monthlyTransactions = remember(transactions, currentMonth) {
        transactions.filter { tx ->
            tx.date.startsWith(currentMonth) || tx.dueDate.startsWith(currentMonth)
        }
    }
    val monthlyIncome = remember(monthlyTransactions, totalIncome) {
        val curr = monthlyTransactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        if (curr > 0.0 || monthlyTransactions.isNotEmpty()) curr else totalIncome
    }
    val monthlyExpense = remember(monthlyTransactions, totalExpense) {
        val curr = monthlyTransactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        if (curr > 0.0 || monthlyTransactions.isNotEmpty()) curr else totalExpense
    }

    val fixedExpenses = transactions.filter { it.type == TransactionType.EXPENSE.name && it.isFixed }.sumOf { it.amount }
    val variableExpenses = transactions.filter { it.type == TransactionType.EXPENSE.name && !it.isFixed }.sumOf { it.amount }

    val todayStr = remember { DateTimeUtils.getTodayIso() }
    val pendingDueSoon = transactions.filter {
        !it.isPaid && it.dueDate.isNotBlank() && it.dueDate <= todayStr
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Top Bar & Active Workspace Indicator
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Almeida Finance",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nuvem Sincronizada",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen,
                            maxLines = 1
                        )
                    }
                }

                // Workspace Badge / Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { onOpenWorkspaceSelector() }
                        .testTag("workspace_selector_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (activeWorkspace != null) Icons.Default.Group else Icons.Default.Person,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeWorkspace?.name ?: "Espaço Pessoal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Minimalist iOS Dashboard Component: Saldo Atual, Receitas do Mês e Despesas do Mês
        item {
            IosBalanceDashboardCard(
                currentBalance = netBalance,
                monthlyIncome = monthlyIncome,
                monthlyExpense = monthlyExpense,
                isBalanceHidden = hideBalance,
                onToggleBalanceVisibility = { hideBalance = !hideBalance },
                workspaceName = activeWorkspace?.name,
                onWorkspaceClick = onOpenWorkspaceSelector
            )
        }

        // Quick Action Carousel (4 prominent fluid actions)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Action 1: Add Transaction
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenAddTransaction() }
                        .testTag("quick_add_tx_button")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF090C12), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Lançar", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Action 2: Boleto Scanner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenSmartBoletoReader() }
                        .testTag("quick_boleto_reader_button")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Ler Boleto", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Action 3: Audit History
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToAudit() }
                        .testTag("quick_audit_button")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Histórico", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Action 4: Bank Sync
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenBankSync() }
                        .testTag("quick_bank_sync_button")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Bancos", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }

        // Gráfico de Pizza / Rosca Interativo estilo Recharts (Despesas por Categoria)
        item {
            InteractiveExpensePieChart(
                transactions = transactions,
                onCategoryClick = { /* interactive selection handled internally */ }
            )
        }

        // Smart Boleto Reader Feature Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSmartBoletoReader() }
                    .testTag("smart_boleto_banner"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "PDF Boleto",
                                tint = Color(0xFF090C12),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Leitor de Boletos & PDFs",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GoldPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GoldPrimary)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("IA", fontSize = 9.sp, color = Color(0xFF090C12), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Text(
                                text = "Lança parcelas e carnês automaticamente",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onOpenSmartBoletoReader,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Ler Boleto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF090C12), maxLines = 1)
                    }
                }
            }
        }

        // Fixed vs Variable Ratio Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gastos Fixos vs Variáveis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Total: R$ ${String.format(Locale.getDefault(), "%,.2f", totalExpense)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val fixedRatio = if (totalExpense > 0) (fixedExpenses / totalExpense).toFloat() else 0.5f

                    LinearProgressIndicator(
                        progress = { fixedRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = FixedExpensePurple,
                        trackColor = VariableExpenseAmber
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FixedExpensePurple))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Fixos: R$ ${String.format(Locale.getDefault(), "%,.2f", fixedExpenses)}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(VariableExpenseAmber))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Variáveis: R$ ${String.format(Locale.getDefault(), "%,.2f", variableExpenses)}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Pending Due Date Alerts (if any)
        if (pendingDueSoon.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Vencimentos Próximos / Pendentes (${pendingDueSoon.size})",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        pendingDueSoon.take(3).forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onOpenBoletoDetail(tx) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Venceu/Vence em: ${DateTimeUtils.formatIsoToBrazilian(tx.dueDate)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ExpenseRed
                                    )
                                }
                                Text(
                                    text = "R$ ${String.format(Locale.getDefault(), "%,.2f", tx.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Movements Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Movimentações Recentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Ver Todas",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToTransactions() }
                )
            }
        }

        // Recent items list
        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nenhuma movimentação registrada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(transactions.take(8), key = { it.id.ifEmpty { it.hashCode().toString() } }) { tx ->
                val isIncome = tx.type == TransactionType.INCOME.name
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBoletoDetail(tx) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isIncome) IncomeGreen.copy(alpha = 0.2f) else ExpenseRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isIncome) IncomeGreen else ExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = tx.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${tx.category} • ${DateTimeUtils.formatIsoToBrazilian(tx.date)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    if (tx.authorName.isNotBlank()) {
                                        Text(
                                            text = " • ${tx.authorName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (isIncome) "+" else "-"} R$ ${String.format(Locale.getDefault(), "%,.2f", tx.amount)}",
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) IncomeGreen else ExpenseRed,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                text = if (tx.isPaid) "Pago" else "Pendente",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tx.isPaid) IncomeGreen else VariableExpenseAmber,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
