package org.walrex.infrastructure.adapters.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.AttachmentEntity;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository reactivo para la entidad AttachmentEntity
 * Proporciona operaciones CRUD y consultas personalizadas usando Panache Reactive
 */
@ApplicationScoped
public class AttachmentRepository implements PanacheRepository<AttachmentEntity> {

    /**
     * Buscar todos los adjuntos de un mensaje
     */
    public Uni<List<AttachmentEntity>> findByMessageId(Long messageId) {
        return find("message.idMessage", Sort.ascending("uploadedAt"), messageId).list();
    }

    /**
     * Contar adjuntos de un mensaje
     */
    public Uni<Long> countByMessageId(Long messageId) {
        return count("message.idMessage", messageId);
    }

    /**
     * Buscar adjunto por nombre de archivo
     */
    public Uni<List<AttachmentEntity>> findByFileName(String fileName) {
        return find("fileName", fileName).list();
    }

    /**
     * Buscar adjuntos por nombre de archivo (búsqueda parcial)
     */
    public Uni<List<AttachmentEntity>> findByFileNameContaining(String fileName) {
        return find("LOWER(fileName) LIKE LOWER(?1)", Sort.descending("uploadedAt"), "%" + fileName + "%")
                .list();
    }

    /**
     * Buscar adjuntos por tipo de archivo
     */
    public Uni<List<AttachmentEntity>> findByFileType(String fileType) {
        return find("fileType", Sort.descending("uploadedAt"), fileType).list();
    }

    /**
     * Buscar adjuntos por tipo de archivo con paginación
     */
    public Uni<List<AttachmentEntity>> findByFileTypePaged(String fileType, int page, int size) {
        return find("fileType", Sort.descending("uploadedAt"), fileType)
                .page(page, size)
                .list();
    }

    /**
     * Buscar adjuntos por extensión (inferida del fileName)
     */
    public Uni<List<AttachmentEntity>> findByExtension(String extension) {
        return find("LOWER(fileName) LIKE LOWER(?1)", Sort.descending("uploadedAt"), "%." + extension)
                .list();
    }

    /**
     * Buscar adjuntos por rango de fechas
     */
    public Uni<List<AttachmentEntity>> findByDateRange(OffsetDateTime startDate, OffsetDateTime endDate) {
        return find("uploadedAt BETWEEN ?1 AND ?2", Sort.descending("uploadedAt"), startDate, endDate)
                .list();
    }

    /**
     * Buscar adjuntos recientes (últimos N días)
     */
    public Uni<List<AttachmentEntity>> findRecentAttachments(int days) {
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(days);
        return find("uploadedAt >= ?1", Sort.descending("uploadedAt"), startDate).list();
    }

    /**
     * Buscar adjuntos de un mensaje por tipo
     */
    public Uni<List<AttachmentEntity>> findByMessageIdAndFileType(Long messageId, String fileType) {
        return find("message.idMessage = ?1 AND fileType = ?2", Sort.ascending("uploadedAt"), messageId, fileType)
                .list();
    }

    /**
     * Buscar adjuntos de imágenes
     */
    public Uni<List<AttachmentEntity>> findImageAttachments() {
        return find("LOWER(fileName) LIKE '%.jpg' OR " +
                    "LOWER(fileName) LIKE '%.jpeg' OR " +
                    "LOWER(fileName) LIKE '%.png' OR " +
                    "LOWER(fileName) LIKE '%.gif' OR " +
                    "LOWER(fileName) LIKE '%.bmp' OR " +
                    "LOWER(fileName) LIKE '%.svg' OR " +
                    "LOWER(fileName) LIKE '%.webp'",
                    Sort.descending("uploadedAt"))
                .list();
    }

