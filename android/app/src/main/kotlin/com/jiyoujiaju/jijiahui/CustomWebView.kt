package com.jiyoujiaju.jijiahui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton

class CustomWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs) {

    private val TAG = "CustomWebView"
    private lateinit var overlayButton: ImageButton
    private val marginDp = 16

    // 1. 回调 Lambda，外部可注册
    private var overlayButtonClickListener: (() -> Unit)? = null

    /** 外部调用此方法来注册按钮点击回调 */
    fun setOnOverlayButtonClickListener(listener: () -> Unit) {
        overlayButtonClickListener = listener
    }

    init {
        settings.javaScriptEnabled = true
        settings.setSupportMultipleWindows(true)

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ) = handleUrl(request.url.toString())

            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView, url: String) = handleUrl(url)

            private fun handleUrl(url: String): Boolean {
                return when {
                    url.startsWith("http://") || url.startsWith("https://") -> false
                    url.startsWith("baiduboxapp://") -> {
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.w(TAG, "无法处理自定义 Scheme: $url", e)
                        }
                        true
                    }
                    else -> {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: Exception) {
                            Log.w(TAG, "找不到处理此 URL 的应用: $url", e)
                        }
                        true
                    }
                }
            }
        }

        // 在第一次布局完成后再添加按钮
        post { addOverlayButton() }
    }

    private fun addOverlayButton() {
        overlayButton = ImageButton(context).apply {
            setImageResource(R.drawable.add)
            background = null
            setOnClickListener {
                Log.d(TAG, "Overlay button clicked!")
                // 2. 点击时触发注册的回调（如果有）
                overlayButtonClickListener?.invoke()
            }
        }

        // 测量并添加
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        overlayButton.measure(spec, spec)
        val params = ViewGroup.LayoutParams(
            overlayButton.measuredWidth,
            overlayButton.measuredHeight
        )
        addView(overlayButton, params)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (::overlayButton.isInitialized) {
            val margin = (marginDp * resources.displayMetrics.density).toInt()
            val extraOffset = (12 * resources.displayMetrics.density).toInt() // 额外下移12dp
            val bw = overlayButton.measuredWidth
            val bh = overlayButton.measuredHeight
            val parentWidth = right - left
            overlayButton.layout(
                parentWidth - margin - bw,
                margin + extraOffset,
                parentWidth - margin,
                margin + extraOffset + bh
            )
        }
    }
    fun loadHomePage() {
        loadUrl("https://vr.justeasy.cn/view/xz165se6x8k14880-1657179172.html")
    }

    override fun onDetachedFromWindow() {
        removeAllViews()
        destroy()
        super.onDetachedFromWindow()
    }
}