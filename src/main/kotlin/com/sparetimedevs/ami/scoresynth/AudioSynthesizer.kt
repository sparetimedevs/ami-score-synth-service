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
import arrow.core.flatMap
import java.io.InputStream
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

class AudioSynthesizer(
    private val fluidSynthClient: FluidSynthClient,
) {
    /**
     * Transforms a MIDI file provided as an InputStream into a WAV file and returns the WAV data as a byte array.
     *
     * @param midiStream InputStream containing the MIDI data.
     * @return Either<DomainError, ByteArray> containing the generated WAV file data or the error in case of a failure.
     */
    suspend fun transformMidiToWav(midiStream: InputStream): Either<DomainError, ByteArray> =
        Either
            .catch {
                // Write the MIDI data to a temporary buffer file
                val midiTempFile = createTempFile("temp-midi", ".mid").apply { deleteIfExists() }
                midiStream.use { input -> midiTempFile.writeBytes(input.readBytes()) }

                // Write the WAV data to a temporary file
                val wavTempFile = createTempFile("temp-wav", ".wav").apply { deleteIfExists() }

                midiTempFile to wavTempFile
            }.mapLeft { exception ->
                ExecutionError(exception.message ?: "Unknown error")
            }.onRight { (midiTempFile, wavTempFile) ->
                fluidSynthClient.transformMidiToWav(midiTempFile, wavTempFile)
            }.flatMap { (midiTempFile, wavTempFile) ->
                Either
                    .catch {
                        // Return the generated WAV data as a byte array
                        wavTempFile.readBytes().also {
                            wavTempFile.deleteIfExists() // Clean up the temporary WAV file
                            midiTempFile.deleteIfExists() // Clean up the temporary MIDI file
                        }
                    }.mapLeft { exception ->
                        ExecutionError(exception.message ?: "Unknown error")
                    }
            }
}
