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

package com.sparetimedevs.ami.scoresynth.orchestration

import arrow.core.NonEmptyList

sealed interface OrchestrationError {
    val reason: String
}

data class OrchestrationCreationError(
    override val reason: String,
) : OrchestrationError

data class OrchestrationRetrievalError(
    override val reason: String,
) : OrchestrationError

data class UnknownOrchestrationError(
    override val reason: String,
) : OrchestrationError

data class AccumulatedOrchestrationValidationErrors(
    override val reason: String = "There were one or more errors while validating the input.",
    val validationErrors: NonEmptyList<OrchestrationValidationError>,
) : OrchestrationError

interface OrchestrationStepFailure : OrchestrationError

data class InternalOrchestrationStepFailure(
    override val reason: String,
) : OrchestrationStepFailure

data class OrchestrationStepRetrievalFailure(
    override val reason: String,
) : OrchestrationStepFailure

data class UnknownInternalOrchestrationStepFailure(
    override val reason: String,
) : OrchestrationStepFailure

data class UnknownExternalOrchestrationStepFailure(
    override val reason: String,
) : OrchestrationStepFailure

data class ContinuationToken(
    override val reason: String,
) : OrchestrationStepFailure

data class OrchestrationValidationError(
    val reason: String,
)
