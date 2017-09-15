package jem.util


object Build {
    const val NAME = "Jem"
    const val FULL_NAME = "PW's book toolkit for Java"
    const val VERSION = "5.0-SNAPSHOT"
    const val AUTHOR_EMAIL = "phylame@163.com"
    const val VENDOR = "Peng Wan, PW"
    const val LICENSE = "Apache License, Version 2.0"
    const val SOURCE = "https://github.com/phylame/jem"
}

class JemException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace)
}