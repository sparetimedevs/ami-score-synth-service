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

package com.sparetimedevs.ami.scoresynth.orchestration.continuations

import com.sparetimedevs.ami.scoresynth.orchestration.continuations.generic.DelimContScope
import com.sparetimedevs.ami.scoresynth.orchestration.continuations.generic.RestrictedScope
import com.sparetimedevs.ami.scoresynth.orchestration.continuations.generic.SuspendMonadContinuation
import com.sparetimedevs.ami.scoresynth.orchestration.continuations.generic.SuspendedScope
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

object Reset {
    /**
     * Allows for building suspending single-shot computation blocks.
     * For short-circuiting, or shifting, a [ShortCircuit] [ControlThrowable] is used.
     * This ensures that any concurrent nested scopes are correctly closed.
     *
     * The usage of `try { ... } catch(e: Throwable) { ... }` will catch the [ShortCircuit] error,
     * and will lead to recover of short-circuiting.
     * You should always prefer to catch the most specific exception class, or
     * use `Either.catch`, `Validated.catch` etc or `e.nonFatalOrThrow()`
     * to ensure you're not catching `ShortCircuit`.
     */
    public suspend fun <A> suspended(block: suspend SuspendedScope<A>.() -> A): A =
        suspendCoroutineUninterceptedOrReturn { cont ->
            SuspendMonadContinuation(cont, block)
                .startCoroutineUninterceptedOrReturn()
        }

    /**
     * Allows for building eager single-shot computation blocks.
     * For short-circuiting, or shifting, `@RestrictSuspension` state machine is used.
     * This doesn't allow nesting of computation blocks, or foreign suspension.
     */
    fun <A> restricted(block: suspend RestrictedScope<A>.() -> A): A = DelimContScope(block).invoke()
}
