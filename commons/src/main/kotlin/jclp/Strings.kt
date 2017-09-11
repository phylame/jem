package jclp

infix fun CharSequence?.or(default: CharSequence) = if (isNullOrEmpty()) default.toString() else this!!.toString()

infix inline fun CharSequence?.or(default: () -> CharSequence) = if (isNullOrEmpty()) default().toString() else this!!.toString()