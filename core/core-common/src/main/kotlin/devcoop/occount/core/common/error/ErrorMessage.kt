package devcoop.occount.core.common.error

enum class ErrorMessage(
    val message: String,
) {

    INVALID_TOKEN("잘못된 토큰 형식입니다."),
    EXPIRED_TOKEN("만료된 토큰입니다."),
    ACCESS_DENIED("접근 권한이 없습니다."),


    USER_NOT_FOUND("존재하지 않는 유저입니다."),
    USER_ALREADY_EXISTS("이미 존재하는 유저입니다."),
    INVALID_PASSWORD("비밀번호가 일치하지 않습니다."),


    INVALID_PIN("핀번호가 틀렸습니다."),


    ITEM_STOCK_NEGATIVE("요청으로 인해 재고가 부족합니다. 재고 수량을 확인해주세요."),
    ITEM_NOT_FOUND("현재 등록되지 않은 상품입니다."),
    ITEM_NOT_SYNCHRONIZED("상품이 토스와 동기화되지 않았습니다."),


    PAYMENT_FAILED("결제 처리에 실패했습니다."),
    TRANSACTION_IN_PROGRESS("이미 진행 중인 거래가 있습니다."),
    INSUFFICIENT_POINTS("포인트가 부족합니다."),
    CARD_PAYMENT_FAILED("카드 결제에 실패했습니다."),
    PAYMENT_TIMEOUT("결제 시간이 초과되었습니다."),
    INVALID_PAYMENT_REQUEST("잘못된 결제 요청입니다."),
    PAYMENT_TYPE_INVALID("지원하지 않는 결제 유형입니다."),
    POINT_CHARGE_FAILED("포인트 충전에 실패했습니다."),
    POINT_DEDUCTION_FAILED("포인트 차감에 실패했습니다."),
    INVALID_CHARGE_AMOUNT("유효하지 않은 충전 금액입니다."),
    PAYMENT_LOG_NOT_FOUND("결제 로그를 찾을 수 없습니다."),
    PAYMENT_LOG_SAVE_FAILED("결제 로그 저장에 실패했습니다."),
    CHARGE_LOG_SAVE_FAILED("충전 기록 저장에 실패했습니다."),
}
