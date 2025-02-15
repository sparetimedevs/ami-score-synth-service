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

package com.sparetimedevs.ami.scoresynth.orchestration

import io.kotest.assertions.fail
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.util.UUID

class MyOrchestratorTest {
    val jsonParser: Json = Json
    val orchestrationRepository: OrchestrationRepository = OrchestrationRepositoryTestDouble()
    val orchestrationStepRepository: OrchestrationStepRepository = OrchestrationStepRepositoryTestDouble(jsonParser)
    val orchestrator = MyOrchestrator(orchestrationRepository, orchestrationStepRepository)

    @Test
    fun `should work`() =
        runTest {
            val orchestrationId: OrchestrationId =
                OrchestrationId(UUID.fromString("003b319c-aea8-446f-ab6f-7dc1f85faff8"))

            val orchestration: Orchestration<String, String> =
                Orchestration<String, String>(
                    orchestrationId,
                    "TestOrchestrator",
                    "TODO",
                    "completed",
                    null,
                )

            val result = orchestrator.orchestrate(orchestration)
            result.fold(
                {
                    fail("Test case should yield a Right.")
                },
                {
                    it shouldContain "And this is my third durable orchestration step"
                },
            )
        }
}
