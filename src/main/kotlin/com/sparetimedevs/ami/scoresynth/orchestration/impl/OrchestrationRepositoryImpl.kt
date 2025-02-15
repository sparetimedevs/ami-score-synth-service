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
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.NonEmptyList
import arrow.core.collectionSizeOrDefault
import arrow.core.identity
import arrow.core.left
import arrow.core.right
import com.sparetimedevs.ami.scoresynth.orchestration.Orchestration
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationCreationError
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationError
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationId
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationRepository
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationRetrievalError
import com.sparetimedevs.ami.scoresynth.orchestration.OrchestrationValidationError
import com.sparetimedevs.ami.scoresynth.orchestration.UnknownOrchestrationError
import com.sparetimedevs.ami.scoresynth.orchestration.util.toEitherAccumulatedValidationErrorsOrA
import com.sparetimedevs.ami.scoresynth.orchestration.validation.validateOrchestration
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.sql.DataSource
import kotlin.reflect.KType

class OrchestrationRepositoryImpl(
    dataSource: DataSource,
    private val clock: Clock,
    private val jsonParser: Json,
) : OrchestrationRepository {
    private val jdbcTemplate: JdbcTemplate = JdbcTemplate(dataSource)
    private val simpleJdbcInsert: SimpleJdbcInsert =
        SimpleJdbcInsert(dataSource)
            .withTableName("orchestrations")
            .usingColumns(
                "id", // TODO, we could try without this one and a default creation in db...
                "name",
                "input",
                "state",
                "result",
            )

    override suspend fun <Input, Result> create(
        orchestrationName: String,
        input: Input,
        inputType: KType,
    ): Either<OrchestrationError, Orchestration<Input, Result>> {
        println("Create record in repo: orchestration name: $orchestrationName, orchestration state: created")

        val id = OrchestrationId(UUID.randomUUID()) // TODO can also be a dedicated helper function.

        val orchestration =
            Orchestration<Input, Result>(
                id = id,
                name = orchestrationName,
                input = input,
                state = "created",
                result = null,
            )

        val serializer: KSerializer<Input> =
            jsonParser.serializersModule.serializer(inputType) as KSerializer<Input>
        val inputJson = jsonParser.encodeToString(serializer, input)

        val parameters: MutableMap<String, Any?> = HashMap()
        parameters["id"] = orchestration.id.value.toString()
        parameters["name"] = orchestration.name
        parameters["input"] = inputJson
        parameters["state"] = orchestration.state
        val someInt = withContext(Dispatchers.IO) { simpleJdbcInsert.execute(parameters) }
        return if (someInt == 1) {
            Either.Right(orchestration)
        } else {
            Either.Left(OrchestrationCreationError("Something is wrong!"))
        }
    }

    override suspend fun <Input, Result> findById(
        id: OrchestrationId,
        inputType: KType,
        resultType: KType,
    ): Either<OrchestrationError, Orchestration<Input, Result>> {
        val sql = "SELECT * FROM orchestrations WHERE id = ?"

        return Either
            .catch {
                withContext(Dispatchers.IO) {
                    jdbcTemplate.queryForObject(
                        sql,
                        RowMapper { rs: ResultSet, rowNum: Int ->
                            validateOrchestration<Input, Result>(
                                rs.getString("id"),
                                rs.getString("name"),
                                rs.getString("input"),
                                rs.getString("state"),
                                rs.getString("result"),
                                jsonParser,
                                inputType,
                                resultType,
                            )
                        },
                        id.value.toString(),
                        // TODO write an integration test which makes sure this implementation always keeps working.
                        // And then swap out `id.value.toString()` with "lolmytest" and see if test fails
                        // (because that is what we want).
                    )
                }
            }.fold(
                {
                    when (it) {
                        is EmptyResultDataAccessException ->
                            OrchestrationRetrievalError(
                                "Could not find orchestration with id $id in database.",
                            ).left()
                        else -> UnknownOrchestrationError(it.message ?: "Unknown").left()
                    }
                },
                {
                    maybeValidatedOrchestration:
                        Either<NonEmptyList<OrchestrationValidationError>, Orchestration<Input, Result>>?,
                    ->
                    maybeValidatedOrchestration
                        ?.toEitherAccumulatedValidationErrorsOrA()
                        ?: OrchestrationRetrievalError("Could not find orchestration with id $id in database.").left()
                },
            )
    }

    override suspend fun <Input, Result> getNext(
        orchestrationName: String,
        inputType: KType,
    ): Either<OrchestrationError, List<Orchestration<Input, Result>>> {
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

        val sql = "SELECT * FROM orchestrations WHERE name = ? AND state != 'completed'" // TODO AND name = ?
        return Either
            .catch { withContext(Dispatchers.IO) { jdbcTemplate.queryForList(sql, orchestrationName) } }
            .fold(
                {
                    UnknownOrchestrationError(it.message ?: "Unknown").left()
                },
                { list: MutableList<MutableMap<String, Any>> ->
                    list
                        .map { row ->
                            validateOrchestration<Input, Result>(
                                row.getValue("id").toString(),
                                row.getValue("name").toString(),
                                row.getValue("input").toString(),
                                row.getValue("state").toString(),
                                row["result"].toString(),
                                jsonParser,
                                inputType,
                            )
                        }.sequence()
                        .toEitherAccumulatedValidationErrorsOrA()
                },
            )
    }

    override suspend fun <Result> update(
        orchestrationId: OrchestrationId,
        orchestrationName: String,
        orchestrationResult: Either<OrchestrationError, Result>,
        resultType: KType,
    ): Either<OrchestrationError, Result> {
        val sql = "UPDATE orchestrations SET state = ?, result = ?, updated_timestamp = ? WHERE id = ?"

        val (state, result) =
            orchestrationResult.fold(
                {
                    "not_completed" to null
                },
                {
                    val serializer: KSerializer<Result> =
                        jsonParser.serializersModule.serializer(resultType) as KSerializer<Result>
                    val resultJson = jsonParser.encodeToString(serializer, it)
                    "completed" to resultJson
                },
            )

        val updatedAt = Instant.now(clock)
        println(
            "Update record in repo: orchestrator name: $orchestrationName, orchestrator state: $state updatedInstant: ${
                updatedAt.atZone(ZoneId.systemDefault())
            }",
        )

        withContext(Dispatchers.IO) {
            jdbcTemplate.update(
                sql,
                state,
                result,
                updatedAt,
                orchestrationId.value.toString(),
            )
        }
        return orchestrationResult
    }
}

fun <E, A> Iterable<Either<E, A>>.sequence(): Either<E, List<A>> = traverse(::identity)

inline fun <E, A, B> Iterable<A>.traverse(f: (A) -> Either<E, B>): Either<E, List<B>> {
    val destination = ArrayList<B>(collectionSizeOrDefault(10))
    for (item in this) {
        when (val res = f(item)) {
            is Right -> destination.add(res.value)
            is Left -> return res
        }
    }
    return destination.right()
}
