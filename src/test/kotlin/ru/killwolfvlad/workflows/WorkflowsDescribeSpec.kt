package ru.killwolfvlad.workflows

import eu.vendeli.rethis.ReThis
import eu.vendeli.rethis.annotations.ReThisInternal
import eu.vendeli.rethis.types.core.toArg
import io.kotest.core.spec.style.DescribeSpec
import io.ktor.util.logging.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import ru.killwolfvlad.workflows.core.WorkflowsConfig
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsClassManager
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.redis.ReThisClient
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ReThisInternal::class)
abstract class WorkflowsDescribeSpec(body: WorkflowsDescribeSpec.() -> Unit = {}) : DescribeSpec() {
    val mainJob = Job()

    lateinit var redisClient: ReThis

    lateinit var defaultKeyValueClient: KeyValueClient

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

    val defaultWorkflowConfig = WorkflowsConfig(
        workerId = "test-worker-1",
        heartbeatInterval = 15.seconds,
        lockTimeout = 1.minutes,
        fetchInterval = 2.minutes,
    )

    init {
        beforeSpec {
            // TODO: use params from config
            redisClient = ReThis("localhost", 6379)

            defaultKeyValueClient = ReThisClient(redisClient)
        }

        beforeEach {
            redisClient.execute(listOf("FLUSHDB").toArg())
        }

        afterEach {
            mainJob.cancelAndJoin()
        }

        afterSpec {
            redisClient.disconnect()
        }

        body()
    }
}
