package org.walrex.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO que representa la información de un archivo adjunto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentInfo {

    /**
     * ID del adjunto
     */
    private Integer id;

    /**
     * Ruta del archivo en el sistema de almacenamiento
     */
    private String filePath;

    /**
     * Nombre del archivo
     */
    private String filename;

    /**
     * Tipo MIME del archivo
     */
    private String fileType;

    /**
     * Fecha de carga del archivo
     */
    private LocalDate uploadedAt;
}
