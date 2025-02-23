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

package com.sparetimedevs.ami.scoresynth.orchestration.util

import arrow.core.Either
import com.sparetimedevs.ami.scoresynth.orchestration.InternalOrchestrationStepFailure
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf

inline fun <reified A> createTypeSafely(): Either<InternalOrchestrationStepFailure, KType> =
    Either
        .catch { A::class.createType(arguments = getTypeProjections<A>()) }
        .mapLeft {
            InternalOrchestrationStepFailure(
                "Something went wrong! Tried to create a KType instance but exception was thrown with message: ${it.message}",
            )
        }

// This function does not work for a type which has type parameters.
fun KClass<*>.createTypeSafely(): Either<InternalOrchestrationStepFailure, KType> =
    Either
        .catch { this.createType() }
        .mapLeft {
            InternalOrchestrationStepFailure(
                "Something went wrong! Tried to create a KType instance but exception was thrown with message: ${it.message}",
            )
        }

inline fun <reified A> getTypeProjections(): List<KTypeProjection> {
    val kType = typeOf<A>()
    val arguments: List<KTypeProjection> = kType.arguments
    return arguments
}
