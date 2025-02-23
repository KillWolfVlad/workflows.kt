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

## Install

See also [official docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package) about using GitHub Packages.

`build.gradle.kts`

```kotlin
plugins {
    id("net.saliman.properties") version "1.5.2"
}

repositories {
    mavenCentral()

    maven {
        url = uri("https://maven.pkg.github.com/KillWolfVlad/workflows.kt")

        credentials {
            username = project.findProperty("gpr.user") as String?
            password = project.findProperty("gpr.key") as String?
        }
    }
}

dependencies {
    implementation("ru.killwolfvlad:workflows:version")
}
```

`gradle-local.properties`

```properties
gpr.user=xxx
gpr.key=xxx
```

You can find latest version in [GitHub Packages](https://github.com/KillWolfVlad/workflows.kt/packages/2397397).

> WARNING! Don' forget add `gradle-local.properties` to `.gitignore`

## Usage

See [exampleApp](./exampleApp) for usage example.

## Deploy

We recommend use `HOSTNAME` environment variable for Worker ID and deploy application to [Stateful Sets](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/).

## Supported platforms

- Java v21+ LTS

## Official supported databases and clients

| DB                       | Client                                       |
| ------------------------ | -------------------------------------------- |
| Redis Standalone v7.4.2+ | [LettuceRedisClient][LettuceRedisClient][^1] |
| Redis Standalone v7.4.2+ | [ReThisRedisClient][ReThisRedisClient][^2]   |

> We recommend use [ReThisRedisClient][ReThisRedisClient] in most cases

### Custom clients

Implement [KeyValueClient][KeyValueClient] to provide support for your DB using your library.

## Maintainers

- [@KillWolfVlad](https://github.com/KillWolfVlad)

## License

This repository is released under version 2.0 of the
[Apache License](https://www.apache.org/licenses/LICENSE-2.0).

[KeyValueClient]: ./src/main/kotlin/ru/killwolfvlad/workflows/core/interfaces/KeyValueClient.kt
[LettuceRedisClient]: ./src/main/kotlin/ru/killwolfvlad/workflows/clients/LettuceRedisClient.kt
[ReThisRedisClient]: ./src/main/kotlin/ru/killwolfvlad/workflows/clients/ReThisRedisClient.kt

[^1]: To use this client you must install [Lettuce for Kotlin API](https://redis.github.io/lettuce/user-guide/kotlin-api/) v6.5.4.RELEASE+
[^2]: To use this client you must install [re.this](https://github.com/vendelieu/re.this) v0.2.9+
