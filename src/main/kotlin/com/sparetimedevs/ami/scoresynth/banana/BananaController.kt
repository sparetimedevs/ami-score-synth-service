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

package com.sparetimedevs.ami.scoresynth.banana

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import com.sparetimedevs.ami.scoresynth.DomainError
import com.sparetimedevs.ami.scoresynth.handler.handleDomainError
import com.sparetimedevs.ami.scoresynth.handler.handleSystemFailure
import com.sparetimedevs.ami.scoresynth.handler.toJson
import com.sparetimedevs.ami.scoresynth.mapLeftToDomainError
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationId
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestrator
import com.sparetimedevs.ami.scoresynth.resolve
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

// Banana driven development.
@RestController
class BananaController(
    private val jsonParser: Json,
    private val orchestrator: Orchestrator<String, String>,
) {
    private val logger: Logger = LoggerFactory.getLogger(BananaController::class.java)

    // curl -v localhost:8080/bananas
    @GetMapping("/bananas")
    suspend fun all(): List<Banana> = listOf(Banana("a", "b"), Banana("c", "d"))

    // curl -v -X POST localhost:8080/bananas -H 'Content-type:application/json' -d '{"x": "The X", "y": "The Y"}'
    @PostMapping("/bananas")
    suspend fun newBanana(
        @RequestBody newBanana: Banana,
    ): ResponseEntity<String> =
        resolve(
            f = {
                orchestrator
                    .registerOrchestration(newBanana.x)
                    .mapLeftToDomainError()
            },
            success = { orchestration ->
                handleSuccessWithAsyncReply(jsonParser, orchestration.id.toAsyncReply())
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

    private fun OrchestrationId.toAsyncReply() = AsyncReply(replyUrl = "http://localhost:8080/replies/${this.value}")
}

@Serializable
data class Banana(
    val x: String,
    val y: String,
)

suspend fun handleSuccessWithAsyncReply(
    jsonParser: Json,
    asyncReply: AsyncReply,
): Either<Throwable, ResponseEntity<String>> =
    toJson(jsonParser, asyncReply)
        .flatMap { jsonAsString ->
            Either.catch {
                ResponseEntity.accepted().contentType(MediaType.APPLICATION_JSON).body(jsonAsString)
            }
        }

@Serializable
data class AsyncReply(
    val replyUrl: String,
)
