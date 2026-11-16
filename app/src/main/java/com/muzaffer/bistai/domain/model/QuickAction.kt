package com.muzaffer.bistai.domain.model

enum class StockQuickAction(val label: String) {
    QUICK_SUMMARY("Hızlı Özet Geç"),
    NEWS_ANALYSIS("Haber Analizi"),
    RISK_AND_LEVELS("Risk & Kritik Seviyeler"),
    SECTOR_COMPARISON("Sektör & Rakip Analizi")
}

enum class PortfolioQuickAction(val label: String) {
    DIVERSIFICATION_RISK("Çeşitlendirme / Risk Analizi"),
    PROFIT_LOSS_REVIEW("Kâr / Zarar Değerlendirmesi"),
    SECTOR_ALLOCATION("Sektörel Dağılım Özeti"),
    STRATEGY_SUMMARY("Portföy Strateji Özeti")
}
