package org.walrex.infrastructure.adapters.inbound.rest;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.walrex.application.ports.input.GetMessagePaginationUseCase;
import org.walrex.domain.model.dto.MessageInboxItemDTO;
import org.walrex.infrastructure.adapters.inbound.rest.dto.PagedResponse;
import org.walrex.infrastructure.adapters.inbound.rest.response.MessageDetailResponse;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Resource para el inbox de mensajes
 * Endpoint: /api/message-inbox
 */
@Path("/api/message-inbox")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Message Inbox", description = "Operaciones para gestionar el inbox de mensajes")
public class MessageInboxResource {

    private static final Logger LOG = Logger.getLogger(MessageInboxResource.class);

    @Inject
    GetMessagePaginationUseCase getMessagePaginationUseCase;

    /**
     * Obtiene los mensajes del inbox del usuario
     *
     * @param userId ID del usuario desde el header X-User-Id
     * @param page Número de página (basado en 0), opcional
     * @param number Número de registros por página, opcional
     * @param search Término de búsqueda opcional (no implementado aún)
     * @return Respuesta paginada con los mensajes del inbox
     */
    @GET
    @Operation(
        summary = "Obtener mensajes del inbox",
        description = "Retorna los mensajes del inbox del usuario con paginación opcional. El ID del usuario se obtiene del header X-User-Id."
    )
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Mensajes obtenidos exitosamente",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PagedResponse.class)
                )
            ),
            @APIResponse(
                responseCode = "400",
                description = "Parámetros inválidos (user ID ausente, parámetros de paginación incorrectos)"
            ),
            @APIResponse(
                responseCode = "500",
                description = "Error interno del servidor"
            )
        }
    )
    @WithSession
    public Uni<Response> getMessageInbox(
        @Parameter(description = "ID del usuario (enviado por el API Gateway)", required = true)
        @HeaderParam("X-User-Id")
        String userId,

        @Parameter(description = "Número de página (basado en 0)", example = "0")
        @QueryParam("page")
        Integer page,

        @Parameter(description = "Número de registros por página", example = "10")
        @QueryParam("number")
        Integer number,

        @Parameter(description = "Término de búsqueda", example = "")
        @QueryParam("search")
        @DefaultValue("")
        String search
    ) {
        LOG.infof("[MessageInboxResource] ⬇️  REQUEST - GET /api/message-inbox - userId: %s, page: %s, size: %s, search: '%s'",
                  userId, page, number, search);

        // 1. Validar que el userId esté presente en el header
        if (userId == null || userId.trim().isEmpty()) {
            LOG.error("[MessageInboxResource] ❌ Validación fallida - Header X-User-Id ausente");
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Missing X-User-Id header"))
                    .build()
            );
        }

        // 2. Convertir userId de String a Integer
        Integer userIdInt;
        try {
            userIdInt = Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            LOG.errorf("[MessageInboxResource] ❌ Validación fallida - Formato de userId inválido: %s", userId);
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid user ID format"))
                    .build()
            );
        }

        // 3. Crear objeto Page si page y number no son null
        Optional<Page> pageOptional = Optional.empty();
        if (page != null && number != null) {
            // Validar parámetros de paginación
            if (page < 0) {
                LOG.errorf("[MessageInboxResource] ❌ Validación fallida - Número de página negativo: %d", page);
                return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Page number cannot be negative"))
                        .build()
                );
            }

            if (number <= 0 || number > 100) {
                LOG.errorf("[MessageInboxResource] ❌ Validación fallida - Tamaño de página inválido: %d (debe estar entre 1 y 100)", number);
                return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Page size must be between 1 and 100"))
                        .build()
                );
            }

            pageOptional = Optional.of(Page.of(page, number));
            LOG.infof("[MessageInboxResource] ✅ Validación exitosa - Paginación habilitada: page=%d, size=%d", page, number);
        } else {
            LOG.info("[MessageInboxResource] ✅ Validación exitosa - Sin paginación (retornará todos los mensajes)");
        }

        // 4. Llamar al caso de uso
        LOG.debugf("[MessageInboxResource] Delegando a GetMessagePaginationUseCase - userId: %d", userIdInt);

        return getMessagePaginationUseCase.getMessageByUser(userIdInt, pageOptional)
            .map(pagedResult -> {
                LOG.debugf("[MessageInboxResource] Respuesta recibida del caso de uso - userId: %d, elementos: %d, total: %d",
                        (Object) userIdInt, (Object) pagedResult.getData().size(), (Object) pagedResult.getTotalElements());

                // Mapear datos de dominio a DTOs
                var messageDTOs = pagedResult.getData().stream()
                    .map(MessageInboxItemDTO::fromDomain)
                    .collect(Collectors.toList());

                // Construir respuesta paginada con metadatos reales de la BD
                PagedResponse<MessageInboxItemDTO> response = PagedResponse.<MessageInboxItemDTO>builder()
                    .data(messageDTOs)
                    .page(pagedResult.getPage())
                    .size(pagedResult.getSize())
                    .totalElements(pagedResult.getTotalElements())
                    .build();

                LOG.infof("[MessageInboxResource] ⬆️  RESPONSE 200 OK - userId: %d, elementos retornados: %d, página: %d de %d",
                        userIdInt, messageDTOs.size(), pagedResult.getPage() + 1,
                        (int) Math.ceil((double) pagedResult.getTotalElements() / pagedResult.getSize()));

                return Response.ok(response).build();
            })
            .onFailure().recoverWithItem(throwable -> {
                LOG.errorf(throwable, "[MessageInboxResource] ❌ ERROR 500 - Error al obtener mensajes para usuario: %d", userIdInt);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error fetching messages: " + throwable.getMessage()))
                    .build();
            });
    }

    /**
     * Obtiene el detalle de un mensaje específico
     *
     * @param userId ID del usuario desde el header X-User-Id
     * @param idMessage ID del mensaje a consultar
     * @return Detalle completo del mensaje
     */
    @GET
    @Path("/{id_message}")
    @Operation(
        summary = "Obtener detalle de un mensaje",
        description = "Retorna el detalle completo de un mensaje específico. El ID del usuario se obtiene del header X-User-Id."
    )
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Mensaje obtenido exitosamente",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = MessageDetailResponse.class)
                )
            ),
            @APIResponse(
                responseCode = "400",
                description = "Parámetros inválidos (user ID ausente o formato incorrecto)"
            ),
            @APIResponse(
                responseCode = "404",
                description = "Mensaje no encontrado"
            ),
            @APIResponse(
                responseCode = "500",
                description = "Error interno del servidor"
            )
        }
    )
    @WithSession
    public Uni<Response> getMessageById(
        @Parameter(description = "ID del usuario (enviado por el API Gateway)", required = true)
        @HeaderParam("X-User-Id")
        String userId,

        @Parameter(description = "ID del mensaje", required = true, example = "12345")
        @PathParam("id_message")
        Integer idMessage
    ) {
        LOG.infof("[MessageInboxResource] ⬇️  REQUEST - GET /api/message-inbox/%d - userId: %s", idMessage, userId);

        // 1. Validar que el userId esté presente en el header
        if (userId == null || userId.trim().isEmpty()) {
            LOG.error("[MessageInboxResource] ❌ Validación fallida - Header X-User-Id ausente");
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Missing X-User-Id header"))
                    .build()
            );
        }

        // 2. Convertir userId de String a Integer
        Integer userIdInt;
        try {
            userIdInt = Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            LOG.errorf("[MessageInboxResource] ❌ Validación fallida - Formato de userId inválido: %s", userId);
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid user ID format"))
                    .build()
            );
        }

        // 3. Validar que el idMessage no sea null
        if (idMessage == null) {
            LOG.error("[MessageInboxResource] ❌ Validación fallida - ID de mensaje ausente");
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Missing message ID"))
                    .build()
            );
        }

        LOG.infof("[MessageInboxResource] ✅ Validación exitosa - userId: %d, idMessage: %d", userIdInt, idMessage);

        // TODO: Implementar llamada al caso de uso para obtener el detalle del mensaje
        // Por ahora retornamos un mock para probar la estructura
        LOG.warn("[MessageInboxResource] ⚠️  Retornando respuesta mock - Implementación pendiente");

        return Uni.createFrom().item(
            Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(new ErrorResponse("Message detail endpoint not implemented yet"))
                .build()
        );
    }

    /**
     * DTO interno para respuestas de error
     */
    public static class ErrorResponse {
        public String error;

        public ErrorResponse() {
        }

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
