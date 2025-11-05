package org.walrex.domain.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Domain model que representa un item del inbox de mensajes de un usuario
 * Objeto de dominio puro sin dependencias de infraestructura
 */
@RegisterForReflection
public class MessageInboxItem {

    private Integer idMessage;
    private String isRead;
    private String message;
    private Integer numAttachments;
    private String senderName;
    private OffsetDateTime createdAt;
    private String timeReceived;

    // Constructor vacío
    public MessageInboxItem() {
    }

    // Constructor completo
    public MessageInboxItem(Integer idMessage, String isRead, String message,
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

    // Builder pattern para facilitar construcción
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer idMessage;
        private String isRead;
        private String message;
        private Integer numAttachments;
        private String senderName;
        private OffsetDateTime createdAt;
        private String timeReceived;

        public Builder idMessage(Integer idMessage) {
            this.idMessage = idMessage;
            return this;
        }

        public Builder isRead(String isRead) {
            this.isRead = isRead;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder numAttachments(Integer numAttachments) {
            this.numAttachments = numAttachments;
            return this;
        }

        public Builder senderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder timeReceived(String timeReceived) {
            this.timeReceived = timeReceived;
            return this;
        }

        public MessageInboxItem build() {
            return new MessageInboxItem(idMessage, isRead, message, numAttachments,
                                       senderName, createdAt, timeReceived);
        }
    }

    // Métodos de ayuda
    public boolean isRead() {
        return "Y".equals(this.isRead);
    }

    public boolean hasAttachments() {
        return numAttachments != null && numAttachments > 0;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageInboxItem that = (MessageInboxItem) o;
        return Objects.equals(idMessage, that.idMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMessage);
    }

    @Override
    public String toString() {
        return "MessageInboxItem{" +
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
