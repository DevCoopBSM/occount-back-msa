package devcoop.occount.member.api.user

import devcoop.occount.core.common.auth.AuthPrincipal
import devcoop.occount.core.common.auth.AuthUser
import devcoop.occount.member.application.query.MemberInfoResponse
import devcoop.occount.member.application.query.UserBarcodeResponse
import devcoop.occount.member.application.query.UserPreOrderInfoResponse
import devcoop.occount.member.application.query.UserQueryService
import devcoop.occount.member.application.usecase.pin.ChangePinRequest
import devcoop.occount.member.application.usecase.pin.ChangePinUseCase
import devcoop.occount.member.application.usecase.pin.PinChangeTicketResponse
import devcoop.occount.member.application.usecase.pin.VerifyPasswordForPinChangeRequest
import devcoop.occount.member.application.usecase.pin.VerifyPasswordForPinChangeUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class MemberController(
    private val userQueryService: UserQueryService,
    private val verifyPasswordForPinChangeUseCase: VerifyPasswordForPinChangeUseCase,
    private val changePinUseCase: ChangePinUseCase,
) {
    @GetMapping("/pre-order-info")
    fun findUserInfo(@AuthUser principal: AuthPrincipal): UserPreOrderInfoResponse {
        return userQueryService.findPreOrderInfo(principal.userId)
    }

    @GetMapping("/barcode")
    fun findUserBarcode(@AuthUser principal: AuthPrincipal): UserBarcodeResponse {
        return userQueryService.findUserBarcode(principal.userId)
    }

    @GetMapping("/me")
    fun findMemberInfo(@AuthUser principal: AuthPrincipal): MemberInfoResponse {
        return userQueryService.findMemberInfo(principal.userId)
    }

    @PostMapping("/pin/change-ticket")
    @ResponseStatus(HttpStatus.CREATED)
    fun issuePinChangeTicket(
        @AuthUser principal: AuthPrincipal,
        @Valid @RequestBody request: VerifyPasswordForPinChangeRequest,
    ): PinChangeTicketResponse {
        return verifyPasswordForPinChangeUseCase.verify(principal.userId, request)
    }

    @PutMapping("/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePin(@AuthUser principal: AuthPrincipal, @Valid @RequestBody request: ChangePinRequest) {
        changePinUseCase.changePin(principal.userId, request)
    }
}
