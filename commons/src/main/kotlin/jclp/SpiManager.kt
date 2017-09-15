package jclp

import jclp.log.Log
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.*

interface ServiceProvider {
    val keys: Set<String>

    val name: String get() = ""
}

open class ServiceManager<T : ServiceProvider>(private val type: Class<T>, private val loader: ClassLoader? = null) {
    private val serviceLoader: ServiceLoader<T>

    private val localRegistry = HashMap<String, T>()
    private val serviceSpis = HashSet<T>()

    init {
        serviceLoader = AccessController.doPrivileged(PrivilegedAction {
            if (loader != null) {
                ServiceLoader.load(type, loader)
            } else {
                ServiceLoader.loadInstalled(type)
            }
        })
        initServices()
    }

    fun reload() {
        serviceSpis.clear()
        localRegistry.clear()
        serviceLoader.reload()
        initServices()
    }

    val services get() = serviceSpis + localRegistry.values

    fun getService(key: String) = localRegistry.getOrPut(key) {
        serviceSpis.firstOrNull { key in it.keys }
    }

    fun registerService(name: String, factory: T) {
        localRegistry.put(name, factory)
    }

    private fun initServices() {
        val it = serviceLoader.iterator()
        try {
            while (it.hasNext()) {
                try {
                    serviceSpis += it.next()
                } catch (e: ServiceConfigurationError) {
                    Log.e(javaClass.simpleName, e) { "providers.next()" }
                }
            }
        } catch (e: ServiceConfigurationError) {
            Log.e(javaClass.simpleName, e) { "providers.hasNext()" }
        }
    }
}
