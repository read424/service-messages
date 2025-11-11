package org.walrex.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa la información del remitente de un mensaje
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SenderInfo {

    /**
     * Apellidos y nombres del personal
     */
    private String apenomPersonal;

    /**
     * ID del empleado
     */
    private Integer idEmpleado;

    /**
     * ID del usuario
     */
    private Integer idUsuario;

    /**
     * Nombre de usuario
     */
    private String noUsuario;
}
