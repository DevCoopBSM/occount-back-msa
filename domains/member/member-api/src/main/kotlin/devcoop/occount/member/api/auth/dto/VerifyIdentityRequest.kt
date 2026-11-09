package devcoop.occount.member.api.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class VerifyIdentityRequest(
    @JsonProperty("identity_verification_id")
    @field:NotBlank(message = "본인인증 ID는 비어있을 수 없습니다.")
    val identityVerificationId: String,
)
