# Consumidores Kafka + Avro

Este paquete contiene diferentes patrones de consumidores Kafka optimizados para Quarkus con Avro.

## Clases Avro Generadas

Las clases Avro están en el paquete externo `com.walrex:avro-schemas`. Para usarlas:

```java
// Ejemplo de imports (ajusta según tus esquemas reales)
import com.walrex.avro.InboxMessage;
import com.walrex.avro.NotificationEvent;
import com.walrex.avro.MessageStatusUpdate;
```

## Patrones de Consumidores

### 1. InboxMessageConsumer (Reactivo Simple)
**Uso:** Procesamiento mensaje por mensaje, control fino de commits
- ✅ Procesamiento rápido (< 100ms)
- ✅ Control fino sobre offsets
- ❌ No apto para alto volumen

**Configuración:**
```yaml
mp.messaging.incoming.inbox-messages:
  batch: false
  enable.auto.commit: false
```

### 2. InboxMessageBatchConsumer (Batch)
**Uso:** Alto throughput, procesamiento masivo
- ✅ Alto volumen (miles msg/s)
- ✅ Operaciones batch en DB
- ❌ Mayor latencia individual

**Configuración:**
```yaml
mp.messaging.incoming.inbox-messages:
  batch: true
  max.poll.records: 500
  enable.auto.commit: false
```

### 3. NotificationEventConsumerWithRetry (Retry + DLQ)
**Uso:** Procesamiento con retry automático y Dead Letter Queue
- ✅ Manejo robusto de errores
- ✅ Separación de mensajes problemáticos
- ✅ Garantías de entrega

**Configuración:**
```yaml
mp.messaging.incoming.notification-events:
  batch: false
  enable.auto.commit: true

mp.messaging.outgoing.notification-events-dlq:
  connector: smallrye-kafka
  topic: notification.events.dlq
```

### 4. ConcurrentMessageConsumer (Alto Paralelismo)
**Uso:** Procesamiento concurrente, operaciones I/O bound
- ✅ Operaciones con APIs/DB externas
- ✅ Maximiza throughput
- ⚠️ Requiere ajuste de recursos

**Configuración:**
```yaml
mp.messaging.incoming.inbox-messages:
  batch: false
  max.poll.records: 100
```

## Cómo Usar

1. **Elige el patrón** según tus necesidades (ver tabla arriba)
2. **Copia la clase** correspondiente
3. **Importa las clases Avro** de `com.walrex.avro.*`
4. **Reemplaza los TODOs** con tu lógica de negocio
5. **Configura el channel** en `application-dev.yml`
6. **Ajusta parámetros** de performance según carga

## Métricas y Monitoreo

Todos los consumidores incluyen logging estructurado:
- 📨 Mensaje recibido
- 🔄 Procesando
- ✅ Éxito
- ❌ Error
- 📊 Métricas de throughput

## Mejores Prácticas

1. **No bloquear el event loop:** Usa `@Blocking` si tu código es bloqueante
2. **Commit manual:** Para control fino usa `enable.auto.commit: false`
3. **Timeouts:** Siempre agrega timeouts a operaciones externas
4. **Retry:** Usa retry con backoff exponencial
5. **DLQ:** Envía mensajes problemáticos a DLQ después de N intentos
6. **Monitoring:** Registra métricas de throughput y errores