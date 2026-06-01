package com.ant.tunes.ytmusic

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import java.security.MessageDigest

object YoutubeAuthManager {

    val isLoggedIn = MutableStateFlow(false)
    private var rawCookies: String = ""

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("yt_auth_prefs", Context.MODE_PRIVATE)
        rawCookies = prefs.getString("cookies", "") ?: ""
        isLoggedIn.value = hasValidSapisid(rawCookies)
    }

    fun saveCookies(context: Context, cookies: String) {
        rawCookies = cookies
        context.getSharedPreferences("yt_auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("cookies", cookies)
            .apply()
        isLoggedIn.value = hasValidSapisid(cookies)
    }

    fun logout(context: Context) {
        rawCookies = ""
        context.getSharedPreferences("yt_auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()

        // 🟢 AGGRESSIVE COOKIE & STORAGE WIPE
        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        android.webkit.WebStorage.getInstance().deleteAllData()

        isLoggedIn.value = false
    }

    fun getRawCookies(): String = rawCookies

    // 🟢 THE SECRET SAUCE: Generates YouTube's internal SAPISIDHASH auth header
    fun getAuthHeader(): String? {
        if (!isLoggedIn.value) return null

        val sapisid = extractSapisid(rawCookies) ?: return null
        val origin = "https://music.youtube.com"
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        val input = "$timestamp $sapisid $origin"
        val hash = sha1(input)

        return "SAPISIDHASH ${timestamp}_$hash"
    }

    private fun hasValidSapisid(cookies: String): Boolean {
        return cookies.contains("SAPISID=")
    }

    private fun extractSapisid(cookies: String): String? {
        val match = Regex("SAPISID=([^;]+)").find(cookies)
        return match?.groupValues?.get(1)
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
