package org.walrex.infrastructure.adapters.outbound.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para transferir el detalle completo de un mensaje desde la capa de persistencia
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDetailsDTO {

    /**
     * ID del mensaje
     */
    private Integer id;

    /**
     * Información del remitente
     */
    private RemitentInfoDTO remitente;

    /**
     * Contenido del mensaje
     */
    private String content;

    /**
     * Fecha de creación
     */
    private LocalDate createAt;

    /**
     * Asunto del mensaje
     */
    private String subject;

    /**
     * Lista de destinatarios
     */
    private List<ReceiverInfoDTO> receivers;

    /**
     * Estado de lectura (1: leído, 0: no leído)
     */
    private String isRead;

    /**
     * Fecha de lectura
     */
    private LocalDate readAt;

    /**
     * Lista de archivos adjuntos
     */
    private List<AttachmentInfoDTO> attachments;
}
