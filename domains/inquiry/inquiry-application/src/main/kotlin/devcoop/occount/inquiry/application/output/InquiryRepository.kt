package devcoop.occount.inquiry.application.output

import devcoop.occount.inquiry.domain.inquiry.Inquiry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface InquiryRepository {
    fun save(inquiry: Inquiry): Inquiry
    fun findPageByUserId(userId: Long, pageable: Pageable): Page<Inquiry>
    fun findById(id: Long): Inquiry?
}
