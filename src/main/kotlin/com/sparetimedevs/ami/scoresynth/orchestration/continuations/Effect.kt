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

import com.sparetimedevs.ami.scoresynth.orchestration.continuations.generic.DelimitedScope

public fun interface Effect<F> {
    public fun control(): DelimitedScope<F>

    public companion object {
        public suspend inline fun <Eff : Effect<*>, F, A> suspended(
            crossinline eff: (DelimitedScope<F>) -> Eff,
            crossinline just: (A) -> F,
            crossinline f: suspend Eff.() -> A,
        ): F = Reset.suspended { just(f(eff(this))) }

        public inline fun <Eff : Effect<*>, F, A> restricted(
            crossinline eff: (DelimitedScope<F>) -> Eff,
            crossinline just: (A) -> F,
            crossinline f: suspend Eff.() -> A,
        ): F = Reset.restricted { just(f(eff(this))) }
    }
}
