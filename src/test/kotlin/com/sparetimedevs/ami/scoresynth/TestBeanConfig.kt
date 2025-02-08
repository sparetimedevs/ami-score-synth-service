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

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestBeanConfig {
    @Bean
    fun jsonParser(): Json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    @Bean
    fun audioSynthesizer(): AudioSynthesizer {
        val fluidSynthPath = "fluidsynth" // Path to FluidSynth executable
        val soundFontPath =
            this::class.java.classLoader
                .getResource(
                    "soundfont.sf2",
                )!!
                .path
        val fluidSynthClient = FluidSynthClientImpl(fluidSynthPath, soundFontPath)
        return AudioSynthesizer(fluidSynthClient)
    }
}
