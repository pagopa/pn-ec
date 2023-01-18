package it.pagopa.pnec.notificationTracker.service;

public interface NotificationtrackerService {

	void getStato(String processId, String currStatus, String clientId, String nextStatus);

	void putNewStato(String processId, String currStatus, String clientId, String nextStatus);

}
