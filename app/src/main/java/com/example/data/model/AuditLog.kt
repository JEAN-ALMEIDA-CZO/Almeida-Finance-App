package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val action: String = "", // CRIOU, EDITOU, EXCLUIU, PAGOU, PARCELOU_BOLETO, SINCRONIZOU_BANCO, VINCULOU_USUARIO
    val entityType: String = "", // TRANSAÇÃO, META, ORÇAMENTO, BOLETO, CONTA
    val entityTitle: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
