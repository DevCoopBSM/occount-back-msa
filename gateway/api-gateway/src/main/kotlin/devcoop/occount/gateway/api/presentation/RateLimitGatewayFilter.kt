package devcoop.occount.gateway.api.presentation

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bucket
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * 인증 엔드포인트에 IP 기준 in-memory rate limit을 건다.
 * 게이트웨이는 단일 인스턴스(replicaCount=1)로 고정되므로 Redis 없이 인메모리 버킷으로 충분하다.
 * 인증 판정·포워딩 비용을 들이기 전에 차단하도록 인증 필터(HIGHEST_PRECEDENCE)보다 한 단계 앞에서 실행한다.
 */
@Component
class RateLimitGatewayFilter(
    private val rateLimitPolicy: RateLimitPolicy,
    private val clientIpResolver: ClientIpResolver,
    private val rateLimitExceededWriter: RateLimitExceededWriter,
) : GlobalFilter, Ordered {
    // IP 무한 증가로 인한 메모리 누수 방지: 일정 시간 미사용 버킷은 만료시킨다.
    private val buckets: Cache<String, Bucket> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .maximumSize(MAX_BUCKETS)
        .build()

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE - 1

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val spec = rateLimitPolicy.resolveLimit(request.method, request.path.value())
            ?: return chain.filter(exchange)

        // IP를 알 수 없으면 fail-open: 모든 주소 불명 요청을 한 버킷으로 묶어 정상 사용자를 차단하지 않는다.
        val clientIp = clientIpResolver.resolve(request)
            ?: return chain.filter(exchange)

        val bucket = buckets.get("${spec.id}:$clientIp") { spec.newBucket() }
        val probe = bucket.tryConsumeAndReturnRemaining(1)
        if (probe.isConsumed) {
            return chain.filter(exchange)
        }

        // nanosToWaitForRefill: greedy 보충에서 '다음 1토큰까지 대기 시간' → Retry-After 최솟값으로 적합.
        val retryAfterSeconds = Duration.ofNanos(probe.nanosToWaitForRefill).toSeconds().coerceAtLeast(1)
        return rateLimitExceededWriter.writeTooManyRequests(exchange, retryAfterSeconds)
    }

    companion object {
        private const val MAX_BUCKETS = 100_000L
    }
}
