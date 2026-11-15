package com.muzaffer.bistai.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.muzaffer.bistai.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BYOK (Bring Your Own Key) Gemini API anahtarını yerel cihazda
 * EncryptedSharedPreferences ile şifreli olarak saklar.
 */
@Singleton
class ApiKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "bistai_secure_prefs"
        private const val FALLBACK_PREFS_NAME = "bistai_secure_prefs_fallback"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val _userApiKey = MutableStateFlow(getUserApiKey())
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    fun getUserApiKey(): String =
        prefs.getString(KEY_GEMINI_API_KEY, "")?.trim().orEmpty()

    fun getEffectiveApiKey(): String =
        getUserApiKey().ifBlank { BuildConfig.GEMINI_API_KEY }

    fun saveApiKey(apiKey: String) {
        val cleaned = apiKey.trim()
        prefs.edit().putString(KEY_GEMINI_API_KEY, cleaned).apply()
        _userApiKey.value = cleaned
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
        _userApiKey.value = ""
    }
}
