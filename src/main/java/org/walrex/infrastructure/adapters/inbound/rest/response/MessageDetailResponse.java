package org.walrex.infrastructure.adapters.inbound.rest.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * DTO de respuesta que representa el detalle completo de un mensaje
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalle completo de un mensaje del inbox")
public class MessageDetailResponse {

    @JsonProperty("id_message")
    @Schema(description = "ID del mensaje", example = "12345")
    private Integer idMessage;

    @JsonProperty("asunto")
    @Schema(description = "Asunto del mensaje", example = "Reunión de equipo")
    private String asunto;

    @JsonProperty("htmlcontent")
    @Schema(description = "Contenido HTML del mensaje")
    private String htmlcontent;

    @JsonProperty("attachment")
    @Schema(description = "Lista de URLs de archivos adjuntos")
    private List<String> attachment;

    @JsonProperty("sender_user")
    @Schema(description = "Información del remitente del mensaje")
    private SenderItem senderUser;
}
