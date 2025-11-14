package org.walrex.infrastructure.adapters.outbound.messaging.producer;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * Productor para enviar mensajes de inbox a Kafka con Avro.
 *
 * CARACTERÍSTICAS:
 * - Serialización automática con Avro (Confluent Schema Registry)
 * - Envío reactivo con Mutiny
 * - Metadata y headers personalizados
 * - Registro automático de esquemas
 *
 * USO:
 * - Inyecta este productor en tus servicios
 * - Llama a sendMessage() con tu objeto Avro
 * - El esquema se registra automáticamente en Schema Registry
 */
@ApplicationScoped
@Slf4j
public class InboxMessageProducer {

    @Channel("inbox-messages-out")
    Emitter<Message<?>> emitter;

    /**
     * Envía un mensaje de inbox a Kafka.
     *
     * @param key       Clave del mensaje (típicamente el ID del mensaje)
     * @param message   Objeto Avro generado (ej: InboxMessage)
     * @return Uni que completa cuando el mensaje fue enviado
     *
     * EJEMPLO DE USO:
     * <pre>
     * // Importar tu clase Avro:
     * import com.walrex.avro.InboxMessage;
     * import com.walrex.avro.MessagePriority;
     * import com.walrex.avro.Attachment;
     *
     * // Crear mensaje Avro:
     * InboxMessage avroMessage = InboxMessage.newBuilder()
     *     .setMessageId(UUID.randomUUID().toString())
     *     .setSenderId("user123")
     *     .setSenderName("Juan Pérez")
     *     .setSenderEmail("juan@example.com")
     *     .setRecipientIds(List.of("user456", "user789"))
     *     .setSubject("Bienvenido")
     *     .setBody("Mensaje de bienvenida...")
     *     .setPriority(MessagePriority.NORMAL)
     *     .setAttachments(Collections.emptyList())
     *     .setCreatedAt(Instant.now().toEpochMilli())
     *     .setMetadata(Map.of("source", "api"))
     *     .build();
     *
     * // Enviar:
     * return inboxMessageProducer.sendMessage("msg-123", avroMessage);
     * </pre>
     */
    public Uni<Void> sendMessage(String key, Object message) {
        log.info("📤 Enviando mensaje - Key: {}", key);

        try {
            // Crear record de Kafka con key y value
            KafkaRecord<String, Object> record = KafkaRecord.of(key, message);

            // Opcional: Agregar headers personalizados
            // record = record.withHeader("correlation-id", correlationId.getBytes());

            // Enviar de manera reactiva
            return Uni.createFrom().completionStage(emitter.send(record))
                    .onItem().invoke(() ->
                        log.info("✅ Mensaje enviado exitosamente - Key: {}", key)
                    )
                    .onFailure().invoke(error ->
                        log.error("❌ Error enviando mensaje - Key: {}, Error: {}",
                                key, error.getMessage(), error)
                    )
                    .replaceWithVoid();

        } catch (Exception e) {
            log.error("❌ Error creando mensaje Kafka - Key: {}", key, e);
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * Envía un mensaje con metadata adicional.
     *
     * @param key           Clave del mensaje
     * @param message       Objeto Avro
     * @param correlationId ID de correlación para trazabilidad
     * @param source        Fuente del mensaje (ej: "api", "batch", "scheduler")
     */
    public Uni<Void> sendMessageWithMetadata(String key,
                                              Object message,
                                              String correlationId,
                                              String source) {
        log.info("📤 Enviando mensaje con metadata - Key: {}, CorrelationId: {}, Source: {}",
                key, correlationId, source);

        try {
            // Crear headers
            Headers headers = new RecordHeaders()
                    .add("correlation-id", correlationId.getBytes())
                    .add("source", source.getBytes())
                    .add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());

            // Crear metadata con los headers
            OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withHeaders(headers)
                    .build();

            // Crear record con metadata
            Message<?> recordWithMetadata = KafkaRecord.of(key, message)
                    .addMetadata(metadata);

            return Uni.createFrom().completionStage(emitter.send(recordWithMetadata))
                    .onItem().invoke(() ->
                        log.info("✅ Mensaje con metadata enviado - Key: {}", key)
                    )
                    .replaceWithVoid();

        } catch (Exception e) {
            log.error("❌ Error enviando mensaje con metadata - Key: {}", key, e);
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * Envía múltiples mensajes en batch (más eficiente).
     *
     * @param messages Mapa de key -> mensaje Avro
     * @return Uni que completa cuando todos los mensajes fueron enviados
     */
    public Uni<Void> sendBatch(java.util.Map<String, Object> messages) {
        log.info("📦 Enviando batch de {} mensajes", messages.size());

        return io.smallrye.mutiny.Multi.createFrom().iterable(messages.entrySet())
                .onItem().transformToUniAndConcatenate(entry ->
                    sendMessage(entry.getKey(), entry.getValue())
                )
                .collect().asList()
                .onItem().invoke(results ->
                    log.info("✅ Batch de {} mensajes enviado", results.size())
                )
                .replaceWithVoid();
    }
}