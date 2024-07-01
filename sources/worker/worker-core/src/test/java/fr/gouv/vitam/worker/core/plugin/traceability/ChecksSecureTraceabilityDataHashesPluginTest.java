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

import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.charset.Charset;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static java.io.File.separator;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChecksSecureTraceabilityDataHashesPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    StorageClientFactory storageClientFactory;

    @Mock
    StorageClient storageClient;

    @Mock
    HandlerIO handler;

    @Mock
    WorkerParameters param;

    private ChecksSecureTraceabilityDataHashesPlugin checksSecureTraceabilityDataHashesPlugin;

    private static final String REPORT_FILENAME = "report";

    private static final String OBJECT_NAME = "OBJECT_NAME";
    private static final String FILE_NAME = "filename.zip";

    private static final String FILE_CONTENT = "toto";
    private static final String HASH =
        "10e06b990d44de0091a2113fd95c92fc905166af147aa7632639c41aa7f26b1620c47443813c605b924c05591c161ecc35944fc69c4433a49d10fc6b04a33611";

    @Before
    public void setUp() throws Exception {
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        checksSecureTraceabilityDataHashesPlugin = new ChecksSecureTraceabilityDataHashesPlugin(storageClientFactory);

        when(param.getObjectName()).thenReturn(OBJECT_NAME);
        when(
            handler.getJsonFromWorkspace(eq(param.getObjectName() + separator + WorkspaceConstants.REPORT))
        ).thenReturn(createObjectNode());

        final File traceabilityFile = temporaryFolder.newFile();
        final TraceabilityEvent traceabilityEvent = new TraceabilityEvent(
            TraceabilityType.OPERATION,
            "",
            "",
            HASH,
            null,
            "",
            "",
            "",
            1L,
            FILE_NAME,
            30L,
            DigestType.SHA512,
            false,
            "",
            null
        );
        JsonHandler.writeAsFile(traceabilityEvent, traceabilityFile);
        when(handler.getInput(eq(0))).thenReturn(traceabilityFile);

        final File dummy = temporaryFolder.newFile();
        when(handler.getNewLocalFile(anyString())).thenReturn(dummy);

        File reportTempFile = temporaryFolder.newFile(REPORT_FILENAME);
        when(handler.getNewLocalFile(endsWith(WorkspaceConstants.REPORT))).thenReturn(reportTempFile);
    }

    @Test
    public void should_verify_hashes_without_error() throws Exception {
        when(handler.getInput(1)).thenReturn(HASH);

        final File file = temporaryFolder.newFile();
        FileUtils.writeStringToFile(file, FILE_CONTENT, Charset.defaultCharset());

        Response response = mock(Response.class);
        when(response.readEntity(eq(File.class))).thenReturn(file);

        when(
            storageClient.getContainerAsync(
                anyString(),
                eq(FILE_NAME),
                eq(DataCategory.LOGBOOK),
                any(AccessLogInfoModel.class)
            )
        ).thenReturn(response);

        ItemStatus itemStatus = checksSecureTraceabilityDataHashesPlugin.execute(param, handler);

        assertEquals(OK, itemStatus.getGlobalStatus());
    }

    @Test
    public void test_when_hashes_are_not_equal_then_KO() throws Exception {
        when(handler.getInput(1)).thenReturn("FAKE_HASH");

        final File file = temporaryFolder.newFile();
        FileUtils.writeStringToFile(file, FILE_CONTENT, Charset.defaultCharset());

        Response response = mock(Response.class);
        when(response.readEntity(eq(File.class))).thenReturn(file);

        when(
            storageClient.getContainerAsync(
                anyString(),
                eq(FILE_NAME),
                eq(DataCategory.LOGBOOK),
                any(AccessLogInfoModel.class)
            )
        ).thenReturn(response);

        ItemStatus itemStatus = checksSecureTraceabilityDataHashesPlugin.execute(param, handler);

        assertEquals(KO, itemStatus.getGlobalStatus());
    }
}
