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

package com.sparetimedevs.ami.scoresynth

import arrow.core.Either
import arrow.core.flatten
import arrow.core.right
import com.sparetimedevs.ami.scoresynth.handler.handleDomainError
import com.sparetimedevs.ami.scoresynth.handler.handleSuccessWithDefaultHandler
import com.sparetimedevs.ami.scoresynth.handler.handleSystemFailure
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.springframework.http.ResponseEntity

/**
 * The resolve function can resolve any function that yields an Either into one type of value.
 *
 * @param f the function that needs to be resolved.
 * @param success the function to apply if [f] yields a success of type [A].
 * @param error the function to apply if [f] yields an error of type [E].
 * @param throwable the function to apply if [f] throws a [Throwable]. Throwing any [Throwable] in
 *   the [throwable] function will render the [resolve] function nondeterministic.
 * @param unrecoverableState the function to apply if [resolve] is in an unrecoverable state.
 * @return the result of applying the [resolve] function.
 */
inline fun <E, A, B> resolve(
    f: () -> Either<E, A>,
    success: (a: A) -> Either<Throwable, B>,
    error: (e: E) -> Either<Throwable, B>,
    throwable: (throwable: Throwable) -> Either<Throwable, B>,
    unrecoverableState: (throwable: Throwable) -> Either<Throwable, Unit>,
): B =
    Either
        .catch(f)
        .fold(
            { t: Throwable -> throwable(t) },
            {
                it.fold(
                    { e: E -> Either.catch { error(e) }.flatten() },
                    { a: A -> Either.catch { success(a) }.flatten() },
                )
            },
        ).fold({ t: Throwable -> throwable(t) }, { b: B -> b.right() })
        .fold(
            { t: Throwable ->
                unrecoverableState(t)
                throw t
            },
            { b: B -> b },
        )

suspend inline fun <reified A> resolve(
    jsonParser: Json,
    logger: Logger,
    f: () -> Either<DomainError, A>,
): ResponseEntity<String> =
    resolve(
        f = f,
        success = { a ->
            handleSuccessWithDefaultHandler(jsonParser, a)
        },
        error = { domainError ->
            handleDomainError(jsonParser, domainError)
        },
        throwable = { throwable ->
            handleSystemFailure(jsonParser, throwable)
        },
        unrecoverableState = { throwable ->
            logger.error("Something horrible happened when resolve was invoked. The exception is: $throwable")
            Unit.right()
        },
    )
