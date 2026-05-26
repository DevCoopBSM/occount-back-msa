package devcoop.occount.inquiry.api.support

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.error.ErrorResponse
import devcoop.occount.core.common.exception.BusinessBaseException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiAdviceHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val fieldErrors = e.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "유효하지 않은 값입니다.")
        }
        val globalErrors = e.bindingResult.globalErrors
            .mapNotNull { error -> error.defaultMessage?.let { "_error" to it } }
            .toMap()
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(fieldErrors + globalErrors)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("요청 형식이 올바르지 않습니다."))
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
