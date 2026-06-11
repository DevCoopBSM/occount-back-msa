package devcoop.occount.member.api.support

import devcoop.occount.core.common.error.ErrorResponse
import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiAdviceHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val errors = e.bindingResult.allErrors.associate {
            // FieldError.field는 Kotlin 프로퍼티명(camelCase)이지만, API는 전역
            // SNAKE_CASE 직렬화 전략을 쓰므로 요청 바디와 동일한 snake_case 키로 응답한다.
            val field = toSnakeCase((it as FieldError).field)
            val message = it.defaultMessage ?: "Invalid value"
            field to message
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors)
    }

    private fun toSnakeCase(field: String): String =
        field.replace(SNAKE_CASE_BOUNDARY, "$1_$2").lowercase()

    @ExceptionHandler(BusinessBaseException::class)
    fun handleBusinessBaseException(e: BusinessBaseException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(resolveStatus(e.errorMessage))
            .body(ErrorResponse.of(e.errorMessage))
    }

    private fun resolveStatus(errorMessage: ErrorMessage): HttpStatus {
        return when (errorMessage) {
            ErrorMessage.INVALID_TOKEN,
            ErrorMessage.EXPIRED_TOKEN,
            ErrorMessage.INVALID_PASSWORD,
            ErrorMessage.INVALID_PIN,
            ErrorMessage.PIN_CHANGE_TICKET_NOT_FOUND,
            ErrorMessage.PIN_CHANGE_TICKET_EXPIRED,
            ErrorMessage.OTP_EXPIRED,
            ErrorMessage.OTP_MISMATCH,
            ErrorMessage.OTP_LOCKED,
            ErrorMessage.EMAIL_NOT_VERIFIED,
            -> HttpStatus.UNAUTHORIZED

            ErrorMessage.OTP_RATE_LIMITED,
            ErrorMessage.KIOSK_LOGIN_LOCKED,
            -> HttpStatus.TOO_MANY_REQUESTS

            ErrorMessage.IDENTITY_VERIFICATION_FAILED -> HttpStatus.BAD_GATEWAY

            ErrorMessage.IDENTITY_NOT_VERIFIED -> HttpStatus.BAD_REQUEST

            ErrorMessage.USER_NOT_FOUND,
            ErrorMessage.ITEM_NOT_FOUND,
            ErrorMessage.ITEM_NOT_SYNCHRONIZED,
            ErrorMessage.PAYMENT_LOG_NOT_FOUND,
            ErrorMessage.OTP_NOT_FOUND,
            -> HttpStatus.NOT_FOUND

            ErrorMessage.USER_ALREADY_EXISTS,
            ErrorMessage.TRANSACTION_IN_PROGRESS,
            -> HttpStatus.CONFLICT

            ErrorMessage.PAYMENT_TIMEOUT -> HttpStatus.REQUEST_TIMEOUT

            ErrorMessage.PAYMENT_LOG_SAVE_FAILED,
            ErrorMessage.CHARGE_LOG_SAVE_FAILED,
            -> HttpStatus.INTERNAL_SERVER_ERROR

            else -> HttpStatus.BAD_REQUEST
        }
    }

    companion object {
        // camelCase 경계(소문자/숫자 뒤 대문자)에 '_'를 삽입 — Jackson SNAKE_CASE 전략과 동일 결과
        private val SNAKE_CASE_BOUNDARY = Regex("([a-z0-9])([A-Z])")
    }
}
