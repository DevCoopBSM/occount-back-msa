package devcoop.occount.gateway.api.presentation

import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.util.pattern.PathPatternParser
import java.time.Duration

/**
 * 인증 엔드포인트 IP 기준 rate limit 정책. 매칭되는 규칙이 없으면 무제한(null).
 * 임계값은 분당 기준이며, brute-force·외부 API 남용 방어 목적의 튜닝 상수다.
 */
@Component
class RateLimitPolicy {
    private val rules: List<RateLimitRule> = listOf(
        // credential stuffing/무차별 대입
        rule(HttpMethod.POST, "/api/v3/auth/login", "auth-login", capacity = 10),
        // NOTE: kiosk/login은 공유 단말(여러 학생이 한 IP) 특성상 IP 기준 제한이 정상 사용자를 오탐한다.
        //       PIN 무차별 방어는 member 서비스의 barcode(계정)별 시도 제한(KioskLoginAttempt)에서 담당한다.
        // IP 기준 이메일 폭탄 방어 (이메일별 60초 쿨다운과 별개)
        rule(HttpMethod.POST, "/api/v3/auth/email/send-otp", "auth-send-otp", capacity = 5),
        // 6자리 OTP 무차별 대입
        rule(HttpMethod.POST, "/api/v3/auth/email/verify-otp", "auth-verify-otp", capacity = 10),
        // 외부 PortOne 호출 비용·지연 → 더 빡빡하게
        rule(HttpMethod.POST, "/api/v3/auth/identity/verify", "auth-identity-verify", capacity = 5),
    )

    fun resolveLimit(method: HttpMethod?, path: String): RateLimitSpec? {
        if (method == null) {
            return null
        }
        return rules
            .firstOrNull { it.matches(method, path) }
            ?.spec
    }

    private fun rule(
        method: HttpMethod,
        pathPattern: String,
        id: String,
        capacity: Long,
        refillPeriod: Duration = Duration.ofMinutes(1),
    ): RateLimitRule {
        return RateLimitRule(
            method = method,
            pathPattern = PathPatternParser.defaultInstance.parse(pathPattern),
            spec = RateLimitSpec(id = id, capacity = capacity, refillPeriod = refillPeriod),
        )
    }
}
