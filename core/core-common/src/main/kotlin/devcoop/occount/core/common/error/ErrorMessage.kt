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
    ITEM_ALREADY_EXISTS("이미 등록된 상품입니다."),
    ITEM_NOT_SYNCHRONIZED("상품이 토스와 동기화되지 않았습니다."),
    ITEM_CONCURRENT_UPDATE("다른 요청에 의해 상품이 변경되었습니다. 잠시 후 다시 시도해주세요."),
    ARIPICK_NOT_FOUND("아리픽 제안을 찾을 수 없습니다."),
    ARIPICK_ACCESS_DENIED("해당 아리픽 제안에 접근할 수 없습니다."),
    ARIPICK_POLICY_VIOLATION("매점 물품 수칙과 맞지 않습니다."),
    ARIPICK_INVALID_BLOCKED_KEYWORD("금지 키워드는 비어 있을 수 없습니다."),
    ARIPICK_BLOCKED_KEYWORD_ALREADY_EXISTS("이미 등록된 금지 키워드입니다."),
    ARIPICK_FOOD_SAFETY_UNAVAILABLE("식약처 연동에 실패했습니다. 잠시 후 다시 시도해주세요."),


    ORDER_NOT_FOUND("주문 정보를 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED("해당 주문에 접근할 수 없습니다."),
    ORDER_CANNOT_CANCEL("현재 상태에서는 주문을 취소할 수 없습니다."),
    ORDER_INVALID_TOTAL_PRICE("주문 항목의 총 금액이 일치하지 않습니다."),
    ORDER_INVALID_PAYMENT_TYPE("비회원 주문은 카드 결제만 지원합니다."),
    ORDER_TRANSACTION_FAILED("주문 트랜잭션 처리에 실패했습니다."),
    ORDER_UNREACHABLE_STATE("도달할 수 없는 주문 처리 상태입니다."),
    ORDER_CONCURRENCY_CONFLICT("동시 주문 처리 충돌이 발생했습니다. 잠시 후 다시 시도해주세요."),
    DUPLICATE_EVENT("중복 이벤트가 감지되었습니다."),


    PAYMENT_FAILED("결제 처리에 실패했습니다."),
    TRANSACTION_IN_PROGRESS("이미 진행 중인 거래가 있습니다."),
    INSUFFICIENT_POINTS("포인트가 부족합니다."),
    CARD_PAYMENT_FAILED("카드 결제에 실패했습니다."),
    PAYMENT_CANCELLED("결제가 취소되었습니다."),
    PAYMENT_TIMEOUT("결제 시간이 초과되었습니다."),
    INVALID_PAYMENT_REQUEST("잘못된 결제 요청입니다."),
    POINT_NOT_FOUND("포인트 정보를 조회할 수 없습니다."),
    POINT_ALREADY_INITIALIZED_EXCEPTION("포인트 정보가 이미 초기화 되았습니다."),
    PAYMENT_TYPE_INVALID("지원하지 않는 결제 유형입니다."),
    POINT_CHARGE_FAILED("포인트 충전에 실패했습니다."),
    POINT_DEDUCTION_FAILED("포인트 차감에 실패했습니다."),
    INVALID_CHARGE_AMOUNT("유효하지 않은 충전 금액입니다."),
    INVALID_DEDUCT_AMOUNT("유효하지 않은 차감 금액입니다."),
    PAYMENT_ALREADY_COMPLETED("이미 완료된 결제는 취소할 수 없습니다."),
    PAYMENT_LOG_NOT_FOUND("결제 로그를 찾을 수 없습니다."),
    PAYMENT_LOG_SAVE_FAILED("결제 로그 저장에 실패했습니다."),
    CHARGE_LOG_SAVE_FAILED("충전 기록 저장에 실패했습니다."),
    KIOSK_TERMINAL_NOT_FOUND("등록되지 않은 키오스크입니다."),

    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다."),
}
