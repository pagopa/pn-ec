package it.pagopa.pn.ec.repositorymanager.constant;

public final class GestoreRepositoryDynamoDbTableName {

    private GestoreRepositoryDynamoDbTableName() {
        throw new IllegalStateException("DynamoTableNameConstant is a class of constant");
    }

    public static final String ANAGRAFICA_TABLE_NAME = "pn-ec-anagrafica-client";
    public static final String REQUEST_METADATA_TABLE_NAME = "";
    public static final String REQUEST_PERSONAL_TABLE_NAME = "";
    public static final String REQUEST_TABLE_NAME = "PnEcTableRichiesteFrancescoC";
}
