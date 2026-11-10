package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.util.DateTimeUtils
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class CategoryExpenseSlice(
    val category: String,
    val amount: Double,
    val percentage: Float, // 0.0 .. 100.0
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color
)

private val CATEGORY_COLOR_PALETTE = mapOf(
    "Alimentação" to Color(0xFFFF7043),   // Coral / Orange
    "Moradia" to Color(0xFF5C6BC0),       // Indigo
    "Transporte" to Color(0xFF26A69A),   // Teal
    "Educação" to Color(0xFFAB47BC),     // Purple
    "Saúde" to Color(0xFFEC407A),        // Pink
    "Lazer" to Color(0xFFFFA726),        // Amber
    "Serviços" to Color(0xFF42A5F5),     // Blue
    "Investimentos" to Color(0xFF66BB6A),// Green
    "Outros" to Color(0xFF78909C)        // Blue Grey
)

private val FALLBACK_COLORS = listOf(
    Color(0xFFFF8A65),
    Color(0xFF7E57C2),
    Color(0xFF29B6F6),
    Color(0xFF26C6DA),
    Color(0xFF9CCC65),
    Color(0xFFFFCA28),
    Color(0xFF8D6E63),
    Color(0xFFFF5252)
)

private fun getCategoryColor(category: String, index: Int): Color {
    return CATEGORY_COLOR_PALETTE[category]
        ?: FALLBACK_COLORS[index % FALLBACK_COLORS.size]
}

