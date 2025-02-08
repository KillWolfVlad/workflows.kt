# Load Test

| DB               | Size | Client                                   | Start workflows duration | DB RAM | Execute workflows duration[^1] | CPU Max[^2] | RAM Max[^2] |
| ---------------- | ---- | ---------------------------------------- | ------------------------ | ------ | ------------------------------ | ----------- | ----------- |
| Redis Standalone | 100K | [LettuceRedisClient][LettuceRedisClient] | 3.898289458s             | 29.96M | 16.945757292s                  | 74%         | 1,57 GB     |
| Redis Standalone | 100K | [ReThisRedisClient][ReThisRedisClient]   | 5.578783917s             | 31.03M | 30.395289875s                  | 45%         | 1,19 GB     |

[LettuceRedisClient]: ./../src/main/kotlin/ru/killwolfvlad/workflows/clients/LettuceRedisClient.kt
[ReThisRedisClient]: ./../src/main/kotlin/ru/killwolfvlad/workflows/clients/ReThisRedisClient.kt

[^1]: Remember, that every workflow has delay of 10 seconds
[^2]: See Java Flight Recorder report for more info
