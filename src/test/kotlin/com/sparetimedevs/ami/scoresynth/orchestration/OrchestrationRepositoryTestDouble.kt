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

import arrow.core.Either
import kotlinx.serialization.Serializable
import test.util.createOrchestrationIdFromStringUnsafe
import java.util.UUID
import kotlin.reflect.KType

class OrchestrationRepositoryTestDouble : OrchestrationRepository {
    override suspend fun <Input, Result> create(
        orchestrationName: String,
        input: Input,
        inputType: KType,
    ): Either<OrchestrationError, Orchestration<Input, Result>> {
        println("Create record in repo: orchestration name: $orchestrationName, orchestration state: created")

        return Either.Right(
            Orchestration<Input, Result>(
                OrchestrationId(UUID.randomUUID()),
                "PizzaOrchestrator",
                PizzaOrder(dough = "test pizza dough", topping = "test pizza salami") as Input,
                "created",
                null,
            ),
        )
    }

    override suspend fun <Input, Result> findById(
        id: OrchestrationId,
        inputType: KType,
        resultType: KType,
    ): Either<OrchestrationError, Orchestration<Input, Result>> =
        when (id) {
            createOrchestrationIdFromStringUnsafe("92d0ec75-fe55-4d7f-a4bb-ae62db9702b7") -> {
                // The tests using this particular ID should expect back
                Either.Right(
                    Orchestration(
                        id,
                        "PizzaOrchestrator",
                        PizzaOrder(dough = "test pizza dough", topping = "test pizza salami") as Input,
                        "created",
                        null,
                    ),
                )
            }

            createOrchestrationIdFromStringUnsafe("34df29ac-3840-473c-85bd-5ad9447d54c7") -> {
                // The tests using this particular ID should expect back
                Either.Right(
                    Orchestration(
                        id,
                        "PizzaOrchestrator",
                        PizzaOrder(dough = "test pizza dough", topping = "test pizza salami") as Input,
                        "not_completed",
                        null,
                    ),
                )
            }

            createOrchestrationIdFromStringUnsafe("7acdd1d6-e170-4ba6-95e7-bafee1716a65") -> {
                // The tests using this particular ID should expect back
                Either.Right(
                    Orchestration(
                        id = id,
                        name = "PizzaOrchestrator",
                        input = PizzaOrder(dough = "test pizza dough", topping = "test pizza salami") as Input,
                        state = "completed",
                        result =
                            Pizza(
                                bottom = PizzaBottom(dough = Dough("test pizza dough")),
                                sauce = PizzaSauce(sauceType = SauceType.SWEET),
                                toppings = listOf(Topping("test pizza salami"), Topping("test pizza mozzarella")),
                            ) as Result,
                    ),
                )
            }

            else -> Either.Right(Orchestration(id, "TestOrchestrator", "TODO" as Input, "completed", null))
        }

    override suspend fun <Input, Result> getNext(
        orchestrationName: String,
        inputType: KType,
    ): Either<OrchestrationError, List<Orchestration<Input, Result>>> = Either.Right(emptyList())

    override suspend fun <A> update(
        orchestrationId: OrchestrationId,
        orchestrationName: String,
        orchestrationResult: Either<OrchestrationError, A>,
        type: KType,
    ): Either<OrchestrationError, A> {
        val state =
            orchestrationResult.fold(
                {
                    "not_completed"
                },
                {
                    "completed"
                },
            )
        println("Update record in repo: orchestrator name: $orchestrationName, orchestrator state: $state")
        return orchestrationResult
    }
}

@Serializable
data class PizzaOrder(
    val dough: String,
    val topping: String,
)

@Serializable
@JvmInline
value class Dough(
    val value: String,
)

@Serializable
data class PizzaBottom(
    val dough: Dough,
)

@Serializable
enum class SauceType {
    SWEET,
    MILD,
    HOT,
}

@Serializable
data class PizzaSauce(
    val sauceType: SauceType,
)

@Serializable
@JvmInline
value class Topping(
    val value: String,
)

@Serializable
data class Pizza(
    val bottom: PizzaBottom,
    val sauce: PizzaSauce,
    val toppings: List<Topping>,
)
