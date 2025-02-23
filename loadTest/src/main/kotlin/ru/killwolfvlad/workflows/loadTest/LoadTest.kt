package ru.killwolfvlad.workflows.loadTest

import eu.vendeli.rethis.ReThis
import io.ktor.http.Url
import io.lettuce.core.RedisClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.Protocol
import ru.killwolfvlad.workflows.clients.LettuceRedisClient
import ru.killwolfvlad.workflows.clients.ReThisRedisClient
import ru.killwolfvlad.workflows.core.WorkflowsConfig
import ru.killwolfvlad.workflows.core.WorkflowsWorker
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

@OptIn(WorkflowsPerformance::class)
suspend fun main() {
    val size = System.getProperty("size").toInt()

    val redisConnectionString = "redis://localhost:6379"
    val jedis = JedisPooled(redisConnectionString)

    val rootJob = SupervisorJob()

    val keyValueClient =
        when (System.getProperty("KeyValueClient")) {
            "ReThisRedisClient" ->
                ReThisRedisClient(
                    Url(redisConnectionString).let {
                        ReThis(it.host, it.port)
                    },
                )

            "LettuceRedisClient" -> LettuceRedisClient(rootJob, RedisClient.create(redisConnectionString))

            else -> throw IllegalArgumentException("unknown KeyValueClient!")
        }

    val workflowsConfig =
        WorkflowsConfig(
            workerId = "load-test-worker-1",
            heartbeatInterval = 15.seconds,
            lockTimeout = 1.minutes,
            fetchInterval = 2.minutes,
            rootJob = rootJob,
        )

    jedis.flushDB()

    startTest(workflowsConfig, "Redis Standalone", size, keyValueClient, jedis)

    rootJob.cancelChildren()
}

@OptIn(WorkflowsPerformance::class)
private suspend fun startTest(
    workflowsConfig: WorkflowsConfig,
    dbName: String,
    size: Int,
    keyValueClient: KeyValueClient,
    jedis: JedisPooled,
) {
    println("DB: $dbName")
    println("Size: $size")
    println("Client: ${keyValueClient::class.simpleName}")

    val results = ConcurrentHashMap<String, String>()

    val loadTestWorkflow =
        LoadTestWorkflow { id, payload ->
            results.putIfAbsent(id, payload)
        }

    val workflowsWorker =
        WorkflowsWorker(
            workflowsConfig,
            keyValueClient,
            WorkflowsClassManagerImpl(loadTestWorkflow),
            WorkflowsExceptionHandlerImpl(),
        )

    workflowsWorker.init()

    startWorkflows(workflowsWorker, size)
    printDbRam(jedis)
    waitWorkflows(workflowsWorker, results, size)
}

private suspend fun startWorkflows(
    workflowsWorker: WorkflowsWorker,
    size: Int,
) {
    val startWorkflowsDuration =
        measureTime {
            CoroutineScope(Dispatchers.IO)
                .launch {
                    (1..size).map { id ->
                        async {
                            workflowsWorker.executeLoadTestWorkflow(
                                LoadTestWorkflow.Context(
                                    id = "id$id",
                                    payload = "payload$id",
                                ),
                            )
                        }
                    }
                }.join()
        }

    println("Start workflows duration: $startWorkflowsDuration")
}

private fun printDbRam(jedis: JedisPooled) {
    val dbRam =
        (jedis.sendCommand(Protocol.Command.INFO) as ByteArray)
            .toString(Charsets.UTF_8)
            .split("\r\n")
            .find {
                it.startsWith("used_memory_human")
            }?.substringAfter("used_memory_human:")

    println("DB RAM: $dbRam")
}

private suspend fun waitWorkflows(
    workflowsWorker: WorkflowsWorker,
    results: ConcurrentHashMap<String, String>,
    size: Int,
) {
    val executeWorkflowsDuration =
        measureTime {
            while (results.size != size) {
                delay(100.milliseconds)
            }

            assert(
                results ==
                    (1..size).associate { id ->
                        "id$id" to "payload$id"
                    },
            )

            while (workflowsWorker.activeWorkflows != 0) {
                delay(100.milliseconds)
            }
        }

    println("Execute workflows duration: $executeWorkflowsDuration")
}
