package com.crawler.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialsManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    private val masterKeyAlias = "crawler_master_key"
    private var encryptedPrefs: SharedPreferences? = null
    private var masterKey: MasterKey? = null

    private fun getEncryptedPrefs(): SharedPreferences {
        return encryptedPrefs ?: synchronized(this) {
            if (masterKey == null) {
                masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .setUserAuthenticationRequired(false)
                    .build()
            }
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "secure_credentials",
                masterKey!!,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            encryptedPrefs!!
        }
    }

    fun putCredentials(key: String, username: String, password: String) {
        val prefs = getEncryptedPrefs()
        prefs.edit()
            .putString("${key}_username", username)
            .putString("${key}_password", password)
            .apply()
    }

    fun getCredentials(key: String): Pair<String, String>? {
        val prefs = getEncryptedPrefs()
        val username = prefs.getString("${key}_username", null)
        val password = prefs.getString("${key}_password", null)
        return if (username != null && password != null) {
            Pair(username, password)
        } else {
            null
        }
    }

    fun deleteCredentials(key: String) {
        val prefs = getEncryptedPrefs()
        prefs.edit()
            .remove("${key}_username")
            .remove("${key}_password")
            .apply()
    }

    fun hasCredentials(key: String): Boolean {
        val prefs = getEncryptedPrefs()
        return prefs.contains("${key}_username") && prefs.contains("${key}_password")
    }

    fun putString(key: String, value: String) {
        getEncryptedPrefs().edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return getEncryptedPrefs().getString(key, defaultValue) ?: defaultValue
    }

    fun remove(key: String) {
        getEncryptedPrefs().edit().remove(key).apply()
    }
}