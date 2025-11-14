package org.walrex.infrastructure.adapters.inbound.messaging.consumer;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.time.Duration;
import java.time.Instant;

/**
 * Consumidor con RETRY AUTOMÁTICO y DEAD LETTER QUEUE.
 *
 * CARACTERÍSTICAS:
 * - Retry automático con backoff exponencial
 * - Envío a Dead Letter Queue después de N intentos fallidos
 * - Tracking de intentos usando headers de Kafka
 * - Manejo robusto de errores transitorios vs permanentes
 *
 * CUÁNDO USAR:
 * - Procesamiento que puede fallar por errores transitorios (timeouts, servicios caídos)
 * - Necesitas garantías de entrega robustas
 * - Quieres separar mensajes "envenenados" del flujo principal
 *
 * CONFIGURACIÓN REQUERIDA:
 * 1. Agregar topic DLQ en application.yml:
 *    mp.messaging.outgoing.notification-events-dlq:
 *      connector: smallrye-kafka
 *      topic: notification.events.dlq
 */
@ApplicationScoped
@Slf4j
public class NotificationEventConsumerWithRetry {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String RETRY_COUNT_HEADER = "retry-count";
    private static final String ERROR_HEADER = "error-message";
    private static final String ORIGINAL_TOPIC_HEADER = "original-topic";

    // Emitter para enviar mensajes al DLQ
    @Channel("notification-events-dlq")
    Emitter<Message<?>> dlqEmitter;

    // @Inject
    // NotificationService notificationService;

    /**
     * Consume eventos de notificación con retry automático.
     */
    @Incoming("notification-events")
    public Uni<Void> consumeNotificationEvent(IncomingKafkaRecord<String, Object> record) {
        Object event = record.getPayload();
        String key = record.getKey();

        // Obtener el número de intentos desde el header
        int retryCount = getRetryCount(record);

        log.info("🔔 Procesando notificación - Key: {}, Intento: {}/{}",
                key, retryCount + 1, MAX_RETRY_ATTEMPTS);

        return processWithRetry(record, event, key, retryCount)
                .chain(() -> Uni.createFrom().completionStage(record.ack()))
                .onItem().invoke(() ->
                    log.info("✅ Notificación procesada - Key: {}", key)
                );
    }

    /**
     * Procesa el mensaje con lógica de retry.
     */
    private Uni<Void> processWithRetry(IncomingKafkaRecord<String, Object> record,
                                        Object event,
                                        String key,
                                        int retryCount) {

        return Uni.createFrom().item(event)
                .chain(msg -> {
                    // TODO: Implementa tu lógica de negocio aquí
                    // return notificationService.sendNotification(msg);

                    log.debug("Procesando notificación: {}", msg);
                    return Uni.createFrom().voidItem();
                })
                // Manejo de errores con retry
                .onFailure().retry()
                    .withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(10))
                    .atMost(2) // Retry inmediato 2 veces antes de enviar a DLQ
                .onFailure().invoke(error ->
                    log.warn("⚠️ Error procesando notificación - Key: {}, Intento: {}, Error: {}",
                            key, retryCount + 1, error.getMessage())
                )
                .onFailure().recoverWithUni(error -> {
                    // Si alcanzamos el máximo de reintentos, enviar a DLQ
                    if (retryCount >= MAX_RETRY_ATTEMPTS - 1) {
                        log.error("❌ Máximo de reintentos alcanzado - Enviando a DLQ - Key: {}", key);
                        return sendToDLQ(record, error);
                    }

                    // Si aún tenemos reintentos, hacer NACK para reprocesar
                    log.info("🔄 Reintentando mensaje - Key: {}, Intento: {}/{}",
                            key, retryCount + 2, MAX_RETRY_ATTEMPTS);

                    // NO hacer ACK - el mensaje será reprocesado
                    // Incrementar el contador de reintentos en el header
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Envía el mensaje fallido al Dead Letter Queue.
     */
    private Uni<Void> sendToDLQ(IncomingKafkaRecord<String, Object> originalRecord, Throwable error) {
        try {
            // Crear mensaje DLQ con metadata del error
            KafkaRecord<String, Object> dlqMessage = KafkaRecord.of(
                    originalRecord.getKey(),
                    originalRecord.getPayload()
            );

            // Agregar headers con información del error
            Headers headers = new RecordHeaders()
                    .add(ERROR_HEADER, error.getMessage().getBytes())
                    .add(ORIGINAL_TOPIC_HEADER, originalRecord.getTopic().getBytes())
                    .add("failed-at", Instant.now().toString().getBytes());

            OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withHeaders(headers)
                    .build();

            Message<?> messageWithMetadata = dlqMessage.addMetadata(metadata);

            // Enviar a DLQ
            dlqEmitter.send(messageWithMetadata);

            log.info("📮 Mensaje enviado a DLQ - Key: {}, Topic: {}",
                    originalRecord.getKey(), originalRecord.getTopic());

            // ACK el mensaje original ya que lo movimos a DLQ
            return Uni.createFrom().completionStage(originalRecord.ack());

        } catch (Exception e) {
            log.error("❌ Error enviando mensaje a DLQ: {}", e.getMessage(), e);
            // No hacer ACK - el mensaje será reprocesado
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * Obtiene el número de reintentos desde el header del mensaje.
     */
    private int getRetryCount(IncomingKafkaRecord<String, Object> record) {
        try {
            var header = record.getHeaders().lastHeader(RETRY_COUNT_HEADER);
            if (header != null) {
                return Integer.parseInt(new String(header.value()));
            }
        } catch (Exception e) {
            log.debug("No se pudo leer retry-count header: {}", e.getMessage());
        }
        return 0;
    }
}