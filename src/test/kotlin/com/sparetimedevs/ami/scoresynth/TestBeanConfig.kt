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

import com.sparetimedevs.ami.scoresynth.audio.AudioSynthesisOrchestrator
import com.sparetimedevs.ami.scoresynth.audio.AudioSynthesizer
import com.sparetimedevs.ami.scoresynth.audio.FileHandler
import com.sparetimedevs.ami.scoresynth.audio.InputFile
import com.sparetimedevs.ami.scoresynth.orchestration.MyOrchestrator
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationRepository
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationRepositoryTestDouble
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStepRepository
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStepRepositoryTestDouble
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestrator
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestratorJobScheduler
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Configuration
@PropertySource("classpath:default.properties")
@PropertySource(value = ["file:local.properties"], ignoreResourceNotFound = true)
class TestBeanConfig {
    @Bean
    fun clock(): Clock = Clock.fixed(Instant.parse("2025-04-12T22:28:41Z"), ZoneId.of("UTC"))

    @Bean
    fun jsonParser(): Json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    @Bean
    fun orchestrationRepository(): OrchestrationRepository = OrchestrationRepositoryTestDouble()

    @Bean
    fun orchestrationStepRepository(jsonParser: Json): OrchestrationStepRepository =
        OrchestrationStepRepositoryTestDouble(jsonParser)

    @Bean
    fun audioSynthesizer(
        @Value("\${fluidsynth.path}") fluidSynthPath: String,
    ): AudioSynthesizer {
        val soundFontPath =
            this::class.java.classLoader
                .getResource("simple-soundfont.sf2")!!
                .path
        val fluidSynthClient = FluidSynthClientImpl(fluidSynthPath, soundFontPath)
        return AudioSynthesizer(fluidSynthClient)
    }

    @Bean
    fun myOrchestrator(
        orchestrationRepository: OrchestrationRepository,
        orchestrationStepRepository: OrchestrationStepRepository,
    ): Orchestrator<String, String> {
        val myOrchestrator = MyOrchestrator(orchestrationRepository, orchestrationStepRepository)
        val myOrchestratorJobScheduler = OrchestratorJobScheduler(myOrchestrator, String::class)

        return myOrchestrator
    }

    @Bean
    fun fileHandler(): FileHandler = FileHandler()

    @Bean
    fun audioSynthesisOrchestrator(
        orchestrationRepository: OrchestrationRepository,
        orchestrationStepRepository: OrchestrationStepRepository,
        fileHandler: FileHandler,
        audioSynthesizer: AudioSynthesizer,
    ): AudioSynthesisOrchestrator {
        val audioSynthesisOrchestrator =
            AudioSynthesisOrchestrator(
                orchestrationRepository,
                orchestrationStepRepository,
                fileHandler,
                audioSynthesizer,
            )
        val audioSynthesisOrchestratorJobScheduler =
            OrchestratorJobScheduler(audioSynthesisOrchestrator, InputFile::class)

        return audioSynthesisOrchestrator
    }
}
