package devcoop.occount.payment.api.wallet

import devcoop.occount.core.common.auth.AuthPrincipal
import devcoop.occount.core.common.auth.AuthUser
import devcoop.occount.payment.api.dto.request.AdminChargeWalletRequest
import devcoop.occount.payment.api.dto.response.AdminChargeWalletResponse
import devcoop.occount.payment.application.query.chargelog.AdminChargeLogListResponse
import devcoop.occount.payment.application.query.chargelog.AdminChargeLogQueryService
import devcoop.occount.payment.application.usecase.wallet.admincharge.AdminChargeCommand
import devcoop.occount.payment.application.usecase.wallet.admincharge.AdminChargeWalletUseCase
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 전용 지갑(포인트) 운영 API.
 * 인가는 게이트웨이의 wallet admin 하위 경로 규칙(ADMIN_ONLY)에서만 강제하며 다운스트림 재검증은 하지 않는다.
 */
@RestController
@RequestMapping("/wallet")
class AdminWalletController(
    private val adminChargeWalletUseCase: AdminChargeWalletUseCase,
    private val adminChargeLogQueryService: AdminChargeLogQueryService,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 100
        private val ALLOWED_SORT_FIELDS = setOf("chargeId", "chargeDate")
        private val DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "chargeId")
    }

    @PostMapping("/admin/charge")
    fun chargeWallets(
        @AuthUser principal: AuthPrincipal,
        @Valid @RequestBody request: AdminChargeWalletRequest,
    ): AdminChargeWalletResponse {
        val result = adminChargeWalletUseCase.charge(
            AdminChargeCommand(
                adminId = principal.userId,
                userIds = request.userIds,
                amount = request.amount,
                reason = request.reason,
            ),
        )
        return AdminChargeWalletResponse.from(result)
    }

    @GetMapping("/admin/charges")
    fun findChargeHistory(
        @PageableDefault(size = 10, sort = ["chargeId"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): AdminChargeLogListResponse {
        return adminChargeLogQueryService.findAll(sanitizePageable(pageable))
    }

    private fun sanitizePageable(pageable: Pageable): Pageable {
        val cappedSize = minOf(pageable.pageSize, MAX_PAGE_SIZE)
        val filteredOrders = pageable.sort.filter { it.property in ALLOWED_SORT_FIELDS }.toList()
        val sanitizedSort = if (filteredOrders.isEmpty()) DEFAULT_SORT else Sort.by(filteredOrders)
        return PageRequest.of(pageable.pageNumber, cappedSize, sanitizedSort)
    }
}
