package devcoop.occount.inquiry.api.inquiry

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.inquiry.application.query.GetInquiryDetailQueryService
import devcoop.occount.inquiry.application.query.GetInquiryListQueryService
import devcoop.occount.inquiry.application.shared.InquiryDetailResponse
import devcoop.occount.inquiry.application.shared.InquiryListResponse
import devcoop.occount.inquiry.application.usecase.create.CreateInquiryRequest
import devcoop.occount.inquiry.application.usecase.create.CreateInquiryResponse
import devcoop.occount.inquiry.application.usecase.create.CreateInquiryUseCase
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
@RequestMapping("/inquiries")
class InquiryController(
    private val createInquiryUseCase: CreateInquiryUseCase,
    private val getInquiryListQueryService: GetInquiryListQueryService,
    private val getInquiryDetailQueryService: GetInquiryDetailQueryService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createInquiry(
        @Valid @RequestBody request: CreateInquiryRequest,
        httpRequest: HttpServletRequest,
    ): CreateInquiryResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return createInquiryUseCase.create(userId, request)
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getInquiryList(httpRequest: HttpServletRequest): InquiryListResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getInquiryListQueryService.getList(userId)
    }

    @GetMapping("/{inquiryId}")
    @ResponseStatus(HttpStatus.OK)
    fun getInquiryDetail(
        @PathVariable inquiryId: Long,
        httpRequest: HttpServletRequest,
    ): InquiryDetailResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getInquiryDetailQueryService.getDetail(userId, inquiryId)
    }
}
