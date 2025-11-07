package org.walrex.infrastructure.adapters.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.MessageRecipientEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository reactivo para la entidad MessageRecipientEntity
 * Proporciona operaciones CRUD y consultas personalizadas usando Panache Reactive
 */
@ApplicationScoped
public class MessageRecipientRepository implements PanacheRepository<MessageRecipientEntity> {

    /**
     * Buscar todos los destinatarios de un mensaje
     */
    public Uni<List<MessageRecipientEntity>> findByMessageId(Long messageId) {
        return find("message.idMessage", messageId).list();
    }

    /**
     * Buscar destinatarios de un mensaje con sus datos de usuario y empleado cargados
     * Optimizado con JOIN FETCH para evitar N+1 queries
     */
    public Uni<List<MessageRecipientEntity>> findByMessageIdWithRecipientDetails(Long messageId) {
        return find("SELECT mr FROM MessageRecipientEntity mr " +
                    "LEFT JOIN FETCH mr.recipient u " +
                    "LEFT JOIN FETCH u.empleado " +
                    "WHERE mr.message.idMessage = ?1", messageId)
                .list();
    }

    /**
     * Buscar todos los mensajes de un destinatario
     */
    public Uni<List<MessageRecipientEntity>> findByRecipientId(Integer recipientId) {
        return find("recipientId", Sort.descending("message.createAt"), recipientId).list();
    }

    /**
     * Buscar mensajes de un destinatario con paginación
     */
    public Uni<List<MessageRecipientEntity>> findByRecipientIdPaged(Integer recipientId, int page, int size) {
        return find("recipientId", Sort.descending("message.createAt"), recipientId)
                .page(page, size)
                .list();
    }

    /**
     * Contar mensajes totales de un destinatario
     */
    public Uni<Long> countByRecipientId(Integer recipientId) {
        return count("recipientId", recipientId);
    }

    /**
     * Buscar mensajes NO leídos de un destinatario
     */
    public Uni<List<MessageRecipientEntity>> findUnreadByRecipientId(Integer recipientId) {
        return find("recipientId = ?1 AND isRead = 'N'", Sort.descending("message.createAt"), recipientId)
                .list();
    }

    /**
     * Buscar mensajes NO leídos de un destinatario con paginación
     */
    public Uni<List<MessageRecipientEntity>> findUnreadByRecipientIdPaged(Integer recipientId, int page, int size) {
        return find("recipientId = ?1 AND isRead = 'N'", Sort.descending("message.createAt"), recipientId)
                .page(page, size)
                .list();
    }

    /**
     * Contar mensajes NO leídos de un destinatario
     */
    public Uni<Long> countUnreadByRecipientId(Integer recipientId) {
        return count("recipientId = ?1 AND isRead = 'N'", recipientId);
    }

    /**
     * Buscar mensajes LEÍDOS de un destinatario
     */
    public Uni<List<MessageRecipientEntity>> findReadByRecipientId(Integer recipientId) {
        return find("recipientId = ?1 AND isRead = 'Y'", Sort.descending("readAt"), recipientId)
                .list();
    }

    /**
     * Buscar mensajes LEÍDOS de un destinatario con paginación
     */
    public Uni<List<MessageRecipientEntity>> findReadByRecipientIdPaged(Integer recipientId, int page, int size) {
        return find("recipientId = ?1 AND isRead = 'Y'", Sort.descending("readAt"), recipientId)
                .page(page, size)
                .list();
    }

    /**
     * Contar mensajes LEÍDOS de un destinatario
     */
    public Uni<Long> countReadByRecipientId(Integer recipientId) {
        return count("recipientId = ?1 AND isRead = 'Y'", recipientId);
    }

    /**
     * Marcar mensaje como leído
     */
    public Uni<MessageRecipientEntity> markAsRead(Long recipientId) {
        return findById(recipientId)
                .onItem().ifNotNull().transform(recipient -> {
                    recipient.markAsRead();
                    return recipient;
                })
                .call(recipient -> persist(recipient));
    }

