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
package fr.gouv.vitam.access.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;

public class SchemaIT extends VitamRuleRunner {

    private static final Integer TENANT_ID = 1;
    private static final String INTERNAL_ONTOLOGIES_PATH = "ontology/ontologies.json";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        SchemaIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            AccessInternalMain.class,
            IngestInternalMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            BatchReportMain.class,
            AccessExternalMain.class,
            IngestExternalMain.class
        )
    );

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        final String configSiegfriedPath = PropertiesUtils.getResourcePath(
            "integration-ingest-internal/format-identifiers.conf"
        ).toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(configSiegfriedPath);
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(
            Sets.newHashSet(
                MetadataCollections.UNIT.getName(),
                MetadataCollections.OBJECTGROUP.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
                FunctionalAdminCollections.SCHEMA.getName(),
                LogbookCollections.OPERATION.getName(),
                LogbookCollections.LIFECYCLE_UNIT.getName(),
                LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
                LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
                LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()
            )
        );

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
            ),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName()
            ),
            ElasticsearchIndexAlias.ofCrossTenantCollection(FunctionalAdminCollections.SCHEMA.getName())
        );
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    @Test
    @RunWithCustomExecutor
    public void shouldCreateRootLeafSchema()
        throws FileNotFoundException, AccessExternalClientException, InvalidParseOperationException, JsonProcessingException {
        try (final AdminExternalClient client = AdminExternalClientFactory.getInstance().getClient()) {
            final VitamContext context = new VitamContext(TENANT_ID)
                .setApplicationSessionId("ApplicationSessionId")
                .setAccessContract("contract");

            final boolean forceUpdate = false;
            final InputStream ontologiesInputStream = combineOntologies(
                INTERNAL_ONTOLOGIES_PATH,
                "ontology/external-ontologies-primitive-leaf.json"
            );
            final RequestResponse<?> ontologiesImportResponse = client.importOntologies(
                forceUpdate,
                context,
                ontologiesInputStream
            );

            assertThat(ontologiesImportResponse.getStatus()).isEqualTo(200);

            final InputStream schemaInputStream = PropertiesUtils.getResourceAsStream(
                "schema/external-unit-schema-primitive-leaf.json"
            );
            final RequestResponse<?> schemaImportResponse = client.importUnitExternalSchema(context, schemaInputStream);
            assertThat(schemaImportResponse.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldNotAllowSchemasWithObjectInOntologies()
        throws FileNotFoundException, JsonProcessingException, AccessExternalClientException, InvalidParseOperationException {
        try (final AdminExternalClient client = AdminExternalClientFactory.getInstance().getClient()) {
            final VitamContext context = new VitamContext(TENANT_ID)
                .setApplicationSessionId("ApplicationSessionId")
                .setAccessContract("contract");

            final boolean forceUpdate = false;
            final List<OntologyModel> ontologies = ontologies(INTERNAL_ONTOLOGIES_PATH);
            final InputStream ontologiesInputStream = ontologiesToInputStream(ontologies);
            final RequestResponse<?> ontologiesImportResponse = client.importOntologies(
                forceUpdate,
                context,
                ontologiesInputStream
            );

            assertThat(ontologiesImportResponse.getStatus()).isEqualTo(200);

            final InputStream schemaInputStream = PropertiesUtils.getResourceAsStream(
                "schema/external-unit-schema-with-object-collapsing-ontologies.json"
            );
            final RequestResponse<?> schemaImportResponse = client.importUnitExternalSchema(context, schemaInputStream);
            assertThat(schemaImportResponse.getStatus()).isEqualTo(400);

            /*
              FIXME: L'API vitam retourne des erreurs inexploitable, impossible de savoir quelle est la nature du problème.
              Ici on supposera que l'erreur 400 est bien causée par la déclaration d'un objet ayant la même clé qu'une ontologie.
             */
            final VitamError<?> vitamError = RequestResponse.parseVitamError(schemaImportResponse.toResponse());
            assertThat(vitamError).isNotNull();
            assertThat(vitamError.getDescription()).isEqualTo(
                "Error with the response, get status: '400' and reason 'Bad Request'."
            );
            assertThat(vitamError.getMessage()).isEqualTo(
                "Error with the response, get status: '400' and reason 'Bad Request'."
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSaveSchemaWhenObjectHasNoShortName()
        throws FileNotFoundException, JsonProcessingException, AccessExternalClientException, InvalidParseOperationException {
        try (final AdminExternalClient client = AdminExternalClientFactory.getInstance().getClient()) {
            final VitamContext context = new VitamContext(TENANT_ID)
                .setApplicationSessionId("ApplicationSessionId")
                .setAccessContract("contract");

            final boolean forceUpdate = false;
            final InputStream ontologiesInputStream = combineOntologies(
                INTERNAL_ONTOLOGIES_PATH,
                "ontology/external-ontologies-with-objects.json"
            );
            final RequestResponse<?> ontologiesImportResponse = client.importOntologies(
                forceUpdate,
                context,
                ontologiesInputStream
            );

            assertThat(ontologiesImportResponse.getStatus()).isEqualTo(200);

            final InputStream schemaInputStream = PropertiesUtils.getResourceAsStream(
                "schema/external-unit-schema-object-without-shortname.json"
            );
            final RequestResponse<?> schemaImportResponse = client.importUnitExternalSchema(context, schemaInputStream);
            assertThat(schemaImportResponse.getStatus()).isEqualTo(200);
        }
    }

    private InputStream combineOntologies(
        final String internalOntologieFilePath,
        final String externalOntologieFilePath
    ) throws FileNotFoundException, JsonProcessingException {
        final List<OntologyModel> internalOntologies = ontologies(internalOntologieFilePath);
        final List<OntologyModel> externalOntologies = ontologies(externalOntologieFilePath);
        final List<OntologyModel> ontologies = new ArrayList<>(internalOntologies);
        ontologies.addAll(externalOntologies);

        return ontologiesToInputStream(ontologies);
    }

    private List<OntologyModel> ontologies(final String path) throws JsonProcessingException, FileNotFoundException {
        final String json = PropertiesUtils.getResourceAsString(path);
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private InputStream ontologiesToInputStream(List<OntologyModel> ontologies) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String ontologiesJson = objectMapper.writeValueAsString(ontologies);
        return new ByteArrayInputStream(ontologiesJson.getBytes(StandardCharsets.UTF_8));
    }
}
