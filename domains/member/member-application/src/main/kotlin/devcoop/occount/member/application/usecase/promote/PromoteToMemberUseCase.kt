package devcoop.occount.member.application.usecase.promote

import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.Role
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 출자 확정 이벤트에 따라 회원을 조합원(ROLE_MEMBER)으로 승격한다.
 * 이미 조합원 이상이면 아무 일도 하지 않으며(멱등), 유저가 없으면 조용히 무시한다
 * (출자 도메인과 회원 도메인의 생성 순서·정합성에 결합되지 않도록).
 */
@Service
class PromoteToMemberUseCase(
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun promote(userId: Long) {
        val user = userRepository.findById(userId)
        if (user == null) {
            log.warn("출자 승격 대상 유저를 찾을 수 없음 - userId={}", userId)
            return
        }
        if (user.getRole() != Role.ROLE_USER) {
            return
        }
        userRepository.save(user.promoteToMember())
        log.info("회원 등급을 ROLE_MEMBER 로 승격 - userId={}", userId)
    }
}
