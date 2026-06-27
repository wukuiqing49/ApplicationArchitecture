package com.wkq.iptc.upload

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import android.util.Log

object UploadConfigStore {

    private const val TAG = "UploadConfigStore"
    private const val KEY_PROFILE_JSON = "press_iptc_upload_profile_json"
    private const val KEY_PROFILES_JSON = "press_iptc_upload_profiles_json"
    private const val KEY_SELECTED_PROFILE_ID = "press_iptc_upload_selected_profile_id"

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            UploadServerConfig::class.java,
            JsonSerializer<UploadServerConfig> { src, _, context ->
                val json = context.serialize(src).asJsonObject
                val type = when (src) {
                    is FtpConfig -> UploadProtocolType.FTP.name
                    is FtpsConfig -> UploadProtocolType.FTPS.name
                    is SftpConfig -> UploadProtocolType.SFTP.name
                    is HttpConfig -> UploadProtocolType.HTTP.name
                    is SmbConfig -> UploadProtocolType.SMB.name
                    is WebDavConfig -> UploadProtocolType.WEBDAV.name
                }
                json.addProperty("configType", type)
                json
            }
        )
        .registerTypeAdapter(
            UploadServerConfig::class.java,
            JsonDeserializer<UploadServerConfig> { json, _, context ->
                val jsonObject = json.asJsonObject
                when (UploadProtocolType.fromValue(jsonObject.get("configType")?.asString)) {
                    UploadProtocolType.FTP -> context.deserialize(json, FtpConfig::class.java)
                    UploadProtocolType.FTPS -> context.deserialize(json, FtpsConfig::class.java)
                    UploadProtocolType.SFTP -> context.deserialize(json, SftpConfig::class.java)
                    UploadProtocolType.HTTP -> context.deserialize(json, HttpConfig::class.java)
                    UploadProtocolType.SMB -> context.deserialize(json, SmbConfig::class.java)
                    UploadProtocolType.WEBDAV -> context.deserialize(json, WebDavConfig::class.java)
                }
            }
        )
        .create()

    fun load(): UploadServerProfile {
        val selected = getSelectedProfile()
        if (selected != null) {
            return selected
        }
        val json = UploadPreferenceStore.getString(KEY_PROFILE_JSON)
        if (json.isBlank()) {
            return UploadServerProfile()
        }
        return runCatching {
            gson.fromJson(json, UploadServerProfile::class.java) ?: UploadServerProfile()
        }.onFailure {
            Log.e(TAG, "Failed to load upload config: ${it.message.orEmpty()}")
        }.getOrDefault(UploadServerProfile())
            .withStoredPassword()
    }

    fun save(profile: UploadServerProfile) {
        Log.i(TAG, "save legacy/default profile: ${profile.toLogText()}")
        saveProfile(profile, selectAfterSave = true)
        UploadPreferenceStore.putString(KEY_PROFILE_JSON, gson.toJson(profile.withoutPassword()))
    }

    fun getProfiles(): List<UploadServerProfile> {
        val raw = UploadPreferenceStore.getString(KEY_PROFILES_JSON)
        val profiles = if (raw.isBlank()) {
            emptyList()
        } else {
            runCatching {
                gson.fromJson<List<UploadServerProfile>>(
                    raw,
                    object : TypeToken<List<UploadServerProfile>>() {}.type
                ) ?: emptyList()
            }.onFailure {
                Log.e(TAG, "Failed to load upload profiles: ${it.message.orEmpty()}")
            }.getOrDefault(emptyList())
        }
        if (profiles.isNotEmpty()) {
            return profiles.map { it.withStoredPassword() }
        }
        val legacy = loadLegacyProfile()
        return if (legacy.config.host.isNotBlank() || legacy.name.isNotBlank()) {
            listOf(legacy.withStoredPassword())
        } else {
            emptyList()
        }
    }

    fun getProfile(profileId: String): UploadServerProfile? {
        return getProfiles().firstOrNull { it.id == profileId }.also {
            Log.i(TAG, "getProfile id=$profileId, found=${it?.toLogText() ?: "null"}")
        }
    }

    fun getSelectedProfile(): UploadServerProfile? {
        val profiles = getProfilesWithoutLegacyFallback()
        if (profiles.isEmpty()) {
            return null
        }
        val selectedId = UploadPreferenceStore.getString(KEY_SELECTED_PROFILE_ID)
        return profiles.firstOrNull { it.id == selectedId } ?: profiles.firstOrNull()
    }

    fun saveProfile(profile: UploadServerProfile, selectAfterSave: Boolean = true) {
        val profiles = getProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            profiles[index] = profile
        } else {
            profiles += profile
        }
        UploadSecretStore.savePassword(profile.id, profile.config.passwordValue())
        saveProfiles(profiles)
        Log.i(
            TAG,
            "saveProfile selectAfterSave=$selectAfterSave, action=${if (index >= 0) "update" else "add"}, " +
                "profile=${profile.toLogText()}, total=${profiles.size}"
        )
        if (selectAfterSave) {
            selectProfile(profile.id)
        }
    }

    fun selectProfile(profileId: String) {
        UploadPreferenceStore.putString(KEY_SELECTED_PROFILE_ID, profileId)
        getProfile(profileId)?.let {
            UploadPreferenceStore.putString(KEY_PROFILE_JSON, gson.toJson(it.withoutPassword()))
            Log.i(TAG, "selectProfile id=$profileId, selected=${it.toLogText()}")
        } ?: run {
            Log.w(TAG, "selectProfile id=$profileId, but profile not found")
        }
    }

    fun deleteProfiles(profileIds: Collection<String>) {
        if (profileIds.isEmpty()) return
        val profiles = getProfiles().filterNot { it.id in profileIds }
        profileIds.forEach { UploadSecretStore.clearPassword(it) }
        saveProfiles(profiles)
        Log.i(TAG, "deleteProfiles ids=${profileIds.joinToString()}, remaining=${profiles.size}")
        val selectedId = UploadPreferenceStore.getString(KEY_SELECTED_PROFILE_ID)
        if (selectedId in profileIds) {
            profiles.firstOrNull()?.let { selectProfile(it.id) }
                ?: UploadPreferenceStore.remove(KEY_SELECTED_PROFILE_ID)
        }
        if (profiles.isEmpty()) {
            UploadPreferenceStore.remove(KEY_PROFILE_JSON)
        }
    }

    fun clear() {
        UploadPreferenceStore.remove(KEY_PROFILE_JSON)
        UploadPreferenceStore.remove(KEY_PROFILES_JSON)
        UploadPreferenceStore.remove(KEY_SELECTED_PROFILE_ID)
    }

    private fun saveProfiles(profiles: List<UploadServerProfile>) {
        UploadPreferenceStore.putString(KEY_PROFILES_JSON, gson.toJson(profiles.map { it.withoutPassword() }))
        Log.i(TAG, "saveProfiles total=${profiles.size}, items=${profiles.joinToString { it.toLogText() }}")
    }

    private fun getProfilesWithoutLegacyFallback(): List<UploadServerProfile> {
        val raw = UploadPreferenceStore.getString(KEY_PROFILES_JSON)
        if (raw.isBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<UploadServerProfile>>(
                raw,
                object : TypeToken<List<UploadServerProfile>>() {}.type
            ) ?: emptyList()
        }.getOrDefault(emptyList()).map { it.withStoredPassword() }
    }

    private fun loadLegacyProfile(): UploadServerProfile {
        val json = UploadPreferenceStore.getString(KEY_PROFILE_JSON)
        if (json.isBlank()) {
            return UploadServerProfile()
        }
        return runCatching {
            gson.fromJson(json, UploadServerProfile::class.java) ?: UploadServerProfile()
        }.getOrDefault(UploadServerProfile()).withStoredPassword()
    }

    private fun UploadServerProfile.toLogText(): String {
        return "id=$id, name=${name.ifBlank { "(empty)" }}, protocol=${protocol.name}, " +
            "host=${config.host}, port=${config.port}, remoteDir=${config.remoteDir}, " +
            "user=${config.username.ifBlank { "(empty)" }}"
    }

    private fun UploadServerProfile.withoutPassword(): UploadServerProfile {
        return copy(config = config.copyPassword(""))
    }

    private fun UploadServerProfile.withStoredPassword(): UploadServerProfile {
        val stored = UploadSecretStore.loadPassword(id)
        if (stored.isBlank()) {
            val legacyPassword = config.passwordValue()
            if (legacyPassword.isNotBlank()) {
                UploadSecretStore.savePassword(id, legacyPassword)
                return withoutPassword().copy(config = config.copyPassword(legacyPassword))
            }
            return this
        }
        return copy(config = config.copyPassword(stored))
    }

    private fun UploadServerConfig.passwordValue(): String {
        return when (this) {
            is FtpConfig -> password
            is FtpsConfig -> password
            is SftpConfig -> password
            is HttpConfig -> password
            is SmbConfig -> password
            is WebDavConfig -> password
        }
    }

    private fun UploadServerConfig.copyPassword(password: String): UploadServerConfig {
        return when (this) {
            is FtpConfig -> copy(password = password)
            is FtpsConfig -> copy(password = password)
            is SftpConfig -> copy(password = password)
            is HttpConfig -> copy(password = password)
            is SmbConfig -> copy(password = password)
            is WebDavConfig -> copy(password = password)
        }
    }
}

