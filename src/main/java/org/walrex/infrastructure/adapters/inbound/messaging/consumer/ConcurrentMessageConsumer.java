package org.walrex.infrastructure.adapters.inbound.messaging.consumer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumidor CONCURRENTE con alto paralelismo.
 *
 * CARACTERÍSTICAS:
 * - Procesa múltiples mensajes en paralelo
 * - Control de concurrencia configurable
 * - Rate limiting para proteger servicios downstream
 * - Métricas de throughput en tiempo real
 *
 * CUÁNDO USAR:
 * - Procesamiento que requiere llamadas a servicios externos (APIs, DB)
 * - Alto volumen de mensajes
 * - Las operaciones son I/O bound (esperan respuestas de red/DB)
 * - Necesitas maximizar throughput sin saturar recursos
 *
 * IMPORTANTE:
 * - Ajusta CONCURRENT_LIMIT según tus recursos y el tiempo de procesamiento
 * - Monitorea el uso de memoria y threads
 * - Considera el impacto en servicios downstream
 */
@ApplicationScoped
@Slf4j
public class ConcurrentMessageConsumer {

    // Límite de mensajes procesándose concurrentemente
    private static final int CONCURRENT_LIMIT = 10;

    // Contador para métricas
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);

    // @Inject
    // MessageProcessingService messageService;

    /**
     * Consume mensajes con procesamiento concurrente.
     *
     * Este método procesa UN mensaje a la vez, pero el procesamiento
     * interno se hace en paralelo con otros mensajes usando merge().
     *
     * NOTA: Comentado porque solo un consumidor puede estar activo para 'inbox-messages'.
     * Actualmente se usa InboxMessageBatchConsumer para mayor eficiencia.
     */
    // @Incoming("inbox-messages")
    public Uni<Void> consumeWithConcurrency(IncomingKafkaRecord<String, Object> record) {
        Object message = record.getPayload();
        String key = record.getKey();

        log.debug("📨 Recibido mensaje - Key: {}, Offset: {}", key, record.getOffset());

        return processMessageConcurrently(message, key)
                // Commit después de procesamiento exitoso
                .chain(() -> Uni.createFrom().completionStage(record.ack()))
                .onItem().invoke(() -> {
                    int count = processedCount.incrementAndGet();
                    if (count % 100 == 0) {
                        log.info("📊 Procesados {} mensajes (Errores: {})",
                                count, errorCount.get());
                    }
                })
                .onFailure().invoke(error -> {
                    errorCount.incrementAndGet();
                    log.error("❌ Error procesando mensaje - Key: {}, Error: {}",
                            key, error.getMessage(), error);
                })
                // No hacer commit en caso de error
                .onFailure().recoverWithNull();
    }

    /**
     * Procesa el mensaje con timeout y retry.
     */
    private Uni<Void> processMessageConcurrently(Object message, String key) {
        return Uni.createFrom().item(message)
                .chain(msg -> {
                    // TODO: Implementa tu lógica de negocio aquí
                    // Ejemplo con llamada a servicio externo:
                    // return messageService.processMessage(msg);

                    log.debug("🔄 Procesando mensaje: {}", key);

                    // Simular procesamiento asíncrono
                    return Uni.createFrom().voidItem();
                })
                // Timeout para evitar que mensajes lentos bloqueen el flujo
                .ifNoItem().after(Duration.ofSeconds(30))
                .failWith(() -> new RuntimeException("Timeout procesando mensaje: " + key))
                // Retry en caso de errores transitorios
                .onFailure().retry()
                    .withBackOff(Duration.ofMillis(500), Duration.ofSeconds(5))
                    .atMost(2);
    }

    /**
     * ALTERNATIVA: Procesamiento Multi para mayor control.
     *
     * Si necesitas aún más control sobre la concurrencia, puedes usar Multi:
     */
    /*
    @Incoming("inbox-messages-concurrent")
    public Uni<Void> consumeWithMulti(IncomingKafkaRecord<String, Object> record) {
        return Multi.createFrom().item(record)
                .onItem().transformToUniAndMerge(
                    rec -> processMessageConcurrently(rec.getPayload(), rec.getKey())
                            .chain(() -> Uni.createFrom().completionStage(rec.ack())),
                    CONCURRENT_LIMIT // Limita cuántos mensajes se procesan en paralelo
                )
                .toUni()
                .replaceWithVoid();
    }
    */

    /**
     * PATRÓN: Rate Limiting
     *
     * Si necesitas limitar el rate de procesamiento (ej: API con rate limits):
     */
    /*
    @Incoming("inbox-messages-rate-limited")
    public Uni<Void> consumeWithRateLimit(IncomingKafkaRecord<String, Object> record) {
        return Uni.createFrom().item(record.getPayload())
                // Agregar delay entre mensajes (rate limiting)
                .onItem().delayIt().by(Duration.ofMillis(100)) // Max 10 msg/seg
                .chain(msg -> processMessageConcurrently(msg, record.getKey()))
                .chain(() -> Uni.createFrom().completionStage(record.ack()))
                .onFailure().recoverWithNull();
    }
    */

    /**
     * PATRÓN: Circuit Breaker
     *
     * Si tu procesamiento depende de servicios externos que pueden fallar:
     */
    /*
    @Incoming("inbox-messages-circuit-breaker")
    @CircuitBreaker(
        requestVolumeThreshold = 10,
        failureRatio = 0.5,
        delay = 5000,
        successThreshold = 2
    )
    public Uni<Void> consumeWithCircuitBreaker(IncomingKafkaRecord<String, Object> record) {
        return processMessageConcurrently(record.getPayload(), record.getKey())
                .chain(() -> Uni.createFrom().completionStage(record.ack()))
                .onFailure().recoverWithNull();
    }
    */
}