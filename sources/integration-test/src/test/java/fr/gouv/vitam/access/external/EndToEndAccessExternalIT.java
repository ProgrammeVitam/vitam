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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.antivirus.rest.AntivirusMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.EnumObjectWhiteListedFields;
import fr.gouv.vitam.common.EnumUnitWhiteListedFields;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.identifier.PurgedPersistentIdentifier;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.model.PersistentIdentifierReconstructionRequest;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextException;
import fr.gouv.vitam.processing.engine.core.operation.OperationContextMonitor;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType.PRODUCTION;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DELETE_GOT_VERSIONS;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.ELIMINATION_ACTION;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.TRANSFER_REPLY;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class EndToEndAccessExternalIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EndToEndAccessExternalIT.class);

    private static final List<String> ALLOWED_TAG_OBJECT = List.of(
        "DataObjectSystemId",
        "DataObjectGroupSystemId",
        "DataObjectVersion"
    );
    private static final Integer tenantId = 0;
    private static final String XML = ".xml";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        EndToEndAccessExternalIT.class,
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
            IngestExternalMain.class,
            AntivirusMain.class
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
    public static void tearDownAfterClass() throws Exception {
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
            )
        );
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    @Test
    @RunWithCustomExecutor
    public void shouldDownloadObject() throws Exception {
        final VitamContext context = new VitamContext(tenantId)
            .setApplicationSessionId("ApplicationSessionId")
            .setAccessContract("aName3");
        ingest(context, "sip/TEST_INGEST_ARK_IDS_AND_AUTOGENERATE_ARK_IDS.zip");

        try (final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            final String persistentIdentifier = "ark:/23567/001a9d7db5eadabac_binary_master";

            try (
                final Response response = client.downloadObjectByObjectPersistentIdentifier(
                    context,
                    persistentIdentifier
                )
            ) {
                assertEquals(200, response.getStatus());
                assertThat(response.getHeaderString("Content-Length")).isEqualTo("6");
                assertNotNull(response.getEntity());
            }
        } catch (VitamClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void verifyWhenAtrOKWithoutConf() throws Exception {
        //given
        final VitamContext context = new VitamContext(tenantId)
            .setApplicationSessionId("ApplicationSessionId")
            .setAccessContract("aName3");

        Map<Integer, List<EnumUnitWhiteListedFields>> confEnum = new HashMap<>();
        List<EnumUnitWhiteListedFields> fieldsEnum = new ArrayList<>();
        fieldsEnum.add(EnumUnitWhiteListedFields.PersistentIdentifier);

        confEnum.put(0, fieldsEnum);

        VitamConfiguration.setIngestReportUnitExtraFields(confEnum);
        //WHEN
        String operationId = ingest(context, "sip/TEST_INGEST_ARK_IDS_AND_AUTOGENERATE_ARK_IDS.zip");

        //THEN
        List<String> allowedTags = Arrays.asList("SystemId", "PersistentIdentifier");

        List<String> expectedFields = new ArrayList<>();
        expectedFields.add("<PersistentIdentifierType>ark</PersistentIdentifierType>");
        expectedFields.add("<PersistentIdentifierContent>ark:/2778447/1234567xyz</PersistentIdentifierContent>");
        Map<String, List<String>> unitMap = new HashMap<>();
        unitMap.put("ID18", expectedFields);

        expectedFields = new ArrayList<>();
        expectedFields.add("<PersistentIdentifierType>ark</PersistentIdentifierType>");
        expectedFields.add("<PersistentIdentifierContent>ark:/666567/001a957db5eadaac</PersistentIdentifierContent>");
        unitMap.put("ID14", expectedFields);

        String atrContent = getAtrContent(context, operationId);
        assertTrue(validateUnit(atrContent, unitMap, allowedTags));

        expectedFields = new ArrayList<>();
        expectedFields.add("<PersistentIdentifierType>ark</PersistentIdentifierType>");
        expectedFields.add("<PersistentIdentifierOrigin>OriginatingAgency</PersistentIdentifierOrigin>");
        expectedFields.add("<PersistentIdentifierReference>Agency-00021</PersistentIdentifierReference>");

        expectedFields.add(
            "<PersistentIdentifierContent>ark:/23567/001a9d7db5eadabac_binary_master</PersistentIdentifierContent>"
        );

        Map<String, List<String>> objectMap = new HashMap<>();
        objectMap.put("ID21", expectedFields);

        List<String> allowedTagsObject = new ArrayList<>(ALLOWED_TAG_OBJECT);
        allowedTagsObject.add("PersistentIdentifier");
        assertTrue(validateBinaryObject(atrContent, objectMap, allowedTagsObject));
    }

    @Test
    @RunWithCustomExecutor
    public void verifyAtrWithThumbnailAndPhysicalObjectWithoutConf() throws Exception {
        // Given
        final VitamContext context = new VitamContext(tenantId)
            .setApplicationSessionId("ApplicationSessionId")
            .setAccessContract("aName3");

        Map<Integer, List<EnumUnitWhiteListedFields>> confEnum = Map.of(
            0,
            Arrays.asList(EnumUnitWhiteListedFields.PersistentIdentifier)
        );
        VitamConfiguration.setIngestReportUnitExtraFields(confEnum);

        // When
        String operationId = ingest(context, "sip/OK_SIP_multiusagesversions_withIdArk_2.3.zip");
        String atrContent = getAtrContent(context, operationId);

        // Then
        // Validation for Unit
        List<String> allowedTags = Arrays.asList("SystemId", "PersistentIdentifier");
        Map<String, List<String>> unitMap = Map.of(
            "ID18",
            Arrays.asList(
                "<PersistentIdentifierType>ark</PersistentIdentifierType>",
                "<PersistentIdentifierContent>ark:/22567/001a957db5blablablasuppr2AU1</PersistentIdentifierContent>"
            )
        );
        assertTrue(validateUnit(atrContent, unitMap, allowedTags));

        // Validation for Physical Object
        List<String> allowedTagsObject = new ArrayList<>(ALLOWED_TAG_OBJECT);
        allowedTagsObject.add("PersistentIdentifier");
        Map<String, List<String>> physicalObjectMap = Map.of(
            "IDPHY2",
            Arrays.asList(
                "<PersistentIdentifierType>ark</PersistentIdentifierType>",
                "<PersistentIdentifierOrigin>OriginatingAgency</PersistentIdentifierOrigin>",
                "<PersistentIdentifierReference>Identifier0</PersistentIdentifierReference>",
                "<PersistentIdentifierContent>ark:/22567/001a957db5blablabla</PersistentIdentifierContent>"
            )
        );
        assertTrue(validatePhysicalObject(atrContent, physicalObjectMap, allowedTagsObject));

        // Validation for Binary Object
        Map<String, List<String>> binaryObjectMap = Map.of(
            "ID00011",
            Arrays.asList(
                "<PersistentIdentifierType>ark</PersistentIdentifierType>",
                "<PersistentIdentifierOrigin>OriginatingAgency</PersistentIdentifierOrigin>",
                "<PersistentIdentifierReference>Identifier0</PersistentIdentifierReference>",
                "<PersistentIdentifierContent>ark:/22567/001a957db5blablablasuppr15</PersistentIdentifierContent>"
            )
        );
        assertTrue(validateBinaryObject(atrContent, binaryObjectMap, allowedTagsObject));
    }

    @Test
    @RunWithCustomExecutor
    public void verifyWhenAtrOKWithConf() throws Exception {
        //given
        final VitamContext context = new VitamContext(0)
            .setApplicationSessionId("ApplicationSessionId")
            .setAccessContract("aName3");

        Map<Integer, List<EnumUnitWhiteListedFields>> confEnum = new HashMap<>();
        List<EnumUnitWhiteListedFields> fieldsEnum = new ArrayList<>();
        fieldsEnum.add(EnumUnitWhiteListedFields.FilePlanPosition);
        fieldsEnum.add(EnumUnitWhiteListedFields.OriginatingSystemId);

        confEnum.put(0, fieldsEnum);

        VitamConfiguration.setIngestReportUnitExtraFields(confEnum);

        Map<Integer, List<EnumObjectWhiteListedFields>> confObjectEnum = new HashMap<>();
        List<EnumObjectWhiteListedFields> fieldsObjectEnum = new ArrayList<>();
        fieldsObjectEnum.add(EnumObjectWhiteListedFields.PersistentIdentifier);

        confObjectEnum.put(0, fieldsObjectEnum);

        VitamConfiguration.setIngestReportObjectExtraFields(confObjectEnum);

        //WHEN
        String operationId = ingest(context, "sip/TEST_INGEST_CAS_OK.zip");

        //THEN
        String atrContent = getAtrContent(context, operationId);
        List<String> allowedTags = Arrays.asList("SystemId", "FilePlanPosition", "OriginatingSystemId");

        List<String> expectedFields = new ArrayList<>();
        expectedFields.add("<FilePlanPosition>13.1.</FilePlanPosition>");
        expectedFields.add("<FilePlanPosition>RATP.13.1.</FilePlanPosition>");
        expectedFields.add("<OriginatingSystemId>123456</OriginatingSystemId>");
        expectedFields.add("<OriginatingSystemId>AZERTY</OriginatingSystemId>");
        Map<String, List<String>> unitMap = new HashMap<>();
        unitMap.put("ID4", expectedFields);

        expectedFields = new ArrayList<>();
        expectedFields.add("<FilePlanPosition>testilePlanPosition</FilePlanPosition>");
        unitMap.put("ID6", expectedFields);
        assertTrue(validateUnit(atrContent, unitMap, allowedTags));

        Map<String, List<String>> objectMap = new HashMap<>();
        objectMap.put("ID13", List.of("<DataObjectVersion>BinaryMaster_1</DataObjectVersion>"));
        assertTrue(validateBinaryObject(atrContent, objectMap, ALLOWED_TAG_OBJECT));
    }

    @Test
    @RunWithCustomExecutor
    public void verifyWhenAtrKOWithConf() throws Exception {
        //given
        final VitamContext context = new VitamContext(0)
            .setApplicationSessionId("ApplicationSessionId")
            .setAccessContract("aName3");

        Map<Integer, List<EnumUnitWhiteListedFields>> confEnum = new HashMap<>();
        List<EnumUnitWhiteListedFields> fieldsEnum = new ArrayList<>();
        fieldsEnum.add(EnumUnitWhiteListedFields.FilePlanPosition);
        fieldsEnum.add(EnumUnitWhiteListedFields.OriginatingSystemId);
        fieldsEnum.add(EnumUnitWhiteListedFields.TransferringAgencyArchiveUnitIdentifier);
        fieldsEnum.add(EnumUnitWhiteListedFields.OriginatingAgencyArchiveUnitIdentifier);
        fieldsEnum.add(EnumUnitWhiteListedFields.ArchivalAgencyArchiveUnitIdentifier);

        confEnum.put(0, fieldsEnum);
        confEnum.put(1, new ArrayList<>());

        VitamConfiguration.setIngestReportUnitExtraFields(confEnum);

        Map<Integer, List<EnumObjectWhiteListedFields>> confObjectEnum = new HashMap<>();
        List<EnumObjectWhiteListedFields> fieldsObjectEnum = new ArrayList<>();
        fieldsObjectEnum.add(EnumObjectWhiteListedFields.PersistentIdentifier);

        confObjectEnum.put(0, fieldsObjectEnum);
        confObjectEnum.put(1, fieldsObjectEnum);

        VitamConfiguration.setIngestReportObjectExtraFields(confObjectEnum);

        //WHEN
        String operationId = ingest(context, "sip/TEST_INGEST_CAS_KO.zip");

        //THEN
        String atrContent = getAtrContent(context, operationId);
        List<String> allowedTags = Arrays.asList(
            "SystemId",
            "FilePlanPosition",
            "OriginatingSystemId",
            "TransferringAgencyArchiveUnitIdentifier",
            "OriginatingAgencyArchiveUnitIdentifier",
            "ArchivalAgencyArchiveUnitIdentifier"
        );

        List<String> expectedFields = new ArrayList<>();
        expectedFields.add("<FilePlanPosition>13.1.</FilePlanPosition>");
        expectedFields.add("<FilePlanPosition>RATP.13.1.</FilePlanPosition>");
        expectedFields.add("<OriginatingSystemId>123456</OriginatingSystemId>");
        expectedFields.add("<OriginatingSystemId>AZERTY</OriginatingSystemId>");
        expectedFields.add("<OriginatingAgencyArchiveUnitIdentifier>7890</OriginatingAgencyArchiveUnitIdentifier>");
        expectedFields.add("<OriginatingAgencyArchiveUnitIdentifier>QWERTY</OriginatingAgencyArchiveUnitIdentifier>");
        expectedFields.add("<TransferringAgencyArchiveUnitIdentifier>Toto1</TransferringAgencyArchiveUnitIdentifier>");
        expectedFields.add("<TransferringAgencyArchiveUnitIdentifier>1Otot</TransferringAgencyArchiveUnitIdentifier>");
        expectedFields.add("<ArchivalAgencyArchiveUnitIdentifier>20170045/1</ArchivalAgencyArchiveUnitIdentifier>");
        expectedFields.add("<ArchivalAgencyArchiveUnitIdentifier>AMN.X/12</ArchivalAgencyArchiveUnitIdentifier>");
        Map<String, List<String>> unitMap = new HashMap<>();
        unitMap.put("ID4", expectedFields);

        expectedFields = new ArrayList<>();
        expectedFields.add("<FilePlanPosition>testilePlanPosition</FilePlanPosition>");
        unitMap.put("ID6", expectedFields);
        assertTrue(validateUnit(atrContent, unitMap, allowedTags));

        Map<String, List<String>> objectMap = new HashMap<>();
        objectMap.put("ID13", List.of("<DataObjectVersion>BinaryMaster_1</DataObjectVersion>"));
        assertTrue(validateBinaryObject(atrContent, objectMap, ALLOWED_TAG_OBJECT));
    }

    @Test
    @RunWithCustomExecutor
    public void verifyWhenAtrWarningWithConf() throws Exception {
        //given
        final VitamContext context = new VitamContext(0)
            .setApplicationSessionId("ApplicationSessionId")
            .setAccessContract("aName3");

        Map<Integer, List<EnumUnitWhiteListedFields>> confEnum = new HashMap<>();
        List<EnumUnitWhiteListedFields> fieldsEnum = new ArrayList<>();
        fieldsEnum.add(EnumUnitWhiteListedFields.FilePlanPosition);
        fieldsEnum.add(EnumUnitWhiteListedFields.OriginatingSystemId);
        fieldsEnum.add(EnumUnitWhiteListedFields.TransferringAgencyArchiveUnitIdentifier);
        fieldsEnum.add(EnumUnitWhiteListedFields.OriginatingAgencyArchiveUnitIdentifier);
        fieldsEnum.add(EnumUnitWhiteListedFields.ArchivalAgencyArchiveUnitIdentifier);

        confEnum.put(0, fieldsEnum);
        confEnum.put(1, new ArrayList<>());

        VitamConfiguration.setIngestReportUnitExtraFields(confEnum);

        Map<Integer, List<EnumObjectWhiteListedFields>> confObjectEnum = new HashMap<>();
        List<EnumObjectWhiteListedFields> fieldsObjectEnum = new ArrayList<>();
        fieldsObjectEnum.add(EnumObjectWhiteListedFields.PersistentIdentifier);

        confObjectEnum.put(0, fieldsObjectEnum);
        confObjectEnum.put(1, fieldsObjectEnum);

        VitamConfiguration.setIngestReportObjectExtraFields(confObjectEnum);

        //WHEN
        String operationId = ingest(context, "sip/TEST_INGEST_CAS_WARNING.zip");

        //THEN
        String atrContent = getAtrContent(context, operationId);
        List<String> allowedTags = Arrays.asList(
            "SystemId",
            "FilePlanPosition",
            "OriginatingSystemId",
            "TransferringAgencyArchiveUnitIdentifier",
            "OriginatingAgencyArchiveUnitIdentifier",
            "ArchivalAgencyArchiveUnitIdentifier"
        );

        List<String> expectedFields = new ArrayList<>();
        expectedFields.add("<FilePlanPosition>13.1.</FilePlanPosition>");
        expectedFields.add("<FilePlanPosition>RATP.13.1.</FilePlanPosition>");
        expectedFields.add("<OriginatingSystemId>123456</OriginatingSystemId>");
        expectedFields.add("<OriginatingSystemId>AZERTY</OriginatingSystemId>");
        expectedFields.add("<OriginatingAgencyArchiveUnitIdentifier>7890</OriginatingAgencyArchiveUnitIdentifier>");
        expectedFields.add("<OriginatingAgencyArchiveUnitIdentifier>QWERTY</OriginatingAgencyArchiveUnitIdentifier>");
        expectedFields.add("<TransferringAgencyArchiveUnitIdentifier>Toto1</TransferringAgencyArchiveUnitIdentifier>");
        expectedFields.add("<TransferringAgencyArchiveUnitIdentifier>1Otot</TransferringAgencyArchiveUnitIdentifier>");
        expectedFields.add("<ArchivalAgencyArchiveUnitIdentifier>20170045/1</ArchivalAgencyArchiveUnitIdentifier>");
        expectedFields.add("<ArchivalAgencyArchiveUnitIdentifier>AMN.X/12</ArchivalAgencyArchiveUnitIdentifier>");
        Map<String, List<String>> unitMap = new HashMap<>();
        unitMap.put("ID4", expectedFields);

        expectedFields = new ArrayList<>();
        expectedFields.add("<FilePlanPosition>testilePlanPosition</FilePlanPosition>");
        unitMap.put("ID6", expectedFields);
        assertTrue(validateUnit(atrContent, unitMap, allowedTags));

        Map<String, List<String>> objectMap = new HashMap<>();
        objectMap.put("ID35", List.of("<DataObjectVersion>BinaryMaster_1</DataObjectVersion>"));
        assertTrue(validateBinaryObject(atrContent, objectMap, ALLOWED_TAG_OBJECT));

        Map<String, List<String>> objectGroupMap = new HashMap<>();
        objectGroupMap.put(
            "ID34",
            List.of("<BinaryDataObject id=\"ID35\">", "<EventTypeCode>LFC.CHECK_OBJECT_SIZE.CHECK_SIZE</EventTypeCode>")
        );
        assertTrue(
            validateLogbook(atrContent, objectGroupMap, List.of("PhysicalDataObject", "BinaryDataObject", "LogBook"))
        );
    }

    private boolean validateAllowedTags(String content, List<String> allowedTags) {
        // Regex to capture only the top-level tags (first-level tags only)
        String parentTagRegex = "<(\\w+)(?:\\s+[^>]*)?>.*?</\\1>";
        Pattern tagPattern = Pattern.compile(parentTagRegex, Pattern.DOTALL);
        Matcher tagMatcher = tagPattern.matcher(content);

        // Iterate through each top-level tag and check if it's in the allowed tags list
        while (tagMatcher.find()) {
            String tagName = tagMatcher.group(1);
            if (!allowedTags.contains(tagName)) {
                System.err.println("The tag <" + tagName + "> is not allowed.");
                return false; // Return false if a disallowed top-level tag is found
            }
        }

        return true; // All top-level tags are allowed
    }

    public String getAtrContent(VitamContext context, String operationId) {
        try (final IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
            try (
                final Response response = client.downloadObjectAsync(
                    context,
                    operationId,
                    IngestCollection.ARCHIVETRANSFERREPLY
                )
            ) {
                try (InputStream atr = response.readEntity(InputStream.class)) {
                    return IOUtils.toString(atr, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Error while reading ATR content", e);
                }
            } catch (VitamClientException e) {
                throw new RuntimeException("Error while downloading ATR", e);
            }
        }
    }

    public boolean validateUnit(String atrContent, Map<String, List<String>> expectedValues, List<String> allowedTags) {
        // Regex to capture each ArchiveUnit with its ID and content block
        String archiveUnitRegex = "<ArchiveUnit id=\"(.*?)\">.*?<Content>(.*?)</Content>.*?</ArchiveUnit>";
        Pattern archiveUnitPattern = Pattern.compile(archiveUnitRegex, Pattern.DOTALL);
        Matcher archiveUnitMatcher = archiveUnitPattern.matcher(atrContent);

        int archiveUnitCount = 0; // Counter for ArchiveUnits found
        boolean foundExpectedUnit = false; // Flag indicating if an expected ArchiveUnit was found

        // Iterate through each ArchiveUnit and check for expected tags and values
        while (archiveUnitMatcher.find()) {
            archiveUnitCount++; // Increment the counter for each ArchiveUnit found
            String unitId = archiveUnitMatcher.group(1);
            String contentBlock = archiveUnitMatcher.group(2);

            // Validate allowed tags in the content block
            if (!validateAllowedTags(contentBlock, allowedTags)) {
                return false; // Return false if a disallowed tag is found
            }

            // Validate expected fields only for specified ArchiveUnits
            if (expectedValues.containsKey(unitId)) {
                foundExpectedUnit = true; // Found an expected ArchiveUnit
                if (!validateExpectedFields(contentBlock, expectedValues.get(unitId))) {
                    return false; // Return false if expected fields do not match
                }
            }
        }

        // Return false if no ArchiveUnits were found or if no expected ArchiveUnit was found
        return archiveUnitCount > 0 && foundExpectedUnit;
    }

    private boolean validateExpectedFields(String contentBlock, List<String> expectedFields) {
        for (String value : expectedFields) {
            if (!contentBlock.contains(value)) {
                return false; // Return false if a field is missing
            }
        }
        return true; // Return true if all expected fields are present
    }

    public boolean validateLogbook(
        String atrContent,
        Map<String, List<String>> expectedValues,
        List<String> allowedTags
    ) {
        // Regex to capture each DataObjectGroup with its ID and content
        String dataObjectGroupRegex = "<DataObjectGroup id=\"(.*?)\">(.*?)</DataObjectGroup>";
        Pattern dataObjectGroupPattern = Pattern.compile(dataObjectGroupRegex, Pattern.DOTALL);
        Matcher dataObjectGroupMatcher = dataObjectGroupPattern.matcher(atrContent);

        int dataObjectGroupCount = 0; // Counter for DataObjectGroups found
        boolean foundExpectedObject = false; // Flag indicating if an expected DataObjectGroup was found

        // Iterate through each DataObjectGroup and check for expected tags and values
        while (dataObjectGroupMatcher.find()) {
            dataObjectGroupCount++; // Increment the counter for each DataObjectGroup found
            String objectId = dataObjectGroupMatcher.group(1);
            String dataObjectGroupBlock = dataObjectGroupMatcher.group(2);

            // Validate allowed top-level tags in the DataObjectGroup block
            if (!validateAllowedTags(dataObjectGroupBlock, allowedTags)) {
                return false; // Return false if a disallowed tag is found
            }

            // Validate expected fields only for specified DataObjectGroups
            if (expectedValues.containsKey(objectId)) {
                foundExpectedObject = true; // Found an expected DataObjectGroup
                if (!validateExpectedFields(dataObjectGroupBlock, expectedValues.get(objectId))) {
                    return false; // Return false if expected fields do not match
                }
            }
        }

        // Return false if no DataObjectGroups were found or if no expected DataObjectGroup was found
        return dataObjectGroupCount > 0 && foundExpectedObject;
    }

    public boolean validateObject(
        String atrContent,
        Map<String, List<String>> expectedValues,
        List<String> allowedTags,
        String objectRegex
    ) {
        // Compile regex to capture each object with its ID and content
        Pattern objectPattern = Pattern.compile(objectRegex, Pattern.DOTALL);
        Matcher objectMatcher = objectPattern.matcher(atrContent);

        int objectCount = 0; // Counter for objects found
        boolean foundExpectedObject = false; // Flag indicating if an expected object was found

        // Iterate through each object and check for expected tags and values
        while (objectMatcher.find()) {
            objectCount++; // Increment the counter for each object found
            String objectId = objectMatcher.group(1);
            String objectBlock = objectMatcher.group(2);

            // Validate allowed tags in the object block
            if (!validateAllowedTags(objectBlock, allowedTags)) {
                return false; // Return false if a disallowed tag is found
            }

            // Validate expected fields only for specified objects
            if (expectedValues.containsKey(objectId)) {
                foundExpectedObject = true; // Found an expected object
                if (!validateExpectedFields(objectBlock, expectedValues.get(objectId))) {
                    return false; // Return false if expected fields do not match
                }
            }
        }

        // Return false if no objects were found or if no expected object was found
        return objectCount > 0 && foundExpectedObject;
    }

    public boolean validateBinaryObject(
        String atrContent,
        Map<String, List<String>> expectedValues,
        List<String> allowedTags
    ) {
        // Regex for BinaryDataObject
        String binaryObjectRegex = "<BinaryDataObject id=\"(.*?)\">(.*?)</BinaryDataObject>";
        return validateObject(atrContent, expectedValues, allowedTags, binaryObjectRegex);
    }

    public boolean validatePhysicalObject(
        String atrContent,
        Map<String, List<String>> expectedValues,
        List<String> allowedTags
    ) {
        // Regex for PhysicalDataObject
        String physicalObjectRegex = "<PhysicalDataObject id=\"(.*?)\">(.*?)</PhysicalDataObject>";
        return validateObject(atrContent, expectedValues, allowedTags, physicalObjectRegex);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldObjectBeNotFoundWhenTryingToDownloadAnArchiveUnitPersistentIdentifier() throws Exception {
        final String persistentIdentifier = "ark:/666567/001a957db5eadaac";
        final VitamContext context = new VitamContext(tenantId)
            .setApplicationSessionId("ApplicationSessionId")
            .setAccessContract("aName3");
        ingest(context, "sip/TEST_INGEST_ARK_IDS_AND_AUTOGENERATE_ARK_IDS.zip");

        try (final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            final VitamClientException vitamClientException = assertThrows(
                VitamClientException.class,
                () -> client.downloadObjectByObjectPersistentIdentifier(context, persistentIdentifier)
            );
            assertThat(vitamClientException.getMessage()).isEqualTo("Persistent identifier not found exception");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldObjectBeNotFoundWhenItsPersistentIdentifierNotExists() {
        final String persistentIdentifier = "ark:/00000/not_existing_ark";
        final VitamContext context = new VitamContext(tenantId)
            .setApplicationSessionId("ApplicationSessionId")
            .setAccessContract("aName3");

        try (final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            final VitamClientException vitamClientException = assertThrows(
                VitamClientException.class,
                () -> client.downloadObjectByObjectPersistentIdentifier(context, persistentIdentifier)
            );
            assertThat(vitamClientException.getMessage()).isEqualTo("Persistent identifier not found exception");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldObjectBeNotFoundWhenItsObjectGroupIsEliminated() throws Exception {
        final String sip = "sip/TEST_INGEST_ARK_IDS_AND_AUTOGENERATE_ARK_IDS.zip";
        final VitamContext context = new VitamContext(tenantId)
            .setApplicationSessionId("ApplicationSessionId")
            .setAccessContract("contract");
        final String ingestOperationId = ingest(context, sip);
        final String date = "2023-01-01";
        final String eliminationOperationId = eliminate(context, ingestOperationId, date);
        assertThat(eliminationOperationId).isNotBlank();

        logicalClock.logicalSleep(10, ChronoUnit.DAYS);

        final String persistentIdentifier = "ark:/23567/001a9d7db5eadabac_binary_master";

        try (final MetaDataClient client = MetaDataClientFactory.getInstance().getClient()) {
            final PersistentIdentifierReconstructionRequest request = new PersistentIdentifierReconstructionRequest();
            request.setTenants(List.of(VitamThreadUtils.getVitamSession().getTenantId()));
            final RequestResponse<?> reconstructionPayload = client.reconstructPersistentIdentifiers(request);
            assertThat(reconstructionPayload).isNotNull();

            final JsonNode purgedPayload = client.getPurgedPersistentIdentifiers(persistentIdentifier, null);
            assertThat(purgedPayload).isNotNull();
        }

        try (
            final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient();
            final Response response = client.downloadObjectByObjectPersistentIdentifier(context, persistentIdentifier)
        ) {
            assertEquals(404, response.getStatus());

            final String entity = response.readEntity(String.class);
            assertThat(entity).isNotBlank();

            final PurgedPersistentIdentifier purgedPersistentIdentifier = JsonHandler.getFromString(
                entity,
                PurgedPersistentIdentifier.class
            );
            assertThat(purgedPersistentIdentifier).isNotNull();
            assertThat(purgedPersistentIdentifier.getOperationType()).isEqualTo(ELIMINATION_ACTION.name());
            assertThat(
                purgedPersistentIdentifier
                    .getPersistentIdentifiers()
                    .stream()
                    .anyMatch(pi -> pi.getPersistentIdentifierContent().equals(persistentIdentifier))
            ).isTrue();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldObjectBeNotFoundWhenItsObjectGroupIsTransferred()
        throws FileNotFoundException, VitamClientException, JsonProcessingException, InvalidParseOperationException {
        final String guid = GUIDFactory.newGUID().getId();
        final String persistentIdentifier = "ark:/00001/transferred_object:" + guid;
        final String json = PropertiesUtils.getResourceAsString(
            "elimination/purgedPersistentIdentifier/transferred-ppi.json"
        )
            .replace("aeaqaaaaaaeaaaabaatogammrnxmtsiaaaaq", guid)
            .replace("ark:/00001/transferred_object", persistentIdentifier);
        final Document document = Document.parse(json);
        final MongoDatabase db = mongoRule.getMongoDatabase();
        db.getCollection("PurgedPersistentIdentifier").insertOne(document);

        try (final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            final VitamContext context = new VitamContext(tenantId)
                .setApplicationSessionId("ApplicationSessionId")
                .setAccessContract("contract");

            try (
                final Response response = client.downloadObjectByObjectPersistentIdentifier(
                    context,
                    persistentIdentifier
                )
            ) {
                assertEquals(404, response.getStatus());

                final String entity = response.readEntity(String.class);
                assertThat(entity).isNotBlank();

                final PurgedPersistentIdentifier purgedPersistentIdentifier = JsonHandler.getFromString(
                    entity,
                    PurgedPersistentIdentifier.class
                );
                assertThat(purgedPersistentIdentifier).isNotNull();
                assertThat(purgedPersistentIdentifier.getOperationType()).isEqualTo(TRANSFER_REPLY.name());
                assertThat(
                    purgedPersistentIdentifier
                        .getPersistentIdentifiers()
                        .stream()
                        .anyMatch(pi -> pi.getPersistentIdentifierContent().equals(persistentIdentifier))
                ).isTrue();
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldObjectBeNotFoundWhenItsVersionIsDeleted()
        throws FileNotFoundException, VitamClientException, JsonProcessingException, InvalidParseOperationException {
        final String guid = GUIDFactory.newGUID().getId();
        final String persistentIdentifier = "ark:/00001/removed_object_version:" + guid;
        final String json = PropertiesUtils.getResourceAsString(
            "elimination/purgedPersistentIdentifier/object-version-removed-ppi.json"
        )
            .replace("aeaqaaaaaaeaaaabaatogammrnxmtsiaaaaq", guid)
            .replace("ark:/00001/removed_object_version", persistentIdentifier);
        final Document document = Document.parse(json);
        final MongoDatabase db = mongoRule.getMongoDatabase();
        db.getCollection("PurgedPersistentIdentifier").insertOne(document);

        try (final AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
            final VitamContext context = new VitamContext(tenantId)
                .setApplicationSessionId("ApplicationSessionId")
                .setAccessContract("contract");

            try (
                final Response response = client.downloadObjectByObjectPersistentIdentifier(
                    context,
                    persistentIdentifier
                )
            ) {
                assertEquals(404, response.getStatus());

                final String entity = response.readEntity(String.class);
                assertThat(entity).isNotBlank();

                final PurgedPersistentIdentifier purgedPersistentIdentifier = JsonHandler.getFromString(
                    entity,
                    PurgedPersistentIdentifier.class
                );
                assertThat(purgedPersistentIdentifier).isNotNull();
                assertThat(purgedPersistentIdentifier.getOperationType()).isEqualTo(DELETE_GOT_VERSIONS.name());
                assertThat(
                    purgedPersistentIdentifier
                        .getPersistentIdentifiers()
                        .stream()
                        .anyMatch(pi -> pi.getPersistentIdentifierContent().equals(persistentIdentifier))
                ).isTrue();
            }
        }
    }

    private InputStream readStoredReport(String filename)
        throws StorageServerClientException, StorageNotFoundException, StorageUnavailableDataFromAsyncOfferClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            Response reportResponse = null;

            try {
                reportResponse = storageClient.getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    filename,
                    DataCategory.REPORT,
                    AccessLogUtils.getNoLogAccessLog()
                );
                assertThat(reportResponse.getStatus()).isEqualTo(Status.OK.getStatusCode());
                return new VitamAsyncInputStream(reportResponse);
            } catch (
                RuntimeException
                | StorageServerClientException
                | StorageNotFoundException
                | StorageUnavailableDataFromAsyncOfferClientException e
            ) {
                StreamUtils.consumeAnyEntityAndClose(reportResponse);
                throw e;
            }
        }
    }

    private void awaitForWorkflowTerminationWithStatus(String operationGuid, StatusCode expectedStatusCode) {
        waitOperation(operationGuid);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationGuid, tenantId);

        try {
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(expectedStatusCode, processWorkflow.getStatus());
        } catch (AssertionError e) {
            tryLogLogbookOperation(operationGuid);
            tryLogATR(operationGuid);
            throw e;
        }
    }

    private void tryLogLogbookOperation(String operationId) {
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode logbookOperation = logbookClient.selectOperationById(operationId);
            LOGGER.error("Operation logbook status : \n" + JsonHandler.prettyPrint(logbookOperation) + "\n\n\n");
        } catch (Exception e) {
            LOGGER.error("Could not retrieve logbook operation for operation " + operationId, e);
        }
    }

    private void tryLogATR(String operationId) {
        try (InputStream atr = readStoredReport(operationId + XML)) {
            LOGGER.error("Operation ATR : \n" + IOUtils.toString(atr, StandardCharsets.UTF_8) + "\n\n\n");
        } catch (StorageNotFoundException ignored) {} catch (Exception e) {
            LOGGER.error("Could not retrieve ATR for operation " + operationId, e);
        }
    }

    private String ingest(final VitamContext context, final String sip) throws Exception {
        try (
            final InputStream inputStream = PropertiesUtils.getResourceAsStream(sip);
            final IngestExternalClient ingestClient = IngestExternalClientFactory.getInstance()
                .setVitamClientType(PRODUCTION)
                .getClient();
            final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance()
                .setVitamClientType(PRODUCTION)
                .getClient()
        ) {
            final RequestResponse<Void> response = ingestClient.ingest(
                context,
                inputStream,
                DEFAULT_WORKFLOW.name(),
                ProcessAction.RESUME.name()
            );
            assertThat(response.isOk()).as(JsonHandler.unprettyPrint(response)).isTrue();

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(adminClient);
            boolean process_timeout = vitamPoolingClient.wait(
                context.getTenantId(),
                operationId,
                ProcessState.COMPLETED,
                1800,
                1_000L,
                TimeUnit.MILLISECONDS
            );
            if (!process_timeout) {
                Assertions.fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
            }
            return operationId;
        }
    }

    private EliminationRequestBody eliminationRequest(final String operationId, final String beginDate)
        throws InvalidCreateOperationException {
        final SelectMultiQuery selectMultiQuery = (SelectMultiQuery) new SelectMultiQuery()
            .addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), operationId));
        final ObjectNode objectNode = selectMultiQuery.getFinalSelect();
        objectNode.remove("$filter");
        objectNode.remove("$facets");
        objectNode.remove("$projection");
        return new EliminationRequestBody(beginDate, objectNode);
    }

    private void containsEliminationInfo(String operationId)
        throws OperationContextException, StorageNotFoundException {
        final OperationContextMonitor operationContextMonitor = new OperationContextMonitor();
        final JsonNode info = operationContextMonitor.getInformation(
            VitamConfiguration.getDefaultStrategy(),
            operationId,
            LogbookTypeProcess.ELIMINATION
        );
        assertThat(info).isNotNull();
        assertThat(JsonHandler.unprettyPrint(info)).contains("ELIMINATION_" + operationId + ".zip");
    }

    private void notContainsEliminationInfo(String operationId)
        throws OperationContextException, StorageNotFoundException {
        final OperationContextMonitor operationContextMonitor = new OperationContextMonitor();
        final JsonNode info = operationContextMonitor.getInformation(
            VitamConfiguration.getDefaultStrategy(),
            operationId,
            LogbookTypeProcess.ELIMINATION
        );
        assertThat(info).isNotNull();
        assertThat(JsonHandler.unprettyPrint(info)).doesNotContain("ELIMINATION_" + operationId + ".zip");
    }

    private String eliminate(final VitamContext context, final String operationId, final String date)
        throws InvalidCreateOperationException, VitamClientException, OperationContextException, StorageNotFoundException, InterruptedException {
        try (
            final AccessExternalClient client = AccessExternalClientFactory.getInstance()
                .setVitamClientType(PRODUCTION)
                .getClient()
        ) {
            final EliminationRequestBody eliminationRequestBody = eliminationRequest(operationId, date);
            final RequestResponse<JsonNode> result = client.startEliminationAction(context, eliminationRequestBody);
            assertThat(result.isOk()).isTrue();

            final String eliminationOperationId = result.getHeaderString(X_REQUEST_ID);
            assertThat(eliminationOperationId).isNotBlank();
            VitamThreadUtils.getVitamSession().setTenantId(context.getTenantId());
            VitamThreadUtils.getVitamSession().setRequestId(eliminationOperationId);
            containsEliminationInfo(eliminationOperationId);
            awaitForWorkflowTerminationWithStatus(eliminationOperationId, StatusCode.OK);
            TimeUnit.SECONDS.sleep(1); // wait until cleanup is finished
            notContainsEliminationInfo(eliminationOperationId);
            return eliminationOperationId;
        }
    }

    public interface IngestCleanupAdminService {
        @POST("/adminmanagement/v1/invalidIngestCleanup/{opi}")
        @Headers({ "Accept: application/json" })
        Call<Void> startIngestCleanupWorkflow(
            @Path("opi") String opi,
            @Header("X-Tenant-Id") Integer tenant,
            @Header("Authorization") String basicAuthnToken
        );
    }
}
