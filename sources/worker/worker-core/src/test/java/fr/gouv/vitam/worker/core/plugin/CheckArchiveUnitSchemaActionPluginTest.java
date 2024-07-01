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

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.ontology.OntologyTestHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.core.validation.OntologyValidator;
import fr.gouv.vitam.metadata.core.validation.UnitValidator;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.worker.core.validation.MetadataValidationProvider;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckArchiveUnitSchemaActionPluginTest {

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;

    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    private static final String ARCHIVE_UNIT = "checkArchiveUnitSchemaActionPlugin/archive-unit_OK.json";
    private static final String ARCHIVE_UNIT_NUMBER =
        "checkArchiveUnitSchemaActionPlugin/archive-unit_OK_Title_Number.json";
    private static final String ARCHIVE_UNIT_INVALID = "checkArchiveUnitSchemaActionPlugin/archive-unit_Invalid.json";
    private static final String ARCHIVE_UNIT_INVALID_CHAR =
        "checkArchiveUnitSchemaActionPlugin/archive-unit_special_char_KO.json";
    private static final String ARCHIVE_UNIT_INVALID_DATE =
        "checkArchiveUnitSchemaActionPlugin/archive-unit_Invalid_date.json";
    private static final String ARCHIVE_UNIT_INVALID_XML =
        "checkArchiveUnitSchemaActionPlugin/archive-unit_Invalid.xml";
    private static final String ARCHIVE_UNIT_INVALID_CONTENT =
        "checkArchiveUnitSchemaActionPlugin/archive-unit_KO_with_content.json";

    private static final String ARCHIVE_UNIT_SIGNATURE_CONTENT =
        "checkArchiveUnitSchemaActionPlugin/archive_unit_OK_with_signature.json";

    private static final String ARCHIVE_UNIT_FINAL_ACTION =
        "checkArchiveUnitSchemaActionPlugin/archive-unit_OK_FinalAction.json";

    private static final String ARCHIVE_UNIT_FINAL = "checkArchiveUnitSchemaActionPlugin/archive-unit_OK_final.json";
    private static final String ARCHIVE_UNIT_INVALID_DESC_LEVEL =
        "checkArchiveUnitSchemaActionPlugin/archive-unit_KO_DescriptionLevel.json";
    private static final String ARCHIVE_UNIT_STARTDATE_AFTER_ENDDATE =
        "checkArchiveUnitSchemaActionPlugin/archive-unit_KO_startDate.json";

    private final InputStream archiveUnit;
    private final InputStream archiveUnitNumber;
    private final InputStream archiveUnitFinal;
    private final InputStream archiveUnitWithSignature;
    private final InputStream archiveUnitFinalAction;

    private final InputStream archiveUnitInvalid;
    private final InputStream archiveUnitInvalidChar;
    private final InputStream archiveUnitInvalidDate;
    private final InputStream archiveUnitInvalidContent;
    private final InputStream archiveUnitInvalidXml;
    private final InputStream archiveUnitInvalidDescLevel;
    private final InputStream archiveUnitStartDateAfterEndDate;

    private List<IOParameter> out;
    private List<IOParameter> in;

    private CheckArchiveUnitSchemaActionPlugin plugin;

    private HandlerIOImpl action;
    private GUID guid = GUIDFactory.newGUID();
    private static final TypeReference<List<OntologyModel>> LIST_TYPE_REFERENCE = new TypeReference<
        List<OntologyModel>
    >() {};

    private final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
        .setUrlWorkspace("http://localhost:8083")
        .setUrlMetadata("http://localhost:8083")
        .setObjectNameList(Lists.newArrayList("archiveUnit.json"))
        .setObjectName("archiveUnit.json")
        .setCurrentStep("currentStep")
        .setContainerName(guid.getId())
        .setLogbookTypeProcess(LogbookTypeProcess.INGEST);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public CheckArchiveUnitSchemaActionPluginTest() throws FileNotFoundException {
        archiveUnit = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT);
        archiveUnitNumber = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_NUMBER);
        archiveUnitFinal = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_FINAL);
        archiveUnitInvalid = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_INVALID);
        archiveUnitInvalidChar = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_INVALID_CHAR);
        archiveUnitInvalidDate = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_INVALID_DATE);
        archiveUnitInvalidXml = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_INVALID_XML);
        archiveUnitInvalidContent = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_INVALID_CONTENT);
        archiveUnitInvalidDescLevel = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_INVALID_DESC_LEVEL);
        archiveUnitWithSignature = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_SIGNATURE_CONTENT);
        archiveUnitFinalAction = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_FINAL_ACTION);
        archiveUnitStartDateAfterEndDate = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_STARTDATE_AFTER_ENDDATE);
    }

    @Before
    public void setUp() throws Exception {
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        String objectId = "objectId";
        action = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            guid.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList(objectId)
        );
        action.setCurrentObjectId(objectId);

        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "unitId.json")));
        action.addOutIOParameters(out);

        in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Ontology/ontology.json")));

        when(workspaceClient.getObject(any(), eq("Ontology/ontology.json"))).thenReturn(
            Response.status(Status.OK).entity(OntologyTestHelper.loadOntologies()).build()
        );
        action.addInIOParameters(in);

        File tempFolder = temporaryFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", tempFolder.getAbsolutePath());
        final File ontologyFile = (File) action.getInput(0);
        if (ontologyFile == null) {
            throw new IllegalStateException("Ontology file not found");
        }
        List<OntologyModel> ontologies = JsonHandler.getFromFileAsTypeReference(ontologyFile, LIST_TYPE_REFERENCE);
        OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologies);
        UnitValidator unitValidator = new UnitValidator(null, null);
        MetadataValidationProvider metadataValidationProvider = new MetadataValidationProvider(
            ontologyValidator,
            null,
            unitValidator
        );

        plugin = new CheckArchiveUnitSchemaActionPlugin(metadataValidationProvider);
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL() {
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.FATAL);
    }

    @Test
    public void givenCorrectArchiveUnitJsonWhenExecuteThenReturnResponseOK() throws Exception {
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnit).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenFinalArchiveUnitJsonWhenExecuteThenReturnResponseOK() throws Exception {
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitFinal).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenFinalArchiveUnitFinalActionWhenExecuteThenReturnResponseOK() throws Exception {
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitFinalAction).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenArchiveUnitWithSignatureJsonWhenExecuteThenReturnResponseOK() throws Exception {
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitWithSignature).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenArchiveUnitWithNumberTitleJsonWhenExecuteThenReturnResponseOK() throws Exception {
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitNumber).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenInvalidArchiveUnitJsonWhenExecuteThenReturnResponseKO() throws Exception {
        // invalid archive unit -> missing title in it
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitInvalid).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        assertEquals(response.getItemId(), "CHECK_UNIT_SCHEMA");
        assertEquals(response.getItemsStatus().get("CHECK_UNIT_SCHEMA").getItemId(), "ONTOLOGY_VALIDATION");
    }

    @Test
    public void givenArchiveUnitWithSpecialCharactersJsonWhenExecuteThenReturnResponseKO() throws Exception {
        // invalid archive unit -> missing title in it
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitInvalidChar).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "INVALID_UNIT");
        assertEquals(response.getItemsStatus().get("CHECK_UNIT_SCHEMA").getItemId(), "UNIT_SANITIZE");
    }

    @Test
    public void givenInvalidArchiveUnitXMLWhenExecuteThenReturnResponseKO() throws Exception {
        // invalid archive unit -> XML File
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitInvalidXml).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.FATAL);
    }

    @Test
    public void givenInvalidDateArchiveUnitJsonWhenExecuteThenReturnResponseKO() throws Exception {
        // invalid archive unit -> year is > 9000
        // need to be fixed
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitInvalidDate).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), CheckArchiveUnitSchemaActionPlugin.INVALID_UNIT);
    }

    @Test
    public void givenInvalidContentArchiveUnitJsonWhenExecuteThenReturnResponseKO() throws Exception {
        // invalid content
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitInvalidContent).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), CheckArchiveUnitSchemaActionPlugin.INVALID_UNIT);
    }

    @Test
    public void givenInvalidDescLevelArchiveUnitJsonWhenExecuteThenReturnResponseKO() throws Exception {
        // invalid desc level
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitInvalidDescLevel).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), CheckArchiveUnitSchemaActionPlugin.INVALID_UNIT);
    }

    @Test
    public void givenStartDateAfterEndDateWhenExecuteThenReturnResponseKO() throws Exception {
        // invalid desc level
        when(workspaceClient.getObject(any(), eq("Units/archiveUnit.json"))).thenReturn(
            Response.status(Status.OK).entity(archiveUnitStartDateAfterEndDate).build()
        );
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), CheckArchiveUnitSchemaActionPlugin.CONSISTENCY);
    }
}
