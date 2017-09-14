package jclp.io

import java.io.File
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.*

const val CLASSPATH_PREFIX = "!"

fun contextClassLoader(): ClassLoader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()

fun getResource(uri: String, loader: ClassLoader? = null): URL? = when {
    uri.isEmpty() -> null
    uri.startsWith(CLASSPATH_PREFIX) -> (loader ?: contextClassLoader()).getResource(uri.substring(CLASSPATH_PREFIX.length))
    else -> File(uri).takeIf(File::exists)?.toURI()?.toURL() ?: try {
        URL(uri)
    } catch (e: MalformedURLException) {
        null
    }
}

fun openResource(uri: String, loader: ClassLoader? = null, reload: Boolean = false): InputStream? {
    return getResource(uri, loader)?.openConnection()?.apply {
        if (reload) useCaches = false
    }?.getInputStream()
}

fun getProperties(uri: String, loader: ClassLoader? = null, reload: Boolean = false): Properties? {
    return openResource(uri, loader, reload)?.use {
        val prop = Properties()
        prop.load(it)
        prop
    } ?: return null
}
