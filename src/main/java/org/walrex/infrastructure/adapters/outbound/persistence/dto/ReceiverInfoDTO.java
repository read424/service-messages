package org.walrex.infrastructure.adapters.outbound.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transferir información de destinatarios desde la capa de persistencia
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiverInfoDTO {

    /**
     * ID del destinatario
     */
    private Integer id;

    /**
     * Apellidos del destinatario
     */
    private String apellidos;

    /**
     * Nombres del destinatario
     */
    private String nombres;

    /**
     * Nombre de usuario
     */
    private String username;
}
