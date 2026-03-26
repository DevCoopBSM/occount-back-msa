package devcoop.occount.payment.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EntityScan(basePackages = ["devcoop.occount.db"])
@EnableJpaRepositories(basePackages = ["devcoop.occount.db"])
@SpringBootApplication(
    scanBasePackages = [
        "devcoop.occount.payment",
        "devcoop.occount.db",
        "devcoop.occount.kafka",
    ],
)
class PaymentApiApplication

fun main(args: Array<String>) {
    runApplication<PaymentApiApplication>(*args)
}
