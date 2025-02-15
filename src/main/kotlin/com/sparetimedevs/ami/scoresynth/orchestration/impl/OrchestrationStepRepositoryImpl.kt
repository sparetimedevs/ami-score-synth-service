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

package com.sparetimedevs.ami.scoresynth.orchestration.impl

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.sparetimedevs.ami.scoresynth.orchestration.ContinuationToken
import com.sparetimedevs.ami.scoresynth.orchestration.InternalOrchestrationStepFailure
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationId
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStep
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStepFailure
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStepRepository
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationStepRetrievalFailure
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationValidationError
import com.sparetimedevs.ami.scoresynth.orchestration.UnknownInternalOrchestrationStepFailure
import com.sparetimedevs.ami.scoresynth.orchestration.util.toEitherAccumulatedValidationErrorsOrA
import com.sparetimedevs.ami.scoresynth.orchestration.validation.validateOrchestrationStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource
import kotlin.reflect.KType

/**
 * We treat the orchestration_steps table as an append only table, so that we can easily debug when there is a problem
 * with an orchestration.
 */
class OrchestrationStepRepositoryImpl(
    dataSource: DataSource,
    private val jsonParser: Json,
) : OrchestrationStepRepository {
    private val jdbcTemplate: JdbcTemplate = JdbcTemplate(dataSource)
    private val simpleJdbcInsert: SimpleJdbcInsert =
        SimpleJdbcInsert(dataSource)
            .withTableName("orchestration_steps")
            .usingColumns(
                "id", // TODO, we could try without this one and a default creation in db...
                "orchestration_id",
                "name",
                "state",
                "result",
            )

    // TODO maybe rename this function (read doesn't seem to cover it).
    // This is the method that checks the repo, if it doesn't find any result, it will allow continuation.
    // if it finds any result but state is not_completed it will allow continuation
    // if it finds a result, and it has state completed, it will parse the result as A and return it.
    @Suppress("UNCHECKED_CAST")
    override suspend fun <A> read(
        stepName: String,
        orchestrationId: OrchestrationId,
        type: KType,
    ): Either<OrchestrationStepFailure, A> =
        findByStepNameOrchestrationId(stepName, orchestrationId)
            .mapLeft { orchestrationStepFailure: OrchestrationStepFailure ->
                when (orchestrationStepFailure) {
                    is OrchestrationStepRetrievalFailure ->
                        ContinuationToken(
                            "There was no record of this step in the database for stepName $stepName and orchestrationId $orchestrationId",
                        )
                    else -> orchestrationStepFailure
                }
            }.flatMap { orchestrationStep ->
                if (orchestrationStep.state == "completed") {
                    val deserializer: KSerializer<A> = jsonParser.serializersModule.serializer(type) as KSerializer<A>
                    jsonParser
                        .decodeFromString(deserializer, orchestrationStep.result)
                        .right() // TODO could be done in a more safe way.
                } else {
                    ContinuationToken(
                        "There was a record of the step for stepName $stepName and orchestrationId $orchestrationId in the database but the state is ${orchestrationStep.state}",
                    ).left()
                }
            }

    private suspend fun findByStepNameOrchestrationId(
        stepName: String,
        orchestrationId: OrchestrationId,
    ): Either<OrchestrationStepFailure, OrchestrationStep> {
        val sql =
            "SELECT * FROM orchestration_steps WHERE name = ? AND orchestration_id = ? ORDER BY creation_timestamp DESC LIMIT 1"

        return Either
            .catch {
                withContext(Dispatchers.IO) {
                    jdbcTemplate.queryForObject(
                        sql,
                        RowMapper { rs: ResultSet, rowNum: Int ->
                            validateOrchestrationStep(
                                rs.getString("id"),
                                rs.getString("orchestration_id"),
                                rs.getString("name"),
                                rs.getString("state"),
                                rs.getString("result"),
                            )
                        },
                        stepName,
                        orchestrationId.value.toString(),
                    )
                }
            }.fold(
                {
                    when (it) {
                        is EmptyResultDataAccessException ->
                            OrchestrationStepRetrievalFailure(
                                "Could not find orchestration step with orchestration id $orchestrationId in database.",
                            ).left()
                        else -> UnknownInternalOrchestrationStepFailure(it.message ?: "Unknown").left()
                    }
                },
                {
                    maybeValidatedOrchestrationStep:
                        Either<NonEmptyList<OrchestrationValidationError>, OrchestrationStep>?,
                    ->
                    maybeValidatedOrchestrationStep
                        ?.toEitherAccumulatedValidationErrorsOrA()
                        ?.mapLeft { OrchestrationStepRetrievalFailure(it.reason) }
                        ?: OrchestrationStepRetrievalFailure(
                            "Could not find orchestration step with orchestration id $orchestrationId in database.",
                        ).left()
                },
            )
    }

    override suspend fun <A> create(
        stepName: String,
        orchestrationId: OrchestrationId,
        stepResult: Either<OrchestrationStepFailure, A>,
        type: KType,
    ): Either<OrchestrationStepFailure, A> {
        val interpretedStepResult =
            stepResult.fold(
                {
                    StepResult("not_completed", it.toString())
                },
                {
                    val serializer: KSerializer<A> = jsonParser.serializersModule.serializer(type) as KSerializer<A>
                    val stepSuccessJson = jsonParser.encodeToString(serializer, it)
                    StepResult("completed", stepSuccessJson)
                },
            )

        val id = UUID.randomUUID()

        val parameters: MutableMap<String, Any?> = HashMap()
        parameters["id"] = id.toString()
        parameters["orchestration_id"] = orchestrationId.value.toString()
        parameters["name"] = stepName
        parameters["state"] = interpretedStepResult.state
        parameters["result"] = interpretedStepResult.result

        println(
            "Create record in repo: step name: $stepName, step state: ${interpretedStepResult.state}, step result ${interpretedStepResult.result}",
        )
        val someInt = withContext(Dispatchers.IO) { simpleJdbcInsert.execute(parameters) }
        return if (someInt == 1) {
            stepResult
        } else {
            Either.Left(
                InternalOrchestrationStepFailure(
                    "Something is wrong! Tried to insert step into database but did not succeed.",
                ),
            )
        }
    }

    data class StepResult(
        val state: String,
        val result: String,
    )
}
