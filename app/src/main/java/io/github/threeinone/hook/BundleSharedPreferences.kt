package io.github.threeinone.hook

import android.content.SharedPreferences
import android.os.Bundle

/**
 * Read-only [SharedPreferences] over the configuration [Bundle] carried by the refresh broadcast.
 *
 * libxposed API 102 has no equivalent of the legacy world-readable preference file: the settings
 * app's XML is private to its own UID, this LSPosed build does not host remote preferences, and
 * SystemUI cannot resolve the settings app's provider on ColorOS. The refresh broadcast is the one
 * cross-process channel that works, so the settings app carries the values in it.
 */
internal class BundleSharedPreferences(private val values: Map<String, Any?>) : SharedPreferences {

    override fun getAll(): Map<String, *> = values
    override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        @Suppress("UNCHECKED_CAST")
        return values[key] as? Set<String> ?: defValues
    }
    override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = throw UnsupportedOperationException("read-only preferences")

    // The settings app pushes a refresh broadcast after saving, so no listener plumbing is needed.
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit

    companion object {
        /** Copies a configuration bundle into a plain map. */
        internal fun toMap(bundle: Bundle): Map<String, Any?> {
            @Suppress("DEPRECATION")
            val keys = bundle.keySet()
            val out = HashMap<String, Any?>(keys.size)
            for (key in keys) out[key] = bundle.get(key)
            return out
        }
    }
}