    /**
     * Marcar mensaje como no leído
     */
    public Uni<MessageRecipientEntity> markAsUnread(Long recipientId) {
        return findById(recipientId)
                .onItem().ifNotNull().transform(recipient -> {
                    recipient.markAsUnread();
                    return recipient;
                })
                .call(recipient -> persist(recipient));
    }

    /**
     * Marcar todos los mensajes de un destinatario como leídos
     */
    public Uni<Integer> markAllAsReadByRecipientId(Integer recipientId) {
        return update("isRead = 'Y', readAt = ?1 WHERE recipientId = ?2 AND isRead = 'N'",
                      LocalDateTime.now(), recipientId);
    }

    /**
     * Buscar destinatario específico de un mensaje
     */
    public Uni<MessageRecipientEntity> findByMessageIdAndRecipientId(Long messageId, Integer recipientId) {
        return find("message.idMessage = ?1 AND recipientId = ?2", messageId, recipientId)
                .firstResult();
    }

    /**
     * Verificar si un destinatario tiene el mensaje
     */
    public Uni<Boolean> existsByMessageIdAndRecipientId(Long messageId, Integer recipientId) {
        return count("message.idMessage = ?1 AND recipientId = ?2", messageId, recipientId)
                .map(count -> count > 0);
    }

    /**
     * Eliminar destinatario por ID
     */
    public Uni<Boolean> deleteById(Long id) {
        return delete("id", id).map(deleted -> deleted > 0);
    }

    /**
     * Eliminar todos los destinatarios de un mensaje
     */
    public Uni<Long> deleteByMessageId(Long messageId) {
        return delete("message.idMessage", messageId);
    }

    /**
     * Buscar mensajes leídos en un rango de fechas
     */
    public Uni<List<MessageRecipientEntity>> findReadByRecipientIdAndDateRange(
            Integer recipientId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return find("recipientId = ?1 AND isRead = 'Y' AND readAt BETWEEN ?2 AND ?3",
                    Sort.descending("readAt"),
                    recipientId, startDate, endDate)
                .list();
    }

    /**
     * Contar destinatarios de un mensaje
     */
    public Uni<Long> countByMessageId(Long messageId) {
        return count("message.idMessage", messageId);
    }

    /**
     * Buscar destinatarios con el mensaje y attachments cargados (EAGER)
     * Los attachments se cargan para permitir el conteo sin lazy loading
     */
    public Uni<List<MessageRecipientEntity>> findByRecipientIdWithMessage(Integer recipientId) {
        return find("SELECT DISTINCT mr FROM MessageRecipientEntity mr " +
                    "JOIN FETCH mr.message m " +
                    "LEFT JOIN FETCH m.attachments " +
                    "WHERE mr.recipientId = ?1 " +
                    "ORDER BY m.createAt DESC", recipientId)
                .list();
    }

    /**
     * Buscar destinatarios con el mensaje y attachments cargados (EAGER) con paginación
     * Los attachments se cargan para permitir el conteo sin lazy loading
     */
    public Uni<List<MessageRecipientEntity>> findByRecipientIdWithMessagePaged(Integer recipientId, int page, int size) {
        return find("SELECT DISTINCT mr FROM MessageRecipientEntity mr " +
                    "JOIN FETCH mr.message m " +
                    "LEFT JOIN FETCH m.attachments " +
                    "WHERE mr.recipientId = ?1 " +
                    "ORDER BY m.createAt DESC", recipientId)
                .page(page, size)
                .list();
    }

    /**
     * Buscar mensajes no leídos con el mensaje cargado (EAGER)
     */
    public Uni<List<MessageRecipientEntity>> findUnreadByRecipientIdWithMessage(Integer recipientId) {
        return find("SELECT mr FROM MessageRecipientEntity mr " +
                    "JOIN FETCH mr.message m " +
                    "WHERE mr.recipientId = ?1 AND mr.isRead = 'N' " +
                    "ORDER BY m.createAt DESC", recipientId)
                .list();
    }
}
