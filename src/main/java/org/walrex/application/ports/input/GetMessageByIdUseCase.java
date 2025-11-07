package org.walrex.application.ports.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.dto.MessageInfo;

/**
 * Puerto de entrada (Input Port) para obtener el detalle de un mensaje por su ID
 * Define el contrato del caso de uso desde la perspectiva del dominio
 *
 * Este puerto será implementado por un servicio en la capa de dominio
 */
public interface GetMessageByIdUseCase {

    /**
     * Obtiene el detalle completo de un mensaje por su ID de forma reactiva
     *
     * @param idMessage ID del mensaje a consultar
     * @return Uni reactivo con la información completa del mensaje
     */
    Uni<MessageInfo> getMessageById(Integer idMessage);
}
