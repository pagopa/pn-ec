package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.rest.v1.dto.WhatsappTemplate;
import reactor.core.publisher.Mono;

/**
 * Adapter verso AWS End User Messaging Social per l'invio di messaggi WhatsApp.
 */
public interface EumSocialService {

    /**
     * Invia un messaggio WhatsApp basato su un template approvato da Meta.
     *
     * @param phoneNumber  numero del destinatario (formato +39..., 0039... o solo cifre con prefisso internazionale)
     * @param template     nome, lingua e parametri del template
     * @param callbackData valore restituito da Meta negli eventi di stato (biz_opaque_callback_data), usato per la correlazione
     * @return identificativo del messaggio restituito da End User Messaging Social
     */
    Mono<String> sendTemplateMessage(String phoneNumber, WhatsappTemplate template, String callbackData);
}
