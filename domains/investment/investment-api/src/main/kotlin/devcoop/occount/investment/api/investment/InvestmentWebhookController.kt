package devcoop.occount.investment.api.investment

import devcoop.occount.investment.application.usecase.confirm.HandlePortOneWebhookUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * PortOne 결제 확정 웹훅 수신부. 게이트웨이에서 인증 없이(PERMIT_ALL) 통과시키며
 * 서명 검증은 [HandlePortOneWebhookUseCase] → PortOneWebhookGateway 에서 수행한다.
 */
@RestController
@RequestMapping("/investments/webhooks")
class InvestmentWebhookController(
    private val handlePortOneWebhookUseCase: HandlePortOneWebhookUseCase,
) {
    @PostMapping("/portone")
    fun onPortOneWebhook(
        @RequestBody rawBody: String,
        @RequestHeader headers: Map<String, String>,
    ): String {
        handlePortOneWebhookUseCase.handle(rawBody, headers)
        return "OK"
    }
}
