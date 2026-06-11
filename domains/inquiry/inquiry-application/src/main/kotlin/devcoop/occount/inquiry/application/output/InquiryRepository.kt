package devcoop.occount.inquiry.application.output

import devcoop.occount.inquiry.domain.inquiry.Inquiry
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface InquiryRepository {
    fun save(inquiry: Inquiry): Inquiry
    fun findPageByUserId(userId: Long, pageable: Pageable): Page<Inquiry>
    fun findById(id: Long): Inquiry?

    /** 어드민 전체 조회. status 가 null 이면 전체, 아니면 해당 상태만 페이징 조회한다. */
    fun findPage(status: InquiryStatus?, pageable: Pageable): Page<Inquiry>
}
