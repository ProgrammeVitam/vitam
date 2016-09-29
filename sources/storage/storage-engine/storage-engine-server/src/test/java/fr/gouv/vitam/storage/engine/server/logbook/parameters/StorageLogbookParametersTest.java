package fr.gouv.vitam.storage.engine.server.logbook.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

/**
 * Test class for StorageLogbookParameters
 */
public class StorageLogbookParametersTest {
    private static final StorageLogbookOutcome OK_STATUS = StorageLogbookOutcome.OK;
    private static final StorageLogbookOutcome KO_STATUS = StorageLogbookOutcome.KO;
    private static final String DATE = "2016-07-29T11:56:35.914";

    @Test
    public void getParametersTest() {
        StorageLogbookParameters parameters = getParameters();
        Map<StorageLogbookParameterName, String> mapParameters = parameters.getMapParameters();
        assertNull(mapParameters.get(StorageLogbookParameterName.outcomeDetailMessage));
        assertNull(mapParameters.get(StorageLogbookParameterName.objectIdentifierIncome));

        parameters.setOutcomDetailMessage("outcomeDetailMessage");
        assertNotNull(mapParameters.get(StorageLogbookParameterName.outcomeDetailMessage));

        parameters.setObjectIdentifierIncome("objectIdentifierIncome");
        assertNotNull(mapParameters.get(StorageLogbookParameterName.objectIdentifierIncome));
        assertEquals(mapParameters.get(StorageLogbookParameterName.outcomeDetailMessage), "outcomeDetailMessage");
        assertEquals(mapParameters.get(StorageLogbookParameterName.objectIdentifierIncome), "objectIdentifierIncome");
        assertEquals(parameters.checkMandatoryParameters(), true);

        LocalDateTime eventDate = parameters.getEventDateTime();
        assertEquals(LocalDateTime.parse(DATE), eventDate);

        StorageLogbookOutcome statusOutcom = parameters.getStatus();
        assertEquals(OK_STATUS, statusOutcom);
    }

    @Test
    public void updateStatusTest() {
        StorageLogbookParameters parameters = getParameters();

        StorageLogbookOutcome statusOutcom = parameters.getStatus();
        assertEquals(OK_STATUS, statusOutcom);

        parameters.setStatus(KO_STATUS);
        statusOutcom = parameters.getStatus();
        assertEquals(KO_STATUS, statusOutcom);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getEmptyParametersTest() {
        StorageLogbookParameters parameters = getEmptyParameters();
        parameters.checkMandatoryParameters();
    }


    private StorageLogbookParameters getEmptyParameters() {
        return new StorageLogbookParameters(new HashMap<>());
    }

    private StorageLogbookParameters getParameters() {
        Map<StorageLogbookParameterName, String> parameters = new TreeMap<>();

        parameters.put(StorageLogbookParameterName.objectIdentifier, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.put(StorageLogbookParameterName.objectGroupIdentifier, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.put(StorageLogbookParameterName.digest, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.put(StorageLogbookParameterName.digestAlgorithm, "SHA-256");
        parameters.put(StorageLogbookParameterName.size, "1024");
        parameters.put(StorageLogbookParameterName.agentIdentifiers, "agentIdentifiers");
        parameters.put(StorageLogbookParameterName.agentIdentifierRequester, "agentIdentifierRequester");
        parameters.put(StorageLogbookParameterName.eventDateTime, DATE);
        parameters.put(StorageLogbookParameterName.outcome, OK_STATUS.name());

        return new StorageLogbookParameters(parameters);
    }

}
