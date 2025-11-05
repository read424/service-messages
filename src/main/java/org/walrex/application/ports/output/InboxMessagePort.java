package org.walrex.application.ports.output;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.MessageInboxItem;
import org.walrex.domain.model.PagedResult;

import java.util.Optional;

/**
 * Puerto de salida (Output Port) para obtener mensajes del inbox desde la persistencia
 * Define el contrato que debe implementar el adaptador de persistencia
 *
 * Este puerto será implementado por un adaptador en la capa de infraestructura
 */
public interface InboxMessagePort {

    /**
     * Obtiene los mensajes del inbox de un usuario con paginación opcional
     * Retorna un PagedResult que incluye los datos y metadatos de paginación
     *
     * @param userId ID del usuario destinatario
     * @param page Parámetro opcional para paginación (Quarkus Panache Page)
     * @return Uni reactivo con el resultado paginado que contiene los mensajes y metadatos
     */
    Uni<PagedResult<MessageInboxItem>> findMessagesByUser(Integer userId, Optional<Page> page);
}
