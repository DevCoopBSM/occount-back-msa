package devcoop.occount.payment.infrastructure.client.van

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

class VanConfigurationTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration::class.java)
        .withPropertyValues(
            "van.api.terminals[1].host=localhost",
            "van.api.terminals[1].port=5555",
            "van.api.protocol.stx=04",
            "van.api.protocol.etx=05",
            "van.api.protocol.separator=1f",
            "van.api.protocol.record-separator=1d",
            "van.api.protocol.blank=21",
            "van.api.protocol.ack=07",
            "van.api.protocol.dle=11",
            "van.api.protocol.form-feed=0e",
            "van.api.protocol.nak=16",
            "van.api.protocol.transaction-timeout-seconds=30",
            "van.api.message.payment-service-type=1101",
            "van.api.message.refund-service-type=2201",
            "van.api.message.terminal-close-service-type=9901",
            "van.api.message.terminal-close-filler=7000700",
            "van.api.message.transaction-type=77",
            "van.api.message.installment-months=09",
            "van.api.codes.cancel-message-type=8899",
            "van.api.codes.reject-status-prefix=ZX",
            "van.api.codes.card-insert-keyword=TEST_INSERT",
        )

    @Test
    fun `binds van properties and creates parser beans`() {
        contextRunner.run { context ->
            assertThat(context).hasNotFailed()

            val properties = context.getBean(VanProperties::class.java)
            val protocolSpec = context.getBean(VanProtocolSpec::class.java)

            assertThat(properties.terminals[1]?.host).isEqualTo("localhost")
            assertThat(properties.terminals[1]?.port).isEqualTo(5555)
            assertThat(properties.protocol.separator).isEqualTo("1f")
            assertThat(properties.message.paymentServiceType).isEqualTo("1101")
            assertThat(context).hasSingleBean(VanProtocolCodes::class.java)
            assertThat(context).hasSingleBean(VanResponseParser::class.java)
            assertThat(context).hasSingleBean(VanMessageParser::class.java)
            assertThat(protocolSpec.separatorChar).isEqualTo(0x1f.toChar())
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(
        VanConfig::class,
        VanProtocolCodes::class,
        VanProtocolSpec::class,
        VanResponseParser::class,
        VanMessageParser::class,
    )
    private class TestConfiguration
}
