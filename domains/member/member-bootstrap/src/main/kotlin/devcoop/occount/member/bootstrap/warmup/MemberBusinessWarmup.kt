package devcoop.occount.member.bootstrap.warmup

import devcoop.occount.member.application.usecase.login.LoginUserUseCase
import devcoop.occount.member.application.usecase.login.MemberLoginRequest
import devcoop.occount.member.application.output.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
class MemberBusinessWarmup(
    private val loginUserUseCase: LoginUserUseCase,
    private val userRepository: UserRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val elapsed = measureTimeMillis {
            warmupMemberLogin()
            warmupRepositoryLookups()
        }
        log.info(
            "Member business warmup completed (login: {} rounds, lookup: {} rounds) in {} ms",
            JIT_WARMUP_COUNT,
            JIT_WARMUP_COUNT,
            elapsed,
        )
    }

    private fun warmupMemberLogin() {
        runCatching {
            repeat(JIT_WARMUP_COUNT) {
                loginUserUseCase.login(MemberLoginRequest(WARMUP_EMAIL, WARMUP_PASSWORD))
            }
        }.onFailure { exception ->
            log.warn("Member login warmup skipped, falling back to lookup warmup", exception)
        }
    }

    private fun warmupRepositoryLookups() {
        repeat(JIT_WARMUP_COUNT) {
            userRepository.findByEmail(WARMUP_EMAIL)
            userRepository.findByUserBarcode(WARMUP_BARCODE)
            userRepository.findById(-1L)
        }
    }

    companion object {
        private const val JIT_WARMUP_COUNT = 10
        private const val WARMUP_EMAIL = "warmup@warmup.internal"
        private const val WARMUP_PASSWORD = "password1234"
        private const val WARMUP_BARCODE = "000000000000"
        private val log = LoggerFactory.getLogger(MemberBusinessWarmup::class.java)
    }
}
