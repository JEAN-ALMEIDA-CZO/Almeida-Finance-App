package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Goal
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addTransaction_authenticatedUser_createsDocumentAndEmits() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = FinanceRepository(firestore)

        val tx = Transaction(
            description = "Salário Novembro",
            type = TransactionType.INCOME.name,
            category = "Salário",
            amount = 4500.0,
            isFixed = true,
            dueDate = "2026-11-05",
            date = "2026-11-05",
            isPaid = true,
            isBankSynced = false
        )

        val txId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addTransaction(tx)
        }
        assertTrue(txId.isNotEmpty())

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTransactions().first { items -> items.any { it.id == txId } }
        }
        assertTrue(list.any { it.id == txId })
    }

    @Test
    fun addGoal_authenticatedUser_createsGoalAndEmits() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = FinanceRepository(firestore)

        val goal = Goal(
            title = "Viagem de Férias",
            targetAmount = 5000.0,
            currentAmount = 1200.0,
            deadline = "2026-12-20",
            category = "Viagem"
        )

        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addGoal(goal)
        }
        assertTrue(goalId.isNotEmpty())

        val goals = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeGoals().first { items -> items.any { it.id == goalId } }
        }
        assertTrue(goals.any { it.id == goalId })
    }

    @Test
    fun observeTransactions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FinanceRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTransactions().first()
            }
            fail("Expected exception when unauthenticated")
        } catch (e: Exception) {
            assertNotNull(e)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@finangest.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
