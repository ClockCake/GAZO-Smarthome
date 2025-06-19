package com.jiyoujiaju.jijiahui

import com.jiyoujiaju.jijiahui.SmartHomeView
import android.content.Context
import android.view.View
import io.flutter.plugin.platform.PlatformView

// PlatformView 适配器：把你的 FrameLayout 包装为 PlatformView
class SmartHomePlatformView(
    context: Context,
    viewId: Int,
    creationParams: Map<String, Any?>?
) : PlatformView {
    private val smartHomeView = SmartHomeView(
        context,
        /*attrs=*/ null,
        /*defStyleAttr=*/ 0,
        creationParams
    )
    override fun getView(): View = smartHomeView

    override fun dispose() {
        // Flutter 端调用 dispose 时销毁资源
        smartHomeView.cleanup()
    }
}