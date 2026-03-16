package devcoop.occount.core.common.event

interface EventPublisher {
    fun publish(topic: String, key: String, eventType: String, payload: Any)
}
