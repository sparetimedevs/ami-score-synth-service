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

import java.io.File

class AudioSynthesizer(
    private val fluidSynthPath: String,
    private val soundFontPath: String,
) {
    /**
     * Transforms a MIDI file into a WAV file using FluidSynth.
     *
     * @param midiFilePath Path to the input MIDI file on the file system.
     * @param wavFilePath Path where the generated WAV file should be stored.
     * @throws IllegalArgumentException If the input paths are invalid.
     * @throws RuntimeException If the FluidSynth process fails.
     */
    fun transformMidiToWav(
        midiFilePath: String,
        wavFilePath: String,
    ) {
        // Validate file paths
        validateFileExists(midiFilePath, "MIDI file")
        validateFileExists(soundFontPath, "SoundFont file")
        validateParentDirectoryExists(wavFilePath, "Output WAV file")

        // Construct the FluidSynth command
        val command =
            listOf(
                fluidSynthPath,
                "-ni", // Non-interactive mode
                "-F",
                wavFilePath, // Output file
                soundFontPath, // SoundFont file
                midiFilePath, // MIDI file
            )

        // Execute the command
        val process =
            ProcessBuilder(command)
                .redirectErrorStream(true) // Redirect error output to standard output
                .start()

        // Read and log the process output
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()

        // Check the exit code
        if (process.exitValue() != 0) {
            throw RuntimeException("FluidSynth failed: $output")
        }

        println("Successfully generated WAV file: $wavFilePath")
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

    private fun validateParentDirectoryExists(
        filePath: String,
        fileType: String,
    ) {
        val parentDir = File(filePath).parentFile
        if (parentDir == null || !parentDir.exists()) {
            throw IllegalArgumentException("Parent directory for $fileType does not exist: $filePath")
        }
    }
}
