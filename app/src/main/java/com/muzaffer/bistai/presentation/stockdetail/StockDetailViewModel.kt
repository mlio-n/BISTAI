package com.muzaffer.bistai.presentation.stockdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.Chat
import com.muzaffer.bistai.data.remote.AiApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockDetailUiState(
    val symbol: String        = "",
    val isLoading: Boolean    = false,
    val analysis: String?     = null,
    val errorMessage: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isChatLoading: Boolean = false
)

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val aiService: AiApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockDetailUiState())
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    /** Aktif Gemini sohbet oturumu (nullable — API anahtarı yoksa oluşturulmaz). */
    private var chat: Chat? = null

    init {
        val symbol = savedStateHandle.get<String>("symbol") ?: ""
        _uiState.update { it.copy(symbol = symbol) }
        fetchAnalysis(symbol)
        initChat(symbol)
    }

    /** Hisse için tek seferlik analiz üretir. */
    fun fetchAnalysis(symbol: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, analysis = null) }
            aiService.analyzeStock(symbol)
                .onSuccess { text ->
                    _uiState.update { it.copy(isLoading = false, analysis = text) }
                }
                .onFailure { error ->
                    val msg = when {
                        error.message == "API_KEY_MISSING"  -> "Gemini API anahtarı tanımlı değil."
                        error.message?.contains("network", ignoreCase = true) == true ->
                            "İnternet bağlantısı kurulamadı."
                        else -> "Analiz şu an yapılamıyor. Lütfen daha sonra tekrar deneyin."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
        }
    }

    /** Gemini sohbet oturumunu başlatır. API anahtarı yoksa sessizce atlar. */
    private fun initChat(symbol: String) {
        if (!aiService.isApiKeySet) return
        try {
            chat = aiService.startStockChat(symbol)
            // Gemini açılış mesajını chat geçmişine ekle
            _uiState.update {
                it.copy(
                    chatMessages = listOf(
                        ChatMessage(
                            text = "Merhaba! **$symbol** hakkında her sorunuzu yanıtlamaya hazırım. Ne merak ediyorsunuz?",
                            isFromUser = false
                        )
                    )
                )
            }
        } catch (e: Exception) {
            // Sohbet başlatılamazsa analiz modundan devam et
        }
    }

    /** Kullanıcı mesajını gönderir ve Gemini yanıtını alır. */
    fun sendChatMessage(message: String) {
        val currentChat = chat ?: return
        if (message.isBlank()) return

        val userMsg = ChatMessage(text = message, isFromUser = true)
        val loadingMsg = ChatMessage(text = "...", isFromUser = false, isLoading = true, id = -1L)

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg + loadingMsg,
                isChatLoading = true
            )
        }

        viewModelScope.launch {
            aiService.sendMessage(currentChat, message)
                .onSuccess { reply ->
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages
                                .filter { m -> !m.isLoading } + ChatMessage(text = reply, isFromUser = false),
                            isChatLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages
                                .filter { m -> !m.isLoading } + ChatMessage(
                                    text = "Yanıt alınamadı: ${error.message ?: "bilinmeyen hata"}",
                                    isFromUser = false
                                ),
                            isChatLoading = false
                        )
                    }
                }
        }
    }
}
