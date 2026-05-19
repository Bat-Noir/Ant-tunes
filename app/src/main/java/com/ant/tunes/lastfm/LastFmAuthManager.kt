package com.ant.tunes.lastfm

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LastFmAuthManager {

    private const val API_KEY = "e2427f83cfff636cb919ccdc4db1b4c1"

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username

    // Call on app start
    fun init(context: Context) {
        _isLoggedIn.value = LastFmRepository.isLoggedIn(context)
        _username.value   = LastFmRepository.getUsername(context)
    }

    // 🟢 Step 1 — Open browser WITH the deep link callback
    fun startAuth(context: Context) {
        try {
            val cb = "anttunes://lastfm" // MUST match AndroidManifest.xml
            val authUrl = "https://www.last.fm/api/auth/?api_key=${API_KEY}&cb=${cb}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Failed to open browser", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 🟢 Step 2 — Called by MainActivity passing the EXACT token from the URL
    suspend fun completeAuth(context: Context, token: String): Boolean {
        // We use the token passed directly from the Deep Link, no more SharedPreferences!
        val result = LastFmRepository.getSession(token) ?: return false
        val (username, sessionKey) = result

        LastFmRepository.saveSession(context, username, sessionKey)
        _isLoggedIn.value = true
        _username.value   = username
        return true
    }

    fun logout(context: Context) {
        LastFmRepository.clearSession(context)
        _isLoggedIn.value = false
        _username.value   = null
    }
}