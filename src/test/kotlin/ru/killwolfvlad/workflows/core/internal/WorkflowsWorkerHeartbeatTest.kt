//package ru.killwolfvlad.workflows.core.internal
//
//import eu.vendeli.rethis.commands.hGetAll
//import eu.vendeli.rethis.commands.hPTTL
//import eu.vendeli.rethis.commands.hSet
//import io.kotest.matchers.longs.shouldBeInRange
//import io.kotest.matchers.shouldBe
//import ru.killwolfvlad.workflows.WorkflowsDescribeSpec
//import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_WORKERS_KEY
//
//class WorkflowsWorkerHeartbeatTest : WorkflowsDescribeSpec({
//    lateinit var workflowsWorkerHeartbeat: WorkflowsWorkerHeartbeat
//
//    beforeEach {
//        workflowsWorkerHeartbeat = WorkflowsWorkerHeartbeat(
//            mainJob,
//            defaultWorkflowsConfig,
//            defaultKeyValueClient,
//            defaultWorkflowsExceptionHandler,
//        )
//    }
//
//    describe("init") {
//        describe("when lock is not set") {
//            beforeEach {
//                workflowsWorkerHeartbeat.init()
//            }
//
//            it("must set lock") {
//                val fieldValues = redisClient.hGetAll(WORKFLOW_WORKERS_KEY)
//
//                fieldValues shouldBe mapOf(defaultWorkflowsConfig.workerId to "OK")
//            }
//
//            it("must set ttl") {
//                val ttl = redisClient.hPTTL(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId).first()
//
//                ttl shouldBeInRange 0L..defaultWorkflowsConfig.lockTimeout.inWholeMilliseconds
//            }
//        }
//
//        describe("when lock already set") {
//            beforeEach {
//                redisClient.hSet(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId to "OK")
//
//                workflowsWorkerHeartbeat.init()
//            }
//
//            it("must update ttl") {
//                val ttl = redisClient.hPTTL(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId).first()
//
//                ttl shouldBeInRange 0L..defaultWorkflowsConfig.lockTimeout.inWholeMilliseconds
//            }
//        }
//    }
//})
