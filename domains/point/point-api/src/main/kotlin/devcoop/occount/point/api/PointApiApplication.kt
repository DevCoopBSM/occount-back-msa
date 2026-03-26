package devcoop.occount.point.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EntityScan(basePackages = ["devcoop.occount.db", "devcoop.occount.point.infrastructure.persistence"])
@EnableJpaRepositories(basePackages = ["devcoop.occount.db", "devcoop.occount.point.infrastructure.persistence"])
@SpringBootApplication(
    scanBasePackages = [
        "devcoop.occount.point",
        "devcoop.occount.db",
        "devcoop.occount.kafka.config",
        "devcoop.occount.kafka.outbox",
    ],
)
class PointApiApplication

fun main(args: Array<String>) {
    runApplication<PointApiApplication>(*args)
}
