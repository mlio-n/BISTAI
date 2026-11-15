package com.muzaffer.bistai.presentation.stockdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.data.remote.AiApiService
import com.muzaffer.bistai.data.remote.SimpleChat
import com.muzaffer.bistai.domain.model.AiPrediction
import com.muzaffer.bistai.domain.repository.AnalysisRepository
import com.muzaffer.bistai.domain.repository.AnalysisSource
import com.muzaffer.bistai.domain.repository.AuthRepository
import com.muzaffer.bistai.domain.repository.NewsRepository
import com.muzaffer.bistai.domain.repository.PortfolioRepository
import com.muzaffer.bistai.presentation.components.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockDetailUiState(
    val symbol: String                  = "",
    val isLoading: Boolean              = false,
    val analysis: String?               = null,
    val prediction: AiPrediction?       = null,
    val analysisSource: AnalysisSource? = null,   // 🔥 Firebase / 💾 Yerel / ✨ Taze
    val analysisAgeMinutes: Long?       = null,   // kaç dakika önce üretildi
    val errorMessage: String?           = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isChatLoading: Boolean          = false
)

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val aiService: AiApiService,
    private val newsRepository: NewsRepository,
    private val analysisRepository: AnalysisRepository,   // ← V2.1: Firebase → API → Room
    private val authRepository: AuthRepository,
    private val portfolioRepository: PortfolioRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockDetailUiState())
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    private var chat: SimpleChat? = null

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
                val msg = when {
                    error.message == "API_KEY_MISSING" ->
                        "API anahtarı tanımlı değil. local.properties dosyasına\nGEMINI_API_KEY=... ekleyin."
                    error.message?.startsWith("API_KEY_INVALID") == true ||
                    error.message?.startsWith("API_MODEL_NOT_FOUND") == true ->
                        "${error.message}\n\n💡 aistudio.google.com adresinden yeni key alın."
                    error.message?.startsWith("QUOTA_EXCEEDED") == true ->
                        "Günlük kota aşıldı. Birkaç dakika bekleyin."
                    error.message?.contains("Unable to resolve host") == true ||
                    error.message?.contains("timeout") == true ->
                        "İnternet bağlantısı kurulamadı."
                    else -> error.message ?: "Analiz şu an yapılamıyor."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                updateChatGreeting(symbol, isError = true)
            }
        }
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
     * Firestore Portföy Koleksiyonuna (users/{uid}/portfolio/{symbol}) hisseyi kaydeder.
     */
    fun addToPortfolio(lotSize: Double, averageCost: Double, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                onResult(false, "İşlem yapmak için giriş yapmalısınız.")
                return@launch
            }
            
            val item = com.muzaffer.bistai.domain.model.PortfolioItem(
                symbol = uiState.value.symbol,
                lotSize = lotSize,
                averageCost = averageCost
            )
            
            val result = portfolioRepository.addOrUpdatePortfolioItem(user.uid, item)
            if (result.isSuccess) {
                onResult(true, "Portföye başarıyla eklendi.")
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Kayıt sırasında hata oluştu.")
            }
        }
    }
}
