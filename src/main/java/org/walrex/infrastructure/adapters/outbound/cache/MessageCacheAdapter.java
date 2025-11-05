package org.walrex.infrastructure.adapters.outbound.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.walrex.application.ports.output.MessageCacheAdapterPort;
import org.walrex.domain.model.MessageInboxItem;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.Pageable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Adapter para gestionar el cache de mensajes paginados en Redis
 * Implementa estrategia de cache-aside con invalidación por patrón
 */
@Dependent
public class MessageCacheAdapter<T> implements MessageCacheAdapterPort<T> {

    private static final Logger LOG = Logger.getLogger(MessageCacheAdapter.class);

    private static final String CACHE_PREFIX = "msg-svc";
    private static final String LIST_SUFFIX = "list";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final ReactiveValueCommands<String, String> valueCommands;
    private final ReactiveKeyCommands<String> keyCommands;
    private final ObjectMapper objectMapper;

    @Inject
    public MessageCacheAdapter(ReactiveRedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key();
        this.objectMapper = objectMapper;
    }

    /**
     * Genera una clave de cache para resultados paginados
     * Formato: msg-svc-{userId}-list-{hashParametros}
     *
     * @param userId ID del usuario
     * @param pageable Parámetros de paginación
     * @return Clave de cache generada
     */
    public String generateCacheKey(Integer userId, Pageable pageable) {
        String paramsHash = generateParametersHash(pageable);
        return String.format("%s-%d-%s-%s", CACHE_PREFIX, userId, LIST_SUFFIX, paramsHash);
    }

