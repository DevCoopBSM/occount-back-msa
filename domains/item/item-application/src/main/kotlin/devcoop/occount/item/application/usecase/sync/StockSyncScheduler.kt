package devcoop.occount.item.application.usecase.sync

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StockSyncScheduler(
    private val stockSyncFacade: StockSyncFacade,
) {
    @Scheduled(cron = "\${app.stock-sync.cron}")
    fun sync() {
        stockSyncFacade.apply()
    }
}
