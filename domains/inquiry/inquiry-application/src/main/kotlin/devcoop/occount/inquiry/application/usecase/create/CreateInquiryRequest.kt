package devcoop.occount.inquiry.application.usecase.create

import devcoop.occount.inquiry.domain.inquiry.InquiryCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateInquiryRequest(
    @field:NotBlank(message = "제목을 입력해주세요.")
    @field:Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    val title: String,

    @field:NotBlank(message = "내용을 입력해주세요.")
    @field:Size(max = 2000, message = "내용은 2000자 이내로 입력해주세요.")
    val content: String,

    @field:NotNull(message = "카테고리를 선택해주세요.")
    val category: InquiryCategory,
)
