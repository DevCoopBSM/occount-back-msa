package devcoop.occount.investment.domain.investment

/**
 * 출자 유형. 레거시의 정수 코드(0/1/2)를 대체한다.
 */
enum class InvestmentType {
    DEPOSIT, // 출자금
    DONATION, // 기부금
    RETURNED, // 반환됨
}
