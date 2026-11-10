package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AuditLog
import com.example.data.model.Budget
import com.example.data.model.Goal
import com.example.data.model.NotificationAlert
import com.example.data.model.Transaction
import com.example.data.model.Workspace
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class FinanceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    private fun currentUserName(): String {
        return auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: "Usuário Almeida"
    }

    private fun currentUserEmail(): String {
        return auth.currentUser?.email ?: ""
    }

    // --- WORKSPACES (MULTI-USER SHARED SPACES) ---

    fun observeWorkspaces(): Flow<List<Workspace>> = flow {
        val uid = requireUserId()
        val path = "workspaces"
        emitAll(
            db.collection("workspaces")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Workspace::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createWorkspace(name: String): Workspace {
        val uid = requireUserId()
        val userEmail = currentUserEmail()
        val docRef = db.collection("workspaces").document()
        val inviteCode = "ALM" + (1000..9999).random()
        val payload = mapOf(
            "name" to name,
            "ownerUid" to uid,
            "ownerEmail" to userEmail,
            "memberUids" to listOf(uid),
            "inviteCode" to inviteCode,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            val ws = Workspace(
                id = docRef.id,
                name = name,
                ownerUid = uid,
                ownerEmail = userEmail,
                memberUids = listOf(uid),
                inviteCode = inviteCode
            )
            logAuditAction(
                workspaceId = docRef.id,
                action = "CRIOU",
                entityType = "ESPAÇO",
                entityTitle = name,
                details = "Espaço multi-usuário criado. Código de convite: $inviteCode"
            )
            return ws
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun joinWorkspaceByInviteCode(inviteCode: String): Workspace? {
        val uid = requireUserId()
        val trimmed = inviteCode.trim().uppercase()
        try {
            val query = db.collection("workspaces")
                .whereEqualTo("inviteCode", trimmed)
                .get()
                .await()
            if (query.isEmpty) return null
            val doc = query.documents.first()
            val ws = doc.toObject(Workspace::class.java)?.copy(id = doc.id) ?: return null
            if (!ws.memberUids.contains(uid)) {
                val updatedMembers = ws.memberUids + uid
                doc.reference.update(
                    "memberUids", updatedMembers,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
                logAuditAction(
                    workspaceId = ws.id,
                    action = "VINCULOU_USUARIO",
                    entityType = "CONTA",
                    entityTitle = currentUserName(),
                    details = "Usuário vinculado ao espaço compartilhado via código $trimmed"
                )
                // Broadcast alert
                addNotification(
                    alert = NotificationAlert(
                        title = "Novo Membro Vinculado",
                        message = "${currentUserName()} entrou no espaço compartilhado.",
                        type = "ACTIVITY",
                        authorName = currentUserName(),
                        workspaceId = ws.id
                    ),
                    workspaceId = ws.id
                )
            }
            return ws
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Failed to join workspace: ${e.message}", e)
            return null
        }
    }

    // --- AUDIT LOGS (IMMUTABLE HISTORY OF MOVEMENTS) ---

    fun observeAuditLogs(workspaceId: String): Flow<List<AuditLog>> = flow {
        val uid = requireUserId()
        if (workspaceId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "workspaces/$workspaceId/audit_logs"
        emitAll(
            db.collection("workspaces").document(workspaceId).collection("audit_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(AuditLog::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun logAuditAction(
        workspaceId: String,
        action: String,
        entityType: String,
        entityTitle: String,
        details: String
    ) {
        if (workspaceId.isBlank()) return
        val uid = auth.currentUser?.uid ?: return
        val userName = currentUserName()
        val userEmail = currentUserEmail()
        val docRef = db.collection("workspaces").document(workspaceId).collection("audit_logs").document()
        val payload = mapOf(
            "workspaceId" to workspaceId,
            "userId" to uid,
            "userName" to userName,
            "userEmail" to userEmail,
            "action" to action,
            "entityType" to entityType,
            "entityTitle" to entityTitle,
            "details" to details,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            Log.w("FinanceRepository", "Audit log write failed: ${e.message}")
        }
    }

    // --- TRANSACTIONS ---

    fun observeTransactions(workspaceId: String? = null): Flow<List<Transaction>> = flow {
        val uid = requireUserId()
        val isShared = !workspaceId.isNullOrBlank()
        val collection = if (isShared) {
            db.collection("workspaces").document(workspaceId!!).collection("transactions")
        } else {
            db.collection("users").document(uid).collection("transactions")
        }
        val path = collection.path

        emitAll(
            collection.snapshots()
                .map { snapshot -> snapshot.toObjects(Transaction::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addTransaction(tx: Transaction, workspaceId: String? = null): String {
        val uid = requireUserId()
        val userName = currentUserName()
        val userEmail = currentUserEmail()
        val effectiveWorkspaceId = workspaceId ?: tx.workspaceId
        val isShared = effectiveWorkspaceId.isNotBlank()

        val collection = if (isShared) {
            db.collection("workspaces").document(effectiveWorkspaceId).collection("transactions")
        } else {
            db.collection("users").document(uid).collection("transactions")
        }
        val docRef = collection.document()

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "description" to tx.description,
            "type" to tx.type,
            "category" to tx.category,
            "amount" to tx.amount,
            "isFixed" to tx.isFixed,
            "dueDate" to tx.dueDate,
            "date" to tx.date,
            "isPaid" to tx.isPaid,
            "isBankSynced" to tx.isBankSynced,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isShared) {
            payload["workspaceId"] = effectiveWorkspaceId
            payload["authorName"] = userName
            payload["authorEmail"] = userEmail
            payload["lastModifiedByName"] = userName
        }
        if (tx.bankName.isNotBlank()) payload["bankName"] = tx.bankName
        if (tx.boletoAttachmentName.isNotBlank()) payload["boletoAttachmentName"] = tx.boletoAttachmentName
        if (tx.boletoAttachmentData.isNotBlank()) payload["boletoAttachmentData"] = tx.boletoAttachmentData
        if (tx.boletoBarCode.isNotBlank()) payload["boletoBarCode"] = tx.boletoBarCode

        try {
            docRef.set(payload).await()

            if (isShared) {
                logAuditAction(
                    workspaceId = effectiveWorkspaceId,
                    action = "CRIOU",
                    entityType = "TRANSAÇÃO",
                    entityTitle = tx.description,
                    details = "R$ ${String.format(Locale.getDefault(), "%,.2f", tx.amount)} - ${tx.category} (${if (tx.type == "INCOME") "Entrada" else "Saída"})"
                )
                // Broadcast alert to linked users
                addNotification(
                    alert = NotificationAlert(
                        title = "Nova Movimentação",
                        message = "$userName lançou: ${tx.description} (R$ ${String.format(Locale.getDefault(), "%,.2f", tx.amount)})",
                        type = "ACTIVITY",
                        authorName = userName,
                        workspaceId = effectiveWorkspaceId
                    ),
                    workspaceId = effectiveWorkspaceId
                )
            }

            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTransaction(tx: Transaction, workspaceId: String? = null) {
        val uid = requireUserId()
        val userName = currentUserName()
        val effectiveWorkspaceId = workspaceId ?: tx.workspaceId
        val isShared = effectiveWorkspaceId.isNotBlank()

        require(tx.id.isNotBlank()) { "Transaction ID must not be blank for update" }
        requireNotNull(tx.createdAt) { "createdAt must not be null for update" }

        val docRef = if (isShared) {
            db.collection("workspaces").document(effectiveWorkspaceId).collection("transactions").document(tx.id)
        } else {
            db.collection("users").document(uid).collection("transactions").document(tx.id)
        }

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "description" to tx.description,
            "type" to tx.type,
            "category" to tx.category,
            "amount" to tx.amount,
            "isFixed" to tx.isFixed,
            "dueDate" to tx.dueDate,
            "date" to tx.date,
            "isPaid" to tx.isPaid,
            "isBankSynced" to tx.isBankSynced,
            "createdAt" to tx.createdAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isShared) {
            payload["workspaceId"] = effectiveWorkspaceId
            payload["lastModifiedByName"] = userName
        }
        if (tx.bankName.isNotBlank()) payload["bankName"] = tx.bankName
        if (tx.boletoAttachmentName.isNotBlank()) payload["boletoAttachmentName"] = tx.boletoAttachmentName
        if (tx.boletoAttachmentData.isNotBlank()) payload["boletoAttachmentData"] = tx.boletoAttachmentData
        if (tx.boletoBarCode.isNotBlank()) payload["boletoBarCode"] = tx.boletoBarCode

        try {
            docRef.set(payload).await()
            if (isShared) {
                logAuditAction(
                    workspaceId = effectiveWorkspaceId,
                    action = if (tx.isPaid) "PAGOU" else "EDITOU",
                    entityType = "TRANSAÇÃO",
                    entityTitle = tx.description,
                    details = "Atualizado para R$ ${String.format(Locale.getDefault(), "%,.2f", tx.amount)} (Status: ${if (tx.isPaid) "Pago" else "Pendente"})"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTransaction(transactionId: String, workspaceId: String? = null, description: String = "Lançamento") {
        val uid = requireUserId()
        val isShared = !workspaceId.isNullOrBlank()

        val docRef = if (isShared) {
            db.collection("workspaces").document(workspaceId!!).collection("transactions").document(transactionId)
        } else {
            db.collection("users").document(uid).collection("transactions").document(transactionId)
        }

        try {
            docRef.delete().await()
            if (isShared) {
                logAuditAction(
                    workspaceId = workspaceId!!,
                    action = "EXCLUIU",
                    entityType = "TRANSAÇÃO",
                    entityTitle = description,
                    details = "Lançamento removido do sistema por ${currentUserName()}"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- GOALS ---

    fun observeGoals(workspaceId: String? = null): Flow<List<Goal>> = flow {
        val uid = requireUserId()
        val isShared = !workspaceId.isNullOrBlank()
        val collection = if (isShared) {
            db.collection("workspaces").document(workspaceId!!).collection("goals")
        } else {
            db.collection("users").document(uid).collection("goals")
        }
        val path = collection.path

        emitAll(
            collection.snapshots()
                .map { snapshot -> snapshot.toObjects(Goal::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addGoal(goal: Goal, workspaceId: String? = null): String {
        val uid = requireUserId()
        val effectiveWorkspaceId = workspaceId ?: goal.workspaceId
        val isShared = effectiveWorkspaceId.isNotBlank()

        val collection = if (isShared) {
            db.collection("workspaces").document(effectiveWorkspaceId).collection("goals")
        } else {
            db.collection("users").document(uid).collection("goals")
        }
        val docRef = collection.document()

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "title" to goal.title,
            "targetAmount" to goal.targetAmount,
            "currentAmount" to goal.currentAmount,
            "deadline" to goal.deadline,
            "category" to goal.category,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isShared) {
            payload["workspaceId"] = effectiveWorkspaceId
        }

        try {
            docRef.set(payload).await()
            if (isShared) {
                logAuditAction(
                    workspaceId = effectiveWorkspaceId,
                    action = "CRIOU",
                    entityType = "META",
                    entityTitle = goal.title,
                    details = "Meta definida: R$ ${String.format(Locale.getDefault(), "%,.2f", goal.targetAmount)}"
                )
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateGoal(goal: Goal, workspaceId: String? = null) {
        val uid = requireUserId()
        val effectiveWorkspaceId = workspaceId ?: goal.workspaceId
        val isShared = effectiveWorkspaceId.isNotBlank()

        require(goal.id.isNotBlank()) { "Goal ID must not be blank for update" }
        requireNotNull(goal.createdAt) { "createdAt must not be null for update" }

        val docRef = if (isShared) {
            db.collection("workspaces").document(effectiveWorkspaceId).collection("goals").document(goal.id)
        } else {
            db.collection("users").document(uid).collection("goals").document(goal.id)
        }

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "title" to goal.title,
            "targetAmount" to goal.targetAmount,
            "currentAmount" to goal.currentAmount,
            "deadline" to goal.deadline,
            "category" to goal.category,
            "createdAt" to goal.createdAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isShared) {
            payload["workspaceId"] = effectiveWorkspaceId
        }

        try {
            docRef.set(payload).await()
            if (isShared) {
                logAuditAction(
                    workspaceId = effectiveWorkspaceId,
                    action = "EDITOU",
                    entityType = "META",
                    entityTitle = goal.title,
                    details = "Progresso atualizado para R$ ${String.format(Locale.getDefault(), "%,.2f", goal.currentAmount)}"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteGoal(goalId: String, workspaceId: String? = null, title: String = "Meta") {
        val uid = requireUserId()
        val isShared = !workspaceId.isNullOrBlank()

        val docRef = if (isShared) {
            db.collection("workspaces").document(workspaceId!!).collection("goals").document(goalId)
        } else {
            db.collection("users").document(uid).collection("goals").document(goalId)
        }

        try {
            docRef.delete().await()
            if (isShared) {
                logAuditAction(
                    workspaceId = workspaceId!!,
                    action = "EXCLUIU",
                    entityType = "META",
                    entityTitle = title,
                    details = "Meta excluída do espaço compartilhado"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- BUDGETS ---

    fun observeBudgets(workspaceId: String? = null): Flow<List<Budget>> = flow {
        val uid = requireUserId()
        val isShared = !workspaceId.isNullOrBlank()
        val collection = if (isShared) {
            db.collection("workspaces").document(workspaceId!!).collection("budgets")
        } else {
            db.collection("users").document(uid).collection("budgets")
        }
        val path = collection.path

        emitAll(
            collection.snapshots()
                .map { snapshot -> snapshot.toObjects(Budget::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun setBudget(category: String, limit: Double, month: String, workspaceId: String? = null): String {
        val uid = requireUserId()
        val isShared = !workspaceId.isNullOrBlank()
        val budgetDocId = "${category.replace("/", "_").lowercase()}_$month"

        val docRef = if (isShared) {
            db.collection("workspaces").document(workspaceId!!).collection("budgets").document(budgetDocId)
        } else {
            db.collection("users").document(uid).collection("budgets").document(budgetDocId)
        }

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "category" to category,
            "monthlyLimit" to limit,
            "month" to month,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isShared) {
            payload["workspaceId"] = workspaceId!!
        }

        try {
            docRef.set(payload).await()
            if (isShared) {
                logAuditAction(
                    workspaceId = workspaceId!!,
                    action = "EDITOU",
                    entityType = "ORÇAMENTO",
                    entityTitle = category,
                    details = "Limite mensal definido para R$ ${String.format(Locale.getDefault(), "%,.2f", limit)} em $month"
                )
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- NOTIFICATIONS ---

    fun observeNotifications(workspaceId: String? = null): Flow<List<NotificationAlert>> = flow {
        val uid = requireUserId()
        val isShared = !workspaceId.isNullOrBlank()
        val collection = if (isShared) {
            db.collection("workspaces").document(workspaceId!!).collection("notifications")
        } else {
            db.collection("users").document(uid).collection("notifications")
        }
        val path = collection.path

        emitAll(
            collection.snapshots()
                .map { snapshot -> snapshot.toObjects(NotificationAlert::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addNotification(alert: NotificationAlert, workspaceId: String? = null): String {
        val uid = requireUserId()
        val effectiveWorkspaceId = workspaceId ?: alert.workspaceId
        val isShared = effectiveWorkspaceId.isNotBlank()

        val collection = if (isShared) {
            db.collection("workspaces").document(effectiveWorkspaceId).collection("notifications")
        } else {
            db.collection("users").document(uid).collection("notifications")
        }
        val docRef = collection.document()

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "title" to alert.title,
            "message" to alert.message,
            "type" to alert.type,
            "isRead" to alert.isRead,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (isShared) {
            payload["workspaceId"] = effectiveWorkspaceId
            if (alert.authorName.isNotBlank()) payload["authorName"] = alert.authorName
        }

        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markNotificationAsRead(notificationId: String, workspaceId: String? = null) {
        val uid = requireUserId()
        val isShared = !workspaceId.isNullOrBlank()

        val docRef = if (isShared) {
            db.collection("workspaces").document(workspaceId!!).collection("notifications").document(notificationId)
        } else {
            db.collection("users").document(uid).collection("notifications").document(notificationId)
        }

        try {
            docRef.update("isRead", true).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
