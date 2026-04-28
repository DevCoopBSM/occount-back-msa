package devcoop.occount.inquiry.api.support

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.error.ErrorResponse
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

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(ErrorMessage.INTERNAL_SERVER_ERROR))
    }

    private fun resolveStatus(errorMessage: ErrorMessage): HttpStatus = when (errorMessage) {
        ErrorMessage.INQUIRY_NOT_FOUND -> HttpStatus.NOT_FOUND
        ErrorMessage.INQUIRY_ACCESS_DENIED -> HttpStatus.FORBIDDEN
        ErrorMessage.INVALID_TOKEN,
        ErrorMessage.EXPIRED_TOKEN -> HttpStatus.UNAUTHORIZED
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }
}
