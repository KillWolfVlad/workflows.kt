package ru.killwolfvlad.workflows

import eu.vendeli.rethis.ReThis
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.mpp.bestName
import io.ktor.http.*
import io.ktor.util.logging.*
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.mockk.clearAllMocks
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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

abstract class WorkflowsDescribeSpec(body: WorkflowsDescribeSpec.() -> Unit = {}) : DescribeSpec() {
    interface TestClient {
        val dbName: String

        val rawClient: RedisCoroutinesCommands<String, String>

        val keyValueClient: KeyValueClient

        val name: String
            get() = "${keyValueClient::class.bestName()} [$dbName]"
    }

    private val redisConnectionString = "redis://localhost:6379"

    private val lettuceRedisClient = RedisClient.create(redisConnectionString)

    val rootJob = SupervisorJob()

    val testClients = listOf<TestClient>(
        object : TestClient {
            override val dbName = "Redis Standalone"

            override val rawClient = lettuceRedisClient.connect().coroutines()

            override val keyValueClient = LettuceRedisClient(rootJob, lettuceRedisClient)
        },
        object : TestClient {
            override val dbName = "Redis Standalone"

            override val rawClient = lettuceRedisClient.connect().coroutines()

            override val keyValueClient = ReThisRedisClient(
                Url(redisConnectionString).let {
                    ReThis(it.host, it.port)
                }
            )
        },
    )

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
        rootJob = rootJob,
    )

    init {
        beforeEach {
            clearAllMocks()
        }

        afterEach {
            rootJob.cancelChildren()
        }

        body()
    }
}

suspend fun RedisCoroutinesCommands<String, String>.hgetallAsMap(key: String): Map<String, String> =
    hgetall(key).map { it.key to it.value }.toList().toMap()
