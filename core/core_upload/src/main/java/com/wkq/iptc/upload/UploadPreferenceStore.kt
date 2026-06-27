package com.wkq.iptc.upload

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tencent.mmkv.MMKV

internal object UploadPreferenceStore {

    private const val TAG = "UploadPreferenceStore"
    private const val PREF_NAME = "core_upload_state"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var legacyMmkv: MMKV? = null

    fun initialize(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        runCatching {
            MMKV.initialize(applicationContext)
            legacyMmkv = MMKV.defaultMMKV()
        }.onFailure {
            Log.w(TAG, "Legacy MMKV migration is unavailable: ${it.message.orEmpty()}")
        }
    }

    fun getString(key: String, defaultValue: String = ""): String {
        val scoped = scopedKey(key)
        val prefs = prefs()
        if (prefs.contains(scoped)) {
            return prefs.getString(scoped, defaultValue) ?: defaultValue
        }
        if (prefs.contains(key)) {
            val legacy = prefs.getString(key, defaultValue) ?: defaultValue
            prefs.edit().putString(scoped, legacy).apply()
            return legacy
        }
        val legacy = legacyMmkv?.decodeString(key, null)
        if (legacy != null) {
            prefs.edit().putString(scoped, legacy).apply()
            return legacy
        }
        return defaultValue
    }

    fun putString(key: String, value: String) {
        prefs().edit().putString(scopedKey(key), value).apply()
        legacyMmkv?.removeValueForKey(key)
    }

    fun remove(key: String) {
        prefs().edit().remove(scopedKey(key)).apply()
        legacyMmkv?.removeValueForKey(key)
    }

    private fun prefs(): SharedPreferences {
        val context = appContext
            ?: throw IllegalStateException("UploadPreferenceStore has not been initialized.")
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun scopedKey(key: String): String {
        val accountId = runCatching {
            val context = appContext ?: return@runCatching "local"
            val mmkv = legacyMmkv
            mmkv?.decodeString("google_user_id", "")?.ifBlank { "local" } ?: "local"
        }.getOrDefault("local")
        return "$accountId:$key"
    }
}
