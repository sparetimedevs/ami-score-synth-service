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

package com.sparetimedevs.ami.scoresynth.orchestration.util

import arrow.core.Either
import com.sparetimedevs.ami.scoresynth.orchestration.InternalOrchestrationStepFailure
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.KType

class CreateTypeSafelyKtTest {
    @Test
    fun `createTypeSafely should return type of Something`() {
        val result: Either<InternalOrchestrationStepFailure, KType> = createTypeSafely<Something>()
        result.fold(
            { e -> fail("Expected Right but was Left and Left was: $e") },
            { it.toString() shouldBe "com.sparetimedevs.ami.scoresynth.orchestration.util.Something" },
        )
    }

    @Test
    fun `createTypeSafely should return type of List of Something`() {
        val result: Either<InternalOrchestrationStepFailure, KType> = createTypeSafely<List<Something>>()
        result.fold(
            { e -> fail("Expected Right but was Left and Left was: $e") },
            {
                it.toString() shouldBe
                    "kotlin.collections.List<com.sparetimedevs.ami.scoresynth.orchestration.util.Something>"
            },
        )
    }

    @Test
    fun `KClass#createTypeSafely should return type of Something`() {
        val type: KClass<*> = Something::class
        val result: Either<InternalOrchestrationStepFailure, KType> = type.createTypeSafely()
        result.fold(
            { e -> fail("Expected Right but was Left and Left was: $e") },
            { it.toString() shouldBe "com.sparetimedevs.ami.scoresynth.orchestration.util.Something" },
        )
    }
}

@Serializable
data class Something(
    val test1: String,
    val test2: List<Int>,
    val test3: Int,
)
