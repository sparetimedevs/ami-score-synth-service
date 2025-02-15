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
import arrow.core.NonEmptyList
import com.sparetimedevs.ami.scoresynth.orchestration.AccumulatedOrchestrationValidationErrors
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationValidationError

fun <A> Either<NonEmptyList<OrchestrationValidationError>, A>.toEitherAccumulatedValidationErrorsOrA(
    accumulatedValidationErrorsMessage: String = "There were one or more errors while validating the input.",
): Either<AccumulatedOrchestrationValidationErrors, A> =
    this
        .mapLeft {
            AccumulatedOrchestrationValidationErrors(
                reason = accumulatedValidationErrorsMessage,
                validationErrors = it,
            )
        }
// TODO might be replaced with asEitherWithAccumulatedValidationErrors() from ami music sdk core
// Or at least more in line. (This one is specific to OrchestrationValidationErrors)
