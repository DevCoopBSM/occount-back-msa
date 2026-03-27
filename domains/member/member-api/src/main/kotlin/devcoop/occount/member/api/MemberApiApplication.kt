package devcoop.occount.member.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EntityScan(basePackages = ["devcoop.occount"])
@EnableJpaRepositories(basePackages = ["devcoop.occount"])
@SpringBootApplication(
    exclude = [UserDetailsServiceAutoConfiguration::class],
    scanBasePackages = ["devcoop.occount"],
)
class MemberApiApplication

fun main(args: Array<String>) {
    runApplication<MemberApiApplication>(*args)
}
