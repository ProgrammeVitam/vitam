package fr.gouv.vitam.storage.engine.server.logbook.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Test class for StorageLogbookParameters
 */
public class StorageLogbookParametersTest {

    @Test
    public void getParametersTest() {
        StorageLogbookParameters parameters = getParameters();
        assertNull(parameters.getEventDateTime());
        assertNull(parameters.getStatus());
        parameters.putParameterValue(StorageLogbookParameterName.eventDateTime, "2016-07-29T11:56:35.914");
        assertNotNull(parameters.getEventDateTime());
        parameters.setStatus(StorageLogbookOutcome.OK);
        assertEquals(parameters.getStatus().toString(), StorageLogbookOutcome.OK.toString());
        assertEquals(parameters.checkMandatoryParameters(), true);
    }


    @Test(expected = IllegalArgumentException.class)
    public void getEmptyParametersTest() {
        StorageLogbookParameters parameters = getEmptyParameters();
        parameters.checkMandatoryParameters();
    }


    private StorageLogbookParameters getEmptyParameters() {
        return new StorageLogbookParameters();
    }

    private StorageLogbookParameters getParameters() {
        StorageLogbookParameters parameters = new StorageLogbookParameters();
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

}

