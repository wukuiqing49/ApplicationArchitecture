package com.wkq.router.api

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * 路由跳转的载体，存储路径、参数、动画等
 */
class Postcard(val path: String) {

    private var extras: Bundle = Bundle()
    private var enterAnim: Int = 0
    private var exitAnim: Int = 0
    private var flags: Int = -1

    fun withBundle(bundle: Bundle): Postcard {
        this.extras = bundle
        return this
    }

    fun withString(key: String, value: String?): Postcard {
        extras.putString(key, value)
        return this
    }

    fun withInt(key: String, value: Int): Postcard {
        extras.putInt(key, value)
        return this
    }

    fun withLong(key: String, value: Long): Postcard {
        extras.putLong(key, value)
        return this
    }

    fun withBoolean(key: String, value: Boolean): Postcard {
        extras.putBoolean(key, value)
        return this
    }

    fun withFloat(key: String, value: Float): Postcard {
        extras.putFloat(key, value)
        return this
    }

    fun withDouble(key: String, value: Double): Postcard {
        extras.putDouble(key, value)
        return this
    }

    fun withSerializable(key: String, value: java.io.Serializable?): Postcard {
        extras.putSerializable(key, value)
        return this
    }

    fun withParcelable(key: String, value: android.os.Parcelable?): Postcard {
        extras.putParcelable(key, value)
        return this
    }

    fun withIntArray(key: String, value: IntArray?): Postcard {
        extras.putIntArray(key, value)
        return this
    }

    fun withLongArray(key: String, value: LongArray?): Postcard {
        extras.putLongArray(key, value)
        return this
    }

    fun withFlags(flags: Int): Postcard {
        this.flags = flags
        return this
    }

    fun withTransition(enterAnim: Int, exitAnim: Int): Postcard {
        this.enterAnim = enterAnim
        this.exitAnim = exitAnim
        return this
    }

    /**
     * 普通跳转
     */
    fun navigation(context: Context) {
        Router.navigate(context, this)
    }

    /**
     * 带 Result 回调的跳转
     */
    fun navigation(activity: FragmentActivity, callback: (ActivityResult) -> Unit) {
        Router.navigateWithResult(activity, this, callback)
    }

    // --- 内部使用的 Getter ---
    fun getExtras() = extras
    fun getEnterAnim() = enterAnim
    fun getExitAnim() = exitAnim
    fun getFlags() = flags
}

/**
 * 内部使用的代理 Fragment，用于承接 ActivityResult
 */
class RouterResultProxyFragment : Fragment() {
    private var callback: ((ActivityResult) -> Unit)? = null
    private var intent: Intent? = null

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        callback?.invoke(result)
        // 完成任务后自毁
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }

    fun setParams(intent: Intent, callback: (ActivityResult) -> Unit) {
        this.intent = intent
        this.callback = callback
    }

    override fun onResume() {
        super.onResume()
        intent?.let {
            launcher.launch(it)
            intent = null // 确保只启动一次
        }
    }
}
