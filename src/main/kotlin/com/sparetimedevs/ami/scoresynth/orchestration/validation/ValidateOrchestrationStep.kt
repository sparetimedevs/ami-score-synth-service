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

package com.sparetimedevs.ami.scoresynth.orchestration.validation

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.toEitherNel
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStep
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStepId
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationValidationError
import java.util.UUID

// TODO write unit tests for these

fun validateOrchestrationStep(
    idValue: String,
    orchestrationIdValue: String,
    nameValue: String,
    stateValue: String,
    resultValue: String,
): Either<NonEmptyList<OrchestrationValidationError>, OrchestrationStep> =
    Either.zipOrAccumulate(
        validateOrchestrationStepId(idValue),
        validateOrchestrationId(orchestrationIdValue),
    ) { id, orchestrationId ->
        OrchestrationStep(
            id,
            orchestrationId,
            nameValue,
            stateValue,
            resultValue,
        )
    }

fun validateOrchestrationStepId(
    value: String,
): Either<NonEmptyList<OrchestrationValidationError>, OrchestrationStepId> =
    Either
        .catch { OrchestrationStepId(UUID.fromString(value)) }
        .mapLeft {
            OrchestrationValidationError(
                "Something went wrong! Tried to create an OrchestrationId instance but exception was thrown with message: ${it.message}",
            )
        }.toEitherNel()
