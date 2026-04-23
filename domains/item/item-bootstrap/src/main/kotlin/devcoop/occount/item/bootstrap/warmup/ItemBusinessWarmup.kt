package devcoop.occount.item.bootstrap.warmup

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.query.ItemQueryService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
class ItemBusinessWarmup(
    private val itemQueryService: ItemQueryService,
    private val itemRepository: ItemRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val elapsed = measureTimeMillis {
            // 실제 데이터 로드 — 쿼리 플랜 캐시 + OS 페이지 캐시
            repeat(DATA_WARMUP_COUNT) {
                itemQueryService.getAllItems()
                itemQueryService.getItemsWithoutBarcode()
            }
            // 단건 조회 — JIT 트리거용 (가벼운 쿼리)
            repeat(JIT_WARMUP_COUNT) {
                itemRepository.findByBarcode("000000000000")
                itemRepository.findById(-1L)
            }
        }
        log.info("Item business warmup completed (data: {} rounds, JIT: {} rounds) in {} ms",
            DATA_WARMUP_COUNT, JIT_WARMUP_COUNT, elapsed)
    }

    companion object {
        private const val DATA_WARMUP_COUNT = 3
        private const val JIT_WARMUP_COUNT = 200
        private val log = LoggerFactory.getLogger(ItemBusinessWarmup::class.java)
    }
}
