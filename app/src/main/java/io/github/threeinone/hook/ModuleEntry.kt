package io.github.threeinone.hook

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * libxposed API 102 entry point.
 *
 * Replaces the legacy `IXposedHookLoadPackage` / `assets/xposed_init` pair. Modules targeting
 * API 102 may not call `de.robv.android.xposed` APIs, so the adapter receives this module
 * instance instead of the static XposedBridge/XposedHelpers helpers.
 */
class ModuleEntry : XposedModule() {

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        // staticScope=true already limits this module to com.android.systemui. PackageLoadedParam
        // exposes no process name, so the package-name check is the available guard here.
        if (param.packageName != TARGET) return        log("3IN1: SystemUI entry")
        runCatching {
            ColorOsAdapter(this, param.defaultClassLoader).install()
        }.onFailure { log("3IN1: adapter unavailable: $it") }
    }

    fun log(message: String) {
        try {
            log(Log.INFO, TAG, message)
        } catch (_: Throwable) {
            // Never let logging break the hook path.
        }
    }

    fun log(message: String, error: Throwable) {
        try {
            log(Log.ERROR, TAG, message, error)
        } catch (_: Throwable) {
            // Never let logging break the hook path.
        }
    }

    companion object {
        const val TAG = "3IN1"
        const val TARGET = "com.android.systemui"
    }
}
