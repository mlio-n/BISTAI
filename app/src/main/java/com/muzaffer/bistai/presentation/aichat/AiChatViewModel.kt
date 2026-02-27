package com.muzaffer.bistai.presentation.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.Chat
import com.muzaffer.bistai.data.remote.AiApiService
import com.muzaffer.bistai.presentation.stockdetail.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiService: AiApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var chat: Chat? = null

    init { initChat() }

    private fun initChat() {
        if (!aiService.isApiKeySet) return
        try {
            // Genel borsa danışmanı bağlamıyla başlat
            val generalContext = com.google.ai.client.generativeai.type.content(role = "user") {
                text("""
                    Sen BISTAI adlı bir Türk finans ve borsa asistanısın.
                    - BIST (Borsa İstanbul) ve genel yatırım konularında uzmanısın.
                    - Türkçe, sade ve anlaşılır yanıtlar veriyorsun.
                    - Yatırım tavsiyesi yerine analiz ve bilgi sunuyorsun.
                    - Kısa ve odaklı cevaplar veriyorsun (max 200 kelime).
                    - Kullanıcıya samimi ve profesyonel bir danışman gibi davranıyorsun.
                """.trimIndent())
            }
            val history = listOf(
                generalContext,
                com.google.ai.client.generativeai.type.content(role = "model") {
                    text("Merhaba! Ben **BISTAI**, senin kişisel borsa danışmanın. BIST hisseleri, piyasa analizi veya yatırım stratejileri hakkında her soruyu yanıtlamaya hazırım. Ne öğrenmek istersin?")
                }
            )
            chat = aiService.startChat(history)
            _uiState.update {
                it.copy(
                    messages = listOf(
                        ChatMessage(
                            text = "Merhaba! Ben **BISTAI**, senin kişisel borsa danışmanın. BIST hisseleri, piyasa analizi veya yatırım stratejileri hakkında her soruyu yanıtlamaya hazırım. Ne öğrenmek istersin?",
                            isFromUser = false
                        )
                    )
                )
            }
        } catch (_: Exception) {}
    }

    fun sendMessage(message: String) {
        val currentChat = chat ?: return
        if (message.isBlank()) return

        val userMsg = ChatMessage(text = message, isFromUser = true)
        val loadingMsg = ChatMessage(text = "...", isFromUser = false, isLoading = true, id = -1L)

        _uiState.update {
            it.copy(messages = it.messages + userMsg + loadingMsg, isLoading = true)
        }

        viewModelScope.launch {
            aiService.sendMessage(currentChat, message)
                .onSuccess { reply ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages.filter { m -> !m.isLoading } + ChatMessage(text = reply, isFromUser = false),
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages.filter { m -> !m.isLoading } + ChatMessage(
                                text = "Yanıt alınamadı. Lütfen tekrar dene.",
                                isFromUser = false
                            ),
                            isLoading = false
                        )
                    }
                }
        }
    }
}

// AiApiService'e genel chat başlatma metodu (symbol parametresiz)
fun AiApiService.startChat(history: List<com.google.ai.client.generativeai.type.Content>): Chat {
    val model = com.google.ai.client.generativeai.GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = com.muzaffer.bistai.BuildConfig.GEMINI_API_KEY
    )
    return model.startChat(history = history)
}
