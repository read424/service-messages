package org.walrex.infrastructure.adapters.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla inbox_messages.message_recipients
 * Representa un destinatario de un mensaje y su estado de lectura
 */
@Entity
@Table(name = "message_recipients", schema = "inbox_messages", indexes = {
    @Index(name = "idx_message_recipients_message_id", columnList = "message_id"),
    @Index(name = "idx_message_recipients_recipient_id", columnList = "recipient_id"),
    @Index(name = "idx_message_recipients_is_read", columnList = "is_read")
})
public class MessageRecipientEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false, foreignKey = @ForeignKey(name = "fk_message_recipients_message"))
    private MessageEntity message;

    @Column(name = "recipient_id", nullable = false, insertable = false, updatable = false)
    private Integer recipientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", referencedColumnName = "id_usuario")
    private UsuarioEntity recipient;

    @Column(name = "is_read", length = 1, columnDefinition = "VARCHAR(1) DEFAULT 'N'")
    private String isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // Constructor por defecto
    public MessageRecipientEntity() {
        this.isRead = "N"; // Por defecto no leído
    }

    // Constructor con parámetros principales
    public MessageRecipientEntity(MessageEntity message, Integer recipientId) {
        this();
        this.message = message;
        this.recipientId = recipientId;
    }

    // Métodos de ayuda
    public boolean isRead() {
        return "Y".equals(this.isRead);
    }

    public void markAsRead() {
        this.isRead = "Y";
        this.readAt = LocalDateTime.now();
    }

    public void markAsUnread() {
        this.isRead = "N";
        this.readAt = null;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MessageEntity getMessage() {
        return message;
    }

    public void setMessage(MessageEntity message) {
        this.message = message;
    }

    public Integer getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Integer recipientId) {
        this.recipientId = recipientId;
    }

    public String getIsRead() {
        return isRead;
    }

    public void setIsRead(String isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public UsuarioEntity getRecipient() {
        return recipient;
    }

    public void setRecipient(UsuarioEntity recipient) {
        this.recipient = recipient;
    }

    @Override
    public String toString() {
        return "MessageRecipientEntity{" +
                "id=" + id +
                ", recipientId=" + recipientId +
                ", isRead='" + isRead + '\'' +
                ", readAt=" + readAt +
                '}';
    }
}
