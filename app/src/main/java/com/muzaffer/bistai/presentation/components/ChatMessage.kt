package com.muzaffer.bistai.presentation.components

/**
 * Bir chat mesajını temsil eden model.
 */
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isFromUser: Boolean,
    val isLoading: Boolean = false   // Gemini yazıyor... balonu için
)