    /**
     * Helper para DESERIALIZAR el String JSON a PagedResult<T> usando Jackson/ObjectMapper.
     * Se requiere 'contentClass' para manejar correctamente el tipo genérico PagedResult<T>.
     */
    private PagedResult<T> deserialize(String json, Class<T> contentClass) {
        if (json == null || json.isEmpty()) return null;
        try {
            // 1. Build the Java type for PagedResult<T> using ObjectMapper's TypeFactory
            JavaType contentType = objectMapper.getTypeFactory().constructType(contentClass);
            JavaType pagedResultType = objectMapper.getTypeFactory()
                    .constructParametricType(PagedResult.class, contentType);

            // 2. Deserialize using the constructed generic type
            return objectMapper.readValue(json, pagedResultType);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "[Cache] Error deserializing JSON to PagedResult<%s> (Corrupt Cache)", contentClass.getSimpleName());
            // If the cache is corrupt or there's an error, treat it as a MISS (return null)
            return null;
        }
    }

    /**
     * Helper para SERIALIZAR PagedResult<T> a una cadena JSON.
     */
    private String serialize(PagedResult<T> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "[Cache] Error al serializar PagedResult");
            throw new RuntimeException("Error de serialización JSON", e);
        }
    }

    /**
     * Genera un hash MD5 de los parámetros de paginación
     *
     * @param pageable Parámetros a hashear
     * @return Hash hexadecimal de los parámetros
     */
    private String generateParametersHash(Pageable pageable) {
        try {
            String params = String.format("page=%d&size=%d", pageable.getPage(), pageable.getSize());
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(params.getBytes(StandardCharsets.UTF_8));

            // Convertir bytes a hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            LOG.error("Error generating parameters hash", e);
            // Fallback: usar toString de los parámetros
            return String.format("%d-%d", pageable.getPage(), pageable.getSize());
        }
    }

    /**
     * Obtiene datos del cache
     *
     * @param userId ID del usuario
     * @param pageable Parámetros de paginación
     * @return Uni con el resultado cacheado o vacío si no existe
     */
    @Override
    public Uni<PagedResult<T>> get(Integer userId, Pageable pageable, Class<T> contentClass) {
        String cacheKey = generateCacheKey(userId, pageable);
        LOG.debugf("[MessageCacheAdapter] Intentando obtener del cache - Key: %s (userId=%d, page=%d, size=%d)",
                cacheKey, userId, pageable.getPage(), pageable.getSize());

        return valueCommands.get(cacheKey)
                .onItem().invoke(result -> {
                    if(result!=null){
                        LOG.infof("[MessageCacheAdapter] Cache HIT - Key: %s, Length: %d",
                                cacheKey, result.length());
                    }else{
                        LOG.infof("[MessageCacheAdapter] Cache MISS - Key: %s, data source will be consulted", cacheKey);
                    }
                })
                .onItem().ifNotNull().transform(result -> deserialize(result, contentClass));
    }

    /**
     * Almacena datos en el cache con TTL personalizado
     *
     * @param userId ID del usuario
     * @param pageable Parámetros de paginación
     * @param data Datos a cachear
     * @param ttl Tiempo de vida del cache
     * @return Uni<Void>
     */
    @Override
    public Uni<Void> set(Integer userId, Pageable pageable, PagedResult<T> data, Duration ttl) {
        String cacheKey = generateCacheKey(userId, pageable);
        String jsonData = serialize(data);

        return valueCommands.setex(cacheKey, ttl.getSeconds(), jsonData)
                .onItem().invoke(() ->
                    LOG.infof("[MessageCacheAdapter] Cache SET exitoso - Key: %s, Elementos: %d, TTL personalizado: %d segundos",
                            cacheKey, data.getData().size(), ttl.getSeconds())
                )
                .onFailure().invoke(throwable ->
                    LOG.errorf(throwable, "[MessageCacheAdapter] Error al guardar en cache - Key: %s", cacheKey)
                )
                .replaceWithVoid();
    }

    /**
     * Invalida todas las claves de cache para un usuario
     * Elimina todas las claves que coincidan con: msg-svc-{userId}-list-*
     *
     * @param userId ID del usuario
     * @return Uni<Long> número de claves eliminadas
     */
    public Uni<Long> invalidateUserCache(Integer userId) {
        String pattern = String.format("%s-%d-%s-*", CACHE_PREFIX, userId, LIST_SUFFIX);
        LOG.infof("[MessageCacheAdapter] Invalidando cache completo para usuario: %d - Pattern: %s", userId, pattern);

        return keyCommands.keys(pattern)
                .onItem().transformToUni(keys -> {
                    if (keys.isEmpty()) {
                        LOG.infof("[MessageCacheAdapter] No se encontraron claves de cache para usuario: %d", userId);
                        return Uni.createFrom().item(0L);
                    }

                    LOG.infof("[MessageCacheAdapter] Encontradas %d claves de cache para usuario: %d - Eliminando...", keys.size(), userId);
                    return keyCommands.del(keys.toArray(new String[0]))
                            .onItem().transform(deleted -> {
                                LOG.infof("[MessageCacheAdapter] Cache invalidado exitosamente - Usuario: %d, Claves eliminadas: %d", userId, deleted);
                                return deleted.longValue();
                            });
                })
                .onFailure().invoke(throwable ->
                    LOG.errorf(throwable, "[MessageCacheAdapter] Error al invalidar cache para usuario: %d", userId)
                );
    }

    /**
     * Invalida una clave específica de cache
     *
     * @param userId ID del usuario
     * @param pageable Parámetros de paginación
     * @return Uni<Boolean> true si se eliminó la clave
     */
    public Uni<Boolean> invalidate(Integer userId, Pageable pageable) {
        String cacheKey = generateCacheKey(userId, pageable);
        LOG.debugf("[MessageCacheAdapter] Invalidando clave específica - Key: %s", cacheKey);

        return keyCommands.del(cacheKey)
                .onItem().transform(deleted -> {
                    boolean wasDeleted = deleted > 0;
                    if (wasDeleted) {
                        LOG.infof("[MessageCacheAdapter] Clave de cache eliminada exitosamente - Key: %s", cacheKey);
                    } else {
                        LOG.debugf("[MessageCacheAdapter] Clave de cache no encontrada - Key: %s", cacheKey);
                    }
                    return wasDeleted;
                })
                .onFailure().invoke(throwable ->
                    LOG.errorf(throwable, "[MessageCacheAdapter] Error al invalidar clave de cache - Key: %s", cacheKey)
                );
    }

    /**
     * Método helper para ejecutar una operación con cache-aside pattern
     * Si existe en cache, lo retorna. Si no existe, ejecuta el supplier, cachea el resultado y lo retorna.
     *
     * @param userId ID del usuario
     * @param pageable Parámetros de paginación
     * @param dataSupplier Supplier que obtiene los datos de la fuente original (BD)
     * @return Uni con los datos (desde cache o desde la fuente)
     */
    @Override
    public Uni<PagedResult<T>> getOrFetch(Integer userId, Pageable pageable, Class<T> contentClass,
                                               Supplier<Uni<PagedResult<T>>> dataSupplier) {
        return this.get(userId, pageable, contentClass)
                .onItem().ifNotNull().transformToUni(cached -> {
                    LOG.debugf("[MessageCacheAdapter] Cache HIT - devolviendo resultado cacheado");
                    return Uni.createFrom().item(cached);
                })
                .onItem().ifNull().switchTo(()->{
                    LOG.debugf("[MessageCacheAdapter] Cache MISS - ejecutando dataSupplier");
                    return dataSupplier.get()
                            .invoke(()->LOG.info("[MessageCacheAdapter] Ejecutando dataSupplier.get() - llamando BD"))
                            .onItem().ifNotNull().call(result->set(userId, pageable, result, DEFAULT_TTL));
                });
    }
}
