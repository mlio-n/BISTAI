package com.muzaffer.bistai.presentation.stockdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.core.Resource
import com.muzaffer.bistai.data.remote.AiApiService
import com.muzaffer.bistai.data.remote.SimpleChat
import com.muzaffer.bistai.domain.model.AiPrediction
import com.muzaffer.bistai.domain.model.StockQuickAction
import com.muzaffer.bistai.domain.repository.AnalysisRepository
import com.muzaffer.bistai.domain.repository.AnalysisSource
import com.muzaffer.bistai.domain.repository.AuthRepository
import com.muzaffer.bistai.domain.repository.NewsRepository
import com.muzaffer.bistai.domain.repository.PortfolioRepository
import com.muzaffer.bistai.domain.repository.StockRepository
import com.muzaffer.bistai.presentation.components.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockDetailUiState(
    val symbol: String                        = "",
    val isLoading: Boolean                    = false,
    val analysis: String?                     = null,
    val prediction: AiPrediction?             = null,
    val analysisSource: AnalysisSource?       = null,   // 🔥 Firebase / 💾 Yerel / ✨ Taze
    val analysisAgeMinutes: Long?             = null,   // kaç dakika önce üretildi
    val errorMessage: String?                 = null,
    val chatMessages: List<ChatMessage>       = emptyList(),
    val isChatLoading: Boolean                = false,
    val selectedQuickAction: StockQuickAction? = null,
    val isQuickActionSheetVisible: Boolean    = false,
    val isQuickActionLoading: Boolean         = false,
    val quickActionResult: String?            = null,
    val quickActionError: String?             = null
)

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val aiService: AiApiService,
    private val newsRepository: NewsRepository,
    private val analysisRepository: AnalysisRepository,   // ← V2.1: Firebase → API → Room
    private val authRepository: AuthRepository,
    private val portfolioRepository: PortfolioRepository,
    private val stockRepository: StockRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockDetailUiState())
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    private var chat: SimpleChat? = null
    private var quickActionJob: Job? = null

    companion object {
        private const val TAG = "BISTAI_VM"
    }

    init {
        val symbol = savedStateHandle.get<String>("symbol") ?: ""
        _uiState.update { it.copy(symbol = symbol) }
        fetchAnalysis(symbol)
        initChat(symbol)
    }

    /**
     * V2.1: Firebase → Gemini API → Room cache öncelik sırası.
     * Tüm kullanıcılar Firebase'deki ortak analizi okur; sadece ilk kullanıcı API çağrısı yapar.
     */
    fun fetchAnalysis(symbol: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, analysis = null,
                    prediction = null, analysisSource = null)
            }

            val news  = newsRepository.getNewsForAsset(symbol)
            val macro = newsRepository.getMacroContext()

            analysisRepository.getAnalysis(
                symbol       = symbol,
                newsItems    = news,
                macroContext = macro
            ).onSuccess { (prediction, source, generatedAt) ->
                val ageMinutes = (System.currentTimeMillis() - generatedAt) / 60_000L
                Log.d(TAG, "✅ Analiz alındı: $symbol | Kaynak: $source | $ageMinutes dk önce")
                _uiState.update {
                    it.copy(
                        isLoading          = false,
                        prediction         = prediction,
                        analysis           = prediction.reasoning,
                        analysisSource     = source,
                        analysisAgeMinutes = ageMinutes
                    )
                }
                updateChatGreeting(symbol, isError = false)
                chat = aiService.startStockChat(symbol, prediction)
            }.onFailure { error ->
                val msg = mapAiError(error)
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                updateChatGreeting(symbol, isError = true)
            }
        }
    }

    fun executeQuickAction(action: StockQuickAction) {
        val symbol = _uiState.value.symbol
        if (symbol.isBlank()) return

        quickActionJob?.cancel()
        _uiState.update {
            it.copy(
                selectedQuickAction       = action,
                isQuickActionSheetVisible = true,
                isQuickActionLoading      = true,
                quickActionResult         = null,
                quickActionError          = null
            )
        }

        quickActionJob = viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid
            val (stock, portfolioItem, news) = coroutineScope {
                val stockDeferred = async {
                    runCatching {
                        stockRepository.getStockDetail(symbol)
                            .filter { it !is Resource.Loading }
                            .firstOrNull()
                    }.getOrNull() as? Resource.Success
                }
                val portfolioDeferred = async {
                    runCatching {
                        portfolioRepository.getPortfolio(uid)
                            .firstOrNull()
                            ?.find { it.symbol.equals(symbol, ignoreCase = true) }
                    }.getOrNull()
                }
                val newsDeferred = async { newsRepository.getNewsForAsset(symbol) }
                Triple(stockDeferred.await()?.data, portfolioDeferred.await(), newsDeferred.await())
            }

            val macro = newsRepository.getMacroContext()
            analysisRepository.runStockQuickAction(
                action        = action,
                symbol        = symbol,
                stock         = stock,
                portfolioItem = portfolioItem,
                newsItems     = news,
                macroContext  = macro,
                prediction    = _uiState.value.prediction
            ).onSuccess { markdown ->
                _uiState.update {
                    it.copy(
                        isQuickActionLoading = false,
                        quickActionResult    = markdown,
                        quickActionError     = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isQuickActionLoading = false,
                        quickActionError     = mapAiError(error)
                    )
                }
            }
        }
    }

    fun dismissQuickActionSheet() {
        quickActionJob?.cancel()
        _uiState.update {
            it.copy(
                isQuickActionSheetVisible = false,
                isQuickActionLoading      = false
            )
        }
    }

    private fun mapAiError(error: Throwable): String = when {
        error.message == "API_KEY_MISSING" ->
            "API anahtarı tanımlı değil. Menü sekmesinden kendi API anahtarınızı kaydedebilir veya local.properties üzerinden GEMINI_API_KEY tanımlayabilirsiniz."
        error.message?.startsWith("API_KEY_INVALID") == true ||
        error.message?.startsWith("API_MODEL_NOT_FOUND") == true ->
            "${error.message}\n\naistudio.google.com adresinden geçerli bir anahtar alabilirsiniz."
        error.message?.startsWith("QUOTA_EXCEEDED") == true ->
            "Günlük kota aşıldı. Lütfen birkaç dakika bekleyin."
        error.message?.contains("Unable to resolve host") == true ||
        error.message?.contains("timeout") == true ->
            "İnternet bağlantısı kurulamadı."
        else -> error.message ?: "Analiz şu an yapılamıyor."
    }

    private fun initChat(symbol: String) {
        if (!aiService.isApiKeySet) return
        try {
            chat = aiService.startStockChat(symbol)
            _uiState.update {
                it.copy(
                    chatMessages = listOf(
                        ChatMessage(
                            text = "**$symbol** analizi yükleniyor, ardından sorularınızı yanıtlayacağım...",
                            isFromUser = false
                        )
                    )
                )
            }
        } catch (_: Exception) {}
    }

    private fun updateChatGreeting(symbol: String, isError: Boolean) {
        val text = if (isError)
            "Analiz yüklenemedi, ancak **$symbol** hakkında sorularınızı yanıtlamaya devam edebilirim."
        else
            "Analiz tamamlandı! **$symbol** için tahminim ve gerekçem hazır. Detaylarını sormak ister misin?"

        _uiState.update { state ->
            val updated = state.chatMessages.toMutableList()
            if (updated.isNotEmpty() && !updated[0].isFromUser) {
                updated[0] = updated[0].copy(text = text)
            }
            state.copy(chatMessages = updated)
        }
    }

    fun sendChatMessage(message: String) {
        val currentChat = chat ?: return
        if (message.isBlank()) return
        val userMsg    = ChatMessage(text = message, isFromUser = true)
        val loadingMsg = ChatMessage(text = "...", isFromUser = false, isLoading = true, id = -1L)
        _uiState.update { it.copy(chatMessages = it.chatMessages + userMsg + loadingMsg, isChatLoading = true) }
        viewModelScope.launch {
            aiService.sendMessage(currentChat, message)
                .onSuccess { reply ->
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages.filter { m -> !m.isLoading } +
                                    ChatMessage(text = reply, isFromUser = false),
                            isChatLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages.filter { m -> !m.isLoading } +
                                    ChatMessage(text = "Yanıt alınamadı: ${error.message}", isFromUser = false),
                            isChatLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Portföye hisse kaydeder. Misafir modunda yalnızca yerel Room veritabanına yazar;
     * giriş yapılmışsa Firestore (users/{uid}/portfolio/{symbol}) ile de senkronize eder.
     */
    fun addToPortfolio(lotSize: Double, averageCost: Double, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid
            val item = com.muzaffer.bistai.domain.model.PortfolioItem(
                symbol = uiState.value.symbol,
                lotSize = lotSize,
                averageCost = averageCost
            )

            val result = portfolioRepository.addOrUpdatePortfolioItem(uid, item)
            if (result.isSuccess) {
                onResult(
                    true,
                    if (uid != null) "Portföye ve buluta kaydedildi." else "Yerel portföye kaydedildi."
                )
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Kayıt sırasında hata oluştu.")
            }
        }
    }
}
