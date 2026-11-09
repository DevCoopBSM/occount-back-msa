package devcoop.occount.gateway.api.presentation

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import java.time.Duration

/**
 * 단일 rate limit 정책. [capacity]개 토큰을 [refillPeriod]마다 그리디 방식으로 채운다.
 * [id]는 IP와 조합해 버킷 캐시 키를 만든다.
 */
data class RateLimitSpec(
    val id: String,
    val capacity: Long,
    val refillPeriod: Duration,
) {
    fun newBucket(): Bucket =
        Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(capacity)
                    .refillGreedy(capacity, refillPeriod)
                    .build(),
            )
            .build()
}
