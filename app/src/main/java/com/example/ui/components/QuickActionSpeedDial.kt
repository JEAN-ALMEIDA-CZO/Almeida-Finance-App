package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IncomeGreen

/**
 * Menu Rápido Flutuante estilo Apple / Speed Dial para adição imediata de
 * Receitas, Despesas, Leitura de Boletos com IA e Sincronização Bancária.
 */
@Composable
fun QuickActionSpeedDial(
    isOpen: Boolean,
    onToggle: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onReadBoleto: () -> Unit,
    onBankSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) 135f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "fab_rotation"
    )

    Box(modifier = modifier) {
        // Scrim transparente ao abrir
        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle() }
            )
        }

        // Coluna com os botões rápidos posicionados acima do FAB principal
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedVisibility(
                visible = isOpen,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpeedDialItem(
                        label = "Nova Receita / Entrada",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconTint = IncomeGreen,
                        backgroundColor = Color(0xFF14241B),
                        onClick = {
                            onToggle()
                            onAddIncome()
                        },
                        testTag = "speed_dial_income_button"
                    )

                    SpeedDialItem(
                        label = "Nova Despesa / Gasto",
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        iconTint = ExpenseRed,
                        backgroundColor = Color(0xFF271518),
                        onClick = {
                            onToggle()
                            onAddExpense()
                        },
                        testTag = "speed_dial_expense_button"
                    )

                    SpeedDialItem(
                        label = "Ler Boleto / PDF (Parcelas)",
                        icon = Icons.Default.PictureAsPdf,
                        iconTint = GoldPrimary,
                        backgroundColor = Color(0xFF262010),
                        onClick = {
                            onToggle()
                            onReadBoleto()
                        },
                        testTag = "speed_dial_boleto_button"
                    )

                    SpeedDialItem(
                        label = "Sincronizar Bancos (Open Finance)",
                        icon = Icons.Default.AccountBalance,
                        iconTint = Color(0xFF60A5FA),
                        backgroundColor = Color(0xFF121E2E),
                        onClick = {
                            onToggle()
                            onBankSync()
                        },
                        testTag = "speed_dial_bank_sync_button"
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Botão Principal Circular em Ouro Luxo
            FloatingActionButton(
                onClick = onToggle,
                shape = CircleShape,
                containerColor = GoldPrimary,
                contentColor = Color(0xFF090C12),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(58.dp)
                    .testTag("quick_action_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isOpen) "Fechar Menu Rápido" else "Menu Rápido de Ações",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF161B26),
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(0.7.dp, Color(0x33FFFFFF)),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(backgroundColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
