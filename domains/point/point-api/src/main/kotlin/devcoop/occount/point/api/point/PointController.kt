package devcoop.occount.point.api.point

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.point.application.point.PointAmountRequest
import devcoop.occount.point.application.point.PointBalanceResponse
import devcoop.occount.point.application.point.PointService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/points")
class PointController(
    private val pointService: PointService,
) {
    @GetMapping("/balance")
    fun getBalance(httpRequest: HttpServletRequest): PointBalanceResponse {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return pointService.getBalance(authPrincipal.userId)
    }

    @PostMapping("/charge")
    @ResponseStatus(HttpStatus.OK)
    fun charge(
        @Valid @RequestBody request: PointAmountRequest,
        httpServletRequest: HttpServletRequest,
    ): PointBalanceResponse {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpServletRequest)
        return pointService.charge(authPrincipal.userId, request.amount)
    }
}
