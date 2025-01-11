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

import arrow.core.Either
import arrow.core.right
import com.sparetimedevs.ami.core.DomainError
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

@RestController
class EntryController(
    // Consider defining a bean of type 'kotlinx.serialization.json.Json' in your configuration.
    private val jsonParser: Json = Json,
) {
    private val logger: Logger = LoggerFactory.getLogger(EntryController::class.java)

    // curl -v -XPOST localhost:8080/midi-to-wav -H "Content-Type: application/json" -d '{"midi":"wav"}'
    @PostMapping("/midi-to-wav", produces = ["application/json"])
    suspend fun midiToWave(
        @RequestBody input: Input,
    ): ResponseEntity<Any?> =
        resolve(
            f = {
                // Path to the FluidSynth executable (ensure it's installed and accessible)
                val fluidSynthPath = "/usr/local/bin/fluidsynth" // Update as needed
                val soundFontPath = "/Users/joram/temp/soundfont.sf2" // Path to your SoundFont file
                val midiFilePath = "/Users/joram/temp/heigh_ho_nobody_home.mid" // Path to your input MIDI file
                val wavFilePath = // Path to the output WAV file
                    "/Users/joram/temp/output-${com.sparetimedevs.ami.core.util.randomUuidString()}.wav"

                val synthesizer = AudioSynthesizer(fluidSynthPath, soundFontPath)
                try {
                    synthesizer.transformMidiToWav(midiFilePath, wavFilePath)
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }

                input
                    .validateInput()
                    .map { a -> Response("The process is completed for: $a") }
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
data class Input(
    val midi: String,
)

@Serializable
data class Response(
    val message: String,
)

fun Input.validateInput(): Either<DomainError, String> {
    // TODO implement validation
    return this.midi.right()
}
