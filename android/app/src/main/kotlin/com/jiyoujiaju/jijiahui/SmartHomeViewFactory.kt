
// ─── SmartHomeViewFactory.kt ──────────────────────────────────────────
package com.jiyoujiaju.jijiahui

import android.content.Context
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class SmartHomeViewFactory : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        // args 就是 creationParams（Map<String, dynamic>）
        @Suppress("UNCHECKED_CAST")
        val params = args as? Map<String, Any?>
        return SmartHomePlatformView(context, viewId, params)
    }
}