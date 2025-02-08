package ru.killwolfvlad.workflows.test

import eu.vendeli.rethis.ReThis
import io.kotest.core.spec.style.DescribeSpec
import io.ktor.http.Url
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.error
import io.lettuce.core.RedisClient
import io.mockk.clearAllMocks
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import redis.clients.jedis.JedisPooled
import ru.killwolfvlad.workflows.clients.LettuceRedisClient
import ru.killwolfvlad.workflows.clients.ReThisRedisClient
import ru.killwolfvlad.workflows.core.WorkflowsConfig
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsClassManager
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.test.interfaces.TestClient
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

abstract class WorkflowsDescribeSpec(
    body: WorkflowsDescribeSpec.() -> Unit = {},
) : DescribeSpec() {
    private val redisConnectionString = "redis://localhost:6379"
    private val jedis = JedisPooled(redisConnectionString)

    val rootJob = SupervisorJob()

    val testClients =
        listOf<TestClient>(
            object : TestClient {
                override val dbName = "Redis Standalone"

                override val testKeyValueClient = JedisTestRedisClient(jedis)

                override val keyValueClient = LettuceRedisClient(rootJob, RedisClient.create(redisConnectionString))
            },
            object : TestClient {
                override val dbName = "Redis Standalone"

                override val testKeyValueClient = JedisTestRedisClient(jedis)

                override val keyValueClient =
                    ReThisRedisClient(
                        Url(redisConnectionString).let {
                            ReThis(it.host, it.port)
                        },
                    )
            },
        )

    val defaultWorkflowsClassManager =
        object : WorkflowsClassManager {
            override fun getInstance(workflowClass: KClass<out Workflow>): Workflow = workflowClass.primaryConstructor!!.call()
        }

    val defaultWorkflowsExceptionHandler =
        object : WorkflowsExceptionHandler {
            private val logger = KtorSimpleLogger("WorkflowsExceptionHandler")

            override suspend fun handle(exception: Exception) {
                logger.error(exception)
            }
        }

    val defaultWorkflowsConfig =
        WorkflowsConfig(
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
