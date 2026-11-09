package devcoop.occount.payment.api.wallet

import devcoop.occount.core.common.auth.AuthPrincipal
import devcoop.occount.core.common.auth.AuthUser
import devcoop.occount.payment.api.dto.response.WalletPointResponse
import devcoop.occount.payment.application.query.chargelog.ChargeHistoryListResponse
import devcoop.occount.payment.application.query.chargelog.GetChargeHistoryQueryService
import devcoop.occount.payment.application.query.wallet.GetWalletPointQueryService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 로그인 사용자 본인의 지갑(포인트) 조회 API.
 * 인가는 게이트웨이의 wallet 하위 경로 규칙(AUTHENTICATED)에서 강제한다.
 */
@RestController
@RequestMapping("/wallet")
class WalletController(
    private val getWalletPointQueryService: GetWalletPointQueryService,
    private val getChargeHistoryQueryService: GetChargeHistoryQueryService,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 100
        private val ALLOWED_SORT_FIELDS = setOf("chargeId", "chargeDate")
        private val DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "chargeId")
    }

    @GetMapping("/point")
    fun getBalance(@AuthUser principal: AuthPrincipal): WalletPointResponse {
        return WalletPointResponse(getWalletPointQueryService.getPoint(principal.userId))
    }

    @GetMapping("/charges")
    fun getChargeHistory(
        @AuthUser principal: AuthPrincipal,
        @PageableDefault(size = 10, sort = ["chargeId"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ChargeHistoryListResponse {
        return getChargeHistoryQueryService.getChargeHistory(principal.userId, sanitizePageable(pageable))
    }

    private fun sanitizePageable(pageable: Pageable): Pageable {
        val cappedSize = minOf(pageable.pageSize, MAX_PAGE_SIZE)
        val filteredOrders = pageable.sort.filter { it.property in ALLOWED_SORT_FIELDS }.toList()
        val sanitizedSort = if (filteredOrders.isEmpty()) DEFAULT_SORT else Sort.by(filteredOrders)
        return PageRequest.of(pageable.pageNumber, cappedSize, sanitizedSort)
    }
}
