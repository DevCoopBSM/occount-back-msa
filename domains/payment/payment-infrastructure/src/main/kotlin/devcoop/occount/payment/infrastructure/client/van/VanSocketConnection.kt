package devcoop.occount.payment.infrastructure.client.van

import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException

class VanSocketConnection(
    private val properties: VanProperties,
) {
    private val log = LoggerFactory.getLogger(VanSocketConnection::class.java)

    private var socket: Socket? = null

    fun ensureConnected(): Boolean {
        synchronized(this) {
            if (isConnected()) {
                return true
            }

            repeat(maxRetries) { attempt ->
                try {
                    close()
                    socket = Socket(properties.host, properties.port).apply {
                        soTimeout = receiveTimeoutMillis
                    }
                    log.info("VAN 서버 연결 성공: {}:{}", properties.host, properties.port)
                    return true
                } catch (e: IOException) {
                    log.error("연결 실패 (시도 {}/{}): {}", attempt + 1, maxRetries, e.message)
                    close()
                    if (attempt + 1 < maxRetries) {
                        Thread.sleep(retryDelayMillis)
                    }
                }
            }

            log.error("VAN 서버 연결 실패: {}:{}", properties.host, properties.port)
            return false
        }
    }

    fun reconnect(): Boolean {
        close()
        return ensureConnected()
    }

    fun refreshConnection(): Boolean {
        synchronized(this) {
            close()
        }
        return ensureConnected()
    }

    fun send(data: ByteArray) {
        synchronized(this) {
            val currentSocket = socket ?: throw IOException("VAN 서버 연결 실패")
            currentSocket.getOutputStream().write(data)
            currentSocket.getOutputStream().flush()
        }
    }

    fun receive(bufferSize: Int = 1024): ByteArray? {
        synchronized(this) {
            val currentSocket = socket ?: return null
            return try {
                val buffer = ByteArray(bufferSize)
                val readBytes = currentSocket.getInputStream().read(buffer)
                if (readBytes <= 0) {
                    close()
                    null
                } else {
                    buffer.copyOf(readBytes)
                }
            } catch (_: SocketTimeoutException) {
                null
            } catch (e: IOException) {
                close()
                throw e
            }
        }
    }

    fun close() {
        synchronized(this) {
            socket?.runCatching { close() }
            socket = null
        }
    }

    fun interrupt() {
        socket?.runCatching { close() }
    }

    fun logMessage(prefix: String, message: ByteArray) {
        log.info("{} 데이터: HEX={}, DEC={}, Length={} bytes", prefix, message.toHex(), message.toDecimalBytes(), message.size)
    }

    private fun isConnected(): Boolean {
        val currentSocket = socket
        return currentSocket != null && currentSocket.isConnected && !currentSocket.isClosed
    }

    private fun ByteArray.toHex(): String {
        return joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun ByteArray.toDecimalBytes(): String {
        return joinToString(prefix = "[", postfix = "]") { (it.toInt() and 0xff).toString() }
    }

    companion object {
        private const val maxRetries = 3
        private const val retryDelayMillis = 5_000L
        private const val receiveTimeoutMillis = 500
    }
}
