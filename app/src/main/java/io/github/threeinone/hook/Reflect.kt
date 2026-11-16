package io.github.threeinone.hook

import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Reflection helpers replacing legacy `de.robv.android.xposed.XposedHelpers`, which is
 * forbidden for modules targeting libxposed API 102.
 *
 * This is plain java.lang.reflect access, not part of any Xposed API surface.
 */
internal object Reflect {
    private val fieldCache = HashMap<Class<*>, HashMap<String, Field>>()
    private val methodCache = HashMap<Class<*>, HashMap<String, Method>>()

    private fun findField(type: Class<*>, name: String): Field {
        var c: Class<*>? = type
        while (c != null) {
            try {
                return c.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                c = c.superclass
            }
        }
        throw NoSuchFieldException("${type.name}.$name")
    }

    fun field(type: Class<*>, name: String): Field = synchronized(fieldCache) {
        fieldCache.getOrPut(type) { HashMap() }.getOrPut(name) { findField(type, name) }
    }

    fun getObjectField(instance: Any?, name: String): Any? {
        requireNotNull(instance) { "null receiver for field $name" }
        return field(instance.javaClass, name).get(instance)
    }

    fun getFloatField(instance: Any?, name: String): Float =
        field(requireNotNull(instance).javaClass, name).getFloat(instance)

    fun getIntField(instance: Any?, name: String): Int =
        field(requireNotNull(instance).javaClass, name).getInt(instance)

    /**
     * Finds a method by name and arity, preferring the most specific assignability match.
     * Legacy XposedHelpers matched on the runtime argument types the same way.
     */
    private fun findMethod(type: Class<*>, name: String, args: Array<out Any?>?): Method {
        val arity = args?.size ?: -1
        val candidates = ArrayList<Method>()
        var c: Class<*>? = type
        while (c != null) {
            for (m in c.declaredMethods) {
                if (m.name == name && (arity < 0 || m.parameterCount == arity)) candidates.add(m)
            }
            c = c.superclass
        }
        if (candidates.isEmpty()) throw NoSuchMethodException("${type.name}.$name/${args?.size ?: "any"}")
        if (candidates.size == 1) return candidates[0].apply { isAccessible = true }
        if (args != null) {
            val exact = candidates.firstOrNull { m ->
                m.parameterTypes.indices.all { i ->
                    val actual = args[i]
                    actual == null || boxed(m.parameterTypes[i]).isInstance(actual)
                }
            }
            if (exact != null) return exact.apply { isAccessible = true }
        }
        // No argument types were supplied or none matched exactly; fall back to the first
        // declaration found, which reflection will still widen or convert if possible.
        return candidates.first().apply { isAccessible = true }
    }

    private fun boxed(c: Class<*>): Class<*> = if (!c.isPrimitive) c else when (c) {
        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
        java.lang.Byte.TYPE -> java.lang.Byte::class.java
        java.lang.Character.TYPE -> java.lang.Character::class.java
        java.lang.Short.TYPE -> java.lang.Short::class.java
        java.lang.Integer.TYPE -> java.lang.Integer::class.java
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        else -> java.lang.Void::class.java
    }

    fun method(type: Class<*>, name: String, vararg args: Any?): Method = synchronized(methodCache) {
        val key = "$name/${args.size}"
        methodCache.getOrPut(type) { HashMap() }.getOrPut(key) { findMethod(type, name, args) }
    }

    fun callMethod(instance: Any?, name: String, vararg args: Any?): Any? {
        requireNotNull(instance) { "null receiver for $name" }
        val m = method(instance.javaClass, name, *args)
        return m.invoke(instance, *args)
    }

    fun callStaticMethod(type: Class<*>, name: String, vararg args: Any?): Any? =
        method(type, name, *args).invoke(null, *args)

    fun newInstance(type: Class<*>, vararg args: Any?): Any {
        val ctor = type.declaredConstructors.firstOrNull { it.parameterCount == args.size }
            ?.apply { isAccessible = true }
            ?: throw NoSuchMethodException("${type.name}/(${args.size})")
        return ctor.newInstance(*args)
    }
}
