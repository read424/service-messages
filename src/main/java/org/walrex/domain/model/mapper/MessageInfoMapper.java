package org.walrex.domain.model.mapper;

import org.walrex.domain.model.dto.AttachmentInfo;
import org.walrex.domain.model.dto.MessageInfo;
import org.walrex.domain.model.dto.SenderInfo;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.AttachmentInfoDTO;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.MessageDetailsDTO;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.RemitentInfoDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para convertir DTOs de la capa de infraestructura a DTOs de la capa de dominio
 * Convierte MessageDetailsDTO (infraestructura) → MessageInfo (dominio)
 */
public class MessageInfoMapper {

    /**
     * Convierte MessageDetailsDTO a MessageInfo
     *
     * @param messageDetailsDTO DTO de la capa de infraestructura
     * @return MessageInfo DTO de la capa de dominio
     */
    public static MessageInfo toMessageInfo(MessageDetailsDTO messageDetailsDTO) {
        if (messageDetailsDTO == null) {
            return null;
        }

        MessageInfo messageInfo = new MessageInfo();

        // Mapear campos básicos
        messageInfo.setIdMessage(messageDetailsDTO.getId());
        messageInfo.setAsunto(messageDetailsDTO.getSubject());
        messageInfo.setHtmlcontent(messageDetailsDTO.getContent());

        // Mapear remitente
        if (messageDetailsDTO.getRemitente() != null) {
            messageInfo.setSenderUser(toSenderInfo(messageDetailsDTO.getRemitente()));
        }

        // Mapear attachments
        if (messageDetailsDTO.getAttachments() != null) {
            messageInfo.setAttachments(toAttachmentInfoList(messageDetailsDTO.getAttachments()));
        }

        return messageInfo;
    }

    /**
     * Convierte RemitentInfoDTO a SenderInfo
     *
     * @param remitentInfoDTO DTO del remitente de la capa de infraestructura
     * @return SenderInfo DTO del remitente de la capa de dominio
     */
    private static SenderInfo toSenderInfo(RemitentInfoDTO remitentInfoDTO) {
        if (remitentInfoDTO == null) {
            return null;
        }

        SenderInfo senderInfo = new SenderInfo();

        senderInfo.setIdUsuario(remitentInfoDTO.getIdUser());
        senderInfo.setIdEmpleado(remitentInfoDTO.getIdEmpleado());
        senderInfo.setNoUsuario(remitentInfoDTO.getUsuario());

        // Concatenar apellidos y nombres
        String apenomPersonal = concatenarApellidosNombres(
            remitentInfoDTO.getApellidos(),
            remitentInfoDTO.getNombres()
        );
        senderInfo.setApenomPersonal(apenomPersonal);

        return senderInfo;
    }

    /**
     * Convierte lista de AttachmentInfoDTO a lista de AttachmentInfo
     *
     * @param attachmentInfoDTOList Lista de DTOs de attachments de infraestructura
     * @return Lista de AttachmentInfo de dominio
     */
    private static List<AttachmentInfo> toAttachmentInfoList(List<AttachmentInfoDTO> attachmentInfoDTOList) {
        if (attachmentInfoDTOList == null) {
            return null;
        }

        List<AttachmentInfo> attachmentInfoList = new ArrayList<>();
        for (AttachmentInfoDTO attachmentInfoDTO : attachmentInfoDTOList) {
            attachmentInfoList.add(toAttachmentInfo(attachmentInfoDTO));
        }

        return attachmentInfoList;
    }

    /**
     * Convierte AttachmentInfoDTO a AttachmentInfo
     *
     * @param attachmentInfoDTO DTO de attachment de infraestructura
     * @return AttachmentInfo DTO de attachment de dominio
     */
    private static AttachmentInfo toAttachmentInfo(AttachmentInfoDTO attachmentInfoDTO) {
        if (attachmentInfoDTO == null) {
            return null;
        }

        AttachmentInfo attachmentInfo = new AttachmentInfo();

        attachmentInfo.setId(attachmentInfoDTO.getId());
        attachmentInfo.setFilePath(attachmentInfoDTO.getFilePath());
        attachmentInfo.setFilename(attachmentInfoDTO.getFileName());
        attachmentInfo.setFileType(attachmentInfoDTO.getFileType());
        attachmentInfo.setUploadedAt(attachmentInfoDTO.getUploadedAt());

        return attachmentInfo;
    }

    /**
     * Concatena apellidos y nombres en un solo string
     *
     * @param apellidos Apellidos del personal
     * @param nombres Nombres del personal
     * @return String concatenado "apellidos, nombres"
     */
    private static String concatenarApellidosNombres(String apellidos, String nombres) {
        if (apellidos == null && nombres == null) {
            return null;
        }

        if (apellidos == null) {
            return nombres;
        }

        if (nombres == null) {
            return apellidos;
        }

        return apellidos + ", " + nombres;
    }
}