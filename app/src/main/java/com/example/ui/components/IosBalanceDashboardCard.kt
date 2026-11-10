package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IncomeGreen
import com.example.util.DateTimeUtils

/**
 * Componente de dashboard no topo da tela principal exibindo o saldo atual,
 * total de receitas e total de despesas do mês, utilizando um estilo minimalista inspirado no iOS.
 */
@Composable
fun IosBalanceDashboardCard(
    currentBalance: Double,
    monthlyIncome: Double,
    monthlyExpense: Double,
    isBalanceHidden: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    workspaceName: String? = null,
    onWorkspaceClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val monthTitle = DateTimeUtils.getCurrentMonthYearFormatted()
    val savingsRate = if (monthlyIncome > 0) {
        (((monthlyIncome - monthlyExpense) / monthlyIncome) * 100).coerceIn(0.0, 100.0)
    } else {
        0.0
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ios_balance_dashboard_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.7.dp, Color(0x28FFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF161A26),
                            Color(0xFF10141F),
                            Color(0xFF0A0D15)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Meta Header: Workspace Badge + Month Indicator + Privacy Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month Pill (iOS Capsule Style)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x1FFFFFFF),
                        border = BorderStroke(0.5.dp, Color(0x22FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = monthTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Workspace Pill & Privacy Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!workspaceName.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldPrimary.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, GoldPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable(enabled = onWorkspaceClick != null) { onWorkspaceClick?.invoke() }
                            ) {
                                Text(
                                    text = workspaceName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleBalanceVisibility,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("ios_balance_privacy_toggle")
                        ) {
                            Icon(
                                imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isBalanceHidden) "Exibir Saldo" else "Ocultar Saldo",
                                tint = Color(0xFFB0B7C3),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Balance Section
                Text(
                    text = "SALDO ATUAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color(0xFF8E95A5)
                )

                Spacer(modifier = Modifier.height(4.dp))

                AnimatedContent(
                    targetState = isBalanceHidden,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "balance_text"
                ) { hidden ->
                    Text(
                        text = if (hidden) "R$ • • • • • •" else DateTimeUtils.formatCurrency(currentBalance),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (currentBalance >= 0) Color.White else ExpenseRed,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Subtle Hairline Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(Color(0x1FFFFFFF))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Monthly Income and Expense Segmented Row (iOS Style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Column: Receitas do Mês
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Receitas do Mês",
                                tint = IncomeGreen,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Receitas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF8E95A5),
                                maxLines = 1
                            )
                            Text(
                                text = if (isBalanceHidden) "••••" else DateTimeUtils.formatCurrency(monthlyIncome),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Vertical Hairline Divider
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(0.6.dp)
                            .background(Color(0x1FFFFFFF))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Column: Despesas do Mês
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = "Despesas do Mês",
                                tint = ExpenseRed,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Despesas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF8E95A5),
                                maxLines = 1
                            )
                            Text(
                                text = if (isBalanceHidden) "••••" else DateTimeUtils.formatCurrency(monthlyExpense),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Micro Economy Indicator Bar
                if (monthlyIncome > 0 && !isBalanceHidden) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Economizado do mês:",
                            fontSize = 11.sp,
                            color = Color(0xFF8E95A5),
                            maxLines = 1
                        )
                        Text(
                            text = "${savingsRate.toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (savingsRate > 20) IncomeGreen else GoldPrimary,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (savingsRate / 100f).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = IncomeGreen,
                        trackColor = Color(0x1AFFFFFF)
                    )
                }
            }
        }
    }
}
