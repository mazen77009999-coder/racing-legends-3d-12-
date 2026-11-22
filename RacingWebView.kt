package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.IOException

@Stable
class RacingWebController {
    private var webView: WebView? = null
    private var isEngineReady = false
    private val pendingScripts = mutableListOf<String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    internal fun attach(view: WebView) {
        webView = view
    }

    internal fun markReady() {
        mainHandler.post {
            isEngineReady = true
            val view = webView ?: return@post
            pendingScripts.forEach { script ->
                view.evaluateJavascript(script, null)
            }
            pendingScripts.clear()
        }
    }

    internal fun detach() {
        webView = null
        isEngineReady = false
        pendingScripts.clear()
    }

    fun evaluate(script: String) {
        mainHandler.post {
            val view = webView
            if (view != null && isEngineReady) {
                view.evaluateJavascript(script, null)
            } else {
                pendingScripts.add(script)
            }
        }
    }

    fun startRace(configJson: String) {
        val escaped = org.json.JSONObject.quote(configJson)
        evaluate("window.RacingEngine && window.RacingEngine.startRace($escaped);")
    }

    fun startGarage(configJson: String) {
        val escaped = org.json.JSONObject.quote(configJson)
        evaluate("window.RacingEngine && window.RacingEngine.startGarage($escaped);")
    }

    fun updateGarageCar(configJson: String) {
        val escaped = org.json.JSONObject.quote(configJson)
        evaluate("window.RacingEngine && window.RacingEngine.updateGarageCar($escaped);")
    }

    fun setInput(throttle: Float, brake: Float, steerLeft: Float, steerRight: Float) {
        evaluate("window.RacingEngine && window.RacingEngine.setInput($throttle, $brake, $steerLeft, $steerRight);")
    }

    fun cycleCamera() {
        evaluate("window.RacingEngine && window.RacingEngine.cycleCameraMode();")
    }

    fun setPaused(paused: Boolean) {
        evaluate("window.RacingEngine && window.RacingEngine.setPaused($paused);")
    }

    fun setQuality(quality: String) {
        evaluate("window.RacingEngine && window.RacingEngine.setQuality('$quality');")
    }

    fun setAudio(enabled: Boolean, volume: Float) {
        evaluate("window.RacingEngine && window.RacingEngine.setAudioSettings($enabled, $volume);")
    }
}

private class RacingJsBridge(
    private val onReady: () -> Unit,
    private val onTelemetry: (String) -> Unit,
    private val onEvent: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onTelemetry(json: String) {
        handler.post { onTelemetry.invoke(json) }
    }

    @JavascriptInterface
    fun onEvent(json: String) {
        handler.post {
            if (json.contains("engine_ready")) {
                onReady()
            }
            onEvent.invoke(json)
        }
    }
}

private class LocalAssetWebViewClient(
    private val context: Context,
    private val onPageLoaded: () -> Unit
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url ?: return null
        return serveLocalAsset(url)
    }

    private fun serveLocalAsset(uri: Uri): WebResourceResponse? {
        if (uri.host != ASSET_HOST) return null
        val path = uri.path?.removePrefix("/assets/")?.removePrefix("/") ?: return null
        val mimeType = when {
            path.endsWith(".html") -> "text/html"
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".json") -> "application/json"
            else -> "application/octet-stream"
        }
        return try {
            val stream = context.assets.open(path)
            WebResourceResponse(mimeType, "UTF-8", stream)
        } catch (e: IOException) {
            null
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageLoaded()
    }

    companion object {
        const val ASSET_HOST = "appassets.androidplatform.net"
        const val ENTRY_URL = "https://$ASSET_HOST/assets/index.html"
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RacingWebView(
    controller: RacingWebController,
    keepScreenOn: Boolean,
    modifier: Modifier = Modifier,
    onTelemetry: (String) -> Unit = {},
    onEvent: (String) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val wv = webViewHolder[0] ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    controller.setPaused(true)
                    wv.onPause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    wv.onResume()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.detach()
            webViewHolder[0]?.let { wv ->
                wv.stopLoading()
                wv.removeJavascriptInterface("RacingBridge")
                wv.destroy()
            }
            webViewHolder[0] = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#070A12"))
                this.keepScreenOn = keepScreenOn
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = false
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                val bridge = RacingJsBridge(
                    onReady = { controller.markReady() },
                    onTelemetry = onTelemetry,
                    onEvent = onEvent
                )
                addJavascriptInterface(bridge, "RacingBridge")

                webViewClient = LocalAssetWebViewClient(
                    context = context,
                    onPageLoaded = { controller.markReady() }
                )

                controller.attach(this)
                webViewHolder[0] = this
                loadUrl(LocalAssetWebViewClient.ENTRY_URL)
            }
        },
        update = { view ->
            view.keepScreenOn = keepScreenOn
        }
    )
}
