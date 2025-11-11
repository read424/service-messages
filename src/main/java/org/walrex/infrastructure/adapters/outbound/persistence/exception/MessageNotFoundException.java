package org.walrex.infrastructure.adapters.outbound.persistence.exception;

/**
 * Excepción lanzada cuando no se encuentra un mensaje en la base de datos
 */
public class MessageNotFoundException extends RuntimeException {

    private final Integer messageId;

    public MessageNotFoundException(Integer messageId) {
        super("Message not found with id: " + messageId);
        this.messageId = messageId;
    }

    public MessageNotFoundException(Integer messageId, String message) {
        super(message);
        this.messageId = messageId;
    }

    public MessageNotFoundException(Integer messageId, String message, Throwable cause) {
        super(message, cause);
        this.messageId = messageId;
    }

    public Integer getMessageId() {
        return messageId;
    }
}
