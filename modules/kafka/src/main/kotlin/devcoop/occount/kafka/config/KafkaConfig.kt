package devcoop.occount.kafka.config

import devcoop.occount.core.common.event.DomainTopics
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@EnableKafka
@Configuration
class KafkaConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    /**
     * `payment.command.v1` 은 kioskId 파티셔닝으로 VAN 단말별 병렬 결제를 처리한다(consumer concurrency=6).
     * 파티션 수를 코드로 선언해 강제한다 — auto-create 로 두면 기본 1파티션이 되어 concurrency 가
     * 무력화되고 단일 컨슈머 head-of-line 블로킹으로 전 키오스크 결제가 지연된다(2026-06/2026-07 인시던트).
     * KafkaAdmin 은 파티션을 늘리기만 하므로(줄이지 않음) 기존 토픽에도 안전하게 적용된다.
     */
    @Bean
    fun paymentCommandTopic(): NewTopic =
        TopicBuilder.name(DomainTopics.PAYMENT_COMMANDS)
            .partitions(6)
            .replicas(1)
            .build()

    @Bean
    fun producerFactory(openTelemetry: ObjectProvider<OpenTelemetry>): ProducerFactory<String, String> {
        val factory = DefaultKafkaProducerFactory<String, String>(mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 5,
        ))
        openTelemetry.ifAvailable?.let { otel ->
            factory.addPostProcessor { KafkaTelemetry.create(otel).wrap(it) }
        }
        return factory
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> =
        KafkaTemplate(producerFactory)

    @Bean
    fun kafkaListenerContainerFactory(
        kafkaConsumerFactory: ConsumerFactory<String, String>,
        kafkaTemplate: KafkaTemplate<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(kafkaConsumerFactory)
            // 처리 실패 메시지는 재시도하지 않고(0회) 즉시 <topic>.DLT 로 격리한다.
            // 기본 핸들러(FixedBackOff(0, 9))가 죽은 결제를 10회 재시도하며 단일 파티션
            // 컨슈머를 통째로 막던 head-of-line 블로킹을 방지한다.
            val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
                TopicPartition("${record.topic()}.DLT", -1)
            }.apply {
                // DLT 전송이 실패해도(토픽 부재 등) 컨슈머를 재시도로 막지 않는다(로그 후 진행).
                setFailIfSendResultIsError(false)
            }
            setCommonErrorHandler(DefaultErrorHandler(recoverer, FixedBackOff(0L, 0L)))
        }

    @Bean
    fun kafkaConsumerFactory(openTelemetry: ObjectProvider<OpenTelemetry>): ConsumerFactory<String, String> {
        val factory = DefaultKafkaConsumerFactory<String, String>(mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to 500,
            ConsumerConfig.FETCH_MIN_BYTES_CONFIG to 1,
            ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 500,
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to 30_000,
        ))
        openTelemetry.ifAvailable?.let { otel ->
            factory.addPostProcessor { KafkaTelemetry.create(otel).wrap(it) }
        }
        return factory
    }
}
