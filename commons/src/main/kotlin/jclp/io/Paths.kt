package jclp.io

fun splitPath(path: String): Pair<Int, Int> {
    var seppos: Int
    var extpos = path.length
    var extFound = false
    seppos = extpos - 1
    while (seppos >= 0) {
        val ch = path[seppos]
        if (ch == '.' && !extFound) {
            extpos = seppos
            extFound = true
        } else if (ch == '/' || ch == '\\') {
            break
        }
        --seppos
    }
    return seppos to extpos
}

fun baseName(path: String) = splitPath(path).let { path.substring(it.first + 1, it.second) }

fun dirName(path: String) = splitPath(path).first.let { if (it != -1) path.substring(0, it) else "" }

fun fullName(path: String) = splitPath(path).first.let { path.substring(if (it != 0) it + 1 else it) }

fun extName(path: String) = splitPath(path).second.let { if (it != path.length) path.substring(it + 1) else "" }
