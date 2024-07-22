/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.engine.server.storagelog.parameters;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

/**
 * Test class for StorageLogbookParameters
 */
public class StorageLogbookParametersTest {

    private static final StorageLogbookOutcome OK_STATUS = StorageLogbookOutcome.OK;
    private static final StorageLogbookOutcome KO_STATUS = StorageLogbookOutcome.KO;
    private static final String DATE = "2016-07-29T11:56:35.914";

    @Test
    public void getParametersTest() {
        final StorageLogbookParameters parameters = getParameters();
        final Map<StorageLogbookParameterName, String> mapParameters = parameters.getMapParameters();
        assertEquals(mapParameters.get(StorageLogbookParameterName.eventType), "CREATE");

        final LocalDateTime eventDate = parameters.getEventDateTime();
        assertEquals(LocalDateUtil.parseMongoFormattedDate(DATE), eventDate);

        final StorageLogbookOutcome statusOutcome = parameters.getStatus();
        assertEquals(OK_STATUS, statusOutcome);
    }

    @Test
    public void updateStatusTest() {
        final StorageLogbookParameters parameters = getParameters();

        StorageLogbookOutcome statusOutcom = parameters.getStatus();
        assertEquals(OK_STATUS, statusOutcom);

        parameters.setStatus(KO_STATUS);
        statusOutcom = parameters.getStatus();
        assertEquals(KO_STATUS, statusOutcom);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getEmptyParametersTest() {
        StorageLogbookParameters.buildCreateLogParameters(new HashMap<>());
    }

    private StorageLogbookParameters getParameters() {
        final Map<StorageLogbookParameterName, String> parameters = new TreeMap<>();

        parameters.put(StorageLogbookParameterName.eventType, "CREATE");
        parameters.put(StorageLogbookParameterName.dataCategory, DataCategory.UNIT.getFolder());
        parameters.put(StorageLogbookParameterName.xRequestId, "abcd");
        parameters.put(StorageLogbookParameterName.tenantId, "0");
        parameters.put(StorageLogbookParameterName.objectIdentifier, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.put(StorageLogbookParameterName.objectGroupIdentifier, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.put(StorageLogbookParameterName.digest, "aeaaaaaaaaaam7mxaaaamakv36y6m3yaaaaq");
        parameters.put(StorageLogbookParameterName.digestAlgorithm, "SHA-256");
        parameters.put(StorageLogbookParameterName.size, "1024");
        parameters.put(StorageLogbookParameterName.agentIdentifiers, "agentIdentifiers");
        parameters.put(StorageLogbookParameterName.agentIdentifierRequester, "agentIdentifierRequester");
        parameters.put(StorageLogbookParameterName.eventDateTime, DATE);
        parameters.put(StorageLogbookParameterName.outcome, OK_STATUS.name());

        return StorageLogbookParameters.buildCreateLogParameters(parameters);
    }
}
