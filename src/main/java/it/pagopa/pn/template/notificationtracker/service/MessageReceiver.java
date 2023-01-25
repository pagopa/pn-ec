package it.pagopa.pn.template.notificationtracker.service;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.template.notificationtracker.model.RequestModel;
import it.pagopa.pn.template.rest.NotificationtrackerController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.template.notificationtracker.constant.QueueNameConstant.*;

@Slf4j
@Service
public class MessageReceiver {

	@Autowired
	NotificationtrackerController  controller;

	@SqsListener(value = NT_STATO_SMS_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS )
	public void receiveSMSObjectMessage(final RequestModel message) throws Exception {
		log.info("object message received {}",message);
		controller.getSMSStatus(message.getProcessId(),message.getCurrStatus(),message.getClientId(),message.getNextStatus());
	}

	@SqsListener(value = NT_STATO_EMAIL_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS )
	public void receiveEmailObjectMessage(final RequestModel message) throws Exception {
		log.info("object message received {}",message);
		controller.getEmailStatus(message.getProcessId(),message.getCurrStatus(),message.getClientId(),message.getNextStatus());
	}
	@SqsListener(value = NT_STATO_PEC_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS )
	public void receivePecObjectMessage(final RequestModel message) throws Exception {
		log.info("object message received {}",message);
		controller.getPecStatus(message.getProcessId(),message.getCurrStatus(),message.getClientId(),message.getNextStatus());
	}

	@SqsListener(value = NT_STATO_CARTACEO_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS )
	public void receiveCartaceoObjectMessage(final RequestModel message) throws Exception {
		log.info("object message received {}",message);
		controller.getCartacepStatus(message.getProcessId(),message.getCurrStatus(),message.getClientId(),message.getNextStatus());
	}

}
