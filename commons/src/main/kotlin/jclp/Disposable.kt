package jclp

interface AutoDisposable {
    fun retain()

    fun release()
}

class DisposedException : IllegalStateException()

abstract class DisposableSupport : AutoDisposable {
    @Volatile
    private var refCount = 1

    private val lock = Any()

    override fun retain() {
        synchronized(lock) {
            if (refCount == 0) {
                throw DisposedException()
            }
            ++refCount
        }
    }

    override fun release() {
        synchronized(lock) {
            if (refCount == 0) {
                throw DisposedException()
            }
            if (--refCount == 0) {
                dispose()
            }
        }
    }

    protected abstract fun dispose()
}

fun <T> T.retain() = apply { (this as? AutoDisposable)?.retain() }

fun <T> T.release() = apply { (this as? AutoDisposable)?.release() }

fun <E, I : Iterable<E>> I.retainAll() = onEach { it.retain() }

fun <E, I : Iterable<E>> I.releaseAll() = onEach { it.release() }
