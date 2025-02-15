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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

class OrchestrationStepRepositoryTestDouble(
    private val jsonParser: Json,
) : OrchestrationStepRepository {
    override suspend fun <A> read(
        stepName: String,
        orchestrationId: OrchestrationId,
        type: KType,
    ): Either<OrchestrationStepFailure, A> {
        // This is the method that checks the repo, if it doesn't find any result, it will allow continuation?
        val theRecord =
            "please continue, cause there was no record of this step in the database for orchestrationId $orchestrationId"
        println(theRecord)
        return Either.Left(ContinuationToken(theRecord))
    }

    override suspend fun <A> create(
        stepName: String,
        orchestrationId: OrchestrationId,
        stepResult: Either<OrchestrationStepFailure, A>,
        type: KType,
    ): Either<OrchestrationStepFailure, A> {
        val result =
            stepResult.fold(
                {
                    StepResult("not_completed", it, null)
                },
                {
                    val serializer: KSerializer<A> = jsonParser.serializersModule.serializer(type) as KSerializer<A>
                    val stepSuccessJson = jsonParser.encodeToString(serializer, it)
                    StepResult("completed", null, stepSuccessJson)
                },
            )

        println(
            "Create record in repo: step name: $stepName, step state: ${result.state}, step success ${result.successJson}",
        )
        return stepResult
    }

    data class StepResult(
        val state: String,
        val failure: OrchestrationStepFailure?,
        val successJson: String?,
    )
}
