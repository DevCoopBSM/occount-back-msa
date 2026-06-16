package devcoop.occount.investment.application.support

import devcoop.occount.core.common.event.EventPublisher

class FakeEventPublisher : EventPublisher {
    data class Published(val topic: String, val key: String, val eventType: String, val payload: Any)

    val published = mutableListOf<Published>()

    override fun publish(topic: String, key: String, eventType: String, payload: Any) {
        published += Published(topic, key, eventType, payload)
    }
}
