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

package com.sparetimedevs.ami.scoresynth.healthcheck

import com.sparetimedevs.ami.scoresynth.TestBeanConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(HealthCheckController::class)
@Import(TestBeanConfig::class)
class HealthCheckControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `liveness endpoint should return status ALIVE`(): Unit =
        runBlocking {
            val mvcResult =
                mockMvc
                    .get("/health/liveness") { contentType = MediaType.APPLICATION_JSON }
                    .andReturn()

            mockMvc
                .perform(MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""{ "status": "ALIVE" }"""))
        }

    @Test
    fun `readiness endpoint should return status UP`(): Unit =
        runBlocking {
            val mvcResult =
                mockMvc
                    .get("/health/readiness") { contentType = MediaType.APPLICATION_JSON }
                    .andReturn()

            mockMvc
                .perform(MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(MockMvcResultMatchers.status().isServiceUnavailable())
                .andExpect(MockMvcResultMatchers.content().json("""{ "status": "DOWN" }"""))
        }
}
