package com.jiyoujiaju.jijiahui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thingclips.smart.android.user.api.ILoginCallback
import com.thingclips.smart.android.user.api.ILogoutCallback
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.bizbundle.initializer.BizBundleInitializer
import com.thingclips.smart.home.sdk.ThingHomeSdk
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result

// ----- 用户数据模型 -----
// 注意：在生产环境中不要存储明文密码
data class UserModel(
    var mobile: String? = null,
    var password: String? = null,
    var nickname: String? = null,
    var name: String? = null,
    var sex: String? = null,
    var avatar: String? = null,
    var tuyaPwd: String? = null,
    var terminalId: String? = null,
    var accessToken: String? = null,
    var city: String? = null,
    var profile: String? = null
)

/**
 * FlutterUserPlugin
 *
 * 1) 与 Flutter 端同步用户数据：syncUser / clearUser
 * 2) 涂鸦登录通道：tuyaLogin / tuyaLogout
 */
class FlutterUserPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var appContext: Context
    private lateinit var userChannel: MethodChannel
    private lateinit var loginChannel: MethodChannel

    companion object {
        private const val TAG = "FlutterUserPlugin"
        const val USER_CHANNEL = "com.example.app/user"
        const val LOGIN_CHANNEL = "com.smartlife.app/login"
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        setupUserChannel(binding.binaryMessenger)
        setupLoginChannel(binding.binaryMessenger)
        Log.d(TAG, "Plugin attached to Flutter engine.")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        userChannel.setMethodCallHandler(null)
        loginChannel.setMethodCallHandler(null)
        UserManager.destroy()
        Log.d(TAG, "Plugin detached from Flutter engine.")
    }

    /** 初始化用户数据同步通道 */
    private fun setupUserChannel(messenger: BinaryMessenger) {
        userChannel = MethodChannel(messenger, USER_CHANNEL).apply {
            setMethodCallHandler(this@FlutterUserPlugin)
        }
        UserManager.initialize(appContext, userChannel)
    }

    /** 初始化涂鸦登录/登出通道 */
    private fun setupLoginChannel(messenger: BinaryMessenger) {
        loginChannel = MethodChannel(messenger, LOGIN_CHANNEL).apply {
            setMethodCallHandler { call, result ->
                when (call.method) {
                    "tuyaLogin" -> handleTuyaLogin(call, result)
                    "tuyaLogout" -> handleTuyaLogout(result)
                    else -> result.notImplemented()
                }
            }
        }
    }

    /** 处理用户数据同步方法 */
    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "syncUser" -> handleSyncUser(call, result)
            "clearUser" -> handleClearUser(result)
            else -> result.notImplemented()
        }
    }

    private fun handleSyncUser(call: MethodCall, result: Result) {
        val args = call.arguments as? Map<*, *> ?: run {
            result.error("INVALID_ARGS", "用户数据为空", null)
            return
        }
        val jsonStr = Gson().toJson(args)
        try {
            val user = Gson().fromJson(jsonStr, UserModel::class.java)
            UserManager.saveUser(user)
            result.success(true)
            Log.d(TAG, "User synchronized to native: $jsonStr")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user data", e)
            result.error("PARSE_ERROR", "解析用户数据失败: ${e.message}", null)
        }
    }

    private fun handleClearUser(result: Result) {
        UserManager.clearUser()
        result.success(true)
        Log.d(TAG, "User data cleared in native.")
    }

    /** 调用涂鸦 SDK 登录 */
    private fun handleTuyaLogin(call: MethodCall, result: Result) {
        val args = call.arguments as? Map<*, *> ?: run {
            result.error("INVALID_ARGS", "登录参数为空", null)
            return
        }
        val mobile = args["mobile"] as? String
        val password = args["password"] as? String
        if (mobile.isNullOrBlank() || password.isNullOrBlank()) {
            result.error("INVALID_ARGS", "手机号或密码为空", null)
            return
        }

        ThingHomeSdk.getUserInstance()
            .loginWithPhonePassword("86", mobile, password, object : ILoginCallback {
                override fun onSuccess(user: User) {
                    Log.d(TAG, "tuya--login success")

                    BizBundleInitializer.onLogin(); //涂鸦登录成功后，通知业务包
                    // 发送本地广播，Flutter 端可监听
                    appContext.sendBroadcast(Intent("loginStatus").apply {
                        `package` = appContext.packageName
                    })

                    result.success(true)
                }

                override fun onError(code: String, error: String) {
                    Log.e(TAG, "tuya--login failure: $code / $error")
                    Toast.makeText(appContext, "登录失败code: " + code + "error:" + error, Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    /** 调用涂鸦 SDK 登出 */
    private fun handleTuyaLogout(result: Result) {
        ThingHomeSdk.getUserInstance().logout(object : ILogoutCallback {
            override fun onSuccess() {
                Log.d(TAG, "tuya--logout success")

                BizBundleInitializer.onLogout(appContext) // 涂鸦登出成功后，通知业务包

                appContext.sendBroadcast(Intent("logoutStatus").apply {
                    `package` = appContext.packageName
                })

                result.success(true)
            }

            override fun onError(code: String, error: String) {
                Log.e(TAG, "tuya--logout failure: $code / $error")
                Toast.makeText(appContext, "登出失败: $error", Toast.LENGTH_SHORT).show()
                result.error("LOGOUT_FAILED", error, code)
            }
        })
    }
}

/**
 * UserManager 单例
 * 负责：
 *  - 本地保存用户到 SharedPreferences
 *  - 与 Flutter 端双向同步
 */
object UserManager {
    private const val PREFS_NAME = "UserPrefs_JijiaHuiApp"
    private const val USER_KEY = "CurrentUserModel"
    private const val TAG = "UserManager"

    private var sharedPreferences: SharedPreferences? = null
    private val gson = Gson()
    private var isNativelySyncingToFlutter = false
    private var channel: MethodChannel? = null

    var currentUser: UserModel? = null
        private set

    fun initialize(context: Context, methodChannel: MethodChannel) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            channel = methodChannel
            loadUser() // 初次加载并同步
            Log.d(TAG, "Initialized with prefs: $PREFS_NAME")
        }
    }

    fun saveUser(user: UserModel) {
        currentUser = user
        val jsonStr = gson.toJson(user)
        sharedPreferences?.edit()?.putString(USER_KEY, jsonStr)?.apply()
        Log.d(TAG, "Saved user: $jsonStr")
        if (!isNativelySyncingToFlutter) {
            syncUserToFlutter()
        }
    }

    fun clearUser() {
        val hadUser = currentUser != null
        currentUser = null
        sharedPreferences?.edit()?.remove(USER_KEY)?.apply()
        Log.d(TAG, "Cleared user from prefs")
        if (hadUser && !isNativelySyncingToFlutter) {
            syncUserToFlutter()
        }
    }

    private fun loadUser() {
        sharedPreferences?.getString(USER_KEY, null)?.let { jsonStr ->
            currentUser = try {
                gson.fromJson(jsonStr, UserModel::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse stored user", e)
                null
            }
            Log.d(TAG, "Loaded user: $jsonStr")
        }
        if (!isNativelySyncingToFlutter) {
            syncUserToFlutter()
        }
    }

    private fun syncUserToFlutter() {
        channel?.let { ch ->
            isNativelySyncingToFlutter = true
            if (currentUser != null) {
                val jsonStr = gson.toJson(currentUser)
                val type = object : TypeToken<Map<String, Any?>>() {}.type
                val map: Map<String, Any?> = gson.fromJson(jsonStr, type)
                ch.invokeMethod("userUpdated", map, callback)
            } else {
                ch.invokeMethod("userCleared", null, callback)
            }
        } ?: Log.w(TAG, "Channel is null, cannot sync to Flutter")
    }

    private val callback = object : Result {
        override fun success(result: Any?) { isNativelySyncingToFlutter = false }
        override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
            isNativelySyncingToFlutter = false
            Log.e(TAG, "Error syncing to Flutter: $errorCode / $errorMessage")
        }
        override fun notImplemented() {
            isNativelySyncingToFlutter = false
            Log.w(TAG, "Sync method not implemented in Flutter.")
        }
    }

    fun destroy() {
        channel = null
        Log.d(TAG, "UserManager destroyed.")
    }
}