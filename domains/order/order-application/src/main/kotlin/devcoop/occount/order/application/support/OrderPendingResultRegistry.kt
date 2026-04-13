package devcoop.occount.order.application.support

import devcoop.occount.order.application.shared.OrderResponse
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
class OrderPendingResultRegistry {
    private val pendingFutures = ConcurrentHashMap<String, CompletableFuture<OrderResponse>>()

    fun registerPendingOrder(
        orderId: String,
        timeoutSeconds: Long,
        onTimeout: () -> Unit,
    ): CompletableFuture<OrderResponse> {
        val resultFuture = CompletableFuture<OrderResponse>()
        pendingFutures[orderId] = resultFuture

        return resultFuture
            .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .exceptionally { throwable ->
                val cause = unwrap(throwable)
                if (cause is TimeoutException) {
                    onTimeout()
                }

                pendingFutures.remove(orderId)
                throw cause
            }
    }

    fun completePendingOrder(orderId: String, response: OrderResponse) {
        pendingFutures.remove(orderId)?.complete(response)
    }

    fun failPendingOrder(orderId: String, throwable: Throwable) {
        pendingFutures.remove(orderId)?.completeExceptionally(throwable)
    }

    fun removePendingOrder(orderId: String) {
        pendingFutures.remove(orderId)
    }

    private fun unwrap(throwable: Throwable): Throwable {
        return throwable.cause?.takeIf {
            throwable is CompletionException || throwable is ExecutionException
        } ?: throwable
    }
}
