package com.example.util

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume

data class AppNotification(
    val hasNotification: Boolean = false,
    val title: String = "",
    val message: String = "",
    val appUrl: String = ""
)

object NetworkService {
    private const val TAG = "NetworkService"
    var PING_URL = "https://irizi.unaux.com/mo7adaraty-apk/api/ping.php"
    var NOTIFICATION_URL = "https://irizi.unaux.com/mo7adaraty-apk/api/get_notification.php"
    private const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private suspend fun getValidCookie(context: Context, targetUrl: String): String? {
        val existingCookie = try {
            CookieManager.getInstance().getCookie(targetUrl)
        } catch (e: Exception) {
            null
        }
        if (!existingCookie.isNullOrEmpty() && existingCookie.contains("__test")) {
            return existingCookie
        }
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val appContext = context.applicationContext
                    val webView = WebView(appContext)
                    webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    webView.settings.javaScriptEnabled = true
                    webView.settings.userAgentString = MOBILE_USER_AGENT
                    try {
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                    } catch (e: Exception) {
                        Log.e(TAG, "CookieManager setup warning: ${e.message}")
                    }
                    var isResumed = false
                    webView.webViewClient = object : WebViewClient() {
                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                            Log.e(TAG, "WebView render process gone in getValidCookie")
                            try { view?.destroy() } catch (_: Exception) {}
                            if (!isResumed && continuation.isActive) {
                                isResumed = true
                                continuation.resume("")
                            }
                            return true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val cookies = try { CookieManager.getInstance().getCookie(targetUrl) } catch (e: Exception) { null }
                            Log.d(TAG, "WebView onPageFinished cookie: $cookies")
                            if (!isResumed && !cookies.isNullOrEmpty() && cookies.contains("__test")) {
                                if (continuation.isActive) {
                                    isResumed = true
                                    continuation.resume(cookies)
                                }
                                view?.postDelayed({ try { webView.destroy() } catch (_: Exception) {} }, 500)
                            } else {
                                view?.postDelayed({
                                    if (!isResumed && continuation.isActive) {
                                        isResumed = true
                                        val recheckedCookie = try { CookieManager.getInstance().getCookie(targetUrl) } catch (e: Exception) { null }
                                        continuation.resume(recheckedCookie ?: "")
                                        try { webView.destroy() } catch (_: Exception) {}
                                    }
                                }, 7000)
                            }
                        }
                    }
                    webView.loadUrl(targetUrl)
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing WebView for bypass: ${e.message}")
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
            }
        }
    }

    suspend fun sendPing(context: Context, deviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cookies = getValidCookie(context, PING_URL)
            val url = URL(PING_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", MOBILE_USER_AGENT)
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            if (!cookies.isNullOrEmpty()) {
                conn.setRequestProperty("Cookie", cookies)
            }
            val postData = "device_id=" + URLEncoder.encode(deviceId, "UTF-8")
            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }
            val responseCode = conn.responseCode
            Log.d(TAG, "Ping sent. Response code: $responseCode")
            conn.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ping: ${e.message}")
            false
        }
    }

    suspend fun fetchNotification(context: Context): AppNotification = withContext(Dispatchers.IO) {
        try {
            val cookies = getValidCookie(context, NOTIFICATION_URL)
            val url = URL(NOTIFICATION_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("User-Agent", MOBILE_USER_AGENT)
            conn.setRequestProperty("Accept", "application/json, text/plain, */*")
            if (!cookies.isNullOrEmpty()) {
                conn.setRequestProperty("Cookie", cookies)
            }
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                conn.disconnect()
                val rawResponse = response.toString().trim()
                val firstBrace = rawResponse.indexOf('{')
                val lastBrace = rawResponse.lastIndexOf('}')
                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    val jsonStr = rawResponse.substring(firstBrace, lastBrace + 1)
                    val json = JSONObject(jsonStr)
                    val hasNotif = json.optBoolean("has_notification", false)
                    val title = json.optString("title", "")
                    val message = json.optString("message", "")
                    val appUrl = json.optString("app_url", "")
                    AppNotification(
                        hasNotification = hasNotif,
                        title = title,
                        message = message,
                        appUrl = appUrl
                    )
                } else {
                    Log.w(TAG, "Response is not valid JSON format: $rawResponse")
                    fetchViaWebView(context, NOTIFICATION_URL)
                }
            } else {
                conn.disconnect()
                fetchViaWebView(context, NOTIFICATION_URL)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch notification: ${e.message}")
            fetchViaWebView(context, NOTIFICATION_URL)
        }
    }

    private suspend fun fetchViaWebView(context: Context, url: String): AppNotification = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                val appContext = context.applicationContext
                val webView = WebView(appContext)
                webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                webView.settings.javaScriptEnabled = true
                webView.settings.userAgentString = MOBILE_USER_AGENT
                var isResumed = false
                webView.webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        Log.e(TAG, "WebView render process gone in fetchViaWebView")
                        try { view?.destroy() } catch (_: Exception) {}
                        if (!isResumed && continuation.isActive) {
                            isResumed = true
                            continuation.resume(AppNotification())
                        }
                        return true
                    }

                    override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                        super.onPageFinished(view, loadedUrl)
                        view?.evaluateJavascript("document.body.innerText") { bodyText ->
                            if (!isResumed && !bodyText.isNullOrEmpty()) {
                                try {
                                    val cleanedText = bodyText.replace("\\\"", "\"").trim('"', ' ', '\n', '\r')
                                    val firstBrace = cleanedText.indexOf('{')
                                    val lastBrace = cleanedText.lastIndexOf('}')
                                    if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                                        val jsonStr = cleanedText.substring(firstBrace, lastBrace + 1)
                                        val json = JSONObject(jsonStr)
                                        val notif = AppNotification(
                                            hasNotification = json.optBoolean("has_notification", false),
                                            title = json.optString("title", ""),
                                            message = json.optString("message", ""),
                                            appUrl = json.optString("app_url", "")
                                        )
                                        if (continuation.isActive) {
                                            isResumed = true
                                            continuation.resume(notif)
                                        }
                                        try { webView.destroy() } catch (_: Exception) {}
                                        return@evaluateJavascript
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing WebView body: ${e.message}")
                                }
                            }
                            view?.postDelayed({
                                if (!isResumed && continuation.isActive) {
                                    isResumed = true
                                    continuation.resume(AppNotification())
                                    try { webView.destroy() } catch (_: Exception) {}
                                }
                            }, 7000)
                        }
                    }
                }
                webView.loadUrl(url)
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(AppNotification())
                }
            }
        }
    }
}