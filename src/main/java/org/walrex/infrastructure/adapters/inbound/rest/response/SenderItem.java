package org.walrex.infrastructure.adapters.inbound.rest.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO que representa la información del remitente de un mensaje
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información del remitente del mensaje")
public class SenderItem {

    @JsonProperty("apenom_personal")
    @Schema(description = "Apellidos y nombres del personal", example = "García Pérez Juan")
    private String apenomPersonal;

    @JsonProperty("id_empleado")
    @Schema(description = "ID del empleado", example = "1001")
    private Integer idEmpleado;

    @JsonProperty("id_usuario")
    @Schema(description = "ID del usuario", example = "2001")
    private Integer idUsuario;

    @JsonProperty("no_usuario")
    @Schema(description = "Nombre de usuario", example = "jgarcia")
    private String noUsuario;
}
