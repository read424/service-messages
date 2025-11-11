package org.walrex.infrastructure.adapters.outbound.persistence.adapter;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.walrex.application.ports.output.InboxMessagePort;
import org.walrex.domain.model.MessageInboxItem;
import org.walrex.domain.model.PagedResult;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.AttachmentInfoDTO;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.MessageDetailsDTO;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.ReceiverInfoDTO;
import org.walrex.infrastructure.adapters.outbound.persistence.dto.RemitentInfoDTO;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.AttachmentEntity;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.EmpleadoEntity;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.MessageEntity;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.MessageRecipientEntity;
import org.walrex.infrastructure.adapters.outbound.persistence.entity.UsuarioEntity;
import org.walrex.infrastructure.adapters.outbound.persistence.repository.AttachmentRepository;
import org.walrex.infrastructure.adapters.outbound.persistence.repository.MessageRecipientRepository;
import org.walrex.infrastructure.adapters.outbound.persistence.repository.MessageRepository;
import org.walrex.infrastructure.adapters.outbound.persistence.mapper.MessageDetailsMapper;
import org.walrex.infrastructure.adapters.outbound.persistence.exception.MessageNotFoundException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa el puerto de salida InboxMessagePort
 * Se encarga de obtener los mensajes desde la base de datos y mapearlos al modelo de dominio
 *
 * Esta clase pertenece a la capa de infraestructura
 */
@ApplicationScoped
public class MessageInboxPersistenceAdapter implements InboxMessagePort {

    private static final Logger LOG = Logger.getLogger(MessageInboxPersistenceAdapter.class);

    private final MessageRecipientRepository messageRecipientRepository;
    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;

    @Inject
    public MessageInboxPersistenceAdapter(
            MessageRecipientRepository messageRecipientRepository,
            MessageRepository messageRepository,
            AttachmentRepository attachmentRepository) {
        this.messageRecipientRepository = messageRecipientRepository;
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
    }

    /**
     * Obtiene los mensajes del inbox de un usuario desde la base de datos
     * Utiliza el repositorio reactivo para obtener los destinatarios con sus mensajes
     * Incluye el conteo total de registros para metadatos de paginación
     *
     * @param userId ID del usuario destinatario
     * @param page Parámetro opcional para paginación (Quarkus Panache Page)
     * @return Uni reactivo con el resultado paginado que contiene los mensajes y metadatos
     */
    @Override
    public Uni<PagedResult<MessageInboxItem>> findMessagesByUser(Integer userId, Optional<Page> page) {
        if (page.isPresent()) {
            Page p = page.get();
            LOG.infof("[MessageInboxPersistenceAdapter] Consultando BD con paginación - userId: %d, page: %d, size: %d",
                    userId, p.index, p.size);

            // Obtener el total de registros y los datos paginados en paralelo
            Uni<Long> totalCountUni = messageRecipientRepository.countByRecipientId(userId);
            Uni<List<MessageRecipientEntity>> recipientsUni =
                messageRecipientRepository.findByRecipientIdWithMessagePaged(userId, p.index, p.size);

            // Combinar ambos resultados
            return Uni.combine().all().unis(recipientsUni, totalCountUni)
                .asTuple()
                .map(tuple -> {
                    List<MessageRecipientEntity> recipients = tuple.getItem1();
                    Long totalCount = tuple.getItem2();

                    LOG.debugf("[MessageInboxPersistenceAdapter] Datos obtenidos de BD - userId: %d, total en BD: %d, página actual: %d registros",
                            (Object) userId, (Object) totalCount, (Object) recipients.size());

                    // Mapear entidades a dominio
                    List<MessageInboxItem> items = recipients.stream()
                        .map(this::mapToDomain)
                        .toList();

                    // Crear PagedResult con metadatos
                    PagedResult<MessageInboxItem> result = new PagedResult<>(items, totalCount, p.index, p.size);
                    LOG.infof("[MessageInboxPersistenceAdapter] Resultado mapeado exitosamente - userId: %d, elementos: %d, total: %d",
                            userId, items.size(), totalCount);
                    return result;
                })
                .onFailure().invoke(throwable ->
                    LOG.errorf(throwable, "[MessageInboxPersistenceAdapter] Error al consultar BD - userId: %d", userId)
                );
        } else {
            LOG.infof("[MessageInboxPersistenceAdapter] Consultando BD sin paginación - userId: %d", userId);

            // Sin paginación - retornar todos los mensajes
            return messageRecipientRepository.findByRecipientIdWithMessage(userId)
                .map(recipients -> {
                    LOG.debugf("[MessageInboxPersistenceAdapter] Datos obtenidos de BD - userId: %d, total: %d registros",
                            (Object) userId, (Object) recipients.size());

                    List<MessageInboxItem> items = recipients.stream()
                        .map(this::mapToDomain)
                        .toList();

                    // Crear PagedResult sin paginación
                    PagedResult<MessageInboxItem> result = new PagedResult<>(items);
                    LOG.infof("[MessageInboxPersistenceAdapter] Resultado mapeado exitosamente - userId: %d, elementos: %d",
                            userId, items.size());
                    return result;
                })
                .onFailure().invoke(throwable ->
                    LOG.errorf(throwable, "[MessageInboxPersistenceAdapter] Error al consultar BD sin paginación - userId: %d", userId)
                );
        }
    }

