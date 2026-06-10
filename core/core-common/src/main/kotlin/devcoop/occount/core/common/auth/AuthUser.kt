package devcoop.occount.core.common.auth

/**
 * 인증된 사용자 정보를 컨트롤러 파라미터로 주입받기 위한 어노테이션.
 *
 * 게이트웨이가 검증 후 내려주는 인증 헤더를 [AuthPrincipal]로 변환해 주입한다.
 *
 * ```
 * @PostMapping("/pin/change")
 * fun changePin(@AuthUser principal: AuthPrincipal, @Valid @RequestBody request: ChangePinRequest) {
 *     changePinUseCase.changePin(principal.userId, request)
 * }
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthUser
