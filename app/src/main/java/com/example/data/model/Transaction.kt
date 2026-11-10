package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TransactionType {
    INCOME,
    EXPENSE
}

@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val workspaceId: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val lastModifiedByName: String = "",
    val description: String = "",
    val type: String = TransactionType.EXPENSE.name,
    val category: String = "Outros",
    val amount: Double = 0.0,
    val isFixed: Boolean = false,
    val dueDate: String = "",
    val date: String = "",
    val isPaid: Boolean = false,
    val boletoAttachmentName: String = "",
    val boletoAttachmentData: String = "",
    val boletoBarCode: String = "",
    val isBankSynced: Boolean = false,
    val bankName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
