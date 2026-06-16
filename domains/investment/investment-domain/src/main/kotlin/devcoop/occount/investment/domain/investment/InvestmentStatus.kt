package devcoop.occount.investment.domain.investment

/**
 * 출자 처리 상태.
 * - PENDING: 결제 시작 후 PortOne 결제 확정 대기.
 * - CONFIRMED: 결제 확정(웹훅) 또는 관리자 수기 등록 완료.
 * - CANCELLED: 취소(현재 범위에서는 생성하지 않음).
 */
enum class InvestmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
}
