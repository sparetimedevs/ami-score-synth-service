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
import arrow.core.EitherNel
import com.sparetimedevs.ami.scoresynth.orchestration.AccumulatedOrchestrationValidationErrors
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationValidationError

const val ACCUMULATED_VALIDATION_ERRORS_MESSAGE_PREFIX: String =
    "There were one or more errors while validating the input: "

fun <A> EitherNel<OrchestrationValidationError, A>.asEitherWithAccumulatedValidationErrors():
    Either<AccumulatedOrchestrationValidationErrors, A> =
    this.mapLeft {
        AccumulatedOrchestrationValidationErrors(
            reason =
                ACCUMULATED_VALIDATION_ERRORS_MESSAGE_PREFIX +
                    it.map { validationError -> validationError.reason }.joinToString(),
            validationErrors = it,
        )
    }
