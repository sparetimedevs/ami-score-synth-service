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

import com.sparetimedevs.ami.scoresynth.audio.AudioSynthesizer
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
    fun jsonParser(): Json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

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
        @Value("\${spring.datasource.url}") dataSourceUrl: String,
        @Value("\${spring.datasource.username}") dataSourceUsername: String,
        @Value("\${spring.datasource.password}") dataSourcePassword: String,
    ): Orchestrator<String, String> {
        val dataSourceProperties =
            DataSourceProperties(
                url = dataSourceUrl,
                username = dataSourceUsername,
                password = dataSourcePassword,
            )

        val dataSource = createDataSource(dataSourceProperties)
        val clock: Clock = Clock.systemUTC()
        val jsonParser: Json = Json
        val orchestrationRepository: OrchestrationRepository =
            OrchestrationRepositoryImpl(dataSource, clock, jsonParser)
        val orchestrationStepRepository: OrchestrationStepRepository =
            OrchestrationStepRepositoryImpl(dataSource, jsonParser)
        val myOrchestrator = MyOrchestrator(orchestrationRepository, orchestrationStepRepository)
        val myOrchestratorJobScheduler = OrchestratorJobScheduler(myOrchestrator, String::class)

        return myOrchestrator
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
