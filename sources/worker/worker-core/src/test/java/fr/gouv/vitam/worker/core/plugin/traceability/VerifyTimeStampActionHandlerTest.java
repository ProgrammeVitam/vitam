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
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.InputStream;
import java.security.Security;
import java.util.List;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.model.WorkspaceConstants.TRACEABILITY_OPERATION_DIRECTORY;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_COMPUTING_INFORMATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_MERKLE_TREE;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_TOKEN;
import static java.io.File.separator;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class VerifyTimeStampActionHandlerTest {

    private static final String OBJECT_ID = "objectId";
    VerifyTimeStampActionHandler verifyTimeStampActionHandler;

    private static final String DETAIL_EVENT_TRACEABILITY = "VerifyTimeStamp/EVENT_DETAIL_DATA.json";

    private static final String TOKEN = "VerifyTimeStamp/token.tsp";
    private static final String COMPUTING_FILE = "VerifyTimeStamp/computing_information.txt";
    private static final String MERKLE_FILE = "VerifyTimeStamp/merkleTree.json";

    private static final String TOKEN_FAKE = "VerifyTimeStamp/token_fake.tsp";

    private static final String REPORT_FILENAME = "report";

    private GUID guid;
    private static final Integer TENANT_ID = 0;
    private List<IOParameter> in;
    private File reportTempFile;

    private static final String HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP = "COMPARE_TOKEN_TIMESTAMP";
    private static final String HANDLER_SUB_ACTION_VALIDATE_TOKEN_TIMESTAMP = "VALIDATE_TOKEN_TIMESTAMP";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private HandlerIO handlerIO;

    @Mock
    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            BouncyCastleProvider provider = new BouncyCastleProvider();
            Security.addProvider(provider);
        }

        File tempFolder = temporaryFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        when(handlerIO.getJsonFromWorkspace(eq(OBJECT_ID + separator + WorkspaceConstants.REPORT))).thenReturn(
            createObjectNode()
        );

        guid = GUIDFactory.newGUID();

        when(params.getObjectName()).thenReturn(OBJECT_ID);

        reportTempFile = temporaryFolder.newFile(REPORT_FILENAME);
        when(handlerIO.getNewLocalFile(anyString())).thenReturn(reportTempFile);
    }

    @Test
    @RunWithCustomExecutor
    public void testVerifyTimeStampThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(handlerIO.getInput(0)).thenReturn(PropertiesUtils.getResourceFile(DETAIL_EVENT_TRACEABILITY));

        verifyTimeStampActionHandler = new VerifyTimeStampActionHandler();

        final InputStream tokenFile = PropertiesUtils.getResourceAsStream(TOKEN);
        final InputStream computingInformationFile = PropertiesUtils.getResourceAsStream(COMPUTING_FILE);
        final InputStream merkleFile = PropertiesUtils.getResourceAsStream(MERKLE_FILE);

        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(TRACEABILITY_OPERATION_DIRECTORY + "/" + OBJECT_ID + "/" + TRACEABILITY_TOKEN)
            )
        ).thenReturn(tokenFile);
        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(TRACEABILITY_OPERATION_DIRECTORY + "/" + OBJECT_ID + "/" + TRACEABILITY_COMPUTING_INFORMATION)
            )
        ).thenReturn(computingInformationFile);
        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(TRACEABILITY_OPERATION_DIRECTORY + "/" + OBJECT_ID + "/" + TRACEABILITY_MERKLE_TREE)
            )
        ).thenReturn(merkleFile);

        final ItemStatus response = verifyTimeStampActionHandler.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertEquals(
            StatusCode.OK,
            response
                .getItemsStatus()
                .get(verifyTimeStampActionHandler.getId())
                .getItemsStatus()
                .get(HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP)
                .getGlobalStatus()
        );
        assertEquals(
            StatusCode.OK,
            response
                .getItemsStatus()
                .get(verifyTimeStampActionHandler.getId())
                .getItemsStatus()
                .get(HANDLER_SUB_ACTION_VALIDATE_TOKEN_TIMESTAMP)
                .getGlobalStatus()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void testVerifyTimeStampWithErrorWorkspaceThenFATAL() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(handlerIO.getInput(0)).thenReturn(PropertiesUtils.getResourceFile(DETAIL_EVENT_TRACEABILITY));

        verifyTimeStampActionHandler = new VerifyTimeStampActionHandler();

        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(TRACEABILITY_OPERATION_DIRECTORY + File.separator + OBJECT_ID + File.separator + "token.tsp")
            )
        ).thenThrow(new ContentAddressableStorageNotFoundException("Token is not existing"));

        final ItemStatus response = verifyTimeStampActionHandler.execute(params, handlerIO);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void testVerifyTimeStampCorruptThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(handlerIO.getInput(0)).thenReturn(PropertiesUtils.getResourceFile(DETAIL_EVENT_TRACEABILITY));

        verifyTimeStampActionHandler = new VerifyTimeStampActionHandler();
        final InputStream tokenFile = PropertiesUtils.getResourceAsStream(TOKEN_FAKE);

        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(TRACEABILITY_OPERATION_DIRECTORY + File.separator + OBJECT_ID + File.separator + "token.tsp")
            )
        ).thenReturn(tokenFile);

        final ItemStatus response = verifyTimeStampActionHandler.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        assertEquals(
            StatusCode.KO,
            response
                .getItemsStatus()
                .get(verifyTimeStampActionHandler.getId())
                .getItemsStatus()
                .get(HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP)
                .getGlobalStatus()
        );
    }
}
