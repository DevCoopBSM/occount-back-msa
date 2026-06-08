package devcoop.occount.member.infrastructure.portone

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "portone")
@Validated
data class PortOneProperties(
    @field:NotBlank(message = "PORTONE_SECRET_KEY가 설정되지 않았습니다.")
    val secretKey: String,
)
