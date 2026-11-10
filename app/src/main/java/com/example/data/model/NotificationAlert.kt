package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class NotificationAlert(
    @DocumentId val id: String = "",
    val userId: String = "",
    val workspaceId: String = "",
    val authorName: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL", // DUE_DATE, BUDGET_EXCEEDED, GOAL_REACHED, BANK_SYNC, GENERAL, BOLETO, ACTIVITY
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
