package devcoop.occount.member.application.otp

/**
 * 이메일 OTP의 용도. 발송 시점에 고정되며, 소비(가입/비밀번호 변경) 시점에
 * 동일한 용도로만 사용할 수 있도록 바인딩한다. (한 용도로 인증된 OTP가 다른
 * 동작에 재사용되는 것을 방지)
 */
enum class OtpPurpose {
    REGISTER,
    PASSWORD_RESET,
}
