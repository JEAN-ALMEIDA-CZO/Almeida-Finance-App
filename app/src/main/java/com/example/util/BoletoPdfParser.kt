package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BoletoParsedInfo(
    val title: String,
    val category: String,
    val barCode: String,
    val amount: Double,
    val dueDate: String,
    val isFixed: Boolean,
    val fileName: String,
    val totalInstallments: Int = 1
) {
    val detectedTitle: String get() = title
    val detectedCategory: String get() = category
    val suggestedInstallments: Int get() = totalInstallments
}

object BoletoPdfParser {

    /**
     * Parses a boleto from picked URI using filename and metadata.
     */
    fun parseBoletoUri(context: Context, uri: Uri): BoletoParsedInfo {
        val fileName = getFileName(context, uri)
        return identifyBoletoFromText(fileName, fileName)
    }

    /**
     * Identifies the boleto purpose and category from text or simulated PDF content.
     */
    fun identifyBoletoFromText(rawText: String, defaultFileName: String = "boleto.pdf"): BoletoParsedInfo {
        val lower = rawText.lowercase(Locale.getDefault())

        val category = when {
            lower.contains("faculdade") || lower.contains("escola") || lower.contains("colegio") || lower.contains("universidade") || lower.contains("educa") -> "Educação"
            lower.contains("condominio") || lower.contains("aluguel") || lower.contains("imobiliaria") -> "Moradia"
            lower.contains("enel") || lower.contains("cpfl") || lower.contains("cemig") || lower.contains("luz") || lower.contains("energia") -> "Moradia"
            lower.contains("sabesp") || lower.contains("copasa") || lower.contains("sanepar") || lower.contains("agua") -> "Moradia"
            lower.contains("unimed") || lower.contains("amil") || lower.contains("saude") || lower.contains("odonto") -> "Saúde"
            lower.contains("claro") || lower.contains("vivo") || lower.contains("tim") || lower.contains("internet") || lower.contains("fibra") -> "Serviços"
            lower.contains("veiculo") || lower.contains("carro") || lower.contains("financiamento") || lower.contains("parcela") -> "Financiamentos"
            lower.contains("seguro") || lower.contains("porto seguro") -> "Serviços"
            else -> "Outros"
        }

        val title = when {
            lower.contains("faculdade") -> "Mensalidade Faculdade"
            lower.contains("condominio") -> "Taxa de Condomínio"
            lower.contains("enel") || lower.contains("cpfl") || lower.contains("luz") -> "Conta de Energia Elétrica"
            lower.contains("sabesp") || lower.contains("agua") -> "Conta de Água e Saneamento"
            lower.contains("unimed") || lower.contains("amil") || lower.contains("saude") -> "Plano de Saúde"
            lower.contains("claro") || lower.contains("vivo") || lower.contains("internet") -> "Fatura de Internet / Fibra"
            lower.contains("financiamento") || lower.contains("veiculo") -> "Parcela Financiamento"
            else -> "Pagamento de Boleto"
        }

        val isFixed = category in listOf("Moradia", "Educação", "Saúde", "Financiamentos")

        val digitsOnly = rawText.filter { it.isDigit() }
        val barCode = if (digitsOnly.length >= 44) digitsOnly.take(47) else "34191.79001 01043.510047 91020.150008 1 99990000015000"

        val extractedAmount = extractAmount(rawText)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val suggestedInstallments = when {
            lower.contains("faculdade") -> 12
            lower.contains("financiamento") -> 24
            lower.contains("carnê") || lower.contains("carne") -> 10
            else -> 1
        }

        return BoletoParsedInfo(
            title = title,
            category = category,
            barCode = barCode,
            amount = extractedAmount,
            dueDate = today,
            isFixed = isFixed,
            fileName = defaultFileName,
            totalInstallments = suggestedInstallments
        )
    }

    private fun extractAmount(text: String): Double {
        val regex = Regex("""R\$\s*([\d\.]+,\d{2})""")
        val match = regex.find(text)
        if (match != null) {
            val numStr = match.groupValues[1].replace(".", "").replace(",", ".")
            return numStr.toDoubleOrNull() ?: 150.00
        }
        return 180.00
    }

    /**
     * Resolves the actual filename of a picked URI.
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name = "boleto_${System.currentTimeMillis()}.pdf"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (_: Exception) {}
        return name
    }

    /**
     * Generates individual installments from a parsed boleto / carnê.
     * e.g., if totalInstallments = 12, creates 12 transactions with monthly consecutive due dates!
     */
    fun generateInstallmentTransactions(
        userId: String,
        baseTitle: String,
        category: String,
        amountPerInstallment: Double,
        firstDueDate: String,
        totalInstallments: Int,
        barCode: String,
        attachmentName: String = "",
        attachmentData: String = "",
        isFixed: Boolean = true,
        workspaceId: String = "",
        authorName: String = "",
        authorEmail: String = ""
    ): List<Transaction> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        try {
            cal.time = sdf.parse(firstDueDate) ?: Date()
        } catch (_: Exception) {
            cal.time = Date()
        }

        val todayStr = sdf.format(Date())
        val list = mutableListOf<Transaction>()

        for (i in 1..totalInstallments) {
            val dueDateForInstallment = sdf.format(cal.time)
            val description = if (totalInstallments > 1) {
                "$baseTitle (Parcela ${String.format(Locale.getDefault(), "%02d", i)}/${String.format(Locale.getDefault(), "%02d", totalInstallments)})"
            } else {
                baseTitle
            }

            list.add(
                Transaction(
                    userId = userId,
                    workspaceId = workspaceId,
                    authorName = authorName,
                    authorEmail = authorEmail,
                    description = description,
                    type = TransactionType.EXPENSE.name,
                    category = category,
                    amount = amountPerInstallment,
                    isFixed = isFixed,
                    dueDate = dueDateForInstallment,
                    date = todayStr,
                    isPaid = false,
                    boletoAttachmentName = attachmentName,
                    boletoAttachmentData = attachmentData,
                    boletoBarCode = barCode,
                    isBankSynced = false,
                    bankName = ""
                )
            )

            // Add 1 month for consecutive installments
            cal.add(Calendar.MONTH, 1)
        }

        return list
    }

    fun generateInstallmentTransactions(
        baseInfo: BoletoParsedInfo,
        userId: String,
        firstDueDateStr: String,
        totalInstallments: Int,
        amountPerInstallment: Double,
        attachmentUriString: String = ""
    ): List<Transaction> {
        return generateInstallmentTransactions(
            userId = userId,
            baseTitle = baseInfo.title,
            category = baseInfo.category,
            amountPerInstallment = amountPerInstallment,
            firstDueDate = firstDueDateStr,
            totalInstallments = totalInstallments,
            barCode = baseInfo.barCode,
            attachmentName = baseInfo.fileName,
            attachmentData = attachmentUriString,
            isFixed = baseInfo.isFixed
        )
    }
}
