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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.ExtractUriResponse;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckObjectsNumberActionHandlerTest {

    private CheckObjectsNumberActionHandler checkObjectsNumberActionHandler;
    private static final String HANDLER_ID = "CHECK_MANIFEST_OBJECTNUMBER";

    private static final String SUBTASK_MANIFEST_INFERIOR_BDO = "MANIFEST_INFERIOR_BDO";
    private static final String SUBTASK_MANIFEST_SUPERIOR_BDO = "MANIFEST_SUPERIOR_BDO";

    private WorkerParameters workParams;

    private SedaUtils sedaUtils;

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;

    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private SedaUtilsFactory sedaUtilsFactory;
    private final Set<URI> uriDuplicatedSetManifestKO = new HashSet<>();
    private final Set<URI> uriSetManifestOK = new HashSet<>();
    private final Set<URI> uriOutNumberSetManifestKO = new HashSet<>();

    private final List<URI> uriListWorkspaceOK = new ArrayList<>();
    private final List<URI> uriOutNumberListWorkspaceKO = new ArrayList<>();

    private ExtractUriResponse extractUriResponseOK;
    private ExtractUriResponse extractDuplicatedUriResponseKO;
    private ExtractUriResponse extractOutNumberUriResponseKO;

    private final List<String> messages = new ArrayList<>();
    HandlerIOImpl handlerIO;

    @Before
    public void setUp() throws Exception {
        workParams = WorkerParametersFactory.newWorkerParameters();
        workParams
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName("CheckObjectsNumberActionHandlerTest");

        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        sedaUtils = mock(SedaUtils.class);
        sedaUtilsFactory = mock(SedaUtilsFactory.class);
        when(sedaUtilsFactory.createSedaUtils(any())).thenReturn(sedaUtils);
        when(sedaUtilsFactory.createSedaUtilsWithSedaIngestParams(any())).thenReturn(sedaUtils);

        handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "CheckObjectsNumberActionHandlerTest",
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );

        // URI LIST MANIFEST
        uriDuplicatedSetManifestKO.add(new URI(URLEncoder.encode("content/file1.pdf", CharsetUtils.UTF_8)));
        uriDuplicatedSetManifestKO.add(new URI(URLEncoder.encode("content/file1.pdf", CharsetUtils.UTF_8)));

        uriSetManifestOK.add(new URI(URLEncoder.encode("content/file1.pdf", CharsetUtils.UTF_8)));
        uriSetManifestOK.add(new URI(URLEncoder.encode("content/file2.pdf", CharsetUtils.UTF_8)));

        uriOutNumberSetManifestKO.add(new URI(URLEncoder.encode("content/file1.pdf", CharsetUtils.UTF_8)));
        uriOutNumberSetManifestKO.add(new URI(URLEncoder.encode("content/file2.pdf", CharsetUtils.UTF_8)));
        uriOutNumberSetManifestKO.add(new URI(URLEncoder.encode("content/file3.pdf", CharsetUtils.UTF_8)));

        // URI LIST WORKSPACE

        uriListWorkspaceOK.add(new URI(URLEncoder.encode("content/file1.pdf", CharsetUtils.UTF_8)));
        uriListWorkspaceOK.add(new URI(URLEncoder.encode("content/file2.pdf", CharsetUtils.UTF_8)));
        // remove this add when the object count is fixed
        uriListWorkspaceOK.add(new URI(URLEncoder.encode("manifest.xml", CharsetUtils.UTF_8)));

        uriOutNumberListWorkspaceKO.add(new URI(URLEncoder.encode("content/file1.pdf", CharsetUtils.UTF_8)));
        uriOutNumberListWorkspaceKO.add(new URI(URLEncoder.encode("content/file2.pdf", CharsetUtils.UTF_8)));
        uriOutNumberListWorkspaceKO.add(new URI(URLEncoder.encode("content/file3.pdf", CharsetUtils.UTF_8)));
        // remove this add when the object count is fixed
        uriOutNumberListWorkspaceKO.add(new URI(URLEncoder.encode("manifest.xml", CharsetUtils.UTF_8)));

        extractUriResponseOK = new ExtractUriResponse();
        extractUriResponseOK.setUriSetManifest(uriSetManifestOK);

        messages.add("Duplicated digital objects " + "content/file1.pdf");
        extractDuplicatedUriResponseKO = new ExtractUriResponse();
        extractDuplicatedUriResponseKO
            .setUriSetManifest(uriDuplicatedSetManifestKO)
            .setErrorDuplicateUri(Boolean.TRUE)
            .setErrorNumber(messages.size());

        extractOutNumberUriResponseKO = new ExtractUriResponse();
        extractOutNumberUriResponseKO.setUriSetManifest(uriOutNumberSetManifestKO);
    }

    @After
    public void clean() {
        handlerIO.partialClose();
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenRaiseXMLStreamExceptionAndReturnResponseFATAL()
        throws ProcessingException, ContentAddressableStorageServerException {
        Mockito.doThrow(new ProcessingException("")).when(sedaUtils).getAllDigitalObjectUriFromManifest();

        checkObjectsNumberActionHandler = new CheckObjectsNumberActionHandler(sedaUtilsFactory);
        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);
        final ItemStatus response = checkObjectsNumberActionHandler.execute(workParams, handlerIO);
        assertThat(response).isNotNull();
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenRaiseProcessingExceptionReturnResponseFATAL()
        throws ProcessingException, ContentAddressableStorageServerException {
        Mockito.doThrow(new ProcessingException("")).when(sedaUtils).getAllDigitalObjectUriFromManifest();

        checkObjectsNumberActionHandler = new CheckObjectsNumberActionHandler(sedaUtilsFactory);
        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);
        final ItemStatus response = checkObjectsNumberActionHandler.execute(workParams, handlerIO);
        assertThat(response).isNotNull();
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(
            response.getItemsStatus().get(HANDLER_ID).getStatusMeter().get(StatusCode.FATAL.getStatusLevel())
        ).isEqualTo(1);
    }

    @Test
    public void givenWorkpaceExistWhenExecuteThenReturnResponseOK()
        throws ProcessingException, ContentAddressableStorageServerException {
        checkObjectsNumberActionHandler = new CheckObjectsNumberActionHandler(sedaUtilsFactory);

        when(sedaUtils.getAllDigitalObjectUriFromManifest()).thenReturn(extractUriResponseOK);
        when(workspaceClient.getListUriDigitalObjectFromFolder(any(), any())).thenReturn(
            new RequestResponseOK().addResult(uriListWorkspaceOK)
        );

        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);

        final ItemStatus response = checkObjectsNumberActionHandler.execute(workParams, handlerIO);
        assertThat(response).isNotNull();
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(
            response.getItemsStatus().get(HANDLER_ID).getStatusMeter().get(StatusCode.OK.getStatusLevel())
        ).isEqualTo(2);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseKOAndDuplicatedURIManifest()
        throws ProcessingException, ContentAddressableStorageServerException {
        checkObjectsNumberActionHandler = new CheckObjectsNumberActionHandler(sedaUtilsFactory);

        when(sedaUtils.getAllDigitalObjectUriFromManifest()).thenReturn(extractDuplicatedUriResponseKO);
        when(workspaceClient.getListUriDigitalObjectFromFolder(any(), any())).thenReturn(
            new RequestResponseOK().addResult(uriListWorkspaceOK)
        );

        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);

        final ItemStatus response = checkObjectsNumberActionHandler.execute(workParams, handlerIO);
        assertThat(response).isNotNull();
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            response.getItemsStatus().get(HANDLER_ID).getStatusMeter().get(StatusCode.KO.getStatusLevel())
        ).isEqualTo(1);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseKOAndOutNumberManifest()
        throws ProcessingException, ContentAddressableStorageServerException, InvalidParseOperationException {
        checkObjectsNumberActionHandler = new CheckObjectsNumberActionHandler(sedaUtilsFactory);

        when(sedaUtils.getAllDigitalObjectUriFromManifest()).thenReturn(extractOutNumberUriResponseKO);
        when(workspaceClient.getListUriDigitalObjectFromFolder(any(), any())).thenReturn(
            new RequestResponseOK().addResult(uriListWorkspaceOK)
        );

        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);

        final ItemStatus response = checkObjectsNumberActionHandler.execute(workParams, handlerIO);
        assertThat(response).isNotNull();
        assertThat(response.getItemsStatus().get(HANDLER_ID).getData("errorNumber")).isEqualTo(1);
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            response.getItemsStatus().get(HANDLER_ID).getStatusMeter().get(StatusCode.KO.getStatusLevel())
        ).isEqualTo(1);
        assertThat(
            response.getItemsStatus().get(HANDLER_ID).getStatusMeter().get(StatusCode.OK.getStatusLevel())
        ).isEqualTo(2);
        JsonNode evDetData = JsonHandler.getFromString((String) response.getData("eventDetailData"));
        assertNotNull(evDetData);
        assertNotNull(evDetData.get("evDetTechData"));
        assertNotNull(evDetData.get("evDetTechData").asText().contains("manifestError"));
        assertEquals(
            SUBTASK_MANIFEST_SUPERIOR_BDO,
            response.getItemsStatus().get(HANDLER_ID).getGlobalOutcomeDetailSubcode().toString()
        );
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseKOAndOutNumberWorkspace()
        throws ProcessingException, ContentAddressableStorageServerException {
        checkObjectsNumberActionHandler = new CheckObjectsNumberActionHandler(sedaUtilsFactory);

        when(sedaUtils.getAllDigitalObjectUriFromManifest()).thenReturn(extractUriResponseOK);
        when(workspaceClient.getListUriDigitalObjectFromFolder(any(), any())).thenReturn(
            new RequestResponseOK().addResult(uriOutNumberListWorkspaceKO)
        );

        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);

        final ItemStatus response = checkObjectsNumberActionHandler.execute(workParams, handlerIO);
        assertThat(response).isNotNull();
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            response.getItemsStatus().get(HANDLER_ID).getStatusMeter().get(StatusCode.KO.getStatusLevel())
        ).isEqualTo(1);
        assertThat(
            response.getItemsStatus().get(HANDLER_ID).getStatusMeter().get(StatusCode.OK.getStatusLevel())
        ).isEqualTo(2);
        assertEquals(
            SUBTASK_MANIFEST_INFERIOR_BDO,
            response.getItemsStatus().get(HANDLER_ID).getGlobalOutcomeDetailSubcode().toString()
        );
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseKOAndNotFoundFile()
        throws ProcessingException, ContentAddressableStorageServerException {
        checkObjectsNumberActionHandler = new CheckObjectsNumberActionHandler(sedaUtilsFactory);

        when(sedaUtils.getAllDigitalObjectUriFromManifest()).thenReturn(extractUriResponseOK);
        when(workspaceClient.getListUriDigitalObjectFromFolder(any(), any())).thenReturn(
            new RequestResponseOK().addResult(uriOutNumberListWorkspaceKO)
        );

        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);

        final ItemStatus response = checkObjectsNumberActionHandler.execute(workParams, handlerIO);
        assertThat(response).isNotNull();
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            response.getItemsStatus().get(HANDLER_ID).getStatusMeter().get(StatusCode.KO.getStatusLevel())
        ).isEqualTo(1);
        assertThat(
            response.getItemsStatus().get(HANDLER_ID).getStatusMeter().get(StatusCode.OK.getStatusLevel())
        ).isEqualTo(2);
    }
}
