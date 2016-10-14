/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
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
        final ObjectMapper mapper = new ObjectMapper();
        storageLogbook.selectOperationsWithASelect(mapper.readTree(selectString));
    }

    private StorageLogbookParameters getParameters() {
        final Map<StorageLogbookParameterName, String> initalParameters = new TreeMap<>();

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

        final StorageLogbookParameters parameters = new StorageLogbookParameters(initalParameters);

        return parameters;
    }

    private StorageLogbookParameters getEmptyParameters() throws StorageException {
        try {
            return new StorageLogbookParameters(new TreeMap<>());
        } catch (final IllegalArgumentException exception) {
            throw new StorageException(exception.getMessage(), exception);
        }
    }

}
