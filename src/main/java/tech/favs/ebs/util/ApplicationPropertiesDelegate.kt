package tech.favs.ebs.util

import java.util.*
import kotlin.reflect.KProperty

class ApplicationProperty<T>(private val key: String) {
    companion object {
        lateinit var propertiesMap: Map<String, Any>

        fun initializeProperties() {
            val properties = Properties()
            ApplicationProperty::class.java.getResourceAsStream("application.properties").use { inputStream ->
                properties.load(inputStream)
                propertiesMap = properties.map { it.key as String to it.value }.toMap()
            }
        }

        fun isPropertiesInitialized() = ::propertiesMap.isInitialized
    }

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!isPropertiesInitialized()) {
            initializeProperties()
        }
        return propertiesMap[key] as T? ?: error("No property named \"$key\"")
    }
}
