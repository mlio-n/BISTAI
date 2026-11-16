package com.muzaffer.bistai.presentation.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzaffer.bistai.data.local.prefs.ApiKeyManager
import com.muzaffer.bistai.data.remote.AiApiService
import com.muzaffer.bistai.data.remote.SimpleChat
import com.muzaffer.bistai.presentation.components.ChatMessage
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
    private val aiService: AiApiService,
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var chat: SimpleChat? = null

    init {
        observeApiKeyChanges()
    }

    private fun observeApiKeyChanges() {
        viewModelScope.launch {
            apiKeyManager.userApiKey.collect {
                restartChatSession()
            }
        }
    }

    private fun restartChatSession() {
        if (!aiService.isApiKeySet) {
            chat = null
            _uiState.update {
                it.copy(
                    messages = listOf(
                        ChatMessage(
                            text = "Gemini API anahtarı tanımlı değil. Menü sekmesinden kendi API anahtarınızı kaydedebilirsiniz.",
                            isFromUser = false
                        )
                    ),
                    isLoading = false
                )
            }
            return
        }
        try {
            chat = aiService.startGeneralChat()
            _uiState.update {
                it.copy(
                    messages = listOf(
                        ChatMessage(
                            text = "Sayın Yatırımcı, ben **BISTAI**. Borsa İstanbul (BİST) ve Türkiye piyasalarına dair hisse analizleri, sektör değerlendirmeleri ve risk özetleri için hazırım.",
                            isFromUser = false
                        )
                    ),
                    isLoading = false
                )
            }
        } catch (_: Exception) {}
    }

    fun sendMessage(message: String) {
        if (chat == null && aiService.isApiKeySet) {
            restartChatSession()
        }
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
                            messages = it.messages.filter { m -> !m.isLoading } +
                                    ChatMessage(text = reply, isFromUser = false),
                            isLoading = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            messages = it.messages.filter { m -> !m.isLoading } +
                                    ChatMessage(text = "Yanıt alınamadı. Lütfen tekrar dene.", isFromUser = false),
                            isLoading = false
                        )
                    }
                }
        }
    }
}
