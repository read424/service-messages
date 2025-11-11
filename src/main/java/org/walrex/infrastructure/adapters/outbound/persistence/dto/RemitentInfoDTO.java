package org.walrex.infrastructure.adapters.outbound.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transferir información del remitente desde la capa de persistencia
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemitentInfoDTO {

    /**
     * ID del usuario
     */
    private Integer idUser;

    /**
     * ID del empleado
     */
    private Integer idEmpleado;

    /**
     * Apellidos del remitente
     */
    private String apellidos;

    /**
     * Nombres del remitente
     */
    private String nombres;

    /**
     * Nombre de usuario
     */
    private String usuario;

    /**
     * Correo electrónico
     */
    private String email;
}
