package devcoop.occount.investment.api.investment

import devcoop.occount.core.common.auth.AuthPrincipal
import devcoop.occount.core.common.auth.AuthUser
import devcoop.occount.investment.api.dto.request.AdminCreateInvestmentRequest
import devcoop.occount.investment.api.dto.request.CreateInvestmentRequest
import devcoop.occount.investment.api.dto.response.PrepareInvestmentResponse
import devcoop.occount.investment.application.query.AdminInvestmentListResponse
import devcoop.occount.investment.application.query.AdminListInvestmentsQueryService
import devcoop.occount.investment.application.query.GetInvestmentDetailQueryService
import devcoop.occount.investment.application.query.GetMyInvestmentsQueryService
import devcoop.occount.investment.application.query.InvestmentListResponse
import devcoop.occount.investment.application.query.InvestmentResult
import devcoop.occount.investment.application.usecase.admin.AdminCreateInvestmentCommand
import devcoop.occount.investment.application.usecase.admin.CreateInvestmentByAdminUseCase
import devcoop.occount.investment.application.usecase.prepare.PrepareInvestmentCommand
import devcoop.occount.investment.application.usecase.prepare.PrepareInvestmentUseCase
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * 출자(출자금) API.
 * - 본인용: 출자 결제 시작, 본인 출자 목록/상세 조회 (게이트웨이 AUTHENTICATED).
 * - 관리자용: 수기 등록, 전체 목록 (게이트웨이 ADMIN_ONLY, /investments/admin).
 * 인가는 게이트웨이 경로 규칙에서만 강제하며 다운스트림 재검증은 하지 않는다.
 */
@RestController
@RequestMapping("/investments")
class InvestmentController(
    private val prepareInvestmentUseCase: PrepareInvestmentUseCase,
    private val createInvestmentByAdminUseCase: CreateInvestmentByAdminUseCase,
    private val getMyInvestmentsQueryService: GetMyInvestmentsQueryService,
    private val getInvestmentDetailQueryService: GetInvestmentDetailQueryService,
    private val adminListInvestmentsQueryService: AdminListInvestmentsQueryService,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 100
        private const val DEFAULT_MANUAL_CONFIRM_METHOD = "수기확인"
        private val ALLOWED_SORT_FIELDS = setOf("investmentId", "depositDate")
        private val DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "investmentId")
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthUser principal: AuthPrincipal,
        @Valid @RequestBody request: CreateInvestmentRequest,
    ): PrepareInvestmentResponse {
        val result = prepareInvestmentUseCase.prepare(
            PrepareInvestmentCommand(userId = principal.userId, amount = request.amount),
        )
        return PrepareInvestmentResponse(paymentId = result.paymentId)
    }

    @GetMapping
    fun getMyInvestments(
        @AuthUser principal: AuthPrincipal,
        @PageableDefault(size = 10, sort = ["investmentId"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): InvestmentListResponse {
        return getMyInvestmentsQueryService.getMyInvestments(principal.userId, sanitizePageable(pageable))
    }

    @GetMapping("/{investmentId}")
    fun getDetail(
        @AuthUser principal: AuthPrincipal,
        @PathVariable investmentId: Long,
    ): InvestmentResult {
        return getInvestmentDetailQueryService.getDetail(investmentId, principal.userId)
    }

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    fun adminCreate(
        @Valid @RequestBody request: AdminCreateInvestmentRequest,
    ) {
        createInvestmentByAdminUseCase.create(
            AdminCreateInvestmentCommand(
                userId = request.userId,
                amount = request.amount,
                type = request.type,
                depositDate = request.depositDate ?: LocalDateTime.now(),
                conversionDate = request.conversionDate,
                confirmMethod = request.confirmMethod?.takeIf { it.isNotBlank() } ?: DEFAULT_MANUAL_CONFIRM_METHOD,
            ),
        )
    }

    @GetMapping("/admin")
    fun adminList(
        @PageableDefault(size = 10, sort = ["investmentId"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): AdminInvestmentListResponse {
        return adminListInvestmentsQueryService.findAll(sanitizePageable(pageable))
    }

    private fun sanitizePageable(pageable: Pageable): Pageable {
        val cappedSize = minOf(pageable.pageSize, MAX_PAGE_SIZE)
        val filteredOrders = pageable.sort.filter { it.property in ALLOWED_SORT_FIELDS }.toList()
        val sanitizedSort = if (filteredOrders.isEmpty()) DEFAULT_SORT else Sort.by(filteredOrders)
        return PageRequest.of(pageable.pageNumber, cappedSize, sanitizedSort)
    }
}
