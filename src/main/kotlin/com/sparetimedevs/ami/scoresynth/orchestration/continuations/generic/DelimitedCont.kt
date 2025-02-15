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

/**
 * Base interface for a continuation
 */
public interface DelimitedContinuation<A, R> {
    public suspend operator fun invoke(a: A): R
}

/**
 * Base interface for our scope.
 */
public interface DelimitedScope<R> {
    /**
     * Exit the [DelimitedScope] with [R]
     */
    public suspend fun <A> shift(r: R): A
}

public interface RestrictedScope<R> : DelimitedScope<R> {
    /**
     * Capture the continuation and pass it to [f].
     */
    public suspend fun <A> shift(f: suspend RestrictedScope<R>.(DelimitedContinuation<A, R>) -> R): A

    public override suspend fun <A> shift(r: R): A = shift { r }
}

public interface SuspendedScope<R> : DelimitedScope<R>
