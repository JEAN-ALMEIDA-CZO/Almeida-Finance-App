package com.example.ui.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Transaction
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IncomeGreen
import com.example.ui.components.DatePickerField
import com.example.util.BoletoParsedInfo
import com.example.util.BoletoPdfParser
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BoletoSamplePreset(
    val label: String,
    val title: String,
    val category: String,
    val amount: Double,
    val barCode: String,
    val defaultInstallments: Int,
    val isFixed: Boolean
)

val PRESET_SAMPLES = listOf(
    BoletoSamplePreset(
        label = "Faculdade (12x)",
        title = "Mensalidade Faculdade Anhembi Morumbi",
        category = "Educação",
        amount = 750.00,
        barCode = "23793.38128 60000.123456 78901.234567 1 99990000075000",
        defaultInstallments = 12,
        isFixed = true
    ),
    BoletoSamplePreset(
        label = "Condomínio",
        title = "Taxa Condominial Edifício Solar",
        category = "Moradia",
        amount = 680.00,
        barCode = "34191.09008 01234.567890 12345.678901 2 99990000068000",
        defaultInstallments = 1,
        isFixed = true
    ),
    BoletoSamplePreset(
        label = "Enel Energia",
        title = "Conta de Energia Elétrica Enel",
        category = "Moradia",
        amount = 215.40,
        barCode = "84670000002 1 15400296202 6 61108202611 8 00123456789 0",
        defaultInstallments = 1,
        isFixed = false
    ),
    BoletoSamplePreset(
        label = "Unimed Saúde",
        title = "Plano de Saúde Unimed Nacional",
        category = "Saúde",
        amount = 520.00,
        barCode = "00190.00009 01234.567890 12345.678901 3 99990000052000",
        defaultInstallments = 1,
        isFixed = true
    ),
    BoletoSamplePreset(
        label = "Financiamento (24x)",
        title = "Parcela Financiamento Veículo BV",
        category = "Transporte",
        amount = 890.00,
        barCode = "34191.12345 67890.123456 78901.123456 4 99990000089000",
        defaultInstallments = 24,
        isFixed = true
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartBoletoReaderDialog(
    currentUserId: String,
    onDismiss: () -> Unit,
    onSaveInstallments: (List<Transaction>) -> Unit
) {
    val context = LocalContext.current
    val today = remember { DateTimeUtils.getTodayIso() }

    var selectedFileName by remember { mutableStateOf<String?>("boleto_mensalidade_educacao.pdf") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Detected Boleto Info
    var detectedTitle by remember { mutableStateOf("Mensalidade Faculdade Anhembi Morumbi") }
    var detectedCategory by remember { mutableStateOf("Educação") }
    var detectedAmountStr by remember { mutableStateOf("750.00") }
    var detectedDueDate by remember { mutableStateOf(today) }
    var detectedBarCode by remember { mutableStateOf("23793.38128 60000.123456 78901.234567 1 99990000075000") }
    var isFixedExpense by remember { mutableStateOf(true) }

    // Installments Settings
    var numberOfInstallments by remember { mutableIntStateOf(12) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "boleto_documento.pdf"
            isProcessing = true

            val parsed: BoletoParsedInfo = BoletoPdfParser.parseBoletoUri(context, uri)
            detectedTitle = parsed.detectedTitle
            detectedCategory = parsed.detectedCategory
            detectedAmountStr = String.format(Locale.US, "%.2f", parsed.amount)
            detectedDueDate = parsed.dueDate
            detectedBarCode = parsed.barCode
            numberOfInstallments = parsed.suggestedInstallments
            isFixedExpense = parsed.isFixed

            isProcessing = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Leitor de Boletos & PDFs",
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Reconhecimento inteligente e parcelamento automático",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancelar", maxLines = 1)
                        }

                        Button(
                            onClick = {
                                val amount = detectedAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (detectedTitle.isNotBlank() && amount > 0.0) {
                                    val transactions = BoletoPdfParser.generateInstallmentTransactions(
                                        userId = currentUserId,
                                        baseTitle = detectedTitle,
                                        category = detectedCategory,
                                        amountPerInstallment = amount,
                                        firstDueDate = detectedDueDate,
                                        totalInstallments = numberOfInstallments,
                                        barCode = detectedBarCode,
                                        attachmentName = selectedFileName ?: "boleto.pdf",
                                        attachmentData = selectedUri?.toString() ?: "",
                                        isFixed = isFixedExpense
                                    )
                                    onSaveInstallments(transactions)
                                }
                            },
                            enabled = detectedTitle.isNotBlank() && (detectedAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0.0,
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("confirm_installments_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF090C12))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (numberOfInstallments > 1) "Lançar $numberOfInstallments Parcelas" else "Lançar Boleto",
                                color = Color(0xFF090C12),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PDF / Image Upload Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF Boleto",
                                tint = GoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = selectedFileName ?: "Nenhum arquivo carregado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Suporta boletos em PDF, carnês escolares e contas em imagem",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                documentPickerLauncher.launch("*/*")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = Color(0xFF090C12))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Selecionar PDF do Dispositivo", color = Color(0xFF090C12), fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                // Quick Presets Carousel
                Column {
                    Text(
                        text = "Exemplos e Pré-configurações Rápidas:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PRESET_SAMPLES) { preset ->
                            val isSelected = detectedTitle == preset.title
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    detectedTitle = preset.title
                                    detectedCategory = preset.category
                                    detectedAmountStr = String.format(Locale.US, "%.2f", preset.amount)
                                    detectedBarCode = preset.barCode
                                    numberOfInstallments = preset.defaultInstallments
                                    isFixedExpense = preset.isFixed
                                    selectedFileName = "${preset.label.lowercase().replace(" ", "_")}.pdf"
                                },
                                label = { Text(preset.label, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color(0xFF090C12)
                                )
                            )
                        }
                    }
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                } else {
                    // Auto-Detected Breakdown Card
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Dados Identificados do Boleto",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GoldPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = IncomeGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Verificado",
                                        color = IncomeGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = detectedTitle,
                                onValueChange = { detectedTitle = it },
                                label = { Text("Finalidade / Emissor Identificado") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = detectedAmountStr,
                                    onValueChange = { detectedAmountStr = it },
                                    label = { Text("Valor da Parcela (R$)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = { Text("R$", fontWeight = FontWeight.Bold, color = GoldPrimary) }
                                )

                                OutlinedTextField(
                                    value = detectedCategory,
                                    onValueChange = { detectedCategory = it },
                                    label = { Text("Categoria") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            DatePickerField(
                                label = "1º Vencimento",
                                selectedDate = detectedDueDate,
                                onDateSelected = { detectedDueDate = DateTimeUtils.formatBrazilianToIso(it) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = detectedBarCode,
                                onValueChange = { detectedBarCode = it },
                                label = { Text("Código de Barras / Linha Digitável") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }

                    // Installment Multiplier Section
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Número de Parcelas Consecutivas",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "O sistema gera automaticamente as datas dos meses seguintes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${numberOfInstallments}x",
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Installment Selector Chips
                            val installmentOptions = listOf(1, 2, 3, 6, 10, 12, 18, 24, 36, 48)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(installmentOptions) { count ->
                                    val isSelected = numberOfInstallments == count
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { numberOfInstallments = count },
                                        label = { Text("${count}x", fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldPrimary,
                                            selectedLabelColor = Color(0xFF090C12)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Fixed Expense Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Classificar como Despesa Fixa",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Ajuda a calcular os gastos fixos vs variáveis no painel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Switch(
                                    checked = isFixedExpense,
                                    onCheckedChange = { isFixedExpense = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                                )
                            }
                        }
                    }

                    // Installment Schedule Preview Table
                    if (numberOfInstallments > 1) {
                        val amount = detectedAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val previewList = remember(numberOfInstallments, detectedDueDate, amount) {
                            BoletoPdfParser.generateInstallmentTransactions(
                                userId = currentUserId,
                                baseTitle = detectedTitle,
                                category = detectedCategory,
                                amountPerInstallment = amount,
                                firstDueDate = detectedDueDate,
                                totalInstallments = numberOfInstallments,
                                barCode = detectedBarCode,
                                attachmentName = selectedFileName ?: "boleto.pdf",
                                isFixed = isFixedExpense
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cronograma Previsto:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                    Text(
                                        text = "Total: R$ ${String.format(Locale.getDefault(), "%,.2f", amount * numberOfInstallments)}",
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                previewList.take(6).forEach { tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tx.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Vence: ${DateTimeUtils.formatIsoToBrazilian(tx.dueDate)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Text(
                                            text = "R$ ${String.format(Locale.getDefault(), "%.2f", tx.amount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                    }
                                }

                                if (previewList.size > 6) {
                                    Text(
                                        text = "+ mais ${previewList.size - 6} parcelas subsequentes geradas com sucesso...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
