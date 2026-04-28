package devcoop.occount.inquiry.application.usecase.create

import devcoop.occount.inquiry.domain.inquiry.InquiryCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateInquiryRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,

    @field:NotBlank
    @field:Size(max = 2000)
    val content: String,

    @field:NotNull
    val category: InquiryCategory,
)
