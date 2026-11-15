# 🤖 BISTAI — Yapay Zeka Destekli Borsa İstanbul Terminali

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/AI-Gemini%20Flash-4285F4?style=for-the-badge&logo=google&logoColor=white"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
  <img src="https://img.shields.io/badge/Min%20SDK-26-brightgreen?style=for-the-badge"/>
</p>

<p align="center">
  <b>BIST hisseleri, emtialar, kripto ve döviz için gerçek zamanlı fiyat takibi ve Gemini AI destekli analiz uygulaması.</b>
</p>

---

## ✨ Özellikler

| Özellik | Açıklama |
|---|---|
| 📈 **Canlı Fiyatlar** | Yahoo Finance API ile gerçek zamanlı BIST hisse verileri |
| 🤖 **Medyum AI** | Google Gemini Flash ile haber + makro bağlam destekli tahmin analizi |
| 🔥 **Merkezi Analiz Önbelleği** | Cloud Firestore (24 saat TTL) + Room yerel yedek önbelleği |
| 📰 **Türkçe Ekonomi Haberleri** | TRT Haber Ekonomi RSS (`XmlPullParser`) ile güncel haber akışı |
| 💼 **Portföy & Google Girişi** | Firebase Auth (Credential Manager) ve Cloud Firestore ile kişisel portföy takibi |
| 💬 **AI Sohbet** | Hem genel piyasa danışmanı hem hisse bazlı interaktif AI sohbeti |
| ⭐ **İzleme Listesi** | Favori hisseleri Room veritabanında sakla |
| 🌙 **Karanlık / Aydınlık Tema** | Material 3 ile tam tema desteği |

---

## 📱 Desteklenen Varlıklar

**BIST Hisseleri:** THYAO, GARAN, SISE, YKBNK, KCHOL, EREGL, AKBNK, ASELS, BIMAS, TUPRS, PGSUS, TOASO, FROTO, SAHOL, TAVHL

**Emtialar:** Altın (TL/gram), Gümüş (TL/gram)

**Kripto & Döviz:** Bitcoin (BTC/USD), Dolar (USD/TRY)

---

## 🏗️ Teknoloji Stack

```text
📦 BISTAI
├── 🏛️ Mimari           → Clean Architecture (core, di, data, domain, presentation)
├── 🎨 UI               → Jetpack Compose + Material 3
├── 🧠 AI               → Google Gemini Flash (REST API)
├── 📈 Hisse Fiyatları  → Yahoo Finance API (ücretsiz, key gerektirmez)
├── 📰 Haberler         → TRT Haber Ekonomi RSS (XmlPullParser)
├── 🔥 Cloud & Auth     → Firebase Auth (Google Sign-In / Credential Manager) + Cloud Firestore
├── 💾 Yerel Veritabanı → Room (StockDao + AnalysisCacheDao)
├── 💉 DI               → Dagger Hilt
├── 🌐 Network          → Retrofit 2 + OkHttp 4
└── ⚡ Async            → Kotlin Coroutines & Flow
```

---

## 🚀 Kurulum

### Gereksinimler
- Android Studio Hedgehog veya üstü
- Android SDK 26+

### Adımlar

```bash
# 1. Repoyu klonla
git clone https://github.com/mlio-n/BISTAI.git
cd BISTAI

# 2. API anahtarını tanımla
echo "GEMINI_API_KEY=senin_api_anahtarin" >> local.properties

# 3. Android Studio'da aç ve çalıştır
```

> **Not:** `local.properties` dosyası `.gitignore`'a eklidir, API anahtarın güvende kalır.

---

## ⚙️ API Yapısı

| API | Kullanım | Limit | Key |
|---|---|---|---|
| **Yahoo Finance** | Hisse fiyatları | ✅ Sınırsız | ❌ Yok |
| **TRT Haber Ekonomi RSS** | Türkçe ekonomi haberleri | ✅ Sınırsız | ❌ Yok |
| **Firebase (Auth & Firestore)** | Google girişi, portföy & merkezi analiz havuzu | Spark/Blaze planı | ✅ `google-services.json` |
| **Google Gemini Flash** | AI analiz & sohbet | Ücretsiz kota / API planı | ✅ Gerekli |

---

## 🗂️ Proje Yapısı

Proje **Clean Architecture** prensiplerine uygun olarak 5 ana katman altında yapılandırılmıştır:

