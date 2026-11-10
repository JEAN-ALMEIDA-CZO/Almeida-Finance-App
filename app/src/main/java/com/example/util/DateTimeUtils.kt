package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utilitários centralizados de data, hora e moeda para o Brasil (Fuso Horário de São Paulo / America/Sao_Paulo, Formato 24h).
 */
object DateTimeUtils {

    val SAO_PAULO_TIMEZONE: TimeZone = TimeZone.getTimeZone("America/Sao_Paulo")
    val LOCALE_PT_BR: Locale = Locale("pt", "BR")

    private val brazilianDateFormatter: SimpleDateFormat
        get() = SimpleDateFormat("dd/MM/yyyy", LOCALE_PT_BR).apply {
            timeZone = SAO_PAULO_TIMEZONE
        }

    private val isoDateFormatter: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", LOCALE_PT_BR).apply {
            timeZone = SAO_PAULO_TIMEZONE
        }

    private val time24hFormatter: SimpleDateFormat
        get() = SimpleDateFormat("HH:mm", LOCALE_PT_BR).apply {
            timeZone = SAO_PAULO_TIMEZONE
        }

    private val fullDateTime24hFormatter: SimpleDateFormat
        get() = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", LOCALE_PT_BR).apply {
            timeZone = SAO_PAULO_TIMEZONE
        }

    /**
     * Retorna a data de hoje no fuso de São Paulo em formato brasileiro dd/MM/yyyy.
     */
    fun getTodayBrazilian(): String {
        return brazilianDateFormatter.format(Date())
    }

    /**
     * Retorna a data de hoje no formato ISO yyyy-MM-dd.
     */
    fun getTodayIso(): String {
        return isoDateFormatter.format(Date())
    }

    /**
     * Retorna o horário atual em 24h (HH:mm) no fuso de São Paulo.
     */
    fun getCurrentTime24h(): String {
        return time24hFormatter.format(Date())
    }

    /**
     * Retorna data e horário completos em 24h (dd/MM/yyyy às HH:mm).
     */
    fun getCurrentDateTime24h(): String {
        return fullDateTime24hFormatter.format(Date())
    }

    /**
     * Converte data ISO (yyyy-MM-dd) para formato brasileiro (dd/MM/yyyy).
     * Se já estiver em formato brasileiro ou inválida, preserva ou ajusta graciosamente.
     */
    fun formatIsoToBrazilian(dateStr: String): String {
        if (dateStr.isBlank()) return getTodayBrazilian()
        if (dateStr.contains("/") && dateStr.length == 10) return dateStr
        return try {
            val parsed = isoDateFormatter.parse(dateStr)
            if (parsed != null) brazilianDateFormatter.format(parsed) else dateStr
        } catch (_: Exception) {
            dateStr
        }
    }

    /**
     * Converte data brasileira (dd/MM/yyyy) para formato ISO (yyyy-MM-dd).
     */
    fun formatBrazilianToIso(dateStr: String): String {
        if (dateStr.isBlank()) return getTodayIso()
        if (dateStr.contains("-") && dateStr.length == 10) return dateStr
        return try {
            val parsed = brazilianDateFormatter.parse(dateStr)
            if (parsed != null) isoDateFormatter.format(parsed) else dateStr
        } catch (_: Exception) {
            dateStr
        }
    }

    /**
     * Converte milissegundos UTC do DatePicker para formato brasileiro dd/MM/yyyy respeitando São Paulo.
     */
    fun formatEpochMillisToBrazilian(utcMillis: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(LOCALE_PT_BR, "%02d/%02d/%04d", day, month, year)
    }

    /**
     * Converte string dd/MM/yyyy para epoch millis para pré-selecionar no DatePicker.
     */
    fun parseBrazilianToEpochMillis(dateStr: String): Long? {
        val iso = formatBrazilianToIso(dateStr)
        return try {
            val parsed = isoDateFormatter.parse(iso)
            parsed?.time
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formatação monetária rigorosa em Real Brasileiro (R$ 1.250,00).
     */
    fun formatCurrency(amount: Double): String {
        val symbols = DecimalFormatSymbols(LOCALE_PT_BR).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
        val df = DecimalFormat("#,##0.00", symbols)
        return "R$ ${df.format(amount)}"
    }

    /**
     * Retorna o ano e mês atual em formato yyyy-MM (ex: 2026-11).
     */
    fun getCurrentYearMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM", LOCALE_PT_BR).apply {
            timeZone = SAO_PAULO_TIMEZONE
        }
        return sdf.format(Date())
    }

    /**
     * Retorna o mês e ano atual formatado em português (ex: "Novembro de 2026").
     */
    fun getCurrentMonthYearFormatted(): String {
        val sdf = SimpleDateFormat("MMMM 'de' yyyy", LOCALE_PT_BR).apply {
            timeZone = SAO_PAULO_TIMEZONE
        }
        val formatted = sdf.format(Date())
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(LOCALE_PT_BR) else it.toString() }
    }
}