    /**
     * Mapea una entidad MessageRecipientEntity a un objeto de dominio MessageInboxItem
     *
     * @param recipientEntity Entidad de persistencia
     * @return Objeto de dominio
     */
    private MessageInboxItem mapToDomain(MessageRecipientEntity recipientEntity) {
        var message = recipientEntity.getMessage();

        return MessageInboxItem.builder()
            .idMessage(message.getIdMessage().intValue())
            .isRead(recipientEntity.getIsRead())
            .message(message.getAsunto()) // Usamos el asunto como mensaje principal
            .numAttachments(message.getAttachments() != null ? message.getAttachments().size() : 0)
            .senderName(getSenderName(message.getSenderId())) // TODO: Obtener nombre real del sender
            .createdAt(message.getCreateAt())
            .timeReceived(formatTimeReceived(message.getCreateAt()))
            .build();
    }

    /**
     * Obtiene el nombre del remitente
     * TODO: Implementar lógica para obtener el nombre real desde un servicio de usuarios
     *
     * @param senderId ID del remitente
     * @return Nombre del remitente
     */
    private String getSenderName(Integer senderId) {
        // Por ahora retornamos un placeholder
        // En producción, esto debería consultar un servicio de usuarios o una tabla de usuarios
        return "User #" + senderId;
    }

