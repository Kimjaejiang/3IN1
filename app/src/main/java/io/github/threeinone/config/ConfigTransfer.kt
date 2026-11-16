package io.github.threeinone.config

/**
 * Serialises the configuration for [android.provider.Settings.Secure].
 *
 * The refresh broadcast only reaches SystemUI while the settings app is running, so it cannot
 * deliver settings after a reboot. `Settings.Secure` is readable by SystemUI at any time, which
 * makes it the durable channel; the settings app writes it alongside the broadcast.
 *
 * Each entry is `type:key=value`, joined by `;`. Only scalar types occur in the configuration.
 */
object ConfigTransfer {
    const val SECURE_KEY = "threeinone_indicator_config"

    fun encode(values: Map<String, *>): String = values.entries.joinToString(";") { (key, value) ->
        when (value) {
            is Boolean -> "b:$key=${if (value) 1 else 0}"
            is Int -> "i:$key=$value"
            is Long -> "l:$key=$value"
            is Float -> "f:$key=$value"
            is String -> "s:$key=${value.replace(";", " ").replace("=", " ")}"
            else -> throw IllegalArgumentException("Unsupported configuration value for $key")
        }
    }

    fun decode(raw: String?): Map<String, Any> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = HashMap<String, Any>()
        for (entry in raw.split(';')) {
            if (entry.length < 4 || entry[1] != ':') continue
            val type = entry[0]
            val body = entry.substring(2)
            val split = body.indexOf('=')
            if (split <= 0) continue
            val key = body.substring(0, split)
            val text = body.substring(split + 1)
            val value: Any? = when (type) {
                'b' -> text == "1"
                'i' -> text.toIntOrNull()
                'l' -> text.toLongOrNull()
                'f' -> text.toFloatOrNull()
                's' -> text
                else -> null
            }
            if (value != null) out[key] = value
        }
        return out
    }
}
