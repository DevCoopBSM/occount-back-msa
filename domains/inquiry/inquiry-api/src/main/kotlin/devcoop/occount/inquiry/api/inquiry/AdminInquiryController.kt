package devcoop.occount.inquiry.api.inquiry

import devcoop.occount.inquiry.application.query.admin.AdminInquiryDetailResponse
import devcoop.occount.inquiry.application.query.admin.AdminInquiryListResponse
import devcoop.occount.inquiry.application.query.admin.AdminInquiryQueryService
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 전용 문의 조회. 전체 사용자의 문의를 상태 필터·페이징으로 조회하고, 임의 문의의 상세를 본다.
 * 인가는 게이트웨이의 inquiries admin 하위 경로 규칙(ADMIN_ONLY)에서만 강제하며 다운스트림 재검증은 하지 않는다.
 */
@RestController
@RequestMapping("/inquiries/admin")
class AdminInquiryController(
    private val adminInquiryQueryService: AdminInquiryQueryService,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 100
        private val ALLOWED_SORT_FIELDS = setOf("createdAt", "status", "category")
        private val DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt")
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getInquiryList(
        @RequestParam(required = false) status: InquiryStatus?,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): AdminInquiryListResponse {
        return adminInquiryQueryService.getList(status, sanitizePageable(pageable))
    }

    @GetMapping("/{inquiryId}")
    @ResponseStatus(HttpStatus.OK)
    fun getInquiryDetail(
        @PathVariable inquiryId: Long,
    ): AdminInquiryDetailResponse {
        return adminInquiryQueryService.getDetail(inquiryId)
    }

    private fun sanitizePageable(pageable: Pageable): Pageable {
        val cappedSize = minOf(pageable.pageSize, MAX_PAGE_SIZE)
        val filteredOrders = pageable.sort.filter { it.property in ALLOWED_SORT_FIELDS }.toList()
        val sanitizedSort = if (filteredOrders.isEmpty()) DEFAULT_SORT else Sort.by(filteredOrders)
        return PageRequest.of(pageable.pageNumber, cappedSize, sanitizedSort)
    }
}
