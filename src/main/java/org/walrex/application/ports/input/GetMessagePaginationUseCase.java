package org.walrex.application.ports.input;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.MessageInboxItem;
import org.walrex.domain.model.PagedResult;

import java.util.Optional;

/**
 * Puerto de entrada (Input Port) para obtener mensajes paginados del inbox
 * Define el contrato del caso de uso desde la perspectiva del dominio
 *
 * Este puerto será implementado por un servicio en la capa de dominio
 */
public interface GetMessagePaginationUseCase {

    /**
     * Obtiene los mensajes del inbox de un usuario de forma reactiva
     * Retorna un PagedResult que incluye los datos y metadatos de paginación
     *
     * @param idUser ID del usuario destinatario
     * @param page Paginación opcional (Quarkus Panache Page)
     * @return Uni reactivo con el resultado paginado que contiene los mensajes y metadatos
     */
    Uni<PagedResult<MessageInboxItem>> getMessageByUser(Integer idUser, Optional<Page> page);
}
