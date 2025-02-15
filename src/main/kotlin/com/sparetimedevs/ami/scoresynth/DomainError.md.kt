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
import arrow.core.NonEmptyList
import com.sparetimedevs.ami.core.validation.ValidationError
import com.sparetimedevs.ami.scoresynth.orchestration.ContinuationToken
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationCreationError
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationError
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationRetrievalError

sealed interface DomainError {
    val message: String
}

data class ExecutionError(
    override val message: String,
) : DomainError

data class ParseError(
    override val message: String,
) : DomainError

data class AccumulatedValidationErrors(
    override val message: String,
    val validationErrors: NonEmptyList<ValidationError>,
) : DomainError

data class InvalidFileFormatError(
    override val message: String,
) : DomainError

data class EntityNotFound(
    override val message: String = "Entity not found.",
) : DomainError

data class ServiceUnavailable(
    override val message: String = "The service is unavailable.",
) : DomainError

data class UnknownError(
    override val message: String = "An unknown error occurred.",
) : DomainError

fun <A> Either<OrchestrationError, A>.mapLeftToDomainError() =
    this.mapLeft {
        when (it) {
            is ContinuationToken -> UnknownError("Should not be possible at this stage.")
            is OrchestrationCreationError -> UnknownError("Should not be possible at this stage.")
            is OrchestrationRetrievalError -> EntityNotFound(it.reason)
            else -> UnknownError("Something went wrong, ${it.reason}")
        }
    }
