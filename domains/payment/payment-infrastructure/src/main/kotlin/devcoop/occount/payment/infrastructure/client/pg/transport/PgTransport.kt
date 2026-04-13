package devcoop.occount.payment.infrastructure.client.pg.transport

interface PgTransport {
    fun ensureConnected()

    fun send(data: ByteArray)

    fun receive(): ByteArray

    fun close()
}
