//package ru.killwolfvlad.workflows.__temp
//
//import eu.vendeli.rethis.annotations.ReThisInternal
//import eu.vendeli.rethis.commands.eval
//import eu.vendeli.rethis.commands.hGetAll
//import eu.vendeli.rethis.commands.hSet
//import eu.vendeli.rethis.commands.keys
//import eu.vendeli.rethis.types.core.RType
//import io.kotest.matchers.collections.shouldHaveSize
//import io.kotest.matchers.shouldBe
//import io.kotest.matchers.types.shouldBeTypeOf
//import ru.killwolfvlad.workflows.WorkflowsDescribeSpec
//import ru.killwolfvlad.workflows.core.internal.LuaScripts
//
//@ReThisInternal
//class RedisScriptsTestWorkflows : WorkflowsDescribeSpec({
//    describe("eval hSetIfKeyExistsScript") {
//        describe("when target key doesn't exist") {
//            lateinit var result: RType
//
//            beforeEach {
//                result = redisClient.eval(
//                    LuaScripts.hSetIfKeyExistsScript,
//                    // keys
//                    1,
//                    "key",
//                    // arguments
//                    "2", // ARGV[1]
//                    "field2",
//                    "value3",
//                )
//            }
//
//            it("must return null") {
//                result.shouldBeTypeOf<RType.Null>()
//            }
//
//            it("must do nothing") {
//                val allKeys = redisClient.keys("*")
//
//                allKeys shouldHaveSize 0
//            }
//        }
//
//        describe("when target key exist") {
//            lateinit var result: RType
//
//            beforeEach {
//                redisClient.hSet("key", "field1" to "value1")
//
//                result = redisClient.eval(
//                    LuaScripts.hSetIfKeyExistsScript,
//                    // keys
//                    1,
//                    "key",
//                    // arguments
//                    "4", // ARGV[1]
//                    "field2",
//                    "value2",
//                    "field3",
//                    "value3",
//                )
//            }
//
//            it("must return null") {
//                result.shouldBeTypeOf<RType.Null>()
//            }
//
//            it("must set values") {
//                val fieldValues = redisClient.hGetAll("key")
//
//                fieldValues shouldBe mapOf(
//                    "field1" to "value1",
//                    "field2" to "value2",
//                    "field3" to "value3",
//                )
//            }
//        }
//    }
//})
