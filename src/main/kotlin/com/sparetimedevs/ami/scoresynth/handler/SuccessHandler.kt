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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

suspend inline fun <reified A> handleSuccessWithDefaultHandler(
    jsonParser: Json,
    a: A,
): Either<Throwable, ResponseEntity<String>> =
    toJson(jsonParser, a)
        .flatMap { jsonAsString ->
            Either.catch {
                ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonAsString)
            }
        }

inline fun <reified A> toJson(
    jsonParser: Json,
    a: A,
): Either<Throwable, String> = Either.catch { jsonParser.encodeToString(a) }
