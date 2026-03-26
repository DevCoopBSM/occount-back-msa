package devcoop.occount.item.api

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
        "devcoop.occount.item",
        "devcoop.occount.db",
        "devcoop.occount.kafka",
    ],
)
class ItemApiApplication

fun main(args: Array<String>) {
    runApplication<ItemApiApplication>(*args)
}
