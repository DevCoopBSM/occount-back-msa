package devcoop.occount.member.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EntityScan(basePackages = ["devcoop.occount.db"])
@EnableJpaRepositories(basePackages = ["devcoop.occount.db"])
@SpringBootApplication(
    exclude = [UserDetailsServiceAutoConfiguration::class],
    scanBasePackages = [
        "devcoop.occount.member.api",
        "devcoop.occount.member.application",
        "devcoop.occount.member.infrastructure",
        "devcoop.occount.db",
        "devcoop.occount.kafka",
    ],
)
class MemberApiApplication

fun main(args: Array<String>) {
    runApplication<MemberApiApplication>(*args)
}
