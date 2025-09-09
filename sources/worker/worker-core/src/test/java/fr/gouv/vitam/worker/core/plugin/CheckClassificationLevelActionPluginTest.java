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
package fr.gouv.vitam.worker.core.plugin;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.configuration.ClassificationLevel;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import jakarta.ws.rs.core.Response;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckClassificationLevelActionPluginTest {

    CheckClassificationLevelActionPlugin plugin;
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;

    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    private static final String ARCHIVE_UNIT = "checkArchiveUnitSchemaActionPlugin/archive-unit_OK.json";

    private final InputStream archiveUnit;
    private List<IOParameter> out;

    private HandlerIOImpl action;
    private GUID guid = GUIDFactory.newGUID();

    private final WorkerParameters params = WorkerParametersFactory.newWorkerParameters(WorkFlowExecutionContext.VITAM)
        .setUrlWorkspace("http://localhost:8083")
        .setUrlMetadata("http://localhost:8083")
        .setObjectNameList(Lists.newArrayList("archiveUnit.json"))
        .setObjectName("archiveUnit.json")
        .setCurrentStep("currentStep")
        .setContainerName(guid.getId())
        .setLogbookTypeProcess(LogbookTypeProcess.INGEST);
    private ClassificationLevel classificationLevel;

    public CheckClassificationLevelActionPluginTest() throws FileNotFoundException {
        archiveUnit = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT);
    }

    @Before
    public void setUp() throws Exception {
        plugin = new CheckClassificationLevelActionPlugin();
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);

        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        action = new HandlerIOImpl(
            WorkFlowExecutionContext.VITAM,
            workspaceClientFactory,
            null,
            logbookLifeCyclesClientFactory,
            guid.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );

        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "unitId.json")));
        action.addOutIOParameters(out);
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    public void givenClassificationLevelNotAutorizedWhenExecuteThenReturnResponseKO() throws Exception {
        List<String> allowList = new ArrayList<>();
        allowList.add("Secret");
        classificationLevel = new ClassificationLevel();
        classificationLevel.setAllowList(allowList);
        classificationLevel.setAuthorizeNotDefined(true);
        VitamConfiguration.setClassificationLevel(classificationLevel);

        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Response.Status.OK).entity(archiveUnit).build()
        );

        final ItemStatus response = plugin.execute(params, action);
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getValidationErrors()).hasSize(1);
        assertThat(response.getValidationErrors().getFirst().getEvId()).isNotNull();
        assertThat(response.getValidationErrors().getFirst().getObId()).isNull();
        assertThat(response.getValidationErrors().getFirst().getEvTypeProc()).isEqualTo("COLLECT_SIP_INGEST");
        assertThat(response.getValidationErrors().getFirst().getOutDetail()).isEqualTo(
            "LFC.CHECK_CLASSIFICATION_LEVEL.KO"
        );
        assertThat(response.getValidationErrors().getFirst().getOutMessg()).isEqualTo(
            "Échec de la vérification du niveau de classification : non autorisé par la plateforme"
        );
        assertThat(response.getValidationErrors().getFirst().getEvDetData()).isNull();
    }

    @Test
    public void givenClassificationLevelAutorizedWhenExecuteThenReturnResponseOK() throws Exception {
        List<String> allowList = new ArrayList<>();
        allowList.add("Secret Défense");
        classificationLevel = new ClassificationLevel();
        classificationLevel.setAllowList(allowList);
        classificationLevel.setAuthorizeNotDefined(true);
        VitamConfiguration.setClassificationLevel(classificationLevel);
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Response.Status.OK).entity(archiveUnit).build()
        );

        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }
}
