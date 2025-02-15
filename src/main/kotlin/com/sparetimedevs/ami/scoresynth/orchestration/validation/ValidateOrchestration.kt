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
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestration
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationId
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationValidationError
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.UUID
import kotlin.reflect.KType

// TODO write unit tests for these

fun <Input, Result> validateOrchestration(
    idValue: String,
    nameValue: String,
    inputJsonValue: String,
    stateValue: String,
    resultJsonValue: String?,
    jsonParser: Json,
    inputType: KType,
): Either<NonEmptyList<OrchestrationValidationError>, Orchestration<Input, Result>> =
    Either.zipOrAccumulate(
        validateOrchestrationId(idValue),
        validateOrchestrationInput<Input>(inputJsonValue, jsonParser, inputType),
        validateOrchestrationResultIsNull<Result>(resultJsonValue),
    ) { id, input, result ->
        Orchestration(id, nameValue, input, stateValue, result)
    }

fun <Input, Result> validateOrchestration(
    idValue: String,
    nameValue: String,
    inputJsonValue: String,
    stateValue: String,
    resultJsonValue: String?,
    jsonParser: Json,
    inputType: KType,
    resultType: KType,
): Either<NonEmptyList<OrchestrationValidationError>, Orchestration<Input, Result>> =
    Either.zipOrAccumulate(
        validateOrchestrationId(idValue),
        validateOrchestrationInput<Input>(inputJsonValue, jsonParser, inputType),
        validateOrchestrationResult<Result>(resultJsonValue, jsonParser, resultType),
    ) { id, input, result ->
        Orchestration(id, nameValue, input, stateValue, result)
    }

fun validateOrchestrationId(value: String): Either<NonEmptyList<OrchestrationValidationError>, OrchestrationId> =
    Either
        .catch { OrchestrationId(UUID.fromString(value)) }
        .mapLeft {
            OrchestrationValidationError(
                "Something went wrong! Tried to create an OrchestrationId instance but exception was thrown with message: ${it.message}",
            )
        }.toEitherNel()

fun <Input> validateOrchestrationInput(
    jsonValue: String,
    jsonParser: Json,
    type: KType,
): Either<NonEmptyList<OrchestrationValidationError>, Input> {
    val deserializer: KSerializer<Input> = jsonParser.serializersModule.serializer(type) as KSerializer<Input>

    return Either
        .catch { jsonParser.decodeFromString(deserializer, jsonValue) }
        .mapLeft {
            OrchestrationValidationError(
                "Something went wrong! Tried to create an Orchestration instance but exception was thrown with message: ${it.message}",
            )
        }.toEitherNel()
}

fun <Result> validateOrchestrationResult(
    jsonValue: String?,
    jsonParser: Json,
    type: KType,
): Either<NonEmptyList<OrchestrationValidationError>, Result?> {
    val deserializer: KSerializer<Result> = jsonParser.serializersModule.serializer(type) as KSerializer<Result>

    val result: Either<NonEmptyList<OrchestrationValidationError>, Result?> =
        if (jsonValue == null) {
            Either.Right(null)
        } else {
            Either
                .catch { jsonParser.decodeFromString(deserializer, jsonValue) }
                .mapLeft {
                    OrchestrationValidationError(
                        "Something went wrong! Tried to create an Orchestration instance but exception was thrown with message: ${it.message}",
                    )
                }.toEitherNel()
        }

    return result
}

fun <Result> validateOrchestrationResultIsNull(
    jsonValue: String?,
): Either<NonEmptyList<OrchestrationValidationError>, Result?> {
    // TODO do something if result is not null, because it should be if it still needs to be completed.

    return Either.Right(null)
}
