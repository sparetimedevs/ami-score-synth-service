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
import arrow.core.left
import arrow.core.right
import com.sparetimedevs.ami.scoresynth.DomainError
import com.sparetimedevs.ami.scoresynth.InvalidFileFormatError
import com.sparetimedevs.ami.scoresynth.banana.AsyncReply
import com.sparetimedevs.ami.scoresynth.banana.handleSuccessWithAsyncReply
import com.sparetimedevs.ami.scoresynth.handler.handleDomainError
import com.sparetimedevs.ami.scoresynth.handler.handleSystemFailure
import com.sparetimedevs.ami.scoresynth.mapLeftToDomainError
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationId
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestrator
import com.sparetimedevs.ami.scoresynth.resolve
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream

@RestController
@RequestMapping("/audio")
class AudioController(
    private val jsonParser: Json,
    private val synthesizer: AudioSynthesizer,
    private val orchestrator: Orchestrator<InputFile, OutputFile>,
) {
    private val logger: Logger = LoggerFactory.getLogger(AudioController::class.java)

    // curl -v -o output-123.wav -XPOST 'localhost:8080/audio/synthesize?inputFileFormat=midi' \
    // --form 'file=@"/Users/joram/temp/heigh_ho_nobody_home.mid"' \
    // --header 'Content-Type: multipart/form-data'
    @PostMapping("/synthesize", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun synthesizeAudio(
        @RequestParam("file") inputFile: MultipartFile,
        @RequestParam(value = "inputFileFormat", required = false) inputFileFormat: String?,
    ): ResponseEntity<*> =
        resolve(
            f = {
                // Infer file type from Content-Type, query parameter, or file content
                val format = inputFileFormat ?: detectFormat(inputFile)

                val wavData: Either<DomainError, ByteArray> =
                    when (format.lowercase()) {
                        "midi" -> synthesizer.transformMidiToWav(inputFile.inputStream)
                        "score" -> synthesizeScoreToWav(inputFile.bytes).right()
                        else ->
                            InvalidFileFormatError("Unsupported file format: $format").left()
                    }

                wavData
            },
            success = { wavData ->
                handleSuccessHandler(wavData)
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

    // curl -v -XPOST 'localhost:8080/audio/synthesize/async?inputFileFormat=midi' \
    // --form 'file=@"/Users/joram/temp/heigh_ho_nobody_home.mid"' \
    // --header 'Content-Type: multipart/form-data'
    @PostMapping("/synthesize/async", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun synthesizeAudioAsync(
        @RequestParam("file") inputFile: MultipartFile,
        @RequestParam(value = "inputFileFormat", required = false) inputFileFormat: String?,
    ): ResponseEntity<*> =
        resolve(
            f = {
                // Infer file type from Content-Type, query parameter, or file content
                val format = inputFileFormat ?: detectFormat(inputFile)

//                val wavData: Either<DomainError, ByteArray> =
//                    when (format.lowercase()) {
//                        "midi" -> synthesizer.transformMidiToWav(inputFile.inputStream)
//                        "score" -> synthesizeScoreToWav(inputFile.bytes).right()
//                        else ->
//                            InvalidFileFormatError("Unsupported file format: $format").left()
//                    }
//
//                wavData

                val inputFile = InputFile("some/input.file")
                orchestrator
                    .registerOrchestration(inputFile)
                    .mapLeftToDomainError()
            },
            success = { orchestration ->
                handleSuccessWithAsyncReply(jsonParser, orchestration.id.toAsyncReply())
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

    private fun OrchestrationId.toAsyncReply() = AsyncReply(replyUrl = "http://localhost:8080/replies/${this.value}")

    private suspend inline fun handleSuccessHandler(wavData: ByteArray): Either<Throwable, ResponseEntity<ByteArray>> {
        // Return WAV file response
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.parseMediaType("audio/wav")
                setContentDispositionFormData("attachment", "output.wav")
            }
        return ResponseEntity(wavData, headers, HttpStatus.OK).right()
    }

    private fun detectFormat(file: MultipartFile): String {
        // TODO: Implement logic to inspect file contents or Content-Type
        return if (file.originalFilename?.endsWith(".midi") == true) "midi" else "score"
    }

    private fun synthesizeScoreToWav(scoreBytes: ByteArray): ByteArray {
        // TODO: Score-to-WAV conversion logic
        return loadMidiData() // Stub
    }
}

fun loadMidiData(): ByteArray {
    val outputStream = ByteArrayOutputStream()

    // Write the MIDI header (standard format 0, one track)
    outputStream.write(byteArrayOf(0x4D.toByte(), 0x54.toByte(), 0x68.toByte(), 0x64.toByte())) // "MThd"
    outputStream.write(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x06.toByte())) // Header length
    outputStream.write(byteArrayOf(0x00.toByte(), 0x00.toByte())) // Format 0
    outputStream.write(byteArrayOf(0x00.toByte(), 0x01.toByte())) // One track
    outputStream.write(byteArrayOf(0x00.toByte(), 0x60.toByte())) // Ticks per quarter note (96)

    // Write the track chunk header
    outputStream.write(byteArrayOf(0x4D.toByte(), 0x54.toByte(), 0x72.toByte(), 0x6B.toByte())) // "MTrk"
    val trackData = ByteArrayOutputStream()

    // Add a "note on" event (middle C, velocity 64)
    // Delta time: 0, Note On, Note: 60, Velocity: 64
    trackData.write(byteArrayOf(0x00.toByte(), 0x90.toByte(), 0x3C.toByte(), 0x40.toByte()))

    // Add a "note off" event (middle C, velocity 64)
    // Delta time: 96, Note Off, Note: 60, Velocity: 64
    trackData.write(byteArrayOf(0x60.toByte(), 0x80.toByte(), 0x3C.toByte(), 0x40.toByte()))

    // End of track
    trackData.write(byteArrayOf(0x00.toByte(), 0xFF.toByte(), 0x2F.toByte(), 0x00.toByte())) // End of track meta event

    // Write track chunk length and data
    val trackBytes = trackData.toByteArray()
    outputStream.write(
        byteArrayOf(
            (trackBytes.size shr 24).toByte(),
            (trackBytes.size shr 16).toByte(),
            (trackBytes.size shr 8).toByte(),
            trackBytes.size.toByte(),
        ),
    )
    outputStream.write(trackBytes)

    return outputStream.toByteArray()
}
