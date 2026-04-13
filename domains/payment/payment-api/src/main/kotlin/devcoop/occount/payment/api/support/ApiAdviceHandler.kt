package devcoop.occount.payment.api.support

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
            val field = (it as FieldError).field
            val message = it.defaultMessage ?: "Invalid value"
            field to message
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors)
    }

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
            -> HttpStatus.UNAUTHORIZED

            ErrorMessage.USER_NOT_FOUND,
            ErrorMessage.POINT_NOT_FOUND,
            ErrorMessage.ITEM_NOT_FOUND,
            ErrorMessage.ITEM_NOT_SYNCHRONIZED,
            ErrorMessage.PAYMENT_LOG_NOT_FOUND,
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
}
