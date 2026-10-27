package com.knektd.arktv

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * ARK Dashboard — a full-screen kiosk WebView for the workshop TV.
 *
 * It loads the PUBLIC dashboard (`/dashboard`, gated server-side by the TV's IP — no login),
 * keeps the screen on, hides all system UI, and auto-recovers from network blips. Declared as
 * a HOME launcher in the manifest so the TV boots straight into it.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private val ui = Handler(Looper.getMainLooper())
    private var retryQueued = false

    // Reload once a day to keep a long-lived WebView healthy (memory, stale assets).
    private val dailyReload = object : Runnable {
        override fun run() {
            web.reload()
            ui.postDelayed(this, RELOAD_INTERVAL_MS)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        web = WebView(this).apply {
            setBackgroundColor(0xFF0E0E0E.toInt()) // ARK dark — no white flash before load
            with(settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                builtInZoomControls = false
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                // Keep every navigation inside this WebView — it's a kiosk, nothing escapes.
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) queueRetry()
                }
            }
        }
        setContentView(web)
        hideSystemUi()
        web.loadUrl(DASHBOARD_URL)

        // Retry the load as soon as the network comes back (TV booted before Wi-Fi, router reboot…).
        (getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager)
            .registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    ui.post { if (retryQueued) { retryQueued = false; web.loadUrl(DASHBOARD_URL) } }
                }
            })

        ui.postDelayed(dailyReload, RELOAD_INTERVAL_MS)
    }

    private fun queueRetry() {
        retryQueued = true
        ui.postDelayed({ if (retryQueued) { retryQueued = false; web.loadUrl(DASHBOARD_URL) } }, RETRY_MS)
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    // Swallow BACK so the kiosk can't be navigated out of; let the dashboard go back if it can.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack()
    }

    // A remote's center/OK could be used later to force a manual refresh; harmless otherwise.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { web.reload(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        web.destroy()
        super.onDestroy()
    }

    companion object {
        private const val DASHBOARD_URL = "https://ark.knektd.com/dashboard"
        private const val RETRY_MS = 8_000L
        private const val RELOAD_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
