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
import kotlin.reflect.KType

interface OrchestrationRepository {
    suspend fun <Input, Result> create(
        orchestrationName: String,
        input: Input,
        inputType: KType,
    ): Either<OrchestrationError, Orchestration<Input, Result>>

    suspend fun <Input, Result> findById(
        id: OrchestrationId,
        inputType: KType,
        resultType: KType,
    ): Either<OrchestrationError, Orchestration<Input, Result>>

    suspend fun <Input, Result> getNext(
        orchestrationName: String,
        inputType: KType,
    ): Either<OrchestrationError, List<Orchestration<Input, Result>>>

    suspend fun <Result> update(
        orchestrationId: OrchestrationId,
        orchestrationName: String,
        orchestrationResult: Either<OrchestrationError, Result>,
        resultType: KType,
    ): Either<OrchestrationError, Result>
}
