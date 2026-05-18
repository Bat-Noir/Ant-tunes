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

    // Step 1 — open Last.fm auth in browser
    suspend fun startAuth(context: Context): String? {
        val token = LastFmRepository.getToken() ?: return null
        // save token for later
        context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
            .edit().putString("lastfm_token", token).apply()

        val authUrl = "https://www.last.fm/api/auth/?api_key=${API_KEY}&token=${token}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return token
    }

    // Step 2 — called after user returns from browser
    suspend fun completeAuth(context: Context): Boolean {
        val token = context.getSharedPreferences("ant_prefs", Context.MODE_PRIVATE)
            .getString("lastfm_token", null) ?: return false

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