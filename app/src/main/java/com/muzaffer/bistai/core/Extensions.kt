package com.muzaffer.bistai.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

private val turkishLocale = Locale("tr", "TR")
private val turkishSymbols = DecimalFormatSymbols(turkishLocale)

private val bistCompanyNames = mapOf(
    "THYAO" to "Türk Hava Yolları",
    "GARAN" to "Garanti BBVA",
    "SISE" to "Şişecam",
    "YKBNK" to "Yapı Kredi",
    "KCHOL" to "Koç Holding",
    "EREGL" to "Erdemir",
    "AKBNK" to "Akbank",
    "ASELS" to "Aselsan",
    "BIMAS" to "BİM Mağazalar",
    "TUPRS" to "Tüpraş",
    "PGSUS" to "Pegasus",
    "TOASO" to "Tofaş",
    "FROTO" to "Ford Otosan",
    "SAHOL" to "Sabancı Holding",
    "TAVHL" to "TAV Havalimanları"
)

fun Double.formatTrCurrency(includeSign: Boolean = false): String {
    val formatter = DecimalFormat("#,##0.00", turkishSymbols)
    val prefix = when {
        this < 0.0 -> "-₺"
        includeSign -> "+₺"
        else -> "₺"
    }
    return prefix + formatter.format(abs(this))
}

fun Double.formatTrPercent(includeSign: Boolean = true): String {
    val formatter = DecimalFormat("#,##0.00", turkishSymbols)
    val sign = when {
        !includeSign -> ""
        this >= 0.0 -> "+"
        else -> "-"
    }
    return "$sign%${formatter.format(abs(this))}"
}

fun Double.formatTrLot(): String {
    return if (this % 1.0 == 0.0) {
        val intFormatter = DecimalFormat("#,##0", turkishSymbols)
        "${intFormatter.format(toLong())} lot"
    } else {
        val decFormatter = DecimalFormat("#,##0.##", turkishSymbols)
        "${decFormatter.format(this)} lot"
    }
}

fun String.toCleanBistName(fallback: String? = null): String {
    val key = trim().uppercase().removeSuffix(".IS")
    return bistCompanyNames[key] ?: fallback?.takeIf { it.isNotBlank() } ?: key
}

fun Double.toCurrencyString(): String = formatTrCurrency()

fun Double.toPercentString(includeSign: Boolean = true): String = formatTrPercent(includeSign)

fun String.toYahooSymbol(): String =
    if (endsWith(".IS")) this else "$this.IS"

fun String.cleanYahooSymbol(): String =
    replace(".IS", "")
