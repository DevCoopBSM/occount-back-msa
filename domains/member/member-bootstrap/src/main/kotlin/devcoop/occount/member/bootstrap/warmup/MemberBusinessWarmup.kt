package devcoop.occount.member.bootstrap.warmup

import devcoop.occount.member.application.output.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
class MemberBusinessWarmup(
    private val userRepository: UserRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val elapsed = measureTimeMillis {
            repeat(3) {
                userRepository.findByEmail("warmup@warmup.internal")
                userRepository.findByUserBarcode("000000000000")
                userRepository.findById(-1L)
            }
        }
        log.info("Member business warmup completed in {} ms", elapsed)
    }

    companion object {
        private val log = LoggerFactory.getLogger(MemberBusinessWarmup::class.java)
    }
}
