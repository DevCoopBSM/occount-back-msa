package devcoop.occount.payment.infrastructure.client.pg.transport

import devcoop.occount.payment.infrastructure.client.pg.config.PgProperties
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class SocketPgTransport(
    private val properties: PgProperties,
) : PgTransport {
    private val log = LoggerFactory.getLogger(SocketPgTransport::class.java)
    private val lock = ReentrantLock()

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override fun ensureConnected() {
        lock.withLock {
            if (socket?.isConnected == true && socket?.isClosed == false) {
                return
            }

            close()

            var lastException: Exception? = null
            repeat(properties.maxRetries) { attempt ->
                try {
                    val createdSocket = Socket()
                    createdSocket.connect(
                        InetSocketAddress(properties.host, properties.port),
                        properties.connectTimeoutMillis,
                    )
                    createdSocket.soTimeout = properties.readTimeoutMillis

                    socket = createdSocket
                    inputStream = createdSocket.getInputStream()
                    outputStream = createdSocket.getOutputStream()
                    log.info("PG terminal connected to {}:{}", properties.host, properties.port)
                    return
                } catch (ex: Exception) {
                    lastException = ex
                    log.warn(
                        "Failed to connect to PG terminal (attempt {}/{}): {}",
                        attempt + 1,
                        properties.maxRetries,
                        ex.message,
                    )
                    if (attempt < properties.maxRetries - 1) {
                        Thread.sleep(properties.retryDelayMillis)
                    }
                }
            }

            throw IllegalStateException("Failed to connect to PG terminal", lastException)
        }
    }

    override fun send(data: ByteArray) {
        lock.withLock {
            ensureConnected()
            val stream = outputStream ?: throw IllegalStateException("PG terminal output stream is not available")
            stream.write(data)
            stream.flush()
        }
    }

    override fun receive(): ByteArray {
        lock.withLock {
            ensureConnected()
            val stream = inputStream ?: throw IllegalStateException("PG terminal input stream is not available")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val readSize = stream.read(buffer)
            if (readSize < 0) {
                close()
                throw IllegalStateException("PG terminal connection closed")
            }
            return buffer.copyOf(readSize)
        }
    }

    @PreDestroy
    override fun close() {
        lock.withLock {
            runCatching { inputStream?.close() }
            runCatching { outputStream?.close() }
            runCatching { socket?.close() }
            inputStream = null
            outputStream = null
            socket = null
        }
    }
}
