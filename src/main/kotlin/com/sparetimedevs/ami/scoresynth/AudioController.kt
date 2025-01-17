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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RestController
class AudioController(
    private val jsonParser: Json,
) {
    private val logger: Logger = LoggerFactory.getLogger(AudioController::class.java)

    @PostMapping("/audio", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun synthesizeAudio(
        @RequestParam("file") inputFile: MultipartFile,
        @RequestParam(value = "fileFormat", required = false) fileFormat: String?,
    ): ResponseEntity<ByteArray> {
        // Infer file type from Content-Type, query parameter, or file content
        val format = fileFormat ?: detectFormat(inputFile)

        val wavData =
            when (format.lowercase()) {
                "midi" -> synthesizeMidiToWav(inputFile.bytes)
                "score" -> synthesizeScoreToWav(inputFile.bytes)
                else -> throw IllegalArgumentException("Unsupported file format: $format")
            }

        // Return WAV file response
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.parseMediaType("audio/wav")
                setContentDispositionFormData("attachment", "output.wav")
            }
        return ResponseEntity(wavData, headers, HttpStatus.OK)
    }

    private fun detectFormat(file: MultipartFile): String {
        // TODO: Implement logic to inspect file contents or Content-Type
        return if (file.originalFilename?.endsWith(".midi") == true) "midi" else "score"
    }

    private fun synthesizeMidiToWav(midiBytes: ByteArray): ByteArray {
        // TODO: MIDI-to-WAV conversion logic
        return byteArrayOf() // Stub
    }

    private fun synthesizeScoreToWav(scoreBytes: ByteArray): ByteArray {
        // TODO: Score-to-WAV conversion logic
        return byteArrayOf() // Stub
    }

    // curl -v -XPOST localhost:8080/midi-to-wav -H "Content-Type: application/json" -d '{"midi":"wav"}'
    @PostMapping("/midi-to-wav", produces = ["application/json"])
    suspend fun midiToWav(
        @RequestBody input: Input,
    ): ResponseEntity<String> =
        resolve(jsonParser, logger) {
            val fluidSynthPath = "/usr/local/bin/fluidsynth" // Path to FluidSynth executable
            val soundFontPath = "/Users/joram/temp/soundfont.sf2" // Path to your SoundFont file

            val synthesizer = AudioSynthesizer(fluidSynthPath, soundFontPath)

            try {
                // Load MIDI data
                val midiData = loadMidiData()
                val midiStream = ByteArrayInputStream(midiData)

                // Transform MIDI to WAV
                val wavData = synthesizer.transformMidiToWav(midiStream)

                println("Generated WAV data size: ${wavData.size} bytes")
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }

            input
                .validateInput()
                .map { a -> Response("The process is completed for: $a") }
        }
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
