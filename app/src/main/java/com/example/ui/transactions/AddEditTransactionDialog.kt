package com.example.ui.transactions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IncomeGreen
import com.example.ui.components.DatePickerField
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val DEFAULT_CATEGORIES = listOf(
    "Alimentação",
    "Moradia",
    "Transporte",
    "Salário",
    "Investimentos",
    "Saúde",
    "Lazer",
    "Educação",
    "Serviços",
    "Outros"
)

val CONNECTED_BANKS = listOf(
    "Carteira Principal",
    "PicPay",
    "Nubank",
    "Itaú Unibanco",
    "Banco do Brasil",
    "Bradesco",
    "Santander",
    "Banco Inter",
    "Caixa Econômica"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    currentUserId: String,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    existingTx: Transaction? = null,
    initialType: TransactionType = TransactionType.EXPENSE
) {
    val today = remember { DateTimeUtils.getTodayIso() }

    var description by remember { mutableStateOf(existingTx?.description ?: "") }
    var amountStr by remember { mutableStateOf(existingTx?.let { String.format(Locale.US, "%.2f", it.amount) } ?: "") }
    var selectedType by remember { mutableStateOf(existingTx?.type?.let { TransactionType.valueOf(it) } ?: initialType) }
    var selectedCategory by remember { mutableStateOf(existingTx?.category ?: DEFAULT_CATEGORIES.first()) }
    var isFixed by remember { mutableStateOf(existingTx?.isFixed ?: false) }
    var dueDate by remember { mutableStateOf(existingTx?.dueDate ?: today) }
    var date by remember { mutableStateOf(existingTx?.date ?: today) }
    var isPaid by remember { mutableStateOf(existingTx?.isPaid ?: false) }
    var boletoAttachmentName by remember { mutableStateOf(existingTx?.boletoAttachmentName ?: "") }
    var boletoAttachmentData by remember { mutableStateOf(existingTx?.boletoAttachmentData ?: "") }
    var boletoBarCode by remember { mutableStateOf(existingTx?.boletoBarCode ?: "") }
    var selectedBank by remember { mutableStateOf(existingTx?.bankName?.ifEmpty { CONNECTED_BANKS.first() } ?: CONNECTED_BANKS.first()) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var bankExpanded by remember { mutableStateOf(false) }
    var showBoletoSection by remember { mutableStateOf(existingTx?.boletoBarCode?.isNotBlank() == true || existingTx?.boletoAttachmentName?.isNotBlank() == true) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            boletoAttachmentName = "comprovante_${System.currentTimeMillis()}.jpg"
            boletoAttachmentData = uri.toString()
            showBoletoSection = true
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
                        Text(
                            text = if (existingTx == null) "Novo Lançamento" else "Editar Lançamento",
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                                val amount = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (description.isNotBlank() && amount > 0.0) {
                                    val tx = Transaction(
                                        id = existingTx?.id ?: "",
                                        userId = currentUserId,
                                        description = description.trim(),
                                        type = selectedType.name,
                                        category = selectedCategory,
                                        amount = amount,
                                        isFixed = isFixed,
                                        dueDate = dueDate,
                                        date = date,
                                        isPaid = isPaid,
                                        boletoAttachmentName = boletoAttachmentName,
                                        boletoAttachmentData = boletoAttachmentData,
                                        boletoBarCode = boletoBarCode,
                                        bankName = selectedBank,
                                        isBankSynced = selectedBank != CONNECTED_BANKS.first(),
                                        createdAt = existingTx?.createdAt
                                    )
                                    onSave(tx)
                                    onDismiss()
                                }
                            },
                            enabled = description.isNotBlank() && (amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0.0,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_transaction_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF090C12))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar", color = Color(0xFF090C12), fontWeight = FontWeight.Bold, maxLines = 1)
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
                // Type Selector (Despesa vs Receita)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    val isExpense = selectedType == TransactionType.EXPENSE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isExpense) ExpenseRed else Color.Transparent)
                            .clickable { selectedType = TransactionType.EXPENSE }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Despesa (Saída)",
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isExpense) IncomeGreen else Color.Transparent)
                            .clickable { selectedType = TransactionType.INCOME }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Receita (Entrada)",
                            fontWeight = FontWeight.Bold,
                            color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }

                // Amount Field
                OutlinedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Valor da Movimentação (R$)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            placeholder = { Text("0,00", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("amount_input"),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == TransactionType.INCOME) IncomeGreen else GoldPrimary
                            ),
                            leadingIcon = {
                                Text(
                                    text = "R$",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        )
                    }
                }

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (ex: Aluguel, Supermercado, Salário)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("description_input")
                )

                // Category Selector (Linha individual)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DEFAULT_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Bank / Account Selector (Linha individual)
                ExposedDropdownMenuBox(
                    expanded = bankExpanded,
                    onExpandedChange = { bankExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedBank,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Conta / Banco") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = bankExpanded,
                        onDismissRequest = { bankExpanded = false }
                    ) {
                        CONNECTED_BANKS.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank) },
                                onClick = {
                                    selectedBank = bank
                                    bankExpanded = false
                                }
                            )
                        }
                    }
                }

                // Fixed Expense Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gasto Fixo Recorrente",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Despesas fixas mensais (ex: condomínio, luz, internet)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Switch(
                            checked = isFixed,
                            onCheckedChange = { isFixed = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                        )
                    }
                }

                // Dates: Competência e Vencimento (Selecionadas via Calendário Nativo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DatePickerField(
                        label = "Data Registro",
                        selectedDate = date,
                        onDateSelected = { date = DateTimeUtils.formatBrazilianToIso(it) },
                        modifier = Modifier.weight(1f)
                    )

                    DatePickerField(
                        label = "Vencimento",
                        selectedDate = dueDate,
                        onDateSelected = { dueDate = DateTimeUtils.formatBrazilianToIso(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Paid Status Switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedType == TransactionType.INCOME) "Recebido" else "Pago",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = if (isPaid) "Liquidado com sucesso" else "Aguardando pagamento",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPaid) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Switch(
                            checked = isPaid,
                            onCheckedChange = { isPaid = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = IncomeGreen)
                        )
                    }
                }

                // Boleto & PDF Attachment Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = GoldPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Anexar Boleto / Documento",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            Switch(
                                checked = showBoletoSection,
                                onCheckedChange = { showBoletoSection = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                            )
                        }

                        AnimatedVisibility(visible = showBoletoSection) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                OutlinedTextField(
                                    value = boletoBarCode,
                                    onValueChange = { boletoBarCode = it },
                                    label = { Text("Código de Barras / Linha Digitável") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(imageVector = Icons.Default.QrCode, contentDescription = null) }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Selecionar Arquivo")
                                    }

                                    if (boletoAttachmentName.isNotBlank()) {
                                        Text(
                                            text = boletoAttachmentName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = IncomeGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
}
