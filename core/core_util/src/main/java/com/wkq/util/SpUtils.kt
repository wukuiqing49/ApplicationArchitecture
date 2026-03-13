package com.wkq.util

import android.content.Context
import android.os.Parcelable
import com.tencent.mmkv.MMKV

/**
 * MMKV 本地存储工具类
 * 注意事项：在使用本工具类前，请务必在 Application 的 onCreate() 中调用初始化方法：
 * SpUtils.init(this)
 */
object SpUtils {

    private var mmkv: MMKV? = null

    /**
     * 初始化 MMKV (必须在 Application 启动时调用)
     */
    fun init(context: Context) {
        val rootDir = MMKV.initialize(context)
        mmkv = MMKV.defaultMMKV()
    }

    /**
     * 获取 MMKV 实例
     */
    fun getMMKV(): MMKV {
        return mmkv ?: throw IllegalStateException("SpUtils has not been initialized. Please call SpUtils.init(context) in your Application.")
    }

    /**
     * 保存数据
     * 支持的类型：String, Int, Boolean, Float, Long, Double, ByteArray, Set<String>, Parcelable
     */
    fun put(key: String, value: Any?) {
        val kv = getMMKV()
        if (value == null) {
            kv.removeValueForKey(key)
            return
        }
        when (value) {
            is String -> kv.encode(key, value)
            is Int -> kv.encode(key, value)
            is Boolean -> kv.encode(key, value)
            is Float -> kv.encode(key, value)
            is Long -> kv.encode(key, value)
            is Double -> kv.encode(key, value)
            is ByteArray -> kv.encode(key, value)
            is Parcelable -> kv.encode(key, value)
            is Set<*> -> {
                try {
                    @Suppress("UNCHECKED_CAST")
                    kv.encode(key, value as Set<String>)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            else -> throw IllegalArgumentException("Unsupported type for MMKV: ${value.javaClass.simpleName}")
        }
    }

    // --- 获取各种类型数据的方法 ---

    fun getString(key: String, defaultValue: String = ""): String {
        return getMMKV().decodeString(key, defaultValue) ?: defaultValue
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return getMMKV().decodeInt(key, defaultValue)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return getMMKV().decodeBool(key, defaultValue)
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return getMMKV().decodeFloat(key, defaultValue)
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return getMMKV().decodeLong(key, defaultValue)
    }

    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        return getMMKV().decodeDouble(key, defaultValue)
    }

    fun getByteArray(key: String, defaultValue: ByteArray = ByteArray(0)): ByteArray {
        return getMMKV().decodeBytes(key, defaultValue) ?: defaultValue
    }

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return getMMKV().decodeStringSet(key, defaultValue) ?: defaultValue
    }

    inline fun <reified T : Parcelable> getParcelable(key: String, defaultValue: T? = null): T? {
        return getMMKV().decodeParcelable(key, T::class.java, defaultValue)
    }

    // --- 其他辅助方法 ---

    /**
     * 移除某个 key 的数据
     */
    fun remove(key: String) {
        getMMKV().removeValueForKey(key)
    }

    /**
     * 清理所有数据
     */
    fun clearAll() {
        getMMKV().clearAll()
    }

    /**
     * 检查是否包含某个 key
     */
    fun contains(key: String): Boolean {
        return getMMKV().containsKey(key)
    }
}