    /**
     * Buscar adjuntos de documentos
     */
    public Uni<List<AttachmentEntity>> findDocumentAttachments() {
        return find("LOWER(fileName) LIKE '%.pdf' OR " +
                    "LOWER(fileName) LIKE '%.doc' OR " +
                    "LOWER(fileName) LIKE '%.docx' OR " +
                    "LOWER(fileName) LIKE '%.xls' OR " +
                    "LOWER(fileName) LIKE '%.xlsx' OR " +
                    "LOWER(fileName) LIKE '%.ppt' OR " +
                    "LOWER(fileName) LIKE '%.pptx' OR " +
                    "LOWER(fileName) LIKE '%.txt' OR " +
                    "LOWER(fileName) LIKE '%.csv'",
                    Sort.descending("uploadedAt"))
                .list();
    }

    /**
     * Buscar adjunto por ruta de archivo (único)
     */
    public Uni<AttachmentEntity> findByFilePath(String filePath) {
        return find("filePath", filePath).firstResult();
    }

    /**
     * Verificar si existe un adjunto con la ruta especificada
     */
    public Uni<Boolean> existsByFilePath(String filePath) {
        return count("filePath", filePath).map(count -> count > 0);
    }

    /**
     * Eliminar adjunto por ID
     */
    public Uni<Boolean> deleteById(Long id) {
        return delete("id", id).map(deleted -> deleted > 0);
    }

    /**
     * Eliminar todos los adjuntos de un mensaje
     */
    public Uni<Long> deleteByMessageId(Long messageId) {
        return delete("message.idMessage", messageId);
    }

    /**
     * Eliminar adjuntos antiguos (mayor a N días)
     */
    public Uni<Long> deleteOldAttachments(int daysOld) {
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(daysOld);
        return delete("uploadedAt < ?1", cutoffDate);
    }

    /**
     * Buscar adjuntos de un mensaje con el mensaje cargado (EAGER)
     */
    public Uni<List<AttachmentEntity>> findByMessageIdWithMessage(Long messageId) {
        return find("SELECT a FROM AttachmentEntity a " +
                    "JOIN FETCH a.message m " +
                    "WHERE m.idMessage = ?1 " +
                    "ORDER BY a.uploadedAt ASC", messageId)
                .list();
    }

    /**
     * Obtener estadísticas de adjuntos por tipo
     */
    public Uni<List<Object[]>> getAttachmentStatsByFileType() {
        return find("SELECT a.fileType, COUNT(a) FROM AttachmentEntity a " +
                    "GROUP BY a.fileType " +
                    "ORDER BY COUNT(a) DESC")
                .project(Object[].class)
                .list();
    }

    /**
     * Contar adjuntos por tipo de archivo
     */
    public Uni<Long> countByFileType(String fileType) {
        return count("fileType", fileType);
    }

    /**
     * Buscar adjuntos de imágenes de un mensaje específico
     */
    public Uni<List<AttachmentEntity>> findImageAttachmentsByMessageId(Long messageId) {
        return find("message.idMessage = ?1 AND (" +
                    "LOWER(fileName) LIKE '%.jpg' OR " +
                    "LOWER(fileName) LIKE '%.jpeg' OR " +
                    "LOWER(fileName) LIKE '%.png' OR " +
                    "LOWER(fileName) LIKE '%.gif' OR " +
                    "LOWER(fileName) LIKE '%.bmp' OR " +
                    "LOWER(fileName) LIKE '%.svg' OR " +
                    "LOWER(fileName) LIKE '%.webp')",
                    Sort.ascending("uploadedAt"),
                    messageId)
                .list();
    }

    /**
     * Buscar adjuntos de documentos de un mensaje específico
     */
    public Uni<List<AttachmentEntity>> findDocumentAttachmentsByMessageId(Long messageId) {
        return find("message.idMessage = ?1 AND (" +
                    "LOWER(fileName) LIKE '%.pdf' OR " +
                    "LOWER(fileName) LIKE '%.doc' OR " +
                    "LOWER(fileName) LIKE '%.docx' OR " +
                    "LOWER(fileName) LIKE '%.xls' OR " +
                    "LOWER(fileName) LIKE '%.xlsx' OR " +
                    "LOWER(fileName) LIKE '%.ppt' OR " +
                    "LOWER(fileName) LIKE '%.pptx')",
                    Sort.ascending("uploadedAt"),
                    messageId)
                .list();
    }
}
