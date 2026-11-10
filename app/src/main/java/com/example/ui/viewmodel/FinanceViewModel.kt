package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLog
import com.example.data.model.Budget
import com.example.data.model.Goal
import com.example.data.model.NotificationAlert
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.Workspace
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MonthlyCashFlowItem(
    val month: String,
    val income: Double,
    val expense: Double,
    val balance: Double
)

data class CategoryExpenseItem(
    val category: String,
    val amount: Double,
    val percentage: Float
)

class FinanceViewModel(
    private val repository: FinanceRepository,
    val currentUserId: String
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBankSyncing = MutableStateFlow(false)
    val isBankSyncing: StateFlow<Boolean> = _isBankSyncing.asStateFlow()

    // Multi-user shared workspace
    private val _selectedWorkspace = MutableStateFlow<Workspace?>(null)
    val selectedWorkspace: StateFlow<Workspace?> = _selectedWorkspace.asStateFlow()

    val userWorkspacesState: StateFlow<UiState<List<Workspace>>> = repository.observeWorkspaces()
        .map<List<Workspace>, UiState<List<Workspace>>> { UiState.Success(it) }
        .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Erro ao carregar espaços")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactionsState: StateFlow<UiState<List<Transaction>>> = _selectedWorkspace
        .flatMapLatest { ws -> repository.observeTransactions(ws?.id) }
        .map<List<Transaction>, UiState<List<Transaction>>> { UiState.Success(it) }
        .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Erro ao carregar transações")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val goalsState: StateFlow<UiState<List<Goal>>> = _selectedWorkspace
        .flatMapLatest { ws -> repository.observeGoals(ws?.id) }
        .map<List<Goal>, UiState<List<Goal>>> { UiState.Success(it) }
        .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Erro ao carregar metas")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetsState: StateFlow<UiState<List<Budget>>> = _selectedWorkspace
        .flatMapLatest { ws -> repository.observeBudgets(ws?.id) }
        .map<List<Budget>, UiState<List<Budget>>> { UiState.Success(it) }
        .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Erro ao carregar orçamentos")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val notificationsState: StateFlow<UiState<List<NotificationAlert>>> = _selectedWorkspace
        .flatMapLatest { ws -> repository.observeNotifications(ws?.id) }
        .map<List<NotificationAlert>, UiState<List<NotificationAlert>>> { UiState.Success(it) }
        .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Erro ao carregar alertas")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val auditLogsState: StateFlow<UiState<List<AuditLog>>> = _selectedWorkspace
        .flatMapLatest { ws ->
            if (ws != null) repository.observeAuditLogs(ws.id)
            else flowOf(emptyList())
        }
        .map<List<AuditLog>, UiState<List<AuditLog>>> { UiState.Success(it) }
        .catch { e -> emit(UiState.Error(e.localizedMessage ?: "Erro ao carregar histórico")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // --- WORKSPACE COLLABORATION ---

    fun selectWorkspace(workspace: Workspace?) {
        _selectedWorkspace.value = workspace
        _statusMessage.value = if (workspace != null) "Espaço ativo: ${workspace.name}" else "Modo pessoal ativado"
    }

    fun createWorkspace(name: String) {
        viewModelScope.launch {
            try {
                val ws = repository.createWorkspace(name)
                _selectedWorkspace.value = ws
                _statusMessage.value = "Espaço '${ws.name}' criado! Código: ${ws.inviteCode}"
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao criar espaço: ${e.message}"
            }
        }
    }

    fun joinWorkspace(inviteCode: String) {
        viewModelScope.launch {
            try {
                val ws = repository.joinWorkspaceByInviteCode(inviteCode)
                if (ws != null) {
                    _selectedWorkspace.value = ws
                    _statusMessage.value = "Vinculado ao espaço '${ws.name}' com sucesso!"
                } else {
                    _statusMessage.value = "Código de convite não encontrado."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao vincular: ${e.message}"
            }
        }
    }

    // --- TRANSACTIONS ---

    fun addTransaction(tx: Transaction) {
        viewModelScope.launch {
            try {
                val wsId = _selectedWorkspace.value?.id
                repository.addTransaction(tx, wsId)
                _statusMessage.value = "Transação registrada com sucesso!"
                checkBudgetAndDueDateAlerts(tx)
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao salvar transação: ${e.message}"
            }
        }
    }

    fun addInstallmentTransactions(transactions: List<Transaction>) {
        viewModelScope.launch {
            try {
                val wsId = _selectedWorkspace.value?.id
                for (tx in transactions) {
                    repository.addTransaction(tx, wsId)
                }
                _statusMessage.value = "${transactions.size} parcela(s) lançada(s) no sistema com sucesso!"
                if (transactions.isNotEmpty()) {
                    repository.addNotification(
                        NotificationAlert(
                            userId = currentUserId,
                            workspaceId = wsId ?: "",
                            title = "Boleto Parcelado Lançado",
                            message = "${transactions.size} parcela(s) de '${transactions.first().description.substringBefore(" (")}' registradas.",
                            type = "BOLETO"
                        ),
                        wsId
                    )
                }
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao lançar parcelas: ${e.message}"
            }
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            try {
                val wsId = _selectedWorkspace.value?.id
                repository.updateTransaction(tx, wsId)
                _statusMessage.value = "Transação atualizada!"
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao atualizar: ${e.message}"
            }
        }
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch {
            try {
                val wsId = _selectedWorkspace.value?.id
                repository.deleteTransaction(txId, wsId)
                _statusMessage.value = "Transação excluída."
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao excluir: ${e.message}"
            }
        }
    }

    fun togglePaidStatus(tx: Transaction) {
        viewModelScope.launch {
            try {
                val wsId = _selectedWorkspace.value?.id
                repository.updateTransaction(tx.copy(isPaid = !tx.isPaid), wsId)
                _statusMessage.value = if (!tx.isPaid) "Marcada como paga!" else "Marcada como pendente."
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao alterar status: ${e.message}"
            }
        }
    }

    // --- GOALS ---

    fun addGoal(goal: Goal) {
        viewModelScope.launch {
            try {
                val wsId = _selectedWorkspace.value?.id
                repository.addGoal(goal, wsId)
                _statusMessage.value = "Meta cadastrada com sucesso!"
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao salvar meta: ${e.message}"
            }
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            try {
                val wsId = _selectedWorkspace.value?.id
                repository.updateGoal(goal, wsId)
                _statusMessage.value = "Progresso da meta atualizado!"
                if (goal.currentAmount >= goal.targetAmount) {
                    repository.addNotification(
                        NotificationAlert(
                            userId = currentUserId,
                            workspaceId = wsId ?: "",
                            title = "Parabéns! Meta Atingida 🎉",
                            message = "Você alcançou a meta '${goal.title}' de R$ ${String.format(Locale.getDefault(), "%,.2f", goal.targetAmount)}!",
                            type = "GOAL_REACHED"
                        ),
                        wsId
                    )
                }
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao atualizar meta: ${e.message}"
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            try {
                val wsId = _selectedWorkspace.value?.id
                repository.deleteGoal(goalId, wsId)
                _statusMessage.value = "Meta excluída."
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao excluir meta: ${e.message}"
            }
        }
    }

    // --- BUDGETS ---

    fun setBudget(category: String, limit: Double, month: String) {
        viewModelScope.launch {
            try {
                val wsId = _selectedWorkspace.value?.id
                repository.setBudget(category, limit, month, wsId)
                _statusMessage.value = "Orçamento de $category atualizado!"
            } catch (e: Exception) {
                _statusMessage.value = "Falha ao definir orçamento: ${e.message}"
            }
        }
    }

    // --- NOTIFICATIONS ---

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            try {
                val wsId = _selectedWorkspace.value?.id
                repository.markNotificationAsRead(id, wsId)
            } catch (_: Exception) {}
        }
    }

    private suspend fun checkBudgetAndDueDateAlerts(newTx: Transaction) {
        val wsId = _selectedWorkspace.value?.id
        if (newTx.type == TransactionType.EXPENSE.name) {
            val currentMonth = if (newTx.date.length >= 7) newTx.date.substring(0, 7) else ""
            val budgets = (budgetsState.value as? UiState.Success)?.data ?: emptyList()
            val categoryBudget = budgets.find { it.category.equals(newTx.category, ignoreCase = true) && it.month == currentMonth }

            if (categoryBudget != null) {
                val txs = (transactionsState.value as? UiState.Success)?.data ?: emptyList()
                val currentCategorySpent = txs
                    .filter { it.type == TransactionType.EXPENSE.name && it.category.equals(newTx.category, ignoreCase = true) && it.date.startsWith(currentMonth) }
                    .sumOf { it.amount } + newTx.amount

                if (currentCategorySpent > categoryBudget.monthlyLimit) {
                    repository.addNotification(
                        NotificationAlert(
                            userId = currentUserId,
                            workspaceId = wsId ?: "",
                            title = "Alerta de Orçamento Ultrapassado",
                            message = "Os gastos em '${newTx.category}' (R$ ${String.format(Locale.getDefault(), "%,.2f", currentCategorySpent)}) ultrapassaram o limite de R$ ${String.format(Locale.getDefault(), "%,.2f", categoryBudget.monthlyLimit)}.",
                            type = "BUDGET_EXCEEDED"
                        ),
                        wsId
                    )
                }
            }
        }

        if (newTx.dueDate.isNotBlank() && !newTx.isPaid) {
            repository.addNotification(
                NotificationAlert(
                    userId = currentUserId,
                    workspaceId = wsId ?: "",
                    title = "Vencimento Programado",
                    message = "${newTx.description} vence em ${newTx.dueDate}. Valor: R$ ${String.format(Locale.getDefault(), "%,.2f", newTx.amount)}.",
                    type = "DUE_DATE"
                ),
                wsId
            )
        }
    }

    // --- BANK INTEGRATION SIMULATION / REAL CONNECTOR ---

    fun syncWithBank(bankName: String, simulatedTransactions: List<Transaction>) {
        viewModelScope.launch {
            _isBankSyncing.value = true
            try {
                val wsId = _selectedWorkspace.value?.id
                kotlinx.coroutines.delay(1200)
                for (tx in simulatedTransactions) {
                    repository.addTransaction(tx, wsId)
                }
                _statusMessage.value = "Sincronização com $bankName concluída: ${simulatedTransactions.size} lançamentos importados."
                repository.addNotification(
                    NotificationAlert(
                        userId = currentUserId,
                        workspaceId = wsId ?: "",
                        title = "Sincronização Bancária Realizada",
                        message = "Conexão com $bankName atualizada com sucesso. ${simulatedTransactions.size} novas transações sincronizadas.",
                        type = "BANK_SYNC"
                    ),
                    wsId
                )
            } catch (e: Exception) {
                _statusMessage.value = "Erro na sincronização: ${e.message}"
            } finally {
                _isBankSyncing.value = false
            }
        }
    }

    // --- EXPORT TO CSV AND PDF/SHARE ---

    fun exportToCSV(context: Context, transactions: List<Transaction>) {
        val csvHeader = "ID,Descrição,Tipo,Categoria,Valor,Fixo/Variável,Data,Vencimento,Status,Sincronizado_Banco,Autor\n"
        val csvRows = transactions.joinToString("\n") { tx ->
            val fixed = if (tx.isFixed) "Fixo" else "Variável"
            val status = if (tx.isPaid) "Pago" else "Pendente"
            val synced = if (tx.isBankSynced) tx.bankName.ifEmpty { "Sim" } else "Manual"
            val author = tx.authorName.ifEmpty { "Pessoal" }
            "\"${tx.id}\",\"${tx.description}\",\"${tx.type}\",\"${tx.category}\",${tx.amount},\"$fixed\",\"${tx.date}\",\"${tx.dueDate}\",\"$status\",\"$synced\",\"$author\""
        }
        val fullCsv = csvHeader + csvRows

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, fullCsv)
            putExtra(Intent.EXTRA_SUBJECT, "Relatório Financeiro Almeida Finance - CSV")
            type = "text/csv"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Exportar dados CSV"))
    }

    fun exportToPdfReport(context: Context, transactions: List<Transaction>, userEmail: String?) {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val net = totalIncome - totalExpense
        val fixedExpenses = transactions.filter { it.type == TransactionType.EXPENSE.name && it.isFixed }.sumOf { it.amount }
        val variableExpenses = transactions.filter { it.type == TransactionType.EXPENSE.name && !it.isFixed }.sumOf { it.amount }
        val wsName = _selectedWorkspace.value?.name ?: "Pessoal"

        val report = buildString {
            appendLine("=========================================")
            appendLine("       ALMEIDA FINANCE - RELATÓRIO       ")
            appendLine("=========================================")
            appendLine("Data de Emissão: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            appendLine("Espaço: $wsName | Usuário: ${userEmail ?: currentUserId}")
            appendLine("-----------------------------------------")
            appendLine("RESUMO EXECUTIVO:")
            appendLine("Receitas Totais: R$ ${String.format(Locale.getDefault(), "%,.2f", totalIncome)}")
            appendLine("Despesas Totais: R$ ${String.format(Locale.getDefault(), "%,.2f", totalExpense)}")
            appendLine("  - Gastos Fixos: R$ ${String.format(Locale.getDefault(), "%,.2f", fixedExpenses)}")
            appendLine("  - Gastos Variáveis: R$ ${String.format(Locale.getDefault(), "%,.2f", variableExpenses)}")
            appendLine("Saldo Líquido: R$ ${String.format(Locale.getDefault(), "%,.2f", net)}")
            appendLine("-----------------------------------------")
            appendLine("DETALHAMENTO DE TRANSAÇÕES:")
            transactions.take(50).forEach { tx ->
                val signal = if (tx.type == TransactionType.INCOME.name) "(+)" else "(-)"
                val paid = if (tx.isPaid) "[PAGO]" else "[PENDENTE]"
                appendLine("$signal R$ ${String.format(Locale.getDefault(), "%,.2f", tx.amount)} | ${tx.description} | ${tx.category} | $paid")
            }
            appendLine("=========================================")
            appendLine("Gerado por Almeida Finance com segurança, sincronização em nuvem e multi-usuário.")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, report)
            putExtra(Intent.EXTRA_SUBJECT, "Relatório Almeida Finance - PDF/Documento")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Exportar Relatório"))
    }

    fun sendEmailSummary(context: Context, userEmail: String?, transactions: List<Transaction>) {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val net = totalIncome - totalExpense
        val wsName = _selectedWorkspace.value?.name ?: "Pessoal"

        val body = """
            Olá!
            
            Aqui está o resumo financeiro atualizado do Almeida Finance:
            
            • Espaço: $wsName
            • Total de Receitas: R$ ${String.format(Locale.getDefault(), "%,.2f", totalIncome)}
            • Total de Despesas: R$ ${String.format(Locale.getDefault(), "%,.2f", totalExpense)}
            • Saldo do Período: R$ ${String.format(Locale.getDefault(), "%,.2f", net)}
            
            Todas as transações, boletos e movimentações estão sincronizados em tempo real entre todos os usuários vinculados.
            
            Atenciosamente,
            Almeida Finance - Gestão Financeira Segura e Inteligente
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            if (!userEmail.isNullOrBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, "Almeida Finance - Resumo Financeiro Consolidado")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Enviar por e-mail"))
        } catch (_: Exception) {
            _statusMessage.value = "Nenhum app de e-mail disponível."
        }
    }
}
