package jclp

import java.util.concurrent.atomic.AtomicInteger

interface AutoDisposable {
    fun retain()

    fun release()
}

fun Any.retainSelf() {
    (this as? AutoDisposable)?.retain()
}

fun Any.releaseSelf() {
    (this as? AutoDisposable)?.release()
}

abstract class DisposableSupport : AutoDisposable {
    private var refCount = AtomicInteger(1)

    override fun retain() {
        require(refCount.get() != 0) { "object is disposed" }
        refCount.incrementAndGet()
    }

    override fun release() {
        require(refCount.get() != 0) { "object is disposed" }
        if (refCount.decrementAndGet() == 0) {
            dispose()
        }
    }

    protected abstract fun dispose()
}

abstract class DisposableCloseable : DisposableSupport(), AutoCloseable {
    override fun dispose() = close()
}
