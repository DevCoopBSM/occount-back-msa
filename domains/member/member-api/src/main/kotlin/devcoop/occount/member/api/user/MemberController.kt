package devcoop.occount.member.api.user

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.member.application.query.UserPreOrderInfoResponse
import devcoop.occount.member.application.query.UserQueryService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class MemberController(
    private val userQueryService: UserQueryService,
) {
    @GetMapping("/pre-order-info")
    fun findUserInfo(httpRequest: HttpServletRequest): UserPreOrderInfoResponse {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return userQueryService.findPreOrderInfo(authPrincipal.userId)
    }
}
