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

package com.sparetimedevs.ami.scoresynth.handler

import arrow.core.Either
import arrow.core.flatMap
import com.sparetimedevs.ami.core.AccumulatedValidationErrors
import com.sparetimedevs.ami.core.DomainError
import com.sparetimedevs.ami.core.ParseError
import com.sparetimedevs.ami.scoresynth.toResponse
import kotlinx.serialization.json.Json
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

suspend fun handleDomainError(
    jsonParser: Json,
    domainError: DomainError,
): Either<Throwable, ResponseEntity<Any?>> = createResponse(jsonParser, domainError)

private suspend fun createResponse(
    jsonParser: Json,
    domainError: DomainError,
): Either<Throwable, ResponseEntity<Any?>> =
    when (domainError) {
        is ParseError -> {
            toJson(jsonParser, domainError.toResponse())
                .flatMap { jsonAsString ->
                    Either.catch {
                        ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(jsonAsString)
                    }
                }
        }

        is AccumulatedValidationErrors -> {
            toJson(jsonParser, domainError.toResponse())
                .flatMap { jsonAsString ->
                    Either.catch {
                        ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(jsonAsString)
                        throw RuntimeException("BOOM!")
                    }
                }
        }
    }
