package devcoop.occount.payment.infrastructure.client.van

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * VAN 프로토콜 코드 설정 클래스
 * 민감한 프로토콜 규격 코드만 외부 설정으로 분리
 */
@Component
class VanProtocolCodes(
    @Value("\${van.api.codes.cancel-message-type}")
    val cancelMessageType: String,

    @Value("\${van.api.codes.reject-status-prefix}")
    val rejectStatusPrefix: String,

    @Value("\${van.api.codes.card-insert-keyword}")
    val cardInsertKeyword: String
)