/**
 * Gráfico de pizza/rosca interativo estilo Recharts para visualização da distribuição
 * de despesas por categoria no Dashboard do Almeida Finance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveExpensePieChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    onCategoryClick: ((String) -> Unit)? = null
) {
    var isMonthlyOnly by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val currentMonth = remember { DateTimeUtils.getCurrentYearMonth() }

    // Filter expenses based on period toggle
    val relevantExpenses = remember(transactions, isMonthlyOnly, currentMonth) {
        val expenseList = transactions.filter { it.type == TransactionType.EXPENSE.name && it.amount > 0 }
        if (isMonthlyOnly) {
            val monthly = expenseList.filter {
                it.date.startsWith(currentMonth) || it.dueDate.startsWith(currentMonth)
            }
            if (monthly.isNotEmpty()) monthly else expenseList
        } else {
            expenseList
        }
    }

    val totalExpenseAmount = remember(relevantExpenses) {
        relevantExpenses.sumOf { it.amount }
    }

    // Group expenses by category
    val slices = remember(relevantExpenses, totalExpenseAmount) {
        if (totalExpenseAmount <= 0.0) {
            emptyList()
        } else {
            val grouped = relevantExpenses.groupBy { it.category }
                .map { (cat, list) -> cat to list.sumOf { it.amount } }
                .sortedByDescending { it.second }

            var currentAngle = -90f // Start at top (12 o'clock)
            grouped.mapIndexed { idx, (cat, amount) ->
                val ratio = (amount / totalExpenseAmount).toFloat()
                val sweep = ratio * 360f
                val slice = CategoryExpenseSlice(
                    category = cat,
                    amount = amount,
                    percentage = ratio * 100f,
                    startAngle = currentAngle,
                    sweepAngle = sweep,
                    color = getCategoryColor(cat, idx)
                )
                currentAngle += sweep
                slice
            }
        }
    }

    val selectedSlice = remember(slices, selectedCategory) {
        slices.find { it.category == selectedCategory }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_expense_pie_chart"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.8.dp, Color(0x22FFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title + Period Toggle Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Gráfico de Pizza Interativo",
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Despesas por Categoria",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isMonthlyOnly) "Mês Corrente • Toque para interagir" else "Geral (Todo o Período)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // Period Switch (Mês vs Geral)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("pie_chart_period_toggle")
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMonthlyOnly) GoldPrimary else Color.Transparent)
                                .clickable { isMonthlyOnly = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Mês",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMonthlyOnly) Color(0xFF090C12) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isMonthlyOnly) GoldPrimary else Color.Transparent)
                                .clickable { isMonthlyOnly = false }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Geral",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isMonthlyOnly) Color(0xFF090C12) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (slices.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DonutLarge,
                            contentDescription = null,
                            tint = Color(0xFF555F72),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhuma despesa registrada",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E95A5)
                        )
                        Text(
                            text = "Cadastre novas despesas para gerar a distribuição",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            } else {
                // Interactive Donut Chart Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val defaultStrokeWidth = 32f
                    val selectedStrokeWidth = 44f

                    Canvas(
                        modifier = Modifier
                            .size(220.dp)
                            .testTag("pie_chart_canvas")
                            .pointerInput(slices, selectedCategory) {
                                detectTapGestures { tapOffset ->
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val dx = tapOffset.x - centerX
                                    val dy = tapOffset.y - centerY
                                    val distance = sqrt(dx * dx + dy * dy)

                                    val outerRadius = min(centerX, centerY) - 8f
                                    val innerRadius = outerRadius - selectedStrokeWidth - 10f

                                    if (distance < innerRadius) {
                                        // Tapped inside center hole -> clear selection
                                        selectedCategory = null
                                    } else if (distance <= outerRadius + 20f) {
                                        // Compute angle in degrees [0..360)
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0f) angle += 360f

                                        // Start angle is -90f (270f in [0..360) coordinates)
                                        val angleFromTop = (angle + 90f) % 360f

                                        var cumulative = 0f
                                        var matchedCategory: String? = null
                                        for (slice in slices) {
                                            if (angleFromTop >= cumulative && angleFromTop < cumulative + slice.sweepAngle) {
                                                matchedCategory = slice.category
                                                break
                                            }
                                            cumulative += slice.sweepAngle
                                        }

                                        if (matchedCategory != null) {
                                            selectedCategory = if (selectedCategory == matchedCategory) null else matchedCategory
                                            onCategoryClick?.invoke(matchedCategory)
                                        }
                                    }
                                }
                            }
                    ) {
                        val canvasSize = size
                        val centerX = canvasSize.width / 2f
                        val centerY = canvasSize.height / 2f
                        val outerRadius = min(centerX, centerY) - 10f

                        // Draw subtle background track
                        drawCircle(
                            color = Color(0x14FFFFFF),
                            radius = outerRadius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = defaultStrokeWidth)
                        )

                        // Draw slices
                        for (slice in slices) {
                            val isSelected = slice.category == selectedCategory
                            val hasAnySelection = selectedCategory != null
                            val strokeWidth = if (isSelected) selectedStrokeWidth else defaultStrokeWidth
                            val sliceAlpha = if (!hasAnySelection || isSelected) 1.0f else 0.35f

                            val arcRadius = if (isSelected) outerRadius + 4f else outerRadius
                            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                            val topLeft = Offset(centerX - arcRadius, centerY - arcRadius)

                            // Gap between slices: leave 1.5 degree gap for visual definition
                            val sweepGap = if (slices.size > 1) (slice.sweepAngle - 2.2f).coerceAtLeast(0.5f) else slice.sweepAngle
                            val startGap = slice.startAngle + 1.1f

                            drawArc(
                                color = slice.color.copy(alpha = sliceAlpha),
                                startAngle = startGap,
                                sweepAngle = sweepGap,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )

                            // If selected, add an elegant highlight outline
                            if (isSelected) {
                                drawArc(
                                    color = Color.White.copy(alpha = 0.8f),
                                    startAngle = startGap,
                                    sweepAngle = sweepGap,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(
                                        width = 1.5f,
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }
                    }

                    // Center Donut Hole Tooltip / Interactive Display (Recharts style)
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF1E2333),
                                        Color(0xFF121622),
                                        Color(0xFF0B0E17)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = if (selectedSlice != null) selectedSlice.color.copy(alpha = 0.6f) else Color(0x28FFFFFF),
                                shape = CircleShape
                            )
                            .clickable { selectedCategory = null }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = selectedSlice,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                            label = "donut_center_content"
                        ) { slice ->
                            if (slice != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = slice.category,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = slice.color,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = DateTimeUtils.formatCurrency(slice.amount),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = slice.color.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.1f%%", slice.percentage),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = slice.color,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "TOTAL DESPESAS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = Color(0xFF8E95A5)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = DateTimeUtils.formatCurrency(totalExpenseAmount),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${slices.size} categorias",
                                        fontSize = 10.sp,
                                        color = GoldLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Legend (Pills / Badges with category, percentage, and value)
                Text(
                    text = "Categorias • Toque para filtrar:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E95A5),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slices.forEach { slice ->
                        val isSelected = slice.category == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) slice.color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (isSelected) 1.2.dp else 0.5.dp,
                                color = if (isSelected) slice.color else Color(0x1FFFFFFF)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedCategory = if (isSelected) null else slice.category
                                    if (selectedCategory != null) {
                                        onCategoryClick?.invoke(slice.category)
                                    }
                                }
                                .testTag("pie_legend_${slice.category.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = slice.category,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", slice.percentage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = slice.color,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${DateTimeUtils.formatCurrency(slice.amount)})",
                                    fontSize = 10.sp,
                                    color = Color(0xFF8E95A5),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
