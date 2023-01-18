package it.pagopa.pnec.notificationTracker.factory;

import it.pagopa.pnec.notificationTracker.model.RequestModel;

public class RequestObjectFactory {

	 public static RequestModel  getStatus() {
		 RequestModel request = new RequestModel();
	        request.setProcessId("INVIO_PEC");
	        request.setCurrStatus("BOOKED");
	        request.setClientId("C050");
	        request.setNextStatus("VALIDATE");
	        return request;
	    }
}
