package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.application.exception.KioskTerminalNotFoundException
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class VanTerminalRegistry(
    properties: VanProperties,
    messageBuilder: VanMessageBuilder,
    messageParser: VanMessageParser,
    protocolSpec: VanProtocolSpec,
) {
    private val log = LoggerFactory.getLogger(VanTerminalRegistry::class.java)

    private val terminals: Map<Int, VanTerminalClient> = properties.terminals
        .filter { it.value.host.isNotBlank() }
        .mapValues { (kioskId, terminal) ->
            log.info("VAN 단말기 등록 - 키오스크 {}: {}:{}", kioskId, terminal.host, terminal.port)
            VanTerminalClient(terminal, messageBuilder, messageParser, protocolSpec)
        }

    fun get(kioskId: String): VanTerminalClient {
        val id = kioskId.toIntOrNull() ?: throw KioskTerminalNotFoundException()
        return terminals[id] ?: throw KioskTerminalNotFoundException()
    }

    @PreDestroy
    fun close() {
        terminals.values.forEach { it.close() }
    }
}
