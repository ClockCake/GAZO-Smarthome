package com.jiyoujiaju.jijiahui // <-- 确保这是你正确的包名

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
// import android.view.ViewGroup // 如果你之前在这里设置了 layoutParams，现在可以考虑是否需要
import androidx.annotation.RequiresApi
import io.flutter.plugin.platform.PlatformView

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class NativeView(
    private val context: Context,
    id: Int, // Flutter 传递过来的视图 ID
    creationParams: Map<String?, Any?>? // 从 Flutter 传递过来的参数
) : PlatformView {
    private val smartHomeView: SmartHomeView // 我们的自定义原生视图

    init {
        Log.d("NativeView", "NativeView 初始化开始，参数: $creationParams")

        // 创建 SmartHomeView 实例，并将从 Flutter 接收到的 creationParams 传递给它
        smartHomeView = SmartHomeView(context, creationParams)

        // 关于 LayoutParams:
        // 通常，当 SmartHomeView 作为 PlatformView 的根视图时，其大小由 Flutter 端 AndroidView widget 的约束决定。
        // 如果 SmartHomeView (例如，一个 FrameLayout) 自身没有填满其容器的默认行为，
        // 或者你需要特定的内部布局行为，你可以在 SmartHomeView 内部处理。
        // 在这里为 smartHomeView 设置 LayoutParams 可能不是必需的，或者效果可能与预期不同，
        // 因为 Flutter 的 AndroidView 会控制 PlatformView 的最终尺寸和位置。
        // smartHomeView.layoutParams = ViewGroup.LayoutParams(
        // ViewGroup.LayoutParams.MATCH_PARENT,
        // ViewGroup.LayoutParams.MATCH_PARENT
        // )

        Log.d("NativeView", "NativeView 初始化完成。")
    }

    // 返回实际的原生视图给 Flutter
    override fun getView(): View {
        return smartHomeView
    }

    // 当 Flutter 端的 PlatformView 被销毁时调用
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun dispose() {
        Log.d("NativeView", "NativeView.dispose() 被调用，开始清理 SmartHomeView。")
        smartHomeView.cleanup() // 调用我们 SmartHomeView 中的清理方法
        // 这里不需要做其他特别的清理，因为 smartHomeView 的清理逻辑应该在它自己的 cleanup 方法中。
    }
}