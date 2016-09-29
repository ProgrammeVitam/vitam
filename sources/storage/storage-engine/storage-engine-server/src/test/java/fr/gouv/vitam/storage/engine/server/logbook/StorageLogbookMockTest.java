package fr.gouv.vitam.storage.engine.server.logbook;


import java.util.Map;
import java.util.TreeMap;

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
        Map<StorageLogbookParameterName, String> initalParameters = new TreeMap<>();

        initalParameters.put(StorageLogbookParameterName.eventDateTime, "2016-07-29T11:56:35.914");
        initalParameters.put(StorageLogbookParameterName.outcome, StorageLogbookOutcome.OK.name());
        initalParameters.put(StorageLogbookParameterName.objectIdentifier, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        initalParameters.put(StorageLogbookParameterName.objectGroupIdentifier, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        initalParameters.put(StorageLogbookParameterName.digest, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        initalParameters.put(StorageLogbookParameterName.digestAlgorithm, "SHA-256");
        initalParameters.put(StorageLogbookParameterName.size, "1024");
        initalParameters.put(StorageLogbookParameterName.agentIdentifiers, "agentIdentifiers");
        initalParameters.put(StorageLogbookParameterName.agentIdentifierRequester, "agentIdentifierRequester");
        initalParameters.put(StorageLogbookParameterName.outcomeDetailMessage, "outcomeDetailMessage");
        initalParameters.put(StorageLogbookParameterName.objectIdentifierIncome, "objectIdentifierIncome");

        StorageLogbookParameters parameters = new StorageLogbookParameters(initalParameters);

        return parameters;
    }

    private StorageLogbookParameters getEmptyParameters() throws StorageException {
        try {
            return new StorageLogbookParameters(new TreeMap<>());
        } catch (IllegalArgumentException exception) {
            throw new StorageException(exception.getMessage(), exception);
        }
    }

}
