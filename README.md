# workflows.kt

<p align="center">
  <img src="./brand/logo.webp" width="250"/>
  <br>
</p>

Embedded Workflows As Code engine for Kotlin powered by Coroutines and Redis-like DB.

## Key goals

- Be fast
- Require minimum resources
- Provide an enjoyable developer experience
- Run a lot of workflows simultaneously 1 000 000+

## Typical Use Cases

- Durable timers for notifications or any other delayed jobs
- Durable workflows in multi-instances application
- Cron jobs (with waiting and not waiting completion)

## Supported platforms

- Java v21+ LTS

## Official supported databases and clients[^1]

| DB                       | Client                                 |
| ------------------------ | -------------------------------------- |
| Redis Standalone v7.4.2+ | [ReThisRedisClient][ReThisRedisClient] |

[^1]: You can add support for your db using your favorite library, implementing [KeyValueClient][KeyValueClient]

[ReThisRedisClient]: ./src/main/kotlin/ru/killwolfvlad/workflows/clients/ReThisRedisClient.kt
[KeyValueClient]: ./src/main/kotlin/ru/killwolfvlad/workflows/core/interfaces/KeyValueClient.kt
