package it.pagopa.pnec.repositorymanager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;

@SpringBootTestWebEnv
class ClientConfigurationTest {
//
//	private static DynamoDbClient ddb;
//	private static DynamoDbEnhancedClient enhancedClient;
//
//
//	RepositoryManagerService rms = new RepositoryManagerService(dynamoDbEnhancedClient);
//	ClientConfigurationController clientController = new ClientConfigurationController(repositoryManagerService);
//	ClientConfigurationDto ccDtoI = new ClientConfigurationDto();
//	ClientConfigurationDto ccDtoO = new ClientConfigurationDto();
//	SenderPhysicalAddressDto spaDto = new SenderPhysicalAddressDto();
//
//	// Define the data members required for the test
//    private static String tableName = "AnagraficaClient";
//    private static String itemVal = "cxId";
//
//    @BeforeAll
//    public static void setUp() {
//
//        // Run tests on Real AWS Resources
//        Region region = Region.EU_CENTRAL_1;
//        ddb = DynamoDbClient.builder().region(region).build();
//
//		// Create a DynamoDbEnhancedClient object
//		enhancedClient = DynamoDbEnhancedClient.builder()
//				.dynamoDbClient(ddb).build();
//    }
//
////    @Test
////    @Order(1)
////    public void whenInitializingEnhancedClient_thenNotNull() {
////        assertNotNull(enhancedClient);
////        System.out.println("Test 1 passed");
////    }
////
////    @Test
////    @Order(2)
////    public void CreateTable() {
////
////       String result = createTable(ddb, tableName, itemVal);
////       assertFalse(result.isEmpty());
////       System.out.println("\n Test 2 passed");
////    }
////
////    @Test
////    @Order(3)
////    public void DescribeTable() {
////       describeDymamoDBTable(ddb,tableName);
////       System.out.println("\n Test 3 passed");
////    }
//
//	//ECGRAC.100.1
//    @Test
//    @Order(4)
//	void testInsertSuccess() {
//
//		spaDto.setName("Mario");
//		spaDto.setAddress("Via senza nome 1");
//		spaDto.setCap("00123");
//		spaDto.setCity("Arezzo");
//		spaDto.setPr("AR");
//
//    	ccDtoI.setCxId("4");
//    	ccDtoI.setSqsArn("ABC");
//    	ccDtoI.setSqsName("MARIO ROSSI");
//    	ccDtoI.setPecReplyTo("mariorossi@pec.it");
//		ccDtoI.setMailReplyTo("mariorossi@yahoo.it");
//
//		ccDtoI.setSenderPhysicalAddress(spaDto);
//
//		System.out.println(ccDtoI.toString());
//
//		Mono<ResponseEntity<ClientConfigurationDto>> response = clientController.insertClient(ccDtoI);
////		ccDtoO = rms.insertClient(ccDtoI);
//		Assertions.assertNotNull(response);
//		System.out.println("\n Test 4 passed");
////		Assertions.assertEquals(clientController.insertClient(cc).block().getStatusCodeValue(), 200);
//	}
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
