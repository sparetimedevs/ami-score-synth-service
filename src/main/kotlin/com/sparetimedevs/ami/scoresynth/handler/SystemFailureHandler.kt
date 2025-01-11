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
import com.sparetimedevs.ami.scoresynth.ErrorResponse
import kotlinx.serialization.json.Json
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

suspend fun handleSystemFailure(
    jsonParser: Json,
    throwable: Throwable,
): Either<Throwable, ResponseEntity<Any?>> = createResponse(jsonParser, throwable)

suspend fun createResponse(
    jsonParser: Json,
    throwable: Throwable,
): Either<Throwable, ResponseEntity<Any?>> =
    toJson(jsonParser, ErrorResponse("$THROWABLE_MESSAGE_PREFIX $throwable"))
        .flatMap { jsonAsString ->
            Either.catch {
                ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonAsString)
            }
        }

private const val THROWABLE_MESSAGE_PREFIX = "An exception was thrown. The exception is:"
