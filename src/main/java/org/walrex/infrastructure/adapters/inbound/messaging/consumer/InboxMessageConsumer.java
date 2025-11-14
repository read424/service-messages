package org.walrex.infrastructure.adapters.inbound.messaging.consumer;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * Consumidor REACTIVO simple para mensajes de inbox.
 *
 * CARACTERÍSTICAS:
 * - Procesa mensajes uno a la vez de manera reactiva
 * - Auto-commit deshabilitado (commit manual tras procesar exitosamente)
 * - Manejo de errores con logging
 * - No bloqueante, usa el event loop de Vert.x
 *
 * CUÁNDO USAR:
 * - Procesamiento simple que no requiere operaciones bloqueantes
 * - Necesitas control fino sobre el commit de offsets
 * - El procesamiento es rápido (< 100ms por mensaje)
 */
@ApplicationScoped
@Slf4j
public class InboxMessageConsumer {

    // Inyecta tus servicios de dominio aquí
    // @Inject
    // MessageProcessingService messageService;

    /**
     * Consume mensajes de inbox de manera reactiva.
     *
     * @param record Mensaje de Kafka con el esquema Avro
     * @return Uni<Void> que completa cuando el mensaje fue procesado
     *
     * NOTA: Comentado porque solo un consumidor puede estar activo para 'inbox-messages'.
     * Actualmente se usa InboxMessageBatchConsumer para mayor eficiencia.
     */
    // @Incoming("inbox-messages")
    public Uni<Void> consumeInboxMessage(IncomingKafkaRecord<String, Object> record) {
        // Obtener el valor del mensaje (clase Avro generada)
        Object message = record.getPayload();
        String key = record.getKey();

        log.info("📨 Procesando mensaje inbox - Key: {}, Partition: {}, Offset: {}",
                key, record.getPartition(), record.getOffset());

        // TODO: Reemplaza 'Object' con tu clase Avro real, ej: InboxMessage
        // InboxMessage avroMessage = (InboxMessage) message;

        return Uni.createFrom().item(message)
                // Procesar el mensaje de manera reactiva
                .chain(msg -> {
                    // TODO: Implementa tu lógica de negocio aquí
                    // Ejemplo: return messageService.processMessage(avroMessage);

                    log.debug("Procesando mensaje: {}", msg);
                    // Simular procesamiento
                    return Uni.createFrom().voidItem();
                })
                // Commit manual del offset solo si el procesamiento fue exitoso
                .chain(() -> Uni.createFrom().completionStage(record.ack()))
                .onItem().invoke(() ->
                    log.info("✅ Mensaje procesado exitosamente - Offset: {}", record.getOffset())
                )
                // Manejo de errores
                .onFailure().invoke(error ->
                    log.error("❌ Error procesando mensaje - Key: {}, Offset: {}, Error: {}",
                            key, record.getOffset(), error.getMessage(), error)
                )
                // NO hacer commit en caso de error - el mensaje será reprocesado
                .onFailure().recoverWithNull();
    }
}