package fr.gouv.vitam.storage.engine.server.logbook;


import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.server.logbook.parameters.StorageLogbookOutcome;
import fr.gouv.vitam.storage.engine.server.logbook.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.engine.server.logbook.parameters.StorageLogbookParameters;



public class StorageLogbookMockTest {

    private static final String selectString = "{\"select\": \"selectQuery\"}";
    private StorageLogbook storageLogbook;

    @Before
    public void setUp() {
        storageLogbook = new StorageLogbookMock();
    }

    @Test()
    public void addTest() throws Exception {
        storageLogbook.add(getParameters());
    }

    @Test(expected = StorageException.class)
    public void addTestInError() throws Exception {
        storageLogbook.add(getEmptyParameters());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void closeTest() {
        storageLogbook.close();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void selectOperationsbyObjectIdTest() throws Exception {
        storageLogbook.selectOperationsbyObjectId("objectId");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void selectOperationsbyObjectGroupIdTest() throws Exception {
        storageLogbook.selectOperationsbyObjectGroupId("objectGroupId");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void selectOperationsWithASelectTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        storageLogbook.selectOperationsWithASelect(mapper.readTree(selectString));
    }

    private StorageLogbookParameters getParameters() {
        StorageLogbookParameters parameters = new StorageLogbookParameters();
        parameters.putParameterValue(StorageLogbookParameterName.eventDateTime, "2016-07-29T11:56:35.914");
        parameters.setStatus(StorageLogbookOutcome.OK);
        parameters.putParameterValue(StorageLogbookParameterName.objectIdentifier,
            "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.putParameterValue(StorageLogbookParameterName.objectGroupIdentifier,
            "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.putParameterValue(StorageLogbookParameterName.digest, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.putParameterValue(StorageLogbookParameterName.digestAlgorithm, "SHA-256");
        parameters.putParameterValue(StorageLogbookParameterName.size, "1024");
        parameters.putParameterValue(StorageLogbookParameterName.agentIdentifiers, "agentIdentifiers");
        parameters.putParameterValue(StorageLogbookParameterName.agentIdentifierRequester, "agentIdentifierRequester");

        parameters.putParameterValue(StorageLogbookParameterName.outcomeDetailMessage, "outcomeDetailMessage");
        parameters.putParameterValue(StorageLogbookParameterName.objectIdentifierIncome, "objectIdentifierIncome");
        
        return parameters;
    }

    private StorageLogbookParameters getEmptyParameters() {
        return new StorageLogbookParameters();
    }

}
