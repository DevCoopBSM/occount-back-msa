package devcoop.occount.member.application.output

import devcoop.occount.member.application.login.KioskLoginAttempt

interface KioskLoginAttemptRepository {
    fun findByBarcode(userBarcode: String): KioskLoginAttempt?
    fun save(attempt: KioskLoginAttempt): KioskLoginAttempt

    /** 해당 barcode의 시도 기록이 없으면 no-op (로그인 성공 시 호출되며 기록이 없을 수 있다). */
    fun deleteByBarcode(userBarcode: String)
}
