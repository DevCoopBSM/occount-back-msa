package devcoop.occount.member.api.user

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.member.application.query.MemberInfoResponse
import devcoop.occount.member.application.query.UserBarcodeResponse
import devcoop.occount.member.application.query.UserPreOrderInfoResponse
import devcoop.occount.member.application.query.UserQueryService
import devcoop.occount.member.application.usecase.pin.ChangePinRequest
import devcoop.occount.member.application.usecase.pin.ChangePinUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class MemberController(
    private val userQueryService: UserQueryService,
    private val changePinUseCase: ChangePinUseCase,
) {
    @GetMapping("/pre-order-info")
    fun findUserInfo(httpRequest: HttpServletRequest): UserPreOrderInfoResponse {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return userQueryService.findPreOrderInfo(authPrincipal.userId)
    }

    @GetMapping("/barcode")
    fun findUserBarcode(httpRequest: HttpServletRequest): UserBarcodeResponse {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return userQueryService.findUserBarcode(authPrincipal.userId)
    }

    @GetMapping("/me")
    fun findMemberInfo(httpRequest: HttpServletRequest): MemberInfoResponse {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        return userQueryService.findMemberInfo(authPrincipal.userId)
    }

    @PostMapping("/pin/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePin(httpRequest: HttpServletRequest, @Valid @RequestBody request: ChangePinRequest) {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpRequest)
        changePinUseCase.changePin(authPrincipal.userId, request)
    }
}
