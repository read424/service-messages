package org.walrex.infrastructure.adapters.inbound.messaging.consumer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecordBatch;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Consumidor BATCH para procesamiento masivo eficiente.
 *
 * CARACTERÍSTICAS:
 * - Recibe múltiples mensajes en un solo lote (configurado en application.yml: max.poll.records)
 * - Procesa mensajes en paralelo usando Multi
 * - Commit después de procesar todo el batch
 * - Mayor throughput que procesamiento mensaje por mensaje
 *
 * CUÁNDO USAR:
 * - Alto volumen de mensajes (miles por segundo)
 * - El procesamiento puede hacerse en paralelo
 * - Puedes agrupar operaciones (ej: inserción batch en DB)
 * - La latencia individual no es crítica (se prioriza throughput)
 *
 * CONFIGURACIÓN REQUERIDA en application.yml:
 * - batch: true
 * - max.poll.records: 500 (o el tamaño que necesites)
 * - enable.auto.commit: false (para commit manual)
 */
@ApplicationScoped
@Slf4j
public class InboxMessageBatchConsumer {

    // @Inject
    // MessageProcessingService messageService;

    /**
     * Consume y procesa mensajes en batch.
     *
     * @param batch Lote de mensajes de Kafka
     * @return Uni<Void> que completa cuando todo el batch fue procesado
     */
    @Incoming("inbox-messages")
    public Uni<Void> consumeInboxMessageBatch(IncomingKafkaRecordBatch<String, Object> batch) {
        int batchSize = batch.getRecords().size();
        log.info("📦 Recibido batch de {} mensajes", batchSize);

        long startTime = System.currentTimeMillis();

        return Multi.createFrom().iterable(batch.getRecords())
                // Procesar cada mensaje en paralelo (ajusta concurrencia según tus recursos)
                .onItem().transformToUniAndConcatenate(record -> {
                    String key = record.getKey();
                    log.debug("📨 Procesando mensaje - Key: {}", key);

                    // TODO: Implementa tu lógica aquí
                    // return messageService.processMessage(record.getPayload());

                    return Uni.createFrom().voidItem();
                })
                // Agregar paralelismo: procesar hasta N mensajes concurrentemente
                // .merge(10) // <- Descomenta esto para procesar 10 mensajes en paralelo
                .collect().asList()
                // Una vez procesados todos, hacer commit del batch completo
                .chain(results -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("✅ Batch completado: {} mensajes en {}ms ({}msg/s)",
                            batchSize, duration, batchSize * 1000.0 / duration);

                    return Uni.createFrom().completionStage(batch.ack());
                })
                // Manejo de errores: si falla algún mensaje, NO hacemos commit
                // El batch completo será reprocesado
                .onFailure().invoke(error -> {
                    log.error("❌ Error procesando batch de {} mensajes: {}",
                            batchSize, error.getMessage(), error);
                    // El batch será reprocesado desde el último offset commiteado
                });
    }

    /**
     * ALTERNATIVA: Procesamiento batch optimizado para DB
     *
     * Si necesitas insertar todos los mensajes en DB de una vez:
     */
    /*
    @Incoming("inbox-messages-db-batch")
    public Uni<Void> consumeAndSaveToDbBatch(IncomingKafkaRecordBatch<String, Object> batch) {
        List<Object> messages = batch.getRecords().stream()
                .map(IncomingKafkaRecord::getPayload)
                .collect(Collectors.toList());

        log.info("📦 Guardando batch de {} mensajes en DB", messages.size());

        // TODO: Implementa inserción batch en DB
        // return messageRepository.saveAll(messages)
        //     .chain(() -> Uni.createFrom().completionStage(batch.ack()));

        return Uni.createFrom().completionStage(batch.ack());
    }
    */
}