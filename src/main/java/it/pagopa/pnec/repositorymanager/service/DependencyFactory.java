package it.pagopa.pnec.repositorymanager.service;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DependencyFactory {

	private DependencyFactory() {
		
	}
	
	/**
     * @return an instance of DynamoDbClient
     */
    public static DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder().build();
    }
	
	public static DynamoDbEnhancedClient dynamoDbEnhancedClient() {
		DynamoDbClient ddb = DependencyFactory.dynamoDbClient();
        return DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();
	}

}
