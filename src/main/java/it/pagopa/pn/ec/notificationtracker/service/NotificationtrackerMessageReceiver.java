package it.pagopa.pn.ec.notificationtracker.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.notificationtracker.model.NotificationRequestModel;
import it.pagopa.pn.ec.notificationtracker.rest.NotificationtrackerController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;

@Slf4j
@Service
public class NotificationtrackerMessageReceiver {

	@Autowired
	NotificationtrackerController controller;

	@SqsListener(value = NT_STATO_SMS_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ALWAYS )
	public void receiveSMSObjectMessage(final NotificationRequestModel message)  {
		log.info("Pull -> {}",message);
		controller.getStatoSmS(message.getProcessId(),message.getCurrStatus(),message.getXpagopaExtchCxId(),message.getNextStatus()).subscribe();
	}

	@SqsListener(value = NT_STATO_EMAIL_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.NEVER )
	public void receiveEmailObjectMessage(final NotificationRequestModel message)  {
		log.info("Pull -> {}",message);
		controller.getEmailStatus(message.getProcessId(),message.getCurrStatus(),message.getXpagopaExtchCxId(),message.getNextStatus()).subscribe();
	}
	@SqsListener(value = NT_STATO_PEC_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.NEVER )
	public void receivePecObjectMessage(final NotificationRequestModel message)  {
		log.info("Pull ->  {}",message);
		controller.getPecStatus(message.getProcessId(),message.getCurrStatus(),message.getXpagopaExtchCxId(),message.getNextStatus()).subscribe();
	}

	@SqsListener(value = NT_STATO_CARTACEO_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.NEVER )
	public void receiveCartaceoObjectMessage(final NotificationRequestModel message)  {
		log.info("Pull -> {}",message);
		controller.getCartaceoStatus(message.getProcessId(),message.getCurrStatus(),message.getXpagopaExtchCxId(),message.getNextStatus()).subscribe();
	}

}
