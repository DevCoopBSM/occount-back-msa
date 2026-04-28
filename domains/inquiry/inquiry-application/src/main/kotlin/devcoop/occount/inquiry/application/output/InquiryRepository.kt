package devcoop.occount.inquiry.application.output

import devcoop.occount.inquiry.domain.inquiry.Inquiry

interface InquiryRepository {
    fun save(inquiry: Inquiry): Inquiry
    fun findAllByUserId(userId: Long): List<Inquiry>
    fun findById(id: Long): Inquiry?
}
