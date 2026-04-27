package devcoop.occount.inquiry.application.usecase.email

import devcoop.occount.inquiry.application.exception.EmailAlreadyVerifiedException
import devcoop.occount.inquiry.application.exception.EmailVerificationCodeExpiredException
import devcoop.occount.inquiry.application.exception.EmailVerificationCodeInvalidException
import devcoop.occount.inquiry.application.exception.EmailVerificationCodeNotFoundException
import devcoop.occount.inquiry.application.output.EmailVerificationCodeRepository
import org.springframework.stereotype.Service

@Service
class VerifyEmailCodeUseCase(
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
) {
    fun verify(email: String, code: String) {
        val verificationCode = emailVerificationCodeRepository.findLatestByEmail(email)
            ?: throw EmailVerificationCodeNotFoundException()

        if (verificationCode.isVerified()) throw EmailAlreadyVerifiedException()

        if (verificationCode.isExpired()) {
            emailVerificationCodeRepository.save(verificationCode.expire())
            throw EmailVerificationCodeExpiredException()
        }

        if (verificationCode.code != code) throw EmailVerificationCodeInvalidException()

        emailVerificationCodeRepository.save(verificationCode.verify())
    }
}
