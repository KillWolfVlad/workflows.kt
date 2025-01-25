package ru.killwolfvlad.workflows

import eu.vendeli.rethis.ReThis
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.mpp.bestName
import io.ktor.http.*
import io.ktor.util.logging.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import ru.killwolfvlad.workflows.clients.LettuceRedisClient
import ru.killwolfvlad.workflows.clients.ReThisRedisClient
import ru.killwolfvlad.workflows.core.WorkflowsConfig
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsClassManager
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalLettuceCoroutinesApi::class)
abstract class WorkflowsDescribeSpec(body: WorkflowsDescribeSpec.() -> Unit = {}) : DescribeSpec() {
    interface Client {
        val dbName: String

        val rawClient: RedisCoroutinesCommands<String, String>

        val keyValueClient: KeyValueClient

        val name: String
            get() = "${keyValueClient::class.bestName()} [$dbName]"
    }

    private val redisConnectionString = "redis://localhost:6379"

    private val lettuceRedisClient = RedisClient.create(redisConnectionString)

    val clients = listOf<Client>(
        object : Client {
            override val dbName = "Redis"

            override val rawClient = lettuceRedisClient.connect().coroutines()

            override val keyValueClient = LettuceRedisClient(lettuceRedisClient)
        },
        object : Client {
            override val dbName = "Redis"

            override val rawClient = lettuceRedisClient.connect().coroutines()

            override val keyValueClient = ReThisRedisClient(
                Url(redisConnectionString).let {
                    ReThis(it.host, it.port)
                }
            )
        },
    )

    val mainJob = Job()

    val defaultWorkflowsClassManager = object : WorkflowsClassManager {
        override fun getInstance(workflowClass: KClass<out Workflow>): Workflow =
            workflowClass.primaryConstructor!!.call()
    }

    val defaultWorkflowsExceptionHandler = object : WorkflowsExceptionHandler {
        private val logger = KtorSimpleLogger("WorkflowsExceptionHandler")

        override suspend fun handle(exception: Exception) {
            logger.error(exception)
        }
    }

    val defaultWorkflowsConfig = WorkflowsConfig(
        workerId = "test-worker-1",
        heartbeatInterval = 15.seconds,
        lockTimeout = 1.minutes,
        fetchInterval = 2.minutes,
    )

    init {
        afterEach {
            mainJob.cancelAndJoin()
        }

        body()
    }
}