    /**
     * Formatea el timestamp a un string legible para el usuario
     * Ejemplos: "Hace 5 minutos", "Hace 2 horas", "Hace 3 días"
     *
     * @param createdAt Timestamp de creación del mensaje
     * @return String formateado con el tiempo transcurrido
     */
    private String formatTimeReceived(OffsetDateTime createdAt) {
        if (createdAt == null) {
            return "Desconocido";
        }

        OffsetDateTime now = OffsetDateTime.now();
        Duration duration = Duration.between(createdAt, now);

        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Hace " + seconds + " segundo" + (seconds != 1 ? "s" : "");
        } else if (minutes < 60) {
            return "Hace " + minutes + " minuto" + (minutes != 1 ? "s" : "");
        } else if (hours < 24) {
            return "Hace " + hours + " hora" + (hours != 1 ? "s" : "");
        } else if (days < 7) {
            return "Hace " + days + " día" + (days != 1 ? "s" : "");
        } else if (days < 30) {
            long weeks = days / 7;
            return "Hace " + weeks + " semana" + (weeks != 1 ? "s" : "");
        } else if (days < 365) {
            long months = days / 30;
            return "Hace " + months + " mes" + (months != 1 ? "es" : "");
        } else {
            // Si es muy antiguo, mostrar fecha completa
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return createdAt.format(formatter);
        }
    }

    /**
     * Obtiene el detalle completo de un mensaje por su ID
     *
     * @param idMessage ID del mensaje a consultar
     * @return Uni reactivo con el detalle completo del mensaje
     */
    @Override
    public Uni<MessageDetailsDTO> getDetailMessageById(Integer idMessage, Integer idDestinatario) {
        LOG.infof("[MessageInboxPersistenceAdapter] Consultando detalle del mensaje - idMessage: %d", idMessage);

        // Primera consulta: cargar mensaje con recipients (y sus relaciones)
        return messageRepository.findByIdWithFullDetails(idMessage.longValue())
            .onItem().ifNull().failWith(() -> {
                LOG.errorf("[MessageInboxPersistenceAdapter] Mensaje no encontrado - idMessage: %d", idMessage);
                return new MessageNotFoundException(idMessage);
            })
            .flatMap(msg -> {
                int recipientsCount = msg.getRecipients() != null ? msg.getRecipients().size() : 0;
                LOG.infof("[MessageInboxPersistenceAdapter] Mensaje encontrado - idMessage: %d, recipients: %d",
                    idMessage, recipientsCount);

                // Segunda consulta (secuencial): cargar attachments
                return messageRepository.findByIdWithAttachments(idMessage.longValue())
                    .onItem().ifNull().continueWith(msg) // Si no hay attachments, continuar con el mensaje original
                    .map(msgWithAttachments -> {
                        // Combinar los datos de ambas consultas
                        List<MessageRecipientEntity> recipientsList = msg.getRecipients();
                        List<AttachmentEntity> attachmentFiles = msgWithAttachments.getAttachments();

                MessageDetailsDTO messageDetailsDTO = new MessageDetailsDTO();

                messageDetailsDTO.setId(msg.getIdMessage().intValue());

                if (msg.getSender() != null) {
                    RemitentInfoDTO remitente = new RemitentInfoDTO();
                    UsuarioEntity sender = msg.getSender();

                    remitente.setIdUser(sender.getId().intValue());
                    remitente.setUsuario(sender.getNameUser());

                    // Si tiene empleado asociado, obtener nombres y apellidos
                    if (sender.getEmpleado() != null) {
                        EmpleadoEntity empleado = sender.getEmpleado();
                        remitente.setIdEmpleado(empleado.getId().intValue());
                        remitente.setNombres(empleado.getNombres());
                        remitente.setApellidos(empleado.getPrimerApellido() + " " + empleado.getSegundoApellido());
                        remitente.setEmail(empleado.getEmail());
                    }

                    messageDetailsDTO.setRemitente(remitente);
                }
                messageDetailsDTO.setContent(msg.getContent());
                if (msg.getCreateAt() != null) {
                    messageDetailsDTO.setCreateAt(msg.getCreateAt().toLocalDate());
                }
                messageDetailsDTO.setSubject(msg.getAsunto());

                if (recipientsList != null && !recipientsList.isEmpty()) {
                    Integer senderId = msg.getSender() != null ? msg.getSender().getId().intValue() : null;
                    List<ReceiverInfoDTO> receivers = new java.util.ArrayList<>();

                    for (MessageRecipientEntity recipientEntity : recipientsList) {
                        Integer recipientId = recipientEntity.getRecipientId();

                        if (recipientId.equals(idDestinatario)) {
                            messageDetailsDTO.setIsRead(recipientEntity.getIsRead());

                            // Convertir LocalDateTime a LocalDate
                            if (recipientEntity.getReadAt() != null) {
                                messageDetailsDTO.setReadAt(recipientEntity.getReadAt().toLocalDate());
                            }
                        }
                        else if (senderId == null || !recipientId.equals(senderId)) {
                            ReceiverInfoDTO receiver = new ReceiverInfoDTO();
                            UsuarioEntity recipientUser = recipientEntity.getRecipient();

                            if (recipientUser != null) {
                                receiver.setId(recipientUser.getId().intValue());
                                receiver.setUsername(recipientUser.getNameUser());

                                // Si tiene empleado asociado, obtener nombres y apellidos
                                if (recipientUser.getEmpleado() != null) {
                                    EmpleadoEntity empleado = recipientUser.getEmpleado();
                                    receiver.setNombres(empleado.getNombres());
                                    receiver.setApellidos(empleado.getPrimerApellido() + " " + empleado.getSegundoApellido());
                                }
                            }

                            receivers.add(receiver);
                        }
                    }

                    messageDetailsDTO.setReceivers(receivers);
                }

                if (attachmentFiles != null && !attachmentFiles.isEmpty()) {
                    List<AttachmentInfoDTO> attachments = attachmentFiles.stream()
                        .map(attachmentEntity -> {
                            AttachmentInfoDTO attachment = new AttachmentInfoDTO();
                            attachment.setId(attachmentEntity.getId());
                            attachment.setFilePath(attachmentEntity.getFilePath());
                            attachment.setFileName(attachmentEntity.getFileName());
                            attachment.setFileType(attachmentEntity.getFileType());

                            // Convertir OffsetDateTime a LocalDate
                            if (attachmentEntity.getUploadedAt() != null) {
                                attachment.setUploadedAt(attachmentEntity.getUploadedAt().toLocalDate());
                            }

                            return attachment;
                        })
                        .toList();

                    messageDetailsDTO.setAttachments(attachments);
                }

                        return messageDetailsDTO;
                    });
            });
    }
}
