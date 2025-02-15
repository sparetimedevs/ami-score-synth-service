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

package com.sparetimedevs.ami.scoresynth.healthcheck

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import com.sparetimedevs.ami.scoresynth.handler.handleDomainError
import com.sparetimedevs.ami.scoresynth.handler.handleSystemFailure
import com.sparetimedevs.ami.scoresynth.handler.toJson
import com.sparetimedevs.ami.scoresynth.resolve
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/health")
class HealthCheckController(
    private val jsonParser: Json,
) {
    private val logger: Logger = LoggerFactory.getLogger(HealthCheckController::class.java)

    @GetMapping
    fun generalHealthCheck(): ResponseEntity<HealthResponse> {
        // Example: Include dependencies or service status here
        val components =
            mapOf(
                "database" to checkDatabaseHealth(),
                "messageBroker" to checkMessageBrokerHealth(),
                "diskSpace" to checkDiskSpace(),
            )

        val status = if (components.values.any { it.status == "DOWN" }) "DOWN" else "UP"

        val response =
            HealthResponse(
                status = status,
                timestamp = Instant.now().toString(),
                components = components,
            )
        val statusCode = if (status == "UP") 200 else 503
        return ResponseEntity.status(statusCode).body(response)
    }

    // curl -v localhost:8080/health/liveness
    @GetMapping("/liveness")
    suspend fun livenessProbe(): ResponseEntity<String> =
        resolve(jsonParser, logger) { LivenessResponse("ALIVE").right() }

    // curl -v localhost:8080/health/readiness
    @GetMapping("/readiness")
    suspend fun readinessProbe(): ResponseEntity<String> =
        resolve(
            f = {
                // Example: Check external dependencies
                val isReady = checkDatabaseHealth().status == "UP" && checkMessageBrokerHealth().status == "UP"
                isReady.right()
            },
            success = { isReady ->
                handleSuccessHandler(jsonParser, isReady)
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

    private suspend inline fun handleSuccessHandler(
        jsonParser: Json,
        isReady: Boolean,
    ): Either<Throwable, ResponseEntity<String>> {
        val statusCode = if (isReady) 200 else 503
        val body = mapOf("status" to if (isReady) "UP" else "DOWN")
        return toJson(jsonParser, body)
            .flatMap { jsonAsString ->
                Either.Companion.catch {
                    ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(jsonAsString)
                }
            }
    }

    @Serializable
    data class HealthResponse(
        val status: String,
        val timestamp: String,
        val components: Map<String, ComponentHealth>,
    )

    @Serializable
    data class ComponentHealth(
        val status: String,
        val error: String? = null,
        val details: Map<String, String>? = null,
    )

    @Serializable
    data class LivenessResponse(
        val status: String,
    )

    // Mock health checks (replace with actual logic)
    private fun checkDatabaseHealth(): ComponentHealth =
        ComponentHealth(
            status = "UP", // Change to "DOWN" if the database is unreachable
        )

    private fun checkMessageBrokerHealth(): ComponentHealth =
        ComponentHealth(
            status = "DOWN",
            error = "Connection timeout",
        )

    private fun checkDiskSpace(): ComponentHealth =
        ComponentHealth(
            status = "UP",
            details = mapOf("total" to "500GB", "free" to "200GB"),
        )
}
