package org.walrex.application.ports.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Pageable;
import org.walrex.domain.model.PagedResult;

import java.time.Duration;
import java.util.function.Supplier;

public interface MessageCacheAdapterPort<T> {
    /**
     * Intenta obtener un resultado paginado del cache.
     * @param userId ID del usuario.
     * @param pageable Información de paginación.
     * @param contentClass Clase del contenido (T) para correcta deserialización.
     * @return Uni<PagedResult<T>> si se encuentra, Uni<null> si hay Cache MISS.
     */
    Uni<PagedResult<T>> get(Integer userId, Pageable pageable, Class<T> contentClass);

    /**
     * Guarda un resultado paginado en el cache.
     * @param userId ID del usuario.
     * @param pageable Información de paginación.
     * @param result El resultado paginado a guardar.
     * @return Uni<Void> que completa al terminar la operación de guardado.
     */
    Uni<Void> set(Integer userId, Pageable pageable, PagedResult<T> result, Duration ttl);

    /**
     * Implementa el patrón Cache-Aside: intenta obtener del cache, y si no existe,
     * ejecuta el Supplier (proveedor de datos, ej: llamada a la BD) y guarda el resultado.
     * @param userId ID del usuario.
     * @param pageable Información de paginación.
     * @param contentClass Clase del contenido (T) para correcta deserialización.
     * @param dataSupplier Lógica para obtener los datos si hay Cache MISS.
     * @return Uni<PagedResult<T>> con el resultado.
     */
    Uni<PagedResult<T>> getOrFetch(Integer userId, Pageable pageable, Class<T> contentClass,
                                   Supplier<Uni<PagedResult<T>>> dataSupplier);
}
