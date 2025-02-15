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

package com.sparetimedevs.ami.scoresynth.audio

import arrow.core.Either
import arrow.core.raise.either
import com.sparetimedevs.ami.scoresynth.DomainError
import com.sparetimedevs.ami.scoresynth.ExecutionError
import com.sparetimedevs.ami.scoresynth.FluidSynthClient
import java.io.InputStream
import java.nio.file.Path
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
    suspend fun transformMidiToWav(midiStream: InputStream): Either<DomainError, ByteArray> {
        var midiTempFile: Path? = null
        var wavTempFile: Path? = null

        val wavDataOrError =
            either<DomainError, ByteArray> {
                midiTempFile = createTempFileSafe("temp-midi", ".mid").bind()
                wavTempFile = createTempFileSafe("temp-wav", ".wav").bind()

                Either.Companion
                    .catch {
                        midiStream.use { input -> midiTempFile.writeBytes(input.readBytes()) }
                    }.mapLeft { exception ->
                        ExecutionError(exception.message ?: "Unknown error")
                    }.bind()

                fluidSynthClient.transformMidiToWav(midiTempFile, wavTempFile).bind()

                Either.Companion
                    .catch {
                        // Return the generated WAV data as a byte array
                        wavTempFile.readBytes()
                    }.mapLeft { exception ->
                        ExecutionError(exception.message ?: "Unknown error")
                    }.bind()
            }

        // Clean up the temporary files
        midiTempFile?.deleteIfExists()
        wavTempFile?.deleteIfExists()

        return wavDataOrError
    }
}

private fun createTempFileSafe(
    prefix: String,
    suffix: String,
): Either<ExecutionError, Path> =
    Either
        .catch {
            createTempFile(prefix, suffix).apply { deleteIfExists() }
        }.mapLeft { exception ->
            ExecutionError(exception.message ?: "Unknown error")
        }
