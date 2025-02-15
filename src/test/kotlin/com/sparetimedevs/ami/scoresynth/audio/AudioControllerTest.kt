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

import com.sparetimedevs.ami.scoresynth.TestBeanConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.io.InputStream

@WebMvcTest(AudioController::class)
@Import(TestBeanConfig::class)
class AudioControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `audio endpoint should return success response for valid MIDI file input`(): Unit =
        runBlocking {
            val fileContent: InputStream =
                this::class.java.classLoader
                    .getResource(
                        "heigh_ho_nobody_home.mid",
                    )!!
                    .openStream()!!
            val multipartFile = MockMultipartFile("file", "heigh_ho_nobody_home.mid", "audio/midi", fileContent)

            val mvcResult =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .multipart("/audio/synthesize")
                            .file(multipartFile)
                            .param("inputFileFormat", "midi")
                            .contentType(MediaType.MULTIPART_FORM_DATA),
                    ).andReturn()

            mockMvc
                .perform(MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("audio/wav"))
                .andExpect { result ->
                    val bytes = result.response.contentAsByteArray
                    assert(bytes.isNotEmpty()) { "Response content is empty" }
                }
        }

    @Test
    fun `audio endpoint should return error response for unsupported file format`(): Unit =
        runBlocking {
            val fileContent = "sample-unsupported-data".toByteArray()
            val multipartFile = MockMultipartFile("file", "unsupported_file.txt", "text/plain", fileContent)

            val mvcResult =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .multipart("/audio/synthesize")
                            .file(multipartFile)
                            .param("inputFileFormat", "txt")
                            .contentType(MediaType.MULTIPART_FORM_DATA),
                    ).andReturn()

            mockMvc
                .perform(MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(
                    MockMvcResultMatchers.content().json("""{ "errorMessage": "Unsupported file format: txt" }"""),
                )
        }

    @Test
    fun `audio endpoint should return error response for missing file parameter`(): Unit =
        runBlocking {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .multipart("/audio/synthesize")
                        .param("inputFileFormat", "midi")
                        .contentType(MediaType.MULTIPART_FORM_DATA),
                ).andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().string(""))
        }
}
