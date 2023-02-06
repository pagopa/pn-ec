package it.pagopa.pn.ec.notificationtracker.service;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.notificationtracker.rest.NotificationtrackerController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;

@Slf4j
@Service
public class NotificationtrackerMessageReceiver {

	@Autowired
	NotificationtrackerController controller;

	@SqsListener(value = NT_STATO_SMS_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ALWAYS )
	public void receiveSMSObjectMessage(final NotificationTrackerQueueDto message)  {
		log.info("START PULL FROM  -> {}",NT_STATO_SMS_QUEUE_NAME);
		log.info("BODY  -> {}",message);

		controller.getStatoSmS(message).subscribe();
	}

	@SqsListener(value = NT_STATO_EMAIL_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ALWAYS )
	public void receiveEmailObjectMessage(final NotificationTrackerQueueDto message)  {
		log.info("START PULL FROM  -> {}",NT_STATO_EMAIL_QUEUE_NAME);
		log.info("Pull -> {}",message);
		controller.getEmailStatus(message).subscribe();
	}
	@SqsListener(value = NT_STATO_PEC_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ALWAYS )
	public void receivePecObjectMessage(final NotificationTrackerQueueDto message)  {
		log.info("Pull ->  {}",message);
		controller.getPecStatus(message).subscribe();
	}

	@SqsListener(value = NT_STATO_CARTACEO_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ALWAYS )
	public void receiveCartaceoObjectMessage(final NotificationTrackerQueueDto message)  {
		log.info("Pull -> {}",message);
		controller.getCartaceoStatus(message).subscribe();
	}

}
