/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
