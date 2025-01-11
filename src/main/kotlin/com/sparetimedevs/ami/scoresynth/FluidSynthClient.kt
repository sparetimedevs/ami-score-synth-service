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
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

interface FluidSynthClient {
    suspend fun transformMidiToWav(
        midiFile: Path,
        wavFile: Path,
    ): Either<DomainError, Unit>
}

class FluidSynthClientImpl(
    private val fluidSynthPath: String,
    private val soundFontPath: String,
) : FluidSynthClient {
    override suspend fun transformMidiToWav(
        midiFile: Path,
        wavFile: Path,
    ): Either<DomainError, Unit> =
        Either
            .catch {
                // Validate SoundFont file
                validateFileExists(soundFontPath, "SoundFont file")

                // Construct the FluidSynth command
                val command =
                    listOf(
                        fluidSynthPath,
                        "-ni", // Non-interactive mode
                        "-F",
                        wavFile.toString(), // Output to WAV file
                        soundFontPath, // SoundFont file
                        midiFile.toString(), // MIDI file
                    )

                // Execute the command
                val process =
                    ProcessBuilder(command)
                        .redirectErrorStream(true) // Redirect error output to standard output
                        .start()

                // Wait for the process to complete and capture output
                val output = process.inputStream.bufferedReader().readText()
                if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
                    throw RuntimeException("FluidSynth failed: $output")
                }

                Unit
            }.mapLeft { exception ->
                ExecutionError(exception.message ?: "Unknown error")
            }

    private fun validateFileExists(
        filePath: String,
        fileType: String,
    ) {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("$fileType does not exist or is not a valid file: $filePath")
        }
    }
}
