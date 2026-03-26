package devcoop.occount.point.api.point

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.point.api.dto.request.ChargePointCommand
import devcoop.occount.point.application.usecase.charge.ChargePointUseCase
import devcoop.occount.point.application.usecase.deduct.DeductPointRequest
import devcoop.occount.point.application.query.balance.PointBalanceResponse
import devcoop.occount.point.application.query.balance.GetPointBalanceQueryService
import devcoop.occount.point.application.usecase.charge.ChargePointRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/points")
class PointController(
    private val getPointBalanceQueryService: GetPointBalanceQueryService,
    private val chargePointUseCase: ChargePointUseCase,
) {
    @GetMapping("/balance")
    fun getBalance(httpRequest: HttpServletRequest): PointBalanceResponse {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return getPointBalanceQueryService.getBalance(authPrincipal.userId)
    }

    @PostMapping("/charge")
    @ResponseStatus(HttpStatus.OK)
    fun charge(
        @Valid @RequestBody request: ChargePointCommand,
        httpServletRequest: HttpServletRequest,
    ): PointBalanceResponse {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpServletRequest)
        val chargeRequest = ChargePointRequest(authPrincipal.userId, request.amount)
        return chargePointUseCase.charge(chargeRequest)
    }
}
