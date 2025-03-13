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
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStepRepository
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestrator
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestratorJobScheduler
import com.sparetimedevs.ami.scoresynth.orchestration.impl.OrchestrationRepositoryImpl
import com.sparetimedevs.ami.scoresynth.orchestration.impl.OrchestrationStepRepositoryImpl
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.time.Clock
import javax.sql.DataSource

@Configuration
@PropertySource("classpath:default.properties")
@PropertySource(value = ["file:local.properties"], ignoreResourceNotFound = true)
class BeanConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun jsonParser(): Json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    @Bean
    fun dataSource(
        @Value("\${spring.datasource.url}") dataSourceUrl: String,
        @Value("\${spring.datasource.username}") dataSourceUsername: String,
        @Value("\${spring.datasource.password}") dataSourcePassword: String,
    ): DataSource {
        val dataSourceProperties =
            DataSourceProperties(
                url = dataSourceUrl,
                username = dataSourceUsername,
                password = dataSourcePassword,
            )
        return createDataSource(dataSourceProperties)
    }

    @Bean
    fun orchestrationRepository(
        dataSource: DataSource,
        jsonParser: Json,
        clock: Clock,
    ): OrchestrationRepository = OrchestrationRepositoryImpl(dataSource, clock, jsonParser)

    @Bean
    fun orchestrationStepRepository(
        dataSource: DataSource,
        jsonParser: Json,
    ): OrchestrationStepRepository = OrchestrationStepRepositoryImpl(dataSource, jsonParser)

    @Bean
    fun audioSynthesizer(
        @Value("\${fluidsynth.path}") fluidSynthPath: String,
        @Value("\${soundfont.path}") soundFontPath: String,
    ): AudioSynthesizer {
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

data class DataSourceProperties(
    val url: String,
    val username: String,
    val password: String,
)

fun createDataSource(properties: DataSourceProperties): DataSource {
    val config = HikariConfig()

    config.jdbcUrl = properties.url
    config.username = properties.username
    config.password = properties.password
    config.driverClassName = "com.mysql.cj.jdbc.Driver"
    return HikariDataSource(config)
}
