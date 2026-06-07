package devcoop.occount.inquiry.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@EntityScan(basePackages = ["devcoop.occount"])
@EnableJpaRepositories(basePackages = ["devcoop.occount"])
@SpringBootApplication(scanBasePackages = ["devcoop.occount"])
class InquiryBootstrapApplication

fun main(args: Array<String>) {
    runApplication<InquiryBootstrapApplication>(*args)
}
