package com.example.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.FixedExpensePurple
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.VariableExpenseAmber
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MonthlyCashFlowItem
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactionsUiState by viewModel.transactionsState.collectAsState()
    val transactions = (transactionsUiState as? UiState.Success)?.data ?: emptyList()

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    // Group transactions by month (YYYY-MM)
    val monthlyData = remember(transactions) {
        val grouped = transactions.groupBy { tx ->
            if (tx.date.length >= 7) tx.date.substring(0, 7) else "Outros"
        }
        grouped.map { (month, list) ->
            val inc = list.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val exp = list.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            MonthlyCashFlowItem(
                month = month,
                income = inc,
                expense = exp,
                balance = inc - exp
            )
        }.sortedByDescending { it.month }
    }

    // Expense by category
    val categoryExpenses = remember(transactions) {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE.name }
        val sumTotal = expenses.sumOf { it.amount }.coerceAtLeast(0.01)
        expenses.groupBy { it.category }
            .map { (cat, list) ->
                val amount = list.sumOf { it.amount }
                cat to (amount to (amount / sumTotal).toFloat())
            }
            .sortedByDescending { it.second.first }
    }

    val userEmail = Firebase.auth.currentUser?.email

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Relatórios & Fluxo de Caixa",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Análise detalhada de desempenho financeiro e exportações",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Executive Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Desempenho Geral Consolidado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Receitas",
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Entradas", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = "R$ ${String.format(Locale.getDefault(), "%,.2f", totalIncome)}",
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen,
                                fontSize = 16.sp
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = "Despesas",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Saídas", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = "R$ ${String.format(Locale.getDefault(), "%,.2f", totalExpense)}",
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed,
                                fontSize = 16.sp
                            )
                        }

                        Column {
                            Text("Saldo Líquido", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "R$ ${String.format(Locale.getDefault(), "%,.2f", netBalance)}",
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance >= 0) IncomeGreen else ExpenseRed,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Cash Flow Bar Chart (Canvas)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Fluxo de Caixa Mensal (Canvas)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (monthlyData.isEmpty()) {
                        Text(
                            text = "Sem transações suficientes para gerar o gráfico de fluxo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val maxVal = monthlyData.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(100.0) ?: 100.0

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            val barWidth = (size.width / (monthlyData.take(5).size * 3)).coerceAtLeast(18f)
                            val spacing = barWidth * 3

                            monthlyData.take(5).reversed().forEachIndexed { index, item ->
                                val xStart = index * spacing + spacing / 2

                                val incomeHeight = (item.income / maxVal * size.height).toFloat()
                                val expenseHeight = (item.expense / maxVal * size.height).toFloat()

                                // Income Bar (Green)
                                drawRect(
                                    color = Color(0xFF10B981),
                                    topLeft = Offset(xStart, size.height - incomeHeight),
                                    size = Size(barWidth, incomeHeight)
                                )

                                // Expense Bar (Red)
                                drawRect(
                                    color = Color(0xFFEF4444),
                                    topLeft = Offset(xStart + barWidth + 4f, size.height - expenseHeight),
                                    size = Size(barWidth, expenseHeight)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(IncomeGreen))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Entradas", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ExpenseRed))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Saídas", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Category Expense Distribution
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Gastos por Categoria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (categoryExpenses.isEmpty()) {
                        Text(
                            text = "Nenhuma despesa para exibir na distribuição.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categoryExpenses.forEach { (cat, pair) ->
                            val amount = pair.first
                            val ratio = pair.second

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "R$ ${String.format(Locale.getDefault(), "%.2f", amount)} (${(ratio * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = EmeraldPrimary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export Actions Section
        item {
            Text(
                text = "Exportação de Dados & Notificações",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Export CSV
                Button(
                    onClick = { viewModel.exportToCSV(context, transactions) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_csv_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Exportar CSV", tint = Color(0xFF090C12))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Dados CSV", color = Color(0xFF090C12), fontWeight = FontWeight.Bold, maxLines = 1)
                }

                // Export PDF
                Button(
                    onClick = { viewModel.exportToPdfReport(context, transactions, userEmail) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_pdf_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF", tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Relatório em PDF", color = GoldPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                // Email notification
                OutlinedButton(
                    onClick = { viewModel.sendEmailSummary(context, userEmail, transactions) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_summary_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = "Enviar por E-mail")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Resumo por E-mail", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
