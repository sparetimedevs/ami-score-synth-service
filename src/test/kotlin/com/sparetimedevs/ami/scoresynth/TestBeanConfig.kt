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
import com.sparetimedevs.ami.scoresynth.audio.OutputFile
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationRepository
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStepRepository
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestrator
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestratorJobScheduler
import com.sparetimedevs.ami.scoresynth.orchestration.impl.OrchestrationRepositoryImpl
import com.sparetimedevs.ami.scoresynth.orchestration.impl.OrchestrationStepRepositoryImpl
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.time.Clock

@Configuration
@PropertySource("classpath:default.properties")
@PropertySource(value = ["file:local.properties"], ignoreResourceNotFound = true)
class TestBeanConfig {
    @Bean
    fun jsonParser(): Json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

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
    fun audioSynthesisOrchestrator(
        @Value("\${spring.datasource.url}") dataSourceUrl: String,
        @Value("\${spring.datasource.username}") dataSourceUsername: String,
        @Value("\${spring.datasource.password}") dataSourcePassword: String,
    ): Orchestrator<InputFile, OutputFile> {
        // TODO should probably use mocks for repositories etc.
        // Currently, the tests only work when the database is running.
        val dataSourceProperties =
            DataSourceProperties(
                url = dataSourceUrl,
                username = dataSourceUsername,
                password = dataSourcePassword,
            )

        val dataSource = createDataSource(dataSourceProperties)
        val clock: Clock = Clock.systemUTC()
        val jsonParser: Json = Json
        // TODO the should not be a reason to create two orchestrationRepositories
        val orchestrationRepository: OrchestrationRepository =
            OrchestrationRepositoryImpl(dataSource, clock, jsonParser)
        val orchestrationStepRepository: OrchestrationStepRepository =
            OrchestrationStepRepositoryImpl(dataSource, jsonParser)

        val audioSynthesisOrchestrator =
            AudioSynthesisOrchestrator(
                orchestrationRepository,
                orchestrationStepRepository,
                FileHandler(),
                AudioSynthesizer(FluidSynthClientImpl("fluidsynthPath", "soundFontPath")),
            )
        val audioSynthesisOrchestratorJobScheduler =
            OrchestratorJobScheduler(audioSynthesisOrchestrator, InputFile::class)

        return audioSynthesisOrchestrator
    }
}
