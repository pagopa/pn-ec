package it.pagopa.pnec.repositorymanager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import it.pagopa.pnec.repositorymanager.controller.ClientConfigurationController;
import it.pagopa.pnec.repositorymanager.model.ClientConfiguration;
import it.pagopa.pnec.repositorymanager.model.SenderPhysicalAddress;
import it.pagopa.pnec.repositorymanager.service.RepositoryManagerService;

class ClientConfigurationTest {
	
	RepositoryManagerService rms = new RepositoryManagerService();
	ClientConfigurationController clientController = new ClientConfigurationController();
	
	//ECGRAC.100.1
	@Test 
	void testInsertSuccess() {
		
		ClientConfiguration cc = new ClientConfiguration();
		SenderPhysicalAddress spa = new SenderPhysicalAddress();
		
		spa.setName("Mario");
		spa.setAddress("Via senza nome 1");
		spa.setCap("00123");
		spa.setCity("Arezzo");
		spa.setPr("AR");
		
		cc.setCxId("1");
		cc.setSqsArn("ABC");
		cc.setSqsName("MARIO ROSSI");
		cc.setPecReplyTo("mariorossi@pec.it");
		cc.setMailReplyTo("mariorossi@yahoo.it");
				
		cc.setSenderPhysicalAddress(spa);
		
		System.out.println(cc.toString());
		
		Assertions.assertEquals(clientController.insertClient(cc).block().getStatusCodeValue(), 200); 
	}
	
	//ECGRAC.100.2
	@Test
	void testInsertFailed() {
		ClientConfiguration cc = new ClientConfiguration();
		SenderPhysicalAddress spa = new SenderPhysicalAddress();
		
		spa.setName("Mario");
		spa.setAddress("Via senza nome 1");
		spa.setCap("00123");
		spa.setCity("Arezzo");
		spa.setPr("AR");
		
		cc.setCxId(null);
		cc.setSqsArn("ABC");
		cc.setSqsName("MARIO ROSSI");
		cc.setPecReplyTo("mariorossi@pec.it");
		cc.setMailReplyTo("mariorossi@yahoo.it");
				
		cc.setSenderPhysicalAddress(spa);
		
		System.out.println(cc.toString());
		
		Assertions.assertEquals(clientController.insertClient(cc).block().getStatusCodeValue(), 403);
	}
	
	//ECGRAC.101.1
	@Test
	void testReadSuccess() {
		Assertions.assertEquals(clientController.getClient("1").block().getStatusCodeValue(), 200);
	}
	
	//ECGRAC.101.2
	@Test
	void testReadFailed() {
		Assertions.assertEquals(clientController.getClient(null).block().getStatusCodeValue(), 404);
	}
	
	//ECGRAC.102.1
	@Test
	void testUpdateSuccess() {
//		record da prendere su db
		ClientConfiguration cc = new ClientConfiguration();
		SenderPhysicalAddress spa = new SenderPhysicalAddress();
		
		spa.setName("Mario");
		spa.setAddress("Via senza nome 1");
		spa.setCap("00123");
		spa.setCity("Arezzo");
		spa.setPr("AR");
		
		cc.getCxId();
		cc.setSqsArn("ABC");
		cc.setSqsName("MARIO ROSSI");
		cc.setPecReplyTo("mariorossi@pec.it");
		cc.setMailReplyTo("mariorossi@yahoo.it");
				
		cc.setSenderPhysicalAddress(spa);
		
		System.out.println(cc.toString());
		
		Assertions.assertEquals(clientController.updateClient(cc).block().getStatusCodeValue(), 200);
	}
	
	//ECGRAC.102.2
	@Test
	void testUpdateFailed() {
		ClientConfiguration cc = new ClientConfiguration();
		SenderPhysicalAddress spa = new SenderPhysicalAddress();
		
		spa.setName("Mario");
		spa.setAddress("Via senza nome 1");
		spa.setCap("00123");
		spa.setCity("Arezzo");
		spa.setPr("AR");
		
		cc.setCxId(null);
		cc.setSqsArn("ABC");
		cc.setSqsName("MARIO ROSSI");
		cc.setPecReplyTo("mariorossi@pec.it");
		cc.setMailReplyTo("mariorossi@yahoo.it");
				
		cc.setSenderPhysicalAddress(spa);
		
		System.out.println(cc.toString());
		
		Assertions.assertEquals(clientController.updateClient(cc).block().getStatusCodeValue(), 403);
	}
	
	//ECGRAC.103.1
	@Test
	void testDeleteSuccess() {
		Assertions.assertEquals(clientController.deleteClient("1").block().getStatusCodeValue(), 200);
	}
	
	//ECGRAC.103.2
	@Test
	void testDeleteFailed() {
		Assertions.assertEquals(clientController.getClient(null).block().getStatusCodeValue(), 404);
	}

}
