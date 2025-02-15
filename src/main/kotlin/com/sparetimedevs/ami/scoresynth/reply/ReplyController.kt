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

package com.sparetimedevs.ami.scoresynth.reply

import arrow.core.flatMap
import arrow.core.right
import com.sparetimedevs.ami.scoresynth.DomainError
import com.sparetimedevs.ami.scoresynth.handler.handleDomainError
import com.sparetimedevs.ami.scoresynth.handler.handleSuccessWithDefaultHandler
import com.sparetimedevs.ami.scoresynth.handler.handleSystemFailure
import com.sparetimedevs.ami.scoresynth.mapLeftToDomainError
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestration
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestrator
import com.sparetimedevs.ami.scoresynth.orchestration.util.toEitherAccumulatedValidationErrorsOrA
import com.sparetimedevs.ami.scoresynth.orchestration.validation.validateOrchestrationId
import com.sparetimedevs.ami.scoresynth.resolve
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ReplyController(
    private val jsonParser: Json,
    private val orchestrator: Orchestrator<String, String>,
) {
    private val logger: Logger = LoggerFactory.getLogger(ReplyController::class.java)

    // curl -v localhost:8080/replies/{uuid}
    @GetMapping("/replies/{uuid}")
    suspend fun getReplyFor(
        @PathVariable uuid: String,
    ): ResponseEntity<String> =
        resolve(
            f = {
                validateOrchestrationId(uuid)
                    .toEitherAccumulatedValidationErrorsOrA()
                    .flatMap { orchestratorId ->
                        orchestrator.orchestrationState(orchestratorId)
                    }.mapLeftToDomainError()
            },
            success = { orchestration ->
                handleSuccessWithDefaultHandler(jsonParser, orchestration.toOrchestrationDto())
            },
            error = { domainError: DomainError ->
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
}

@Serializable
data class OrchestrationDto<Input, Result>(
    val id: String,
    val name: String,
    val input: Input,
    val state: String,
    val result: Result?,
)

fun <Input, Result> Orchestration<Input, Result>.toOrchestrationDto() =
    OrchestrationDto(
        id = this.id.value.toString(),
        name = this.name,
        input = this.input,
        state = this.state,
        result = this.result,
    )
