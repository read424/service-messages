package org.walrex.domain.service;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.walrex.application.ports.input.GetMessagePaginationUseCase;
import org.walrex.application.ports.output.InboxMessagePort;
import org.walrex.domain.model.MessageInboxItem;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.Pageable;
import org.walrex.infrastructure.adapters.outbound.cache.MessageCacheAdapter;

import java.util.Optional;

/**
 * Servicio de dominio que implementa el caso de uso de obtener mensajes paginados
 * Actúa como orquestador entre los puertos de entrada y salida
 * Implementa cache-aside pattern para optimizar consultas repetidas
 *
 * Esta clase pertenece a la capa de dominio y contiene la lógica de negocio
 */
@ApplicationScoped
public class MessageInboxService implements GetMessagePaginationUseCase {

    private static final Logger LOG = Logger.getLogger(MessageInboxService.class);

    private final InboxMessagePort inboxMessagePort;
    private final MessageCacheAdapter<MessageInboxItem> cacheAdapter;

    @Inject
    public MessageInboxService(InboxMessagePort inboxMessagePort, MessageCacheAdapter<MessageInboxItem> cacheAdapter) {
        this.inboxMessagePort = inboxMessagePort;
        this.cacheAdapter = cacheAdapter;
    }

    /**
     * Obtiene los mensajes del inbox de un usuario con paginación opcional
     * Implementa cache-aside pattern:
     * 1. Intenta obtener del cache
     * 2. Si no existe en cache, consulta la BD y cachea el resultado
     *
     * @param idUser ID del usuario destinatario
     * @param page Paginación opcional (Quarkus Panache Page)
     * @return Uni reactivo con el resultado paginado que contiene los mensajes y metadatos
     */
    @Override
    public Uni<PagedResult<MessageInboxItem>> getMessageByUser(Integer idUser, Optional<Page> page) {
        LOG.infof("[MessageInboxService] Iniciando obtención de mensajes para usuario: %d, página: %s",
                idUser, page.map(p -> "page=" + p.index + ", size=" + p.size).orElse("sin paginación"));

        // Convertir Quarkus Panache Page a domain Pageable
        Pageable pageable = convertToPageable(page);

        // Usar cache-aside pattern: intenta cache primero, si no existe consulta BD y cachea
        return cacheAdapter.getOrFetch(
                idUser,
                pageable,
                MessageInboxItem.class,
                () -> {
                    LOG.debugf("[MessageInboxService] Cache MISS - delegando a InboxMessagePort para usuario: %d", idUser);
                    return inboxMessagePort.findMessagesByUser(idUser, page);
                }
        )
        .onItem().invoke(result ->
            LOG.infof("[MessageInboxService] Mensajes obtenidos exitosamente para usuario: %d - Total: %d, Página actual: %d elementos",
                    idUser, result.getTotalElements(), result.getData().size())
        )
        .onFailure().invoke(throwable ->
            LOG.errorf(throwable, "[MessageInboxService] Error al obtener mensajes para usuario: %d", idUser)
        );
    }

    /**
     * Convierte Quarkus Panache Page a domain Pageable
     * Si no se proporciona paginación, usa valores por defecto (página 0, tamaño 20)
     *
     * @param page Paginación opcional de Quarkus Panache
     * @return Pageable del dominio
     */
    private Pageable convertToPageable(Optional<Page> page) {
        if (page.isPresent()) {
            Page p = page.get();
            return Pageable.of(p.index, p.size);
        }
        // Valores por defecto si no se proporciona paginación
        return Pageable.of(0, 20);
    }

    /**
     * Invalida el cache completo de un usuario
     * Debe llamarse cuando se crean, actualizan o eliminan mensajes
     *
     * @param userId ID del usuario
     * @return Uni con el número de claves eliminadas del cache
     */
    public Uni<Long> invalidateUserCache(Integer userId) {
        LOG.infof("[MessageInboxService] Invalidando cache para usuario: %d", userId);
        return cacheAdapter.invalidateUserCache(userId)
                .onItem().invoke(deletedKeys ->
                    LOG.infof("[MessageInboxService] Cache invalidado exitosamente para usuario: %d - %d claves eliminadas",
                            userId, deletedKeys)
                )
                .onFailure().invoke(throwable ->
                    LOG.errorf(throwable, "[MessageInboxService] Error al invalidar cache para usuario: %d", userId)
                );
    }
}
