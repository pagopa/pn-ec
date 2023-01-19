package it.pagopa.pn.ec.repositorymanager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pn.ec.repositorymanager.controller.ClientConfigurationController;
import it.pagopa.pn.ec.repositorymanager.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.repositorymanager.dto.SenderPhysicalAddressDto;
import it.pagopa.pn.ec.repositorymanager.service.RepositoryManagerService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class ClientConfigurationTest {
	
	@Autowired
    private WebTestClient webClient;
	
	@Autowired
	private DynamoDbEnhancedClient enhancedClient;
	
	//ECGRAC.100.1
    @Test
	void testInsertSuccess() {

    	ClientConfigurationDto ccDtoI = new ClientConfigurationDto();
    	SenderPhysicalAddressDto spaDto = new SenderPhysicalAddressDto();
    	
    	spaDto.setName("Mario");
		spaDto.setAddress("Via senza nome 1");
		spaDto.setCap("00123");
		spaDto.setCity("Arezzo");
		spaDto.setPr("AR");

    	ccDtoI.setCxId("11111");
    	ccDtoI.setSqsArn("ABC");
    	ccDtoI.setSqsName("MARIO ROSSI");
    	ccDtoI.setPecReplyTo("mariorossi@pec.it");
		ccDtoI.setMailReplyTo("mariorossi@yahoo.it");

		ccDtoI.setSenderPhysicalAddress(spaDto);
    	
    	webClient.post()
		    	 .uri("localhost:8080/client")
		         .accept(APPLICATION_JSON)
		         .contentType(APPLICATION_JSON)
		         .body(BodyInserters.fromValue(ccDtoI))
		         .exchange()
		         .expectStatus().isOk();
    	
	}
    
    
    
//
////	//ECGRAC.100.2
////	@Test
////	void testInsertFailed() {
////		ClientConfiguration cc = new ClientConfiguration();
////		SenderPhysicalAddress spa = new SenderPhysicalAddress();
////
////		spa.setName("Mario");
////		spa.setAddress("Via senza nome 1");
////		spa.setCap("00123");
////		spa.setCity("Arezzo");
////		spa.setPr("AR");
////
////		cc.setCxId(null);
////		cc.setSqsArn("ABC");
////		cc.setSqsName("MARIO ROSSI");
////		cc.setPecReplyTo("mariorossi@pec.it");
////		cc.setMailReplyTo("mariorossi@yahoo.it");
////
////		cc.setSenderPhysicalAddress(spa);
////
////		System.out.println(cc.toString());
////
////		Assertions.assertEquals(clientController.insertClient(cc).block().getStatusCodeValue(), 403);
////	}
////
////	//ECGRAC.101.1
////	@Test
////	void testReadSuccess() {
////		Assertions.assertEquals(clientController.getClient("1").block().getStatusCodeValue(), 200);
////	}
////
////	//ECGRAC.101.2
////	@Test
////	void testReadFailed() {
////		Assertions.assertEquals(clientController.getClient(null).block().getStatusCodeValue(), 404);
////	}
////
////	//ECGRAC.102.1
////	@Test
////	void testUpdateSuccess() {
//////		record da prendere su db
////		ClientConfiguration cc = new ClientConfiguration();
////		SenderPhysicalAddress spa = new SenderPhysicalAddress();
////
////		spa.setName("Mario");
////		spa.setAddress("Via senza nome 1");
////		spa.setCap("00123");
////		spa.setCity("Arezzo");
////		spa.setPr("AR");
////
////		cc.getCxId();
////		cc.setSqsArn("ABC");
////		cc.setSqsName("MARIO ROSSI");
////		cc.setPecReplyTo("mariorossi@pec.it");
////		cc.setMailReplyTo("mariorossi@yahoo.it");
////
////		cc.setSenderPhysicalAddress(spa);
////
////		System.out.println(cc.toString());
////
////		Assertions.assertEquals(clientController.updateClient(cc).block().getStatusCodeValue(), 200);
////	}
////
////	//ECGRAC.102.2
////	@Test
////	void testUpdateFailed() {
////		ClientConfiguration cc = new ClientConfiguration();
////		SenderPhysicalAddress spa = new SenderPhysicalAddress();
////
////		spa.setName("Mario");
////		spa.setAddress("Via senza nome 1");
////		spa.setCap("00123");
////		spa.setCity("Arezzo");
////		spa.setPr("AR");
////
////		cc.setCxId(null);
////		cc.setSqsArn("ABC");
////		cc.setSqsName("MARIO ROSSI");
////		cc.setPecReplyTo("mariorossi@pec.it");
////		cc.setMailReplyTo("mariorossi@yahoo.it");
////
////		cc.setSenderPhysicalAddress(spa);
////
////		System.out.println(cc.toString());
////
////		Assertions.assertEquals(clientController.updateClient(cc).block().getStatusCodeValue(), 403);
////	}
////
////	//ECGRAC.103.1
////	@Test
////	void testDeleteSuccess() {
////		Assertions.assertEquals(clientController.deleteClient("1").block().getStatusCodeValue(), 200);
////	}
////
////	//ECGRAC.103.2
////	@Test
////	void testDeleteFailed() {
////		Assertions.assertEquals(clientController.getClient(null).block().getStatusCodeValue(), 404);
////	}
//
//	// snippet-start:[dynamodb.java2.create_table.main]
//    public static String createTable(DynamoDbClient ddb, String tableName, String key) {
//        DynamoDbWaiter dbWaiter = ddb.waiter();
//        CreateTableRequest request = CreateTableRequest.builder()
//            .attributeDefinitions(AttributeDefinition.builder()
//                .attributeName(key)
//                .attributeType(ScalarAttributeType.S)
//                .build())
//            .keySchema(KeySchemaElement.builder()
//                .attributeName(key)
//                .keyType(KeyType.HASH)
//                .build())
//            .provisionedThroughput(ProvisionedThroughput.builder()
//                .readCapacityUnits(new Long(5))
//                .writeCapacityUnits(new Long(5))
//                .build())
//            .tableName(tableName)
//            .build();
//
//        String newTable ="";
//        try {
//            CreateTableResponse response = ddb.createTable(request);
//            DescribeTableRequest tableRequest = DescribeTableRequest.builder()
//                .tableName(tableName)
//                .build();
//
//            // Wait until the Amazon DynamoDB table is created.
//            WaiterResponse<DescribeTableResponse> waiterResponse = dbWaiter.waitUntilTableExists(tableRequest);
//            waiterResponse.matched().response().ifPresent(System.out::println);
//            newTable = response.tableDescription().tableName();
//            return newTable;
//
//        } catch (DynamoDbException e) {
//            System.err.println(e.getMessage());
//            System.exit(1);
//        }
//       return "";
//    }
//
// // snippet-start:[dynamodb.java2.describe_table.main]
//    public static void describeDymamoDBTable(DynamoDbClient ddb,String tableName ) {
//
//        DescribeTableRequest request = DescribeTableRequest.builder()
//            .tableName(tableName)
//            .build();
//
//        try {
//            TableDescription tableInfo = ddb.describeTable(request).table();
//            if (tableInfo != null) {
//                System.out.format("Table name  : %s\n", tableInfo.tableName());
//                System.out.format("Table ARN   : %s\n", tableInfo.tableArn());
//                System.out.format("Status      : %s\n", tableInfo.tableStatus());
//                System.out.format("Item count  : %d\n", tableInfo.itemCount().longValue());
//                System.out.format("Size (bytes): %d\n", tableInfo.tableSizeBytes().longValue());
//
//                ProvisionedThroughputDescription throughputInfo = tableInfo.provisionedThroughput();
//                System.out.println("Throughput");
//                System.out.format("  Read Capacity : %d\n", throughputInfo.readCapacityUnits().longValue());
//                System.out.format("  Write Capacity: %d\n", throughputInfo.writeCapacityUnits().longValue());
//
//                List<AttributeDefinition> attributes = tableInfo.attributeDefinitions();
//                System.out.println("Attributes");
//
//                for (AttributeDefinition a : attributes) {
//                    System.out.format("  %s (%s)\n", a.attributeName(), a.attributeType());
//                }
//            }
//
//        } catch (DynamoDbException e) {
//            System.err.println(e.getMessage());
//            System.exit(1);
//        }
//        System.out.println("\nDone!");
//    }
//
}
