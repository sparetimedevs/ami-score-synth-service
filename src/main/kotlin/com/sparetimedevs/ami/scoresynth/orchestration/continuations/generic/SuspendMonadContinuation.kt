/*
 * Copyright (c) 2025 sparetimedevs and respective authors and developers.
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

package com.sparetimedevs.ami.scoresynth.orchestration.continuations.generic

import arrow.atomic.Atomic
import arrow.atomic.loop
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.resumeWithException

internal const val UNDECIDED = 0
internal const val SUSPENDED = 1

@Suppress("UNCHECKED_CAST")
internal open class SuspendMonadContinuation<R>(
    private val parent: Continuation<R>,
    val f: suspend SuspendedScope<R>.() -> R,
) : Continuation<R>,
    SuspendedScope<R> {
    /**
     * State is either
     *  0 - UNDECIDED
     *  1 - SUSPENDED
     *  Any? (3) `resumeWith` always stores it upon UNDECIDED, and `getResult` can atomically get it.
     */
    private val decisionRef = Atomic<Any>(UNDECIDED)
    private val token: Token = Token()

    override val context: CoroutineContext = parent.context

    override fun resumeWith(result: Result<R>) {
        decisionRef.loop { decision ->
            when (decision) {
                UNDECIDED -> {
                    val r: R? = result.fold({ it }) { EMPTY_VALUE.unbox(it.shiftedOrNull()) }
                    when {
                        r == null -> {
                            parent.resumeWithException(result.exceptionOrNull()!!)
                            return
                        }

                        decisionRef.compareAndSet(UNDECIDED, r) -> {
                            return
                        }

                        else -> Unit // loop again
                    }
                }

                else -> { // If not `UNDECIDED` then we need to pass result to `parent`
                    val res: Result<R> =
                        result.fold(
                            { Result.success(it) },
                            { t ->
                                val x = t.shiftedOrNull()
                                if (x === EMPTY_VALUE) {
                                    Result.failure(t)
                                } else {
                                    Result.success(EMPTY_VALUE.unbox(x))
                                }
                            },
                        )
                    parent.resumeWith(res)
                    return
                }
            }
        }
    }

    @PublishedApi // return the result
    internal fun getResult(): Any? =
        decisionRef.loop { decision ->
            when (decision) {
                UNDECIDED -> if (this.decisionRef.compareAndSet(UNDECIDED, SUSPENDED)) return COROUTINE_SUSPENDED
                else -> return decision
            }
        }

    // If ShortCircuit causes CancellationException, we also want to shift back to R
    private tailrec fun Throwable.shortCircuitCause(): ShortCircuit? =
        when (val cause = this.cause) {
            null -> null
            is ShortCircuit -> cause
            else -> cause.shortCircuitCause()
        }

    private fun Throwable.shiftedOrNull(): Any? {
        val shortCircuit = if (this is ShortCircuit) this else shortCircuitCause()
        return if (shortCircuit != null && shortCircuit.token === token) {
            shortCircuit.raiseValue as R
        } else {
            EMPTY_VALUE
        }
    }

    public override suspend fun <A> shift(r: R): A = throw ShortCircuit(token, r)

    fun startCoroutineUninterceptedOrReturn(): Any? =
        try {
            f.startCoroutineUninterceptedOrReturn(this, this)?.let {
                if (it == COROUTINE_SUSPENDED) {
                    getResult()
                } else {
                    it
                }
            }
        } catch (e: Throwable) {
            val x = e.shiftedOrNull()
            if (x === EMPTY_VALUE) throw e else x
        }
}
