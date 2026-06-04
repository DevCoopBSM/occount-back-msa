package devcoop.occount.member.api.contribution

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.member.application.query.ContributionAccountResponse
import devcoop.occount.member.application.query.ContributionDepositRequestListResponse
import devcoop.occount.member.application.query.ContributionWithdrawalRequestListResponse
import devcoop.occount.member.application.query.GetContributionDepositRequestsQueryService
import devcoop.occount.member.application.query.GetContributionWithdrawalRequestsQueryService
import devcoop.occount.member.application.query.GetMyContributionAccountQueryService
import devcoop.occount.member.application.query.GetMyContributionDepositRequestsQueryService
import devcoop.occount.member.application.query.GetMyContributionWithdrawalRequestsQueryService
import devcoop.occount.member.application.query.AdminContributionDepositRequestListResponse
import devcoop.occount.member.application.query.AdminContributionWithdrawalRequestListResponse
import devcoop.occount.member.application.usecase.contribution.ApproveContributionDepositRequestUseCase
import devcoop.occount.member.application.usecase.contribution.ApproveContributionWithdrawalRequestUseCase
import devcoop.occount.member.application.usecase.contribution.ContributionDepositRequestResponse
import devcoop.occount.member.application.usecase.contribution.ContributionWithdrawalRequestResponse
import devcoop.occount.member.application.usecase.contribution.RejectContributionDepositRequestUseCase
import devcoop.occount.member.application.usecase.contribution.RejectContributionWithdrawalRequestUseCase
import devcoop.occount.member.application.usecase.contribution.RejectContributionRequest
import devcoop.occount.member.application.usecase.contribution.SubmitContributionDepositRequest
import devcoop.occount.member.application.usecase.contribution.SubmitContributionDepositRequestUseCase
import devcoop.occount.member.application.usecase.contribution.SubmitContributionWithdrawalRequest
import devcoop.occount.member.application.usecase.contribution.SubmitContributionWithdrawalRequestUseCase
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequestStatus
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/contributions")
class ContributionController(
    private val submitContributionDepositRequestUseCase: SubmitContributionDepositRequestUseCase,
    private val getContributionDepositRequestsQueryService: GetContributionDepositRequestsQueryService,
    private val getMyContributionDepositRequestsQueryService: GetMyContributionDepositRequestsQueryService,
    private val getMyContributionAccountQueryService: GetMyContributionAccountQueryService,
    private val approveContributionDepositRequestUseCase: ApproveContributionDepositRequestUseCase,
    private val rejectContributionDepositRequestUseCase: RejectContributionDepositRequestUseCase,
    private val submitContributionWithdrawalRequestUseCase: SubmitContributionWithdrawalRequestUseCase,
    private val getContributionWithdrawalRequestsQueryService: GetContributionWithdrawalRequestsQueryService,
    private val getMyContributionWithdrawalRequestsQueryService: GetMyContributionWithdrawalRequestsQueryService,
    private val approveContributionWithdrawalRequestUseCase: ApproveContributionWithdrawalRequestUseCase,
    private val rejectContributionWithdrawalRequestUseCase: RejectContributionWithdrawalRequestUseCase,
) {
    @PostMapping("/deposit-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun submitDepositRequest(
        httpRequest: HttpServletRequest,
        @Valid @RequestBody request: SubmitContributionDepositRequest,
    ): ContributionDepositRequestResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return submitContributionDepositRequestUseCase.submit(userId, request)
    }

    @GetMapping("/deposit-requests")
    fun getDepositRequests(
        @RequestParam(required = false) status: ContributionDepositRequestStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): AdminContributionDepositRequestListResponse {
        return getContributionDepositRequestsQueryService.findAll(status, page, size)
    }

    @GetMapping("/deposit-requests/my")
    fun getMyDepositRequests(httpRequest: HttpServletRequest): ContributionDepositRequestListResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getMyContributionDepositRequestsQueryService.findAll(userId)
    }

    @GetMapping("/account/my")
    fun getMyAccount(httpRequest: HttpServletRequest): ContributionAccountResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getMyContributionAccountQueryService.getAccount(userId)
    }

    @PatchMapping("/deposit-requests/{requestId}/approve")
    fun approveDepositRequest(@PathVariable requestId: Long): ContributionDepositRequestResponse {
        return approveContributionDepositRequestUseCase.approve(requestId)
    }

    @PatchMapping("/deposit-requests/{requestId}/reject")
    fun rejectDepositRequest(
        @PathVariable requestId: Long,
        @Valid @RequestBody(required = false) request: RejectContributionRequest?,
    ): ContributionDepositRequestResponse {
        return rejectContributionDepositRequestUseCase.reject(requestId, request ?: RejectContributionRequest())
    }

    @PostMapping("/withdrawal-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun submitWithdrawalRequest(
        httpRequest: HttpServletRequest,
        @Valid @RequestBody request: SubmitContributionWithdrawalRequest,
    ): ContributionWithdrawalRequestResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return submitContributionWithdrawalRequestUseCase.submit(userId, request)
    }

    @GetMapping("/withdrawal-requests")
    fun getWithdrawalRequests(
        @RequestParam(required = false) status: ContributionWithdrawalRequestStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): AdminContributionWithdrawalRequestListResponse {
        return getContributionWithdrawalRequestsQueryService.findAll(status, page, size)
    }

    @GetMapping("/withdrawal-requests/my")
    fun getMyWithdrawalRequests(httpRequest: HttpServletRequest): ContributionWithdrawalRequestListResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getMyContributionWithdrawalRequestsQueryService.findAll(userId)
    }

    @PatchMapping("/withdrawal-requests/{requestId}/approve")
    fun approveWithdrawalRequest(@PathVariable requestId: Long): ContributionWithdrawalRequestResponse {
        return approveContributionWithdrawalRequestUseCase.approve(requestId)
    }

    @PatchMapping("/withdrawal-requests/{requestId}/reject")
    fun rejectWithdrawalRequest(
        @PathVariable requestId: Long,
        @Valid @RequestBody(required = false) request: RejectContributionRequest?,
    ): ContributionWithdrawalRequestResponse {
        return rejectContributionWithdrawalRequestUseCase.reject(requestId, request ?: RejectContributionRequest())
    }
}
