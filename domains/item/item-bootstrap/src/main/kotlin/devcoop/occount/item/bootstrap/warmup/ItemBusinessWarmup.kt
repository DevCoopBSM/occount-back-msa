package devcoop.occount.item.bootstrap.warmup

import devcoop.occount.item.application.query.ItemQueryService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
class ItemBusinessWarmup(
    private val itemQueryService: ItemQueryService,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val elapsed = measureTimeMillis {
            repeat(3) {
                itemQueryService.getAllItems()
                itemQueryService.getItemsWithoutBarcode()
            }
        }
        log.info("Item business warmup completed in {} ms", elapsed)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ItemBusinessWarmup::class.java)
    }
}
