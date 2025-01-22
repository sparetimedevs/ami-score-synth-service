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

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AudioController::class)
@Import(JsonConfig::class)
class AudioControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    // TODO write tests for endpoint 'localhost:8080/audio?inputFileFormat=midi'

    @Test
    fun `should return success response for valid input`(): Unit =
        runBlocking {
            val validInput = """{ "midi": "sample-midi-data" }"""
            val expectedResponse = """{"message":"Generated WAV data size: 715308 bytes"}"""

            // Perform the initial request and retrieve the async result
            val mvcResult =
                mockMvc
                    .post("/midi-to-wav") {
                        contentType = MediaType.APPLICATION_JSON
                        content = validInput
                    }.andReturn()

            // Wait for the async result to complete and dispatch it
            mockMvc
                .perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse))
        }
}
