package org.walrex.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * DTO para transferir datos de MessageInboxItem entre capas
 * Usado principalmente para serialización/deserialización JSON en APIs REST
 */
public class MessageInboxItemDTO {

    @JsonProperty("id_message")
    private Integer idMessage;

    @JsonProperty("is_read")
    private String isRead;

    @JsonProperty("message")
    private String message;

    @JsonProperty("num_attachments")
    private Integer numAttachments;

    @JsonProperty("sender_name")
    private String senderName;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("time_received")
    private String timeReceived;

    // Constructor vacío (requerido por Jackson)
    public MessageInboxItemDTO() {
    }

    // Constructor completo
    public MessageInboxItemDTO(Integer idMessage, String isRead, String message,
                              Integer numAttachments, String senderName,
                              OffsetDateTime createdAt, String timeReceived) {
        this.idMessage = idMessage;
        this.isRead = isRead;
        this.message = message;
        this.numAttachments = numAttachments;
        this.senderName = senderName;
        this.createdAt = createdAt;
        this.timeReceived = timeReceived;
    }

    // Método estático para crear desde domain model
    public static MessageInboxItemDTO fromDomain(org.walrex.domain.model.MessageInboxItem domain) {
        if (domain == null) {
            return null;
        }
        return new MessageInboxItemDTO(
            domain.getIdMessage(),
            domain.getIsRead(),
            domain.getMessage(),
            domain.getNumAttachments(),
            domain.getSenderName(),
            domain.getCreatedAt(),
            domain.getTimeReceived()
        );
    }

    // Método para convertir a domain model
    public org.walrex.domain.model.MessageInboxItem toDomain() {
        return org.walrex.domain.model.MessageInboxItem.builder()
            .idMessage(this.idMessage)
            .isRead(this.isRead)
            .message(this.message)
            .numAttachments(this.numAttachments)
            .senderName(this.senderName)
            .createdAt(this.createdAt)
            .timeReceived(this.timeReceived)
            .build();
    }

    // Getters y Setters
    public Integer getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(Integer idMessage) {
        this.idMessage = idMessage;
    }

    public String getIsRead() {
        return isRead;
    }

    public void setIsRead(String isRead) {
        this.isRead = isRead;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getNumAttachments() {
        return numAttachments;
    }

    public void setNumAttachments(Integer numAttachments) {
        this.numAttachments = numAttachments;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTimeReceived() {
        return timeReceived;
    }

    public void setTimeReceived(String timeReceived) {
        this.timeReceived = timeReceived;
    }

    @Override
    public String toString() {
        return "MessageInboxItemDTO{" +
                "idMessage=" + idMessage +
                ", isRead='" + isRead + '\'' +
                ", message='" + message + '\'' +
                ", numAttachments=" + numAttachments +
                ", senderName='" + senderName + '\'' +
                ", createdAt=" + createdAt +
                ", timeReceived='" + timeReceived + '\'' +
                '}';
    }
}
