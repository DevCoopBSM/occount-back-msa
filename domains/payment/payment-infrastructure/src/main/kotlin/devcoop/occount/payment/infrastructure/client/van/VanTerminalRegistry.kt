package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.application.exception.KioskTerminalNotFoundException
import io.micrometer.tracing.Tracer
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

@Component
class VanTerminalRegistry(
    properties: VanProperties,
    messageBuilder: VanMessageBuilder,
    messageParser: VanMessageParser,
    protocolSpec: VanProtocolSpec,
    tracerProvider: ObjectProvider<Tracer>,
) {
    private val log = LoggerFactory.getLogger(VanTerminalRegistry::class.java)

    private val terminals: Map<Int, VanTerminalClient> = properties.terminals
        .filter { it.value.host.isNotBlank() }
        .mapValues { (kioskId, terminal) ->
            log.info("VAN 단말기 등록 - 키오스크 {}: {}:{}", kioskId, terminal.host, terminal.port)
            VanTerminalClient(terminal, messageBuilder, messageParser, protocolSpec, tracerProvider.ifAvailable)
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
