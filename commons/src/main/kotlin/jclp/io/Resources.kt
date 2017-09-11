package jclp.io

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.*

const val CLASSPATH_PREFIX = "!"

fun getResource(uri: String, loader: ClassLoader? = null): URL? {
    if (uri.isEmpty()) {
        return null
    } else if (uri.startsWith(CLASSPATH_PREFIX)) {
        return (loader ?: Thread.currentThread().contextClassLoader).getResource(uri.substring(CLASSPATH_PREFIX.length))
    }
    val file = File(uri)
    return if (file.exists()) {
        file.toURI().toURL()
    } else try {
        URL(uri)
    } catch (e: MalformedURLException) {
        null
    }
}

@Throws(IOException::class)
fun openResource(uri: String, loader: ClassLoader? = null, reload: Boolean = false): InputStream? {
    val conn = getResource(uri, loader)?.openConnection() ?: return null
    if (reload) {
        conn.useCaches = false
    }
    return conn.getInputStream()
}

@Throws(IOException::class)
fun getProperties(uri: String, loader: ClassLoader? = null, reload: Boolean = false): Properties? {
    return openResource(uri, loader, reload)?.use {
        val prop = Properties()
        prop.load(it)
        prop
    } ?: return null
}
