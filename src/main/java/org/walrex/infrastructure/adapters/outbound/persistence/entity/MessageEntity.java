package org.walrex.infrastructure.adapters.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para la tabla inbox_messages.messages
 * Representa un mensaje en el sistema de mensajería
 */
@Entity
@Table(name = "messages", schema = "inbox_messages")
public class MessageEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_message")
    private Long idMessage;

    @Column(name = "sender_id", nullable = false)
    private Integer senderId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "create_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createAt;

    @Column(name = "asunto", columnDefinition = "TEXT")
    private String asunto;

    // Relación bidireccional con destinatarios
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MessageRecipientEntity> recipients = new ArrayList<>();

    // Relación bidireccional con adjuntos
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AttachmentEntity> attachments = new ArrayList<>();

    // Constructor por defecto
    public MessageEntity() {
        this.createAt = OffsetDateTime.now();
    }

    // Constructor con parámetros principales
    public MessageEntity(Integer senderId, String asunto, String content) {
        this();
        this.senderId = senderId;
        this.asunto = asunto;
        this.content = content;
    }

    // Métodos de ayuda para manejar relaciones bidireccionales
    public void addRecipient(MessageRecipientEntity recipient) {
        recipients.add(recipient);
        recipient.setMessage(this);
    }

    public void removeRecipient(MessageRecipientEntity recipient) {
        recipients.remove(recipient);
        recipient.setMessage(null);
    }

    public void addAttachment(AttachmentEntity attachment) {
        attachments.add(attachment);
        attachment.setMessage(this);
    }

    public void removeAttachment(AttachmentEntity attachment) {
        attachments.remove(attachment);
        attachment.setMessage(null);
    }

    // Getters y Setters
    public Long getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(Long idMessage) {
        this.idMessage = idMessage;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OffsetDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(OffsetDateTime createAt) {
        this.createAt = createAt;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public List<MessageRecipientEntity> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<MessageRecipientEntity> recipients) {
        this.recipients = recipients;
    }

    public List<AttachmentEntity> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentEntity> attachments) {
        this.attachments = attachments;
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "idMessage=" + idMessage +
                ", senderId=" + senderId +
                ", asunto='" + asunto + '\'' +
                ", createAt=" + createAt +
                '}';
    }
}
