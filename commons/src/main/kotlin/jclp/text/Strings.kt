package jclp.text

operator fun String.times(n: Int) = repeat(n)

operator fun Int.times(cs: CharSequence) = cs.repeat(this)

operator fun String.get(start: Int, end: Int) = substring(start, end)

infix fun CharSequence?.or(default: CharSequence) = if (isNullOrEmpty()) default.toString() else this!!.toString()

infix inline fun CharSequence?.or(default: () -> CharSequence) = if (isNullOrEmpty()) default().toString() else this!!.toString()