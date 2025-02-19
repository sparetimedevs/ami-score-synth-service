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

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import com.sparetimedevs.ami.scoresynth.DomainError
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestration
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationError
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationRepository
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStepFailure
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStepRepository
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestrator
import com.sparetimedevs.ami.scoresynth.orchestration.UnknownExternalOrchestrationStepFailure
import kotlinx.serialization.Serializable

class AudioSynthesisOrchestrator(
    orchestrationRepository: OrchestrationRepository,
    orchestrationStepRepository: OrchestrationStepRepository,
    private val fileHandler: FileHandler,
    private val audioSynthesizer: AudioSynthesizer,
) : Orchestrator<InputFile, OutputFile>(
        orchestrationRepository,
        orchestrationStepRepository,
        InputFile::class,
        OutputFile::class,
    ) {
    override suspend fun orchestrate(
        registeredOrchestration: Orchestration<InputFile, OutputFile>,
    ): Either<OrchestrationError, OutputFile> =
        registeredOrchestration.id.orchestration {
            // TODO start with validating input
            // and input should be inputFile: InputFile

            val inputFile: InputFile = registeredOrchestration.input

            // TODO maybe do want to do this in one big step, because we can't really save the intermediate results in the steps table.

            val theThing =
                orchestrationStep("doTheThing") {
                    fileHandler
                        .getFile(inputFile.path)
                        .flatMap { fileContents ->
//                            audioSynthesizer.transformMidiToWav(fileContents)
                            fileContents.right() // TODO this should not be here.
                        }.flatMap { fileContents ->
                            fileHandler.saveFile("hello/world.wav")
                        }.mapLeft { domainError: DomainError -> domainError.toOrchestrationStepFailure() }
                }

            theThing
        }

    private fun DomainError.toOrchestrationStepFailure(): OrchestrationStepFailure {
        return UnknownExternalOrchestrationStepFailure(this.message) // TODO this could be improved.
    }
}

@Serializable
data class InputFile(
    val path: String,
)

@Serializable
data class OutputFile(
    val path: String,
)

class FileHandler {
    suspend fun getFile(path: String): Either<DomainError, String> {
        // TODO implement this
        return "Some file content, streaming".right()
    }

    suspend fun saveFile(path: String): Either<DomainError, OutputFile> {
        // TODO implement this
        return OutputFile(path).right()
    }
}
