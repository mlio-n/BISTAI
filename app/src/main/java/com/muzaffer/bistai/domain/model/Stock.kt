package com.muzaffer.bistai.domain.model

/**
 * Bir hisse senedini temsil eden domain-layer veri modeli.
 *
 * @param symbol        Borsa kodu (örn. "THYAO", "GARAN")
 * @param name          Şirket adı (örn. "Türk Hava Yolları")
 * @param currentPrice  Güncel fiyat (TL cinsinden)
 * @param changePercent Günlük değişim yüzdesi (+/-). Pozitif → yükseliş, negatif → düşüş.
 * @param previousClose Önceki kapanış fiyatı (opsiyonel)
 * @param marketCap     Piyasa kapitalizasyonu (opsiyonel, TL)
 * @param volume        Günlük işlem hacmi (opsiyonel)
 */
data class Stock(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val changePercent: Double,
    val previousClose: Double? = null,
    val marketCap: Long? = null,
    val volume: Long? = null
) {
    /** Mutlak fiyat değişimi (TL) */
    val changeAmount: Double
        get() = previousClose?.let { currentPrice - it } ?: 0.0

    /** Hisse yükselişte mi? */
    val isBullish: Boolean
        get() = changePercent >= 0.0

    /** Formatlı değişim yüzdesi, örn. "+2.34%" veya "-1.17%" */
    val formattedChangePercent: String
        get() = if (isBullish) "+%.2f%%".format(changePercent)
                else "%.2f%%".format(changePercent)
}