```text
app/src/main/java/com/muzaffer/bistai/
├── BistaiApp.kt                 # Hilt Application sınıfı (@HiltAndroidApp)
├── MainActivity.kt              # Tek Activity giriş noktası (@AndroidEntryPoint)
├── core/                        # Sabitler, extension fonksiyonları ve hata/durum yönetimi
│   ├── Constants.kt             # API URL'leri, DB adı, Firestore koleksiyon ve TTL sabitleri
│   ├── Extensions.kt            # Para birimi, yüzde ve hisse sembolü extension fonksiyonları
│   └── Resource.kt              # Generic ağ/veritabanı durum sarmalayıcısı (Success/Error/Loading)
├── di/                          # Dagger Hilt Dependency Injection modülleri
│   ├── DatabaseModule.kt        # Room Database, StockDao, AnalysisCacheDao ve WatchlistModule
│   ├── FirebaseModule.kt        # FirebaseFirestore ve FirebaseAuth bağımlılıkları
│   ├── NetworkModule.kt         # OkHttp (@YfHttpClient), Retrofit ve API servisleri
│   ├── NewsModule.kt            # NewsRepository bağlaması
│   └── RepositoryModule.kt      # Stock, Analysis, Auth ve Portfolio repository bağlamaları
├── data/                        # Veri katmanı (Remote API, Local DB, Mapper, Repository Impl)
│   ├── local/
│   │   ├── dao/                 # StockDao, AnalysisCacheDao
│   │   ├── database/            # BistaiDatabase (Room)
│   │   ├── entity/              # StockEntity, AnalysisCacheEntity
│   │   └── fake/                # FakeStockDataSource, FakeNewsDataSource
│   ├── mapper/                  # StockMapper (DTO → Domain model dönüşümleri)
│   ├── remote/
│   │   ├── dto/                 # StockApiResponse ve yf/YfResponse DTO modelleri
│   │   ├── AiApiService.kt      # Gemini Flash REST API istemcisi
│   │   ├── RemoteNewsDataSource.kt  # TRT Haber Ekonomi RSS XML parser
│   │   ├── RemoteStockDataSource.kt # Yahoo Finance veri kaynağı
│   │   ├── StockApiService.kt   # Borsa REST API arayüzü
│   │   └── YfApiService.kt      # Yahoo Finance Retrofit arayüzü
│   └── repository/              # Analysis, Auth, News, Portfolio, Stock, Watchlist implementasyonları
├── domain/                      # İş mantığı katmanı (Saf Kotlin modelleri, arayüzler ve UseCase'ler)
│   ├── model/                   # AiPrediction, NewsItem, PortfolioItem, Stock, User
│   ├── repository/              # Analysis, Auth, News, Portfolio, Stock, Watchlist arayüzleri
│   └── usecase/                 # GetStocksUseCase
└── presentation/                # Sunum katmanı (Jetpack Compose UI, ViewModel, Navigation, Tema)
    ├── aichat/                  # AiChatScreen, AiChatViewModel
    ├── auth/                    # LoginScreen, AuthViewModel
    ├── components/              # ChatBubble, ChatMessage, ShimmerStockCard
    ├── favorites/               # FavoritesScreen
    ├── navigation/              # BistaiNavGraph, Screen
    ├── portfolio/               # PortfolioScreen, PortfolioViewModel
    ├── splash/                  # SplashActivity
    ├── stockdetail/             # StockDetailScreen, StockDetailViewModel
    └── theme/                   # Color, Theme, Type (Material 3 tema tanımları)
```

---

## 🤖 AI Analiz Nasıl Çalışır?

```text
1. Hisse seçilir (örn. THYAO)
        ↓
2. Öncelik 1: Cloud Firestore (analyses/{SYMBOL}) kontrol edilir (< 24 saat ise doğrudan döner ve Room'a yedeklenir)
        ↓
3. Güncel değilse TRT Haber Ekonomi RSS'den güncel haber başlıkları + makro bağlam alınır
        ↓
4. Öncelik 2: Google Gemini Flash API'ye prompt gönderilir ve yapılandırılmış JSON tahmin üretilir
        ↓
5. Sonuç hem Cloud Firestore'a hem Room (analysis_cache) veritabanına kaydedilir
        ↓
6. API başarısız olursa Öncelik 3: Room yerel önbelleğindeki son analiz gösterilir 🎯
```

---

## 📄 Lisans

Bu proje [MIT Lisansı](LICENSE) ile lisanslanmıştır.

---

<p align="center">
  <i>⭐ Beğendiysen yıldız atmayı unutma!</i>
</p>
