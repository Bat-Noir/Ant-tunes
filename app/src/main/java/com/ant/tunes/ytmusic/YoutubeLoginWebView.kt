package com.ant.tunes.ytmusic

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeLoginWebView(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    // 🟢 AGGRESSIVE CACHE WIPE ON START
                    clearCache(true)
                    clearHistory()

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.162 Mobile Safari/537.36"

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            val cookieManager = CookieManager.getInstance()
                            val cookies = cookieManager.getCookie("https://youtube.com") ?: ""

                            if (cookies.contains("SAPISID=")) {
                                YoutubeAuthManager.saveCookies(context, cookies)
                                onSuccess()
                            }
                        }
                    }

                    // 🟢 THE FIX: Force the AccountChooser so you can easily switch accounts
                    val loginUrl = "https://accounts.google.com/AccountChooser?service=youtube&continue=https://music.youtube.com/"
                    loadUrl(loginUrl)
                }
            }
        )
    }
}
