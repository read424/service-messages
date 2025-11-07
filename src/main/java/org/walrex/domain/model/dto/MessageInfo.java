package org.walrex.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa la información completa de un mensaje
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageInfo {

    /**
     * ID del mensaje
     */
    private Integer idMessage;

    /**
     * Asunto del mensaje
     */
    private String asunto;

    /**
     * Contenido HTML del mensaje
     */
    private String htmlcontent;

    /**
     * Lista de archivos adjuntos
     */
    private List<AttachmentInfo> attachments;

    /**
     * Información del remitente
     */
    private SenderInfo senderUser;
}
