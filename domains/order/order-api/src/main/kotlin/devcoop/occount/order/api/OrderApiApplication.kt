package devcoop.occount.order.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EntityScan(basePackages = ["devcoop.occount"])
@EnableJpaRepositories(basePackages = ["devcoop.occount"])
@SpringBootApplication(
    scanBasePackages = ["devcoop.occount"],
)
class OrderApiApplication

fun main(args: Array<String>) {
    runApplication<OrderApiApplication>(*args)
}
