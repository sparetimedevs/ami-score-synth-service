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

import arrow.core.right
import com.sparetimedevs.ami.music.input.Score
import com.sparetimedevs.ami.music.input.toInput
import com.sparetimedevs.ami.music.input.validation.validateInput
import com.sparetimedevs.ami.scoresynth.handler.handleDomainError
import com.sparetimedevs.ami.scoresynth.handler.handleSuccessWithDefaultHandler
import com.sparetimedevs.ami.scoresynth.handler.handleSystemFailure
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Suppress("ktlint:standard:max-line-length")
@RestController
class PingpongController(
    private val jsonParser: Json = Json, // Consider defining a bean of type 'kotlinx.serialization.json.Json' in your configuration.
) {
    private val logger: Logger = LoggerFactory.getLogger(PingpongController::class.java)

    // curl -v -XPOST localhost:8080/score -H "Content-Type: application/json" -d '{"id":"d737b4ae-fbaa-4b0d-9d36-d3651e30e93a","parts":[{"id":"p-1","name":"Part one","instrument":{"name":"Grand Piano","midiChannel":0,"midiProgram":1},"measures":[{"notes":[{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"C","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"G","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"A","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"E","alter":0,"octave":4},"noteAttributes":{}}]},{"notes":[{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"C","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"C","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"A","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"A","alter":0,"octave":4},"noteAttributes":{}}]}]},{"id":"p-2","name":"Part two","instrument":{"name":"Overdriven Guitar","midiChannel":1,"midiProgram":30},"measures":[{"notes":[{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"C","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"G","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"A","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"E","alter":0,"octave":4},"noteAttributes":{}}]},{"notes":[{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"C","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"C","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"A","alter":0,"octave":4},"noteAttributes":{}},{"duration":{"noteValue":"QUARTER","modifier":"NONE"},"pitch":{"noteName":"A","alter":0,"octave":4},"noteAttributes":{}}]}]}]}'
    @PostMapping("/score", produces = ["application/json"])
    suspend fun pingpongScore(
        @RequestBody inputScore: Score,
    ): ResponseEntity<Any?> =
        resolve(
            f = {
                inputScore
                    .validateInput()
                    .map { score -> ResponseInputScore(score.toInput()) }
            },
            success = { a ->
                handleSuccessWithDefaultHandler(jsonParser, a)
            },
            error = { domainError ->
                handleDomainError(jsonParser, domainError)
            },
            throwable = { throwable ->
                handleSystemFailure(jsonParser, throwable)
            },
            unrecoverableState = { throwable ->
                logger.error("Something horrible happened when GET /score was invoked. The exception is: $throwable")
                Unit.right()
            },
        )
}

@Serializable
data class ResponseInputScore(
    val inputScore: Score,
)
