package devcoop.occount.member.api.user

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.member.application.query.UserPreOrderInfoResponse
import devcoop.occount.member.application.query.UserPreOrderInfoQueryService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class MemberController(
    private val userPreOrderInfoQueryService: UserPreOrderInfoQueryService,
) {
    @GetMapping("/pre-order-info")
    fun findUserInfo(httpRequest: HttpServletRequest): UserPreOrderInfoResponse {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return userPreOrderInfoQueryService.findPreOrderInfo(authPrincipal.userId)
    }
}
