package org.walrex.infrastructure.adapters.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.AttachmentInfoDTO;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.MessageDetailsDTO;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.ReceiverInfoDTO;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.RemitentInfoDTO;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.AttachmentEntity;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.EmpleadoEntity;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.MessageEntity;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.MessageRecipientEntity;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.UsuarioEntity;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mapper de MapStruct para convertir entidades a DTOs
 * Utiliza CDI para inyección de dependencias en Quarkus
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface MessageDetailsMapper {

    /**
     * Mapea MessageEntity con todas sus relaciones a MessageDetailsDTO
     */
    @Mapping(source = "idMessage", target = "id")
    @Mapping(source = "asunto", target = "subject")
    @Mapping(source = "createAt", target = "createAt", qualifiedByName = "offsetDateTimeToLocalDate")
    @Mapping(source = "sender", target = "remitente")
    @Mapping(target = "receivers", ignore = true) // Se mapea manualmente en el adapter
    @Mapping(target = "attachments", ignore = true) // Se mapea manualmente en el adapter
    @Mapping(target = "isRead", ignore = true) // Se obtiene del recipient específico
    @Mapping(target = "readAt", ignore = true) // Se obtiene del recipient específico
    MessageDetailsDTO toMessageDetailsDTO(MessageEntity messageEntity);

    /**
     * Mapea UsuarioEntity + EmpleadoEntity a RemitentInfoDTO
     */
    @Mapping(source = "id", target = "idUser")
    @Mapping(source = "nameUser", target = "usuario")
    @Mapping(source = "empleado.id", target = "idEmpleado")
    @Mapping(source = "empleado", target = "nombres", qualifiedByName = "mapNombres")
    @Mapping(source = "empleado", target = "apellidos", qualifiedByName = "mapApellidos")
    @Mapping(source = "empleado.email", target = "email")
    RemitentInfoDTO toRemitentInfoDTO(UsuarioEntity usuario);

    /**
     * Mapea MessageRecipientEntity a ReceiverInfoDTO
     */
    @Mapping(source = "recipientId", target = "id")
    @Mapping(source = "recipient.empleado", target = "nombres", qualifiedByName = "mapNombres")
    @Mapping(source = "recipient.empleado", target = "apellidos", qualifiedByName = "mapApellidos")
    @Mapping(source = "recipient.nameUser", target = "username")
    ReceiverInfoDTO toReceiverInfoDTO(MessageRecipientEntity recipientEntity);

    /**
     * Mapea lista de MessageRecipientEntity a lista de ReceiverInfoDTO
     */
    List<ReceiverInfoDTO> toReceiverInfoDTOList(List<MessageRecipientEntity> recipientEntities);

    /**
     * Mapea AttachmentEntity a AttachmentInfoDTO
     */
    @Mapping(source = "idAttachment", target = "id")
    @Mapping(source = "uploadedAt", target = "uploadedAt", qualifiedByName = "offsetDateTimeToLocalDate")
    AttachmentInfoDTO toAttachmentInfoDTO(AttachmentEntity attachmentEntity);

    /**
     * Mapea lista de AttachmentEntity a lista de AttachmentInfoDTO
     */
    List<AttachmentInfoDTO> toAttachmentInfoDTOList(List<AttachmentEntity> attachmentEntities);

    /**
     * Método custom para concatenar nombres del empleado
     */
    @Named("mapNombres")
    default String mapNombres(EmpleadoEntity empleado) {
        if (empleado == null) {
            return "Sin información";
        }
        return empleado.getNombres();
    }

    /**
     * Método custom para concatenar apellidos del empleado
     */
    @Named("mapApellidos")
    default String mapApellidos(EmpleadoEntity empleado) {
        if (empleado == null) {
            return "Sin información";
        }
        String primerApellido = empleado.getPrimerApellido() != null ? empleado.getPrimerApellido() : "";
        String segundoApellido = empleado.getSegundoApellido() != null ? empleado.getSegundoApellido() : "";
        return (primerApellido + " " + segundoApellido).trim();
    }

    /**
     * Convierte OffsetDateTime a LocalDate
     */
    @Named("offsetDateTimeToLocalDate")
    default java.time.LocalDate offsetDateTimeToLocalDate(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDate() : null;
    }

    /**
     * Convierte LocalDateTime a LocalDate
     */
    @Named("localDateTimeToLocalDate")
    default java.time.LocalDate localDateTimeToLocalDate(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.toLocalDate() : null;
    }
}