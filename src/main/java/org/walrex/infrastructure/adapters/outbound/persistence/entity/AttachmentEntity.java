package org.walrex.infrastructure.adapters.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Entidad JPA para la tabla inbox_messages.attachments
 * Representa un archivo adjunto asociado a un mensaje
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
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
}
