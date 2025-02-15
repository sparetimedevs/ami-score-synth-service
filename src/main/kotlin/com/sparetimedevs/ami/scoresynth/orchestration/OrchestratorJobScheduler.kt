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

import arrow.resilience.Schedule
import com.sparetimedevs.ami.scoresynth.orchestration.util.createTypeSafely
import kotlinx.coroutines.delay
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

class OrchestratorJobScheduler(
    private val orchestrator: Orchestrator<*, *>,
    private val inputType: KClass<*>,
) {
    init {
        suspend {
            // Start the first execution of the job a little later
            // so that we can be more sure that the application is started up.
            delay(FORTYFIVE_SECONDS)
            scheduleJob()
        }.startCoroutine(
            Continuation(EmptyCoroutineContext) { result: Result<Unit> ->
                if (result.isSuccess) {
                    println("Stopping the job scheduler")
                } else {
                    println("The job scheduler has been stopped because of a failure; ${result.exceptionOrNull()}")
                }
            },
        )
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun scheduleJob() {
        Schedule.spaced<Unit>(THIRTY_SECONDS).repeat {
            inputType
                .createTypeSafely()
                .fold(
                    { error ->
                        throw RuntimeException(error.reason) // TODO could be more sophisticated.
                    },
                    { type -> orchestrator.job(type) },
                )
        }
    }

    companion object {
        private const val FORTYFIVE_SECONDS_AS_INT: Int = 45
        private val FORTYFIVE_SECONDS: Duration = FORTYFIVE_SECONDS_AS_INT.toDuration(DurationUnit.SECONDS)
        private const val THIRTY_SECONDS_AS_INT: Int = 30
        private val THIRTY_SECONDS: Duration = THIRTY_SECONDS_AS_INT.toDuration(DurationUnit.SECONDS)
    }
}
