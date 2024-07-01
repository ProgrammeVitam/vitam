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
package fr.gouv.vitam.worker.core.plugin.traceability;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.InputStream;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.model.WorkspaceConstants.TRACEABILITY_OPERATION_DIRECTORY;
import static fr.gouv.vitam.worker.core.plugin.traceability.VerifyMerkleTreeActionHandler.DATA_FILE;
import static java.io.File.separator;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class VerifyMerkleTreeActionHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String DETAIL_EVENT_TRACEABILITY = "EVENT_DETAIL_DATA.json";
    private static final String DETAIL_EVENT_TRACEABILITY_WRONG_ROOT = "EVENT_DETAIL_DATA_WRONG_ROOT.json";
    private static final String FAKE_DETAIL_EVENT_TRACEABILITY = "sip.xml";

    private static final String MERKLE_TREE_JSON = "merkleTree.json";
    private static final String MERKLE_TREE_JSON_WRONG_ROOT = "merkleTreeWrongRoot.json";

    private static final String OPERATIONS_WRONG_DATES_JSON = "operations_wrong_dates.json";

    private static final String OBJECT_NAME = "objectName.json";
    private static final String REPORT_FILENAME = "report";

    private static final String ZIP_DATA_FOLDER = TRACEABILITY_OPERATION_DIRECTORY + File.separator + OBJECT_NAME;

    private VerifyMerkleTreeActionHandler verifyMerkleTreeActionHandler;
    private File reportTempFile;

    @Mock
    private HandlerIO handler;

    @Mock
    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {
        when(params.getObjectName()).thenReturn(OBJECT_NAME);
        when(
            handler.getJsonFromWorkspace(eq(params.getObjectName() + separator + WorkspaceConstants.REPORT))
        ).thenReturn(createObjectNode());

        final File traceabilityFile = PropertiesUtils.getResourceFile(DETAIL_EVENT_TRACEABILITY);
        when(handler.getInput(eq(0), eq(File.class))).thenReturn(traceabilityFile);

        reportTempFile = temporaryFolder.newFile(REPORT_FILENAME);
        when(handler.getNewLocalFile(anyString())).thenReturn(reportTempFile);

        verifyMerkleTreeActionHandler = new VerifyMerkleTreeActionHandler();
    }

    @Test
    public void testVerifyMerkleTreeThenOK() throws Exception {
        final InputStream operationsJson = PropertiesUtils.getResourceAsStream(DATA_FILE);
        final InputStream merkleTreeJson = PropertiesUtils.getResourceAsStream(MERKLE_TREE_JSON);

        when(handler.getInputStreamFromWorkspace(eq(ZIP_DATA_FOLDER + File.separator + DATA_FILE))).thenReturn(
            operationsJson
        );
        when(handler.getJsonFromWorkspace(eq(ZIP_DATA_FOLDER + File.separator + MERKLE_TREE_JSON))).thenReturn(
            JsonHandler.getFromInputStream(merkleTreeJson)
        );

        final ItemStatus response = verifyMerkleTreeActionHandler.execute(params, handler);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void testVerifyMerkleTreeWithCompareToLoggedHashKOThenKO() throws Exception {
        final File traceabilityFile = PropertiesUtils.getResourceFile(DETAIL_EVENT_TRACEABILITY_WRONG_ROOT);
        when(handler.getInput(eq(0), eq(File.class))).thenReturn(traceabilityFile);

        final InputStream operationsJson = PropertiesUtils.getResourceAsStream(DATA_FILE);
        final InputStream merkleTreeJson = PropertiesUtils.getResourceAsStream(MERKLE_TREE_JSON);

        when(handler.getInputStreamFromWorkspace(eq(ZIP_DATA_FOLDER + File.separator + DATA_FILE))).thenReturn(
            operationsJson
        );
        when(handler.getJsonFromWorkspace(eq(ZIP_DATA_FOLDER + File.separator + MERKLE_TREE_JSON))).thenReturn(
            JsonHandler.getFromInputStream(merkleTreeJson)
        );
        final ItemStatus response = verifyMerkleTreeActionHandler.execute(params, handler);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        ItemStatus itemStatus = response.getItemsStatus().get("CHECK_MERKLE_TREE");
        assertEquals(
            StatusCode.OK,
            itemStatus.getItemsStatus().get("COMPARE_MERKLE_HASH_WITH_SAVED_HASH").getGlobalStatus()
        );
        assertEquals(
            StatusCode.KO,
            itemStatus.getItemsStatus().get("COMPARE_MERKLE_HASH_WITH_INDEXED_HASH").getGlobalStatus()
        );
    }

    @Test
    public void testVerifyMerkleTreeWithCompareToSecuredHashKOThenKO() throws Exception {
        final InputStream operationsJson = PropertiesUtils.getResourceAsStream(DATA_FILE);
        final InputStream merkleTreeJson = PropertiesUtils.getResourceAsStream(MERKLE_TREE_JSON_WRONG_ROOT);

        when(handler.getInputStreamFromWorkspace(eq(ZIP_DATA_FOLDER + File.separator + DATA_FILE))).thenReturn(
            operationsJson
        );
        when(handler.getJsonFromWorkspace(eq(ZIP_DATA_FOLDER + File.separator + MERKLE_TREE_JSON))).thenReturn(
            JsonHandler.getFromInputStream(merkleTreeJson)
        );
        final ItemStatus response = verifyMerkleTreeActionHandler.execute(params, handler);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        ItemStatus itemStatus = response.getItemsStatus().get("CHECK_MERKLE_TREE");
        assertEquals(
            StatusCode.KO,
            itemStatus.getItemsStatus().get("COMPARE_MERKLE_HASH_WITH_SAVED_HASH").getGlobalStatus()
        );
        assertEquals(
            StatusCode.OK,
            itemStatus.getItemsStatus().get("COMPARE_MERKLE_HASH_WITH_INDEXED_HASH").getGlobalStatus()
        );
    }

    @Test
    public void testVerifyMerkleTreeWithIncorrectDatesThenKO() throws Exception {
        final InputStream operationsJson = PropertiesUtils.getResourceAsStream(OPERATIONS_WRONG_DATES_JSON);
        final InputStream merkleTreeJson = PropertiesUtils.getResourceAsStream(MERKLE_TREE_JSON);

        when(handler.getInputStreamFromWorkspace(eq(ZIP_DATA_FOLDER + File.separator + DATA_FILE))).thenReturn(
            operationsJson
        );
        when(handler.getJsonFromWorkspace(eq(ZIP_DATA_FOLDER + File.separator + MERKLE_TREE_JSON))).thenReturn(
            JsonHandler.getFromInputStream(merkleTreeJson)
        );
        final ItemStatus response = verifyMerkleTreeActionHandler.execute(params, handler);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    public void testVerifyMerkleTreeWithIncorrectTraceabilityEventThenFATAL() throws Exception {
        final File traceabilityFile = PropertiesUtils.getResourceFile(FAKE_DETAIL_EVENT_TRACEABILITY);
        when(handler.getInput(0)).thenReturn(traceabilityFile);
        final ItemStatus response = verifyMerkleTreeActionHandler.execute(params, handler);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void testVerifyMerkleTreeWithDataFileNotFoundThenFATAL() throws Exception {
        when(handler.getInputStreamFromWorkspace(eq(ZIP_DATA_FOLDER + File.separator + DATA_FILE))).thenThrow(
            new ContentAddressableStorageNotFoundException(DATA_FILE + " not found")
        );
        final ItemStatus response = verifyMerkleTreeActionHandler.execute(params, handler);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }
}
