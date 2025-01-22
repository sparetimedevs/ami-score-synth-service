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
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

class AudioSynthesizer(
    private val fluidSynthPath: String,
    private val soundFontPath: String,
) {
    /**
     * Transforms a MIDI file provided as an InputStream into a WAV file and returns the WAV data as a byte array.
     *
     * @param midiStream InputStream containing the MIDI data.
     * @return Either<DomainError, ByteArray> containing the generated WAV file data or the error in case of a failure.
     */
    fun transformMidiToWav(midiStream: InputStream): Either<DomainError, ByteArray> =
        Either
            .catch {
                // Validate SoundFont file
                validateFileExists(soundFontPath, "SoundFont file")

                // Write the MIDI data to a temporary buffer file
                val midiTempFile = createTempFile("temp-midi", ".mid").apply { deleteIfExists() }
                midiStream.use { input -> midiTempFile.writeBytes(input.readBytes()) }

                // Write the WAV data to a temporary file
                val wavTempFile = createTempFile("temp-wav", ".wav").apply { deleteIfExists() }

                // Construct the FluidSynth command
                val command =
                    listOf(
                        fluidSynthPath,
                        "-ni", // Non-interactive mode
                        "-F",
                        wavTempFile.toString(), // Output to WAV file
                        soundFontPath, // SoundFont file
                        midiTempFile.toString(), // MIDI file
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

                // Return the generated WAV data as a byte array
                wavTempFile.readBytes().also {
                    wavTempFile.deleteIfExists() // Clean up the temporary WAV file
                    midiTempFile.deleteIfExists() // Clean up the temporary MIDI file
                }
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
