package tech.favs.ebs.util

import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

class ApplicationProperty<T>(private val key: String) {
    companion object {
        lateinit var propertiesMap: Map<String, String>

        fun initializeProperties() {
            val properties = Properties()
            Thread.currentThread().contextClassLoader.getResourceAsStream("application.properties").use { inputStream ->
                properties.load(inputStream)
                propertiesMap = properties.map { it.key as String to it.value as String }.toMap()
            }
        }

        fun isPropertiesInitialized() = ::propertiesMap.isInitialized
    }

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!isPropertiesInitialized()) {
            initializeProperties()
        }
        val value = propertiesMap.getValue(key).let {
            if (property.returnType.isSubtypeOf(Number::class.starProjectedType)) {
                it.toInt()
            } else it
        }
        return value as T? ?: error("No property named \"$key\"")
    }
}
