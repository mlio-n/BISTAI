package com.muzaffer.bistai.core

fun Double.toCurrencyString(): String = "₺%.2f".format(this)

fun Double.toPercentString(includeSign: Boolean = true): String =
    if (includeSign && this >= 0.0) "+%.2f%%".format(this)
    else "%.2f%%".format(this)

fun String.toYahooSymbol(): String =
    if (endsWith(".IS")) this else "$this.IS"

fun String.cleanYahooSymbol(): String =
    replace(".IS", "")
