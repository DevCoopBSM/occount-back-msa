package devcoop.occount.point.api.point

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.point.api.dto.request.ChargePointCommand
import devcoop.occount.point.application.query.balance.GetPointBalanceQueryService
import devcoop.occount.point.application.query.balance.PointBalanceResponse
import devcoop.occount.point.application.query.chargelog.ChargeLogResult
import devcoop.occount.point.application.query.chargelog.GetChargeHistoryQueryService
import devcoop.occount.point.application.usecase.charge.ChargePointRequest
import devcoop.occount.point.application.usecase.charge.ChargePointUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/points")
class PointController(
    private val getPointBalanceQueryService: GetPointBalanceQueryService,
    private val chargePointUseCase: ChargePointUseCase,
    private val getChargeHistoryQueryService: GetChargeHistoryQueryService,
) {
    @GetMapping("/balance")
    fun getBalance(httpRequest: HttpServletRequest): PointBalanceResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getPointBalanceQueryService.getBalance(userId)
    }

    @PostMapping("/charge")
    @ResponseStatus(HttpStatus.OK)
    fun charge(
        @Valid @RequestBody request: ChargePointCommand,
        httpServletRequest: HttpServletRequest,
    ) {
        val userId = RequestAuthPrincipalResolver.resolve(httpServletRequest).userId
        chargePointUseCase.charge(ChargePointRequest(userId, request.amount))
    }

    @GetMapping("/charges")
    @ResponseStatus(HttpStatus.OK)
    fun getChargeHistory(httpRequest: HttpServletRequest): List<ChargeLogResult> {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getChargeHistoryQueryService.getChargeHistory(userId)
    }


}
