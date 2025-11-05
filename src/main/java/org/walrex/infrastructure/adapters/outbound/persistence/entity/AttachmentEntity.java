package org.walrex.infrastructure.adapters.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Entidad JPA para la tabla inbox_messages.attachments
 * Representa un archivo adjunto asociado a un mensaje
 */
@Entity
@Table(name = "attachments", schema = "inbox_messages")
public class AttachmentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attachments_message"))
    private MessageEntity message;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", length = 255)
    private String fileType;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    // Constructor por defecto
    public AttachmentEntity() {
        this.uploadedAt = OffsetDateTime.now();
    }

    // Constructor con parámetros principales
    public AttachmentEntity(MessageEntity message, String filePath, String fileName, String fileType) {
        this();
        this.message = message;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileType = fileType;
    }

    // Métodos de ayuda
    public String getFileExtension() {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "";
    }

    public boolean isImage() {
        String ext = getFileExtension().toLowerCase();
        return ext.matches("jpg|jpeg|png|gif|bmp|svg|webp");
    }

    public boolean isPdf() {
        return "pdf".equalsIgnoreCase(getFileExtension());
    }

    public boolean isDocument() {
        String ext = getFileExtension().toLowerCase();
        return ext.matches("doc|docx|xls|xlsx|ppt|pptx|txt|csv");
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MessageEntity getMessage() {
        return message;
    }

    public void setMessage(MessageEntity message) {
        this.message = message;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(OffsetDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "AttachmentEntity{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", fileType='" + fileType + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
