package org.walrex.infrastructure.adapters.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.MessageEntity;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository reactivo para la entidad MessageEntity
 * Proporciona operaciones CRUD y consultas personalizadas usando Panache Reactive
 */
@ApplicationScoped
public class MessageRepository implements PanacheRepository<MessageEntity> {

    /**
     * Buscar todos los mensajes con paginación y ordenamiento
     */
    public Uni<List<MessageEntity>> findAllPaged(int page, int size) {
        return findAll(Sort.descending("createAt"))
                .page(page, size)
                .list();
    }

    /**
     * Contar total de mensajes
     */
    public Uni<Long> countAll() {
        return count();
    }

    /**
     * Buscar mensaje por ID con sus relaciones
     */
    public Uni<MessageEntity> findByIdWithRelations(Long id) {
        return find("SELECT m FROM MessageEntity m " +
                    "LEFT JOIN FETCH m.recipients " +
                    "LEFT JOIN FETCH m.attachments " +
                    "WHERE m.idMessage = ?1", id)
                .firstResult();
    }

    /**
     * Buscar mensaje por ID con todas las relaciones incluyendo sender y empleado
     * Optimizado para evitar el problema N+1 usando JOIN FETCH
     */
    public Uni<MessageEntity> findByIdWithFullDetails(Long id) {
        return find("SELECT DISTINCT m FROM MessageEntity m " +
                    "LEFT JOIN FETCH m.sender s " +
                    "LEFT JOIN FETCH s.empleado " +
                    "WHERE m.idMessage = ?1", id)
                .firstResult();
    }

    /**
     * Buscar mensajes por remitente
     */
    public Uni<List<MessageEntity>> findBySenderId(Integer senderId) {
        return find("senderId", Sort.descending("createAt"), senderId).list();
    }

    /**
     * Buscar mensajes por remitente con paginación
     */
    public Uni<List<MessageEntity>> findBySenderIdPaged(Integer senderId, int page, int size) {
        return find("senderId", Sort.descending("createAt"), senderId)
                .page(page, size)
                .list();
    }

    /**
     * Contar mensajes por remitente
     */
    public Uni<Long> countBySenderId(Integer senderId) {
        return count("senderId", senderId);
    }

    /**
     * Buscar mensajes por asunto (búsqueda parcial)
     */
    public Uni<List<MessageEntity>> findByAsuntoContaining(String asunto) {
        return find("LOWER(asunto) LIKE LOWER(?1)", Sort.descending("createAt"), "%" + asunto + "%")
                .list();
    }

    /**
     * Buscar mensajes por contenido (búsqueda parcial)
     */
    public Uni<List<MessageEntity>> findByContentContaining(String content) {
        return find("LOWER(content) LIKE LOWER(?1)", Sort.descending("createAt"), "%" + content + "%")
                .list();
    }

    /**
     * Buscar mensajes por rango de fechas
     */
    public Uni<List<MessageEntity>> findByDateRange(OffsetDateTime startDate, OffsetDateTime endDate) {
        return find("createAt BETWEEN ?1 AND ?2", Sort.descending("createAt"), startDate, endDate)
                .list();
    }

    /**
     * Buscar mensajes recientes (últimos N días)
     */
    public Uni<List<MessageEntity>> findRecentMessages(int days) {
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(days);
        return find("createAt >= ?1", Sort.descending("createAt"), startDate).list();
    }

    /**
     * Buscar mensajes de un remitente en un rango de fechas
     */
    public Uni<List<MessageEntity>> findBySenderAndDateRange(Integer senderId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return find("senderId = ?1 AND createAt BETWEEN ?2 AND ?3",
                    Sort.descending("createAt"),
                    senderId, startDate, endDate)
                .list();
    }

    /**
     * Eliminar mensaje por ID
     */
    public Uni<Boolean> deleteById(Long id) {
        return delete("idMessage", id).map(deleted -> deleted > 0);
    }

    /**
     * Eliminar mensajes antiguos (mayor a N días)
     */
    public Uni<Long> deleteOldMessages(int daysOld) {
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(daysOld);
        return delete("createAt < ?1", cutoffDate);
    }

    /**
     * Verificar si existe un mensaje
     */
    public Uni<Boolean> existsById(Long id) {
        return count("idMessage", id).map(count -> count > 0);
    }

    /**
     * Buscar mensajes con adjuntos
     */
    public Uni<List<MessageEntity>> findMessagesWithAttachments() {
        return find("SELECT DISTINCT m FROM MessageEntity m " +
                    "LEFT JOIN FETCH m.attachments a " +
                    "WHERE SIZE(m.attachments) > 0")
                .list();
    }

    /**
     * Buscar mensajes sin adjuntos
     */
    public Uni<List<MessageEntity>> findMessagesWithoutAttachments() {
        return find("SELECT m FROM MessageEntity m " +
                    "WHERE SIZE(m.attachments) = 0")
                .list();
    }

    /**
     * Contar mensajes por remitente en un rango de fechas
     */
    public Uni<Long> countBySenderAndDateRange(Integer senderId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return count("senderId = ?1 AND createAt BETWEEN ?2 AND ?3", senderId, startDate, endDate);
    }
}
