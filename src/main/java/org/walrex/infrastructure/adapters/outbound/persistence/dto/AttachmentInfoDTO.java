package org.walrex.infrastructure.adapters.outbound.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para transferir información de archivos adjuntos desde la capa de persistencia
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentInfoDTO {

    /**
     * ID del adjunto
     */
    private Integer id;

    /**
     * Ruta del archivo
     */
    private String filePath;

    /**
     * Nombre del archivo
     */
    private String fileName;

    /**
     * Tipo de archivo
     */
    private String fileType;

    /**
     * Fecha de carga
     */
    private LocalDate uploadedAt;
}
