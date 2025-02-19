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
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.sparetimedevs.ami.scoresynth.orchestration.continuations.Effect
import com.sparetimedevs.ami.scoresynth.orchestration.continuations.generic.DelimitedScope
import com.sparetimedevs.ami.scoresynth.orchestration.util.createTypeSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * This is not a new idea. This is piggybacking off of Azure Durable Functions.
 *
 * Concepts to oblige to;
 * The implementation should promote local reasoning.
 * Backpressure is needed.
 */
abstract class Orchestrator<Input : Any, Result : Any>(
    protected val orchestrationRepository: OrchestrationRepository,
    protected val orchestrationStepRepository: OrchestrationStepRepository,
    private val inputType: KClass<Input>,
    private val resultType: KClass<Result>,
) {
    protected val orchestrationName: String = this::class.java.simpleName

    suspend fun registerOrchestration(input: Input): Either<OrchestrationError, Orchestration<Input, Result>> =
        inputType
            .createTypeSafely()
            .flatMap { inputType ->
                orchestrationRepository
                    .create<Input, Result>(orchestrationName, input, inputType)
                    .onRight { orchestration ->
                        // Launch the first orchestrate
                        // Might want to change this.
                        GlobalScope.launch(Dispatchers.Default) {
                            orchestrate(orchestration)
                        }
                    }
            }

    abstract suspend fun orchestrate(
        registeredOrchestration: Orchestration<Input, Result>,
    ): Either<OrchestrationError, Result>

    suspend fun orchestrationState(
        orchestrationId: OrchestrationId,
    ): Either<OrchestrationError, Orchestration<Input, Result>> =
        inputType
            .createTypeSafely()
            .flatMap { inputType ->
                resultType.createTypeSafely().map { inputType to it }
            }.flatMap { (inputType, resultType) ->
                orchestrationRepository.findById(orchestrationId, inputType, resultType)
            }

    protected abstract class OrchestrationEitherEffect2<A> : Effect<Either<OrchestrationError, A>> {
        abstract val orchestrationId: OrchestrationId

        abstract val orchestrationStepRepository: OrchestrationStepRepository

        suspend inline fun <reified B> orchestrationStep(
            stepName: String,
            noinline step: suspend () -> Either<OrchestrationStepFailure, B>,
        ): B {
            val persistedStep: Either<OrchestrationStepFailure, B> =
                persistOrchestrationStep(orchestrationId, stepName, step)

            return when (persistedStep) {
                is Right -> persistedStep.value
                is Left -> control().shift(persistedStep)
            }
        }

        protected suspend inline fun <reified A> persistOrchestrationStep(
            orchestrationId: OrchestrationId,
            stepName: String,
            step: suspend () -> Either<OrchestrationStepFailure, A>,
        ): Either<OrchestrationStepFailure, A> =
            createTypeSafely<A>()
                .flatMap { type ->
                    orchestrationStepRepository
                        .read<A>(stepName, orchestrationId, type)
                        .fold(
                            { error ->
                                when (error) {
                                    is ContinuationToken -> {
                                        val stepResult = step()
                                        orchestrationStepRepository.create(stepName, orchestrationId, stepResult, type)
                                    }

                                    else -> error.left()
                                }
                            },
                            {
                                it.right()
                            },
                        )
                }
    }

    /**
     * This is very much inspired by either block implementation, see: /arrow/core/computations/EitherEffect.class
     */
    protected suspend inline fun <reified A> OrchestrationId.orchestration(
        crossinline c: suspend OrchestrationEitherEffect2<A>.() -> A,
    ): Either<OrchestrationError, A> {
        val orchestrationResult =
            Effect.suspended(eff = { delimitedScope: DelimitedScope<Either<OrchestrationError, A>> ->
                OrchestrationEitherEffect2Impl(
                    delimitedScope,
                    this,
                    orchestrationStepRepository,
                )
            }, f = c, just = { it.right() })
        return createTypeSafely<A>()
            .flatMap { type ->
                orchestrationRepository.update(this, orchestrationName, orchestrationResult, type)
            }
    }

    protected class OrchestrationEitherEffect2Impl<A>(
        private val delimitedScope: DelimitedScope<Either<OrchestrationError, A>>,
        override val orchestrationId: OrchestrationId,
        override val orchestrationStepRepository: OrchestrationStepRepository,
    ) : OrchestrationEitherEffect2<A>() {
        override fun control(): DelimitedScope<Either<OrchestrationError, A>> = delimitedScope
    }

    suspend fun job(inputType: KType) {
        // We need some kind of job which invokes the orchestrate method on a regular interval (every 30 seconds for instance).
        // To determine for which orchestrations the orchestrate method should be called, the database needs to be queried.
        // In this query, an exponential backoff could be incorporated.
        // Something like: there are 5 tries. Then take all orchestrations with one try.
        // If room left, take 10 (? or more) with 2 tries which are older than 15 seconds.
        // If room left, take 10 (? or more) with 3 tries which are older than 30 seconds.
        // If room left, take 10 (? or more) with 4 tries which are older than 60 seconds.
        // If room left, take 10 (? or more) with 5 tries which are older than 120 seconds.
        // Probably this needs to be tuned accordingly. And maybe even configurable per use case.
        // Also think about backpressure. Only take work if the system can handle the work.
        // Maybe the solution is to take oldest always first. Then if the system gets more requests, it will just take a little longer to process them, but eventually the system will go through them all.

        orchestrationRepository
            .getNext<Input, Result>(orchestrationName, inputType)
            .flatMap { orchestrations -> orchestrateAll(orchestrations) }
    }

    private suspend fun orchestrateAll(
        orchestrations: List<Orchestration<Input, Result>>,
    ): Either<OrchestrationError, Unit> =
        Either
            .catch {
                orchestrations.forEach { orchestration ->
                    orchestrate(orchestration)
                }
            }.mapLeft {
                // TODO this is now silently failing. Some visibility when we are in this state might be good. logger?
                UnknownOrchestrationError(it.message ?: "An unknown exception occurred.")
            }
}

class MyOrchestrator(
    orchestrationRepository: OrchestrationRepository,
    orchestrationStepRepository: OrchestrationStepRepository,
) : Orchestrator<String, String>(orchestrationRepository, orchestrationStepRepository, String::class, String::class) {
    override suspend fun orchestrate(
        registeredOrchestration: Orchestration<String, String>,
    ): Either<OrchestrationError, String> =
        registeredOrchestration.id.orchestration<String> {
            // By this time, the input should be validated
            val s = "input"
            val r1: String =
                orchestrationStep(
                    "myDurableOrchestrationStep1",
                ) { Right("$s \nAnd this is my first durable orchestration step") }
            val r2 =
                orchestrationStep(
                    "mySecondDurableOrchestrationStep",
                ) { Right("$s \nAnd this is my second durable orchestration step") }
            val r3 =
                orchestrationStep("mySecondSecondDurableOrchestrationStep") { Right((1337 * 2) + r2.length) }
            // val r444 = orchestrationStep(Left(WaitOnStep("the name of the function...")))
            val r5 =
                orchestrationStep(
                    "my123DurableOrchestrationStep",
                ) { Right((1337 * 2) + r3) }
            val r6 =
                orchestrationStep(
                    "myDurableOrchestrationStep3",
                ) { Right("$r5 \nAnd this is my third durable orchestration step") }

            r6
        }
}
