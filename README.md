# workflows.kt

<p align="center">
  <img src="./brand/logo.webp" width="250"/>
  <br>
</p>

Embedded Workflows As Code engine for Kotlin powered by Coroutines and Redis-like DB.

## Features

- Durable workflows execution with scaling support
- Minimal overhead for durable and fast workflows execution
- Durable timers for notifications or any other delayed jobs
- Run 100K+ workflows simultaneously, see [loadTest](./loadTest) for more info

## Usage

See [exampleApp](./exampleApp) for usage example.

## Supported platforms

- Java v21+ LTS

## Official supported databases and clients[^1]

| DB                       | Client                                       |
| ------------------------ | -------------------------------------------- |
| Redis Standalone v7.4.2+ | [LettuceRedisClient][LettuceRedisClient][^2] |
| Redis Standalone v7.4.2+ | [ReThisRedisClient][ReThisRedisClient][^3]   |

> We recommend use [ReThisRedisClient][ReThisRedisClient] in most cases

## Maintainers

- [@KillWolfVlad](https://github.com/KillWolfVlad)

## License

This repository is released under version 2.0 of the
[Apache License](https://www.apache.org/licenses/LICENSE-2.0).

[KeyValueClient]: ./src/main/kotlin/ru/killwolfvlad/workflows/core/interfaces/KeyValueClient.kt
[LettuceRedisClient]: ./src/main/kotlin/ru/killwolfvlad/workflows/clients/LettuceRedisClient.kt
[ReThisRedisClient]: ./src/main/kotlin/ru/killwolfvlad/workflows/clients/ReThisRedisClient.kt

[^1]: Implement [KeyValueClient][KeyValueClient] to provide support for your db using your library
[^2]: To use this client you must install [Lettuce for Kotlin API](https://redis.github.io/lettuce/user-guide/kotlin-api/) v6.5.3.RELEASE+
[^3]: To use this client you must install [re.this](https://github.com/vendelieu/re.this) v0.2.8+
