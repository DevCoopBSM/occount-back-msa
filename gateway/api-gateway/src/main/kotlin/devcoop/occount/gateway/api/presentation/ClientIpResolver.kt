package devcoop.occount.gateway.api.presentation

import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component

/**
 * 클라이언트 IP를 추출한다. k8s 인그레스 뒤에서는 실제 IP가 `X-Forwarded-For`에 담기므로
 * 헤더의 첫 번째(원 클라이언트) 값을 우선하고, 없으면 소켓 주소로 폴백한다.
 *
 * 전제: Nginx Ingress가 신뢰 CIDR(proxy-real-ip-cidr)에서 온 트래픽에만 XFF를 덧붙이고
 *       외부에서 위조해 들어온 XFF는 제거하도록 구성되어 있어야 한다. 이 전제가 깨지면
 *       클라이언트가 XFF를 위조해 rate limit을 우회/오염시킬 수 있으므로 인프라 변경 시 재검토한다.
 *
 * IP를 끝내 알 수 없으면(헤더·소켓 주소 모두 부재) null을 반환한다. 이 경우 모든 "주소 불명"
 * 요청을 한 버킷으로 묶어 정상 사용자를 차단하는 것을 피하기 위해 호출 측에서 fail-open 처리한다.
 */
@Component
class ClientIpResolver {
    fun resolve(request: ServerHttpRequest): String? {
        request.headers.getFirst(X_FORWARDED_FOR)?.let { header ->
            val first = header.substringBefore(',').trim()
            if (first.isNotEmpty()) {
                return first
            }
        }
        return request.remoteAddress?.address?.hostAddress
    }

    companion object {
        private const val X_FORWARDED_FOR = "X-Forwarded-For"
    }
}
