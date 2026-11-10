package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldPrimary
import com.example.util.DateTimeUtils

/**
 * Campo seletor de data interativo e nativo (estilo Apple / Material 3).
 * Garante que o usuário selecione a data em um calendário em vez de digitar,
 * formatado no padrão brasileiro (dd/MM/yyyy) e fuso de São Paulo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: String, // formato dd/MM/yyyy ou yyyy-MM-dd
    onDateSelected: (String) -> Unit, // retorna dd/MM/yyyy
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val displayDate = remember(selectedDate) {
        DateTimeUtils.formatIsoToBrazilian(selectedDate)
    }

    val initialMillis = remember(selectedDate) {
        DateTimeUtils.parseBrazilianToEpochMillis(displayDate) ?: System.currentTimeMillis()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayDate,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Selecionar data",
                    tint = GoldPrimary
                )
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                focusedLabelColor = GoldPrimary,
                cursorColor = GoldPrimary
            )
        )

        // Camada transparente clicável sobre todo o campo
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val brDate = DateTimeUtils.formatEpochMillisToBrazilian(millis)
                            onDateSelected(brDate)
                        }
                        showDialog = false
                    }
                ) {
                    Text("Confirmar", color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = GoldPrimary,
                    selectedDayContentColor = androidx.compose.ui.graphics.Color(0xFF090C12),
                    todayDateBorderColor = GoldPrimary,
                    todayContentColor = GoldPrimary
                )
            )
        }
    }
}
