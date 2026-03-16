package devcoop.occount.point.api.point

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.point.application.point.PointAmountRequest
import devcoop.occount.point.application.point.PointBalanceResponse
import devcoop.occount.point.application.point.PointService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

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

    @GetMapping("/{userId}/balance")
    fun getBalanceByUserId(@PathVariable userId: Long): PointBalanceResponse {
        return pointService.getBalance(userId)
    }

    @PostMapping("/{userId}/charge")
    @ResponseStatus(HttpStatus.OK)
    fun charge(
        @PathVariable userId: Long,
        @Valid @RequestBody request: PointAmountRequest,
    ): PointBalanceResponse {
        return pointService.charge(userId, request.amount)
    }

    @PostMapping("/{userId}/deduct")
    @ResponseStatus(HttpStatus.OK)
    fun deduct(
        @PathVariable userId: Long,
        @Valid @RequestBody request: PointAmountRequest,
    ): PointBalanceResponse {
        return pointService.deduct(userId, request.amount)
    }
}
