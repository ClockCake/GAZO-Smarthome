package com.jiyoujiaju.jijiahui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

// ----- 用户数据模型 -----
data class UserModel(
    var mobile: String? = null,
    var password: String? = null, // 安全提示：不建议在客户端存储明文密码
    var nickname: String? = null,
    var name: String? = null,
    var sex: String? = null,
    var avatar: String? = null,
    var tuyaPwd: String? = null, // 安全提示：不建议在客户端存储明文密码
    var terminalId: String? = null,
    var accessToken: String? = null,
    var city: String? = null,
    var profile: String? = null
)

// ----- Flutter 插件实现 -----
class FlutterUserPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var appContext: Context

    companion object {
        // 确保这个通道名称与 Flutter 端 UserManager.dart 中定义的完全一致
        const val CHANNEL_NAME = "com.example.app/user"
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        try {
            appContext = binding.applicationContext
            channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
            channel.setMethodCallHandler(this)

            // 初始化 UserManager 并传入 MethodChannel 实例
            UserManager.initialize(appContext, channel)
            Log.d("FlutterUserPlugin", "插件已成功附加到 Flutter 引擎。")
        } catch (e: Exception) {
            Log.e("FlutterUserPlugin", "附加到 Flutter 引擎时出错。", e)
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        try {
            Log.d("FlutterUserPlugin", "原生收到方法调用: ${call.method}")
            Log.d("FlutterUserPlugin", "参数: ${call.arguments}")

            when (call.method) {
                "syncUser" -> {
                    val args = call.arguments as? Map<*, *> // Flutter端 _user?.toJson() 应该会是 Map<String, Any?>
                    if (args != null) {
                        val gson = Gson()
                        val jsonStr = gson.toJson(args) // 将Map转为JSON字符串
                        Log.d("FlutterUserPlugin", "正在同步用户数据 (JSON): $jsonStr")

                        try {
                            val user = gson.fromJson(jsonStr, UserModel::class.java) // 将JSON字符串转为UserModel对象
                            UserManager.saveUser(user) // 保存用户（这会触发 syncToFlutter 如果 UserManager.isSyncing 为 false）
                            result.success(true) // 通知Flutter成功
                            Log.d("FlutterUserPlugin", "用户数据同步成功。")
                        } catch (e: Exception) {
                            Log.e("FlutterUserPlugin", "解析用户数据时出错。", e)
                            result.error("PARSE_ERROR", "从Flutter解析用户数据失败: ${e.message}", e.stackTraceToString())
                        }
                    } else {
                        Log.e("FlutterUserPlugin", "syncUser 收到的参数为空。")
                        result.error("INVALID_ARGS", "用户数据为空。", null)
                    }
                }
                "clearUser" -> {
                    UserManager.clearUser() // 清除用户（这会触发 syncToFlutter 如果 UserManager.isSyncing 为 false）
                    result.success(true) // 通知Flutter成功
                    Log.d("FlutterUserPlugin", "用户数据已成功清除。")
                }
                "tuyaLogin" -> { // 处理来自Flutter的涂鸦登录请求
                    val args = call.arguments as? Map<*, *>
                    if (args != null) {
                        val mobile = args["mobile"] as? String
                        val password = args["password"] as? String // 再次提醒注意密码安全
                        if (mobile != null && password != null) {
                            Log.d("FlutterUserPlugin", "尝试涂鸦登录: mobile=$mobile")
                            // TODO: 在此处集成你的涂鸦 SDK 登录逻辑
                            // 示例: TuyaHomeSdk.getUserInstance().loginWithPhonePassword("国家码", mobile, password, callback)
                            // 根据涂鸦SDK的实际调用结果来调用 result.success() 或 result.error()
                            result.success("涂鸦登录请求已在原生端收到（待实现）。") // 假设成功，实际应基于SDK回调
                        } else {
                            Log.e("FlutterUserPlugin", "涂鸦登录缺少手机号或密码。")
                            result.error("INVALID_ARGS", "涂鸦登录缺少手机号或密码。", null)
                        }
                    } else {
                        Log.e("FlutterUserPlugin", "涂鸦登录收到的参数为空。")
                        result.error("INVALID_ARGS", "涂鸦登录参数为空。", null)
                    }
                }
                else -> {
                    Log.w("FlutterUserPlugin", "未实现的方法: ${call.method}")
                    result.notImplemented()
                }
            }
        } catch (e: Exception) {
            Log.e("FlutterUserPlugin", "处理方法调用时发生意外错误。", e)
            result.error("UNEXPECTED_ERROR", "原生端发生未预期的错误: ${e.message}", e.stackTraceToString())
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        UserManager.destroy() // 添加一个销毁方法以清除对channel的引用（可选但推荐）
        Log.d("FlutterUserPlugin", "插件已从 Flutter 引擎分离。")
    }
}

// ----- 用户管理单例对象 -----
object UserManager {
    private const val PREFS_NAME = "UserPrefs_JijiaHuiApp" // SharedPreferences文件名，可以自定义
    private const val USER_KEY = "CurrentUserModel" // 存储用户对象的键
    private var sharedPreferences: SharedPreferences? = null
    private val gson = Gson()
    var currentUser: UserModel? = null
        private set // 只允许 UserManager 内部修改

    // 用于防止同步循环的标志位 (主要用于原生主动更新时通知Flutter)
    private var isNativelySyncingToFlutter: Boolean = false
    private var flutterMethodChannel: MethodChannel? = null // 存储从插件传递过来的 MethodChannel

    fun initialize(context: Context, channel: MethodChannel) {
        if (sharedPreferences == null) { // 防止重复初始化
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            this.flutterMethodChannel = channel
            loadUser() // 初始化时加载本地用户数据
            Log.d("UserManager", "UserManager 已初始化。")
        }
    }

    fun saveUser(user: UserModel) {
        currentUser = user
        val jsonStr = gson.toJson(user)
        sharedPreferences?.edit()?.putString(USER_KEY, jsonStr)?.apply()
        Log.d("UserManager", "用户已保存到 SharedPreferences: $jsonStr")

        // 如果这个保存操作不是由 syncToFlutter 自身触发的（即不是Flutter回传的数据），
        // 并且数据确实发生了变化，则通知Flutter。
        // Flutter端UserManager.dart中的_isSyncing标志会防止Flutter收到此更新后再次调用原生syncUser，从而避免循环。
        if (!isNativelySyncingToFlutter) {
            syncUserToFlutter()
        }
    }

    fun loadUser() {
        val jsonStr = sharedPreferences?.getString(USER_KEY, null)
        if (jsonStr != null) {
            try {
                currentUser = gson.fromJson(jsonStr, UserModel::class.java)
                Log.d("UserManager", "用户已从 SharedPreferences 加载: $jsonStr")
            } catch (e: Exception) {
                Log.e("UserManager", "从 SharedPreferences 加载用户数据时解析失败。", e)
                currentUser = null
            }
        } else {
            Log.d("UserManager", "SharedPreferences 中没有找到用户数据。")
            currentUser = null
        }
        // 初始加载后，也同步一次状态到Flutter，确保Flutter侧状态一致
        // Flutter端UserManager.dart中的_isSyncing标志会防止Flutter收到此更新后再次调用原生syncUser。
        if (!isNativelySyncingToFlutter) {
            syncUserToFlutter()
        }
    }

    @Suppress("unused") // 如果有其他原生模块需要调用更新，此方法有用
    fun updateUserByNative(updateFn: (UserModel?) -> UserModel?) {
        val updatedUser = updateFn(currentUser?.copy()) // 使用copy确保操作的是副本
        if (updatedUser != null) {
            saveUser(updatedUser)
        } else if (currentUser != null) { // 如果updateFn返回null，表示可能要清除用户
            clearUser()
        }
    }

    fun clearUser() {
        val userWasPresent = currentUser != null
        currentUser = null
        sharedPreferences?.edit()?.remove(USER_KEY)?.apply()
        Log.d("UserManager", "用户数据已从 SharedPreferences 清除。")

        // 只有当之前确实有用户数据时，才通知Flutter清除
        if (userWasPresent && !isNativelySyncingToFlutter) {
            syncUserToFlutter() // 这会调用 userCleared
        }
    }

    // 将当前用户状态同步到 Flutter 端
    private fun syncUserToFlutter() {
        val channel = flutterMethodChannel ?: run {
            Log.w("UserManager", "无法同步到Flutter：MethodChannel 未初始化。")
            return
        }

        isNativelySyncingToFlutter = true // 设置标志，表示是原生主动向Flutter同步
        Log.d("UserManager", "准备将用户状态同步到 Flutter。当前用户: ${if (currentUser != null) gson.toJson(currentUser) else "null"}")

        if (currentUser != null) {
            val jsonStr = gson.toJson(currentUser)
            val type = object : TypeToken<Map<String, Any?>>() {}.type // 确保Map的值类型可以是null
            val userMap: Map<String, Any?> = gson.fromJson(jsonStr, type)

            channel.invokeMethod("userUpdated", userMap, object : Result {
                override fun success(result: Any?) {
                    Log.d("UserManager", "成功将 'userUpdated' 事件发送到 Flutter。")
                    isNativelySyncingToFlutter = false
                }
                override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                    Log.e("UserManager", "发送 'userUpdated' 事件到 Flutter 失败: $errorCode - $errorMessage")
                    isNativelySyncingToFlutter = false
                }
                override fun notImplemented() {
                    Log.w("UserManager", "'userUpdated' 方法在 Flutter 端未实现。")
                    isNativelySyncingToFlutter = false
                }
            })
        } else {
            channel.invokeMethod("userCleared", null, object : Result {
                override fun success(result: Any?) {
                    Log.d("UserManager", "成功将 'userCleared' 事件发送到 Flutter。")
                    isNativelySyncingToFlutter = false
                }
                override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                    Log.e("UserManager", "发送 'userCleared' 事件到 Flutter 失败: $errorCode - $errorMessage")
                    isNativelySyncingToFlutter = false
                }
                override fun notImplemented() {
                    Log.w("UserManager", "'userCleared' 方法在 Flutter 端未实现。")
                    isNativelySyncingToFlutter = false
                }
            })
        }
    }

    // 当插件分离时，清除对 MethodChannel 的引用
    fun destroy() {
        flutterMethodChannel = null
        Log.d("UserManager", "UserManager 已销毁 (清除了 MethodChannel 引用)。")
    }
}