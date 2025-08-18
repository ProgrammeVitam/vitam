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
package fr.gouv.vitam.collect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.culture.archivesdefrance.seda.v2.LegalStatusType;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.antivirus.rest.AntivirusMain;
import fr.gouv.vitam.collect.common.dto.MetadataUnitUp;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.collect.external.external.rest.CollectExternalMain;
import fr.gouv.vitam.collect.internal.CollectInternalMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import jakarta.ws.rs.core.Response;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.collect.CollectTestHelper.closeTransaction;
import static fr.gouv.vitam.collect.CollectTestHelper.createProject;
import static fr.gouv.vitam.collect.CollectTestHelper.createTransaction;
import static fr.gouv.vitam.collect.CollectTestHelper.initProjectData;
import static fr.gouv.vitam.collect.CollectTestHelper.initTransaction;
import static fr.gouv.vitam.collect.CollectTestHelper.uploadZipTransaction;
import static fr.gouv.vitam.common.TestZipUtils.unzipFile;
import static fr.gouv.vitam.common.TestZipUtils.zipFolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CollectIngestIT extends AbstractCollectIT {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static String prefix;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        CollectIngestIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            AccessInternalMain.class,
            IngestInternalMain.class,
            AccessExternalMain.class,
            IngestExternalMain.class,
            AntivirusMain.class,
            CollectInternalMain.class,
            CollectExternalMain.class
        )
    );

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Before
    public void setUp() throws Exception {
        prefix = MetadataCollections.UNIT.getPrefix();
        runner.startMetadataCollectServer();
        runner.startWorkspaceCollectServer();
        runner.startWorkspaceServer();
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-ingest-internal").prepareData();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
    }

    @After
    public void tearDown() throws Exception {
        MetadataCollections.UNIT.setPrefix(prefix);
        runner.stopMetadataCollectServer(false);
        runner.stopWorkspaceCollectServer();
        runner.stopMetadataServer(true);
        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Test
    @RunWithCustomExecutor
    public void should_ingest_sip_from_transaction() throws Exception {
        String idTransaction;
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();

            ProjectDto projectDtoResult = createProject(vitamContext, projectDto).orElseThrow();
            TransactionDto transactiondto = createTransaction(vitamContext, projectDtoResult.getId());

            RequestResponse<JsonNode> transactionResponse = collectClient.initTransaction(
                vitamContext,
                transactiondto,
                projectDtoResult.getId()
            );
            assertThat(transactionResponse.getStatus()).isEqualTo(200);

            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult = JsonHandler.getFromJsonNode(
                requestResponseOK.getFirstResult(),
                TransactionDto.class
            );
            idTransaction = transactionDtoResult.getId();
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/arbo_to_ingest.zip")) {
                RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDtoResult.getId(),
                    inputStream,
                    null,
                    null
                );
                assertThat(response.getStatus()).isEqualTo(200);
            }

            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDtoResult.getId(),
                new SelectMultiQuery().getFinalSelect()
            );

            assertEquals(6, unitsByTransaction.getResults().size());
            collectClient.closeTransaction(vitamContext, transactionDtoResult.getId());
        }

        InputStream inputStream = generateSip(idTransaction);

        switchToVitamMetadataHack(runner);

        String processId;

        processId = ingestToVitam(inputStream);

        try (AccessExternalClient accessExternalClient = AccessExternalClientFactory.getInstance().getClient()) {
            List<JsonNode> results = selectVitamMetadataUnitsByOpi(processId);

            assertEquals(6, results.size());

            // Try download binary
            JsonNode unitWithGot = getUnitByTitle(results, "BAD0431E2C5E80E5BD42D547zzzzzA3ED5966.odt");
            Response binaryContent = accessExternalClient.getObjectStreamByUnitId(
                vitamContext,
                unitWithGot.get(VitamFieldsHelper.id()).asText(),
                DataObjectVersionType.BINARY_MASTER.getName(),
                1
            );
            assertThat(binaryContent.readEntity(InputStream.class)).hasDigest(
                "SHA-512",
                "942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7"
            );

            // Check the folder "dossier1" which has an attached binary via "ObjectFiles" field
            JsonNode dossier1 = getUnitByTitle(results, "dossier1");

            assertThat(dossier1.hasNonNull(VitamFieldsHelper.object())).isTrue();
            String dossier1UnitId = dossier1.get(VitamFieldsHelper.id()).asText();
            String dossier1ObjectGroupId = dossier1.get(VitamFieldsHelper.object()).asText();

            SelectMultiQuery selectGot = new SelectMultiQuery();
            selectGot.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), dossier1ObjectGroupId));
            JsonNode dossier1ObjectGroup =
                ((RequestResponseOK<JsonNode>) accessExternalClient.selectObjects(
                        vitamContext,
                        selectGot.getFinalSelect()
                    )).getFirstResult();

            assertThat(
                StreamSupport.stream(dossier1ObjectGroup.get(VitamFieldsHelper.unitups()).spliterator(), false).map(
                    JsonNode::textValue
                )
            ).containsExactly(dossier1UnitId);

            Response dossier1Content = accessExternalClient.getObjectStreamByUnitId(
                vitamContext,
                dossier1UnitId,
                DataObjectVersionType.BINARY_MASTER.getName(),
                1
            );
            assertThat(dossier1Content.readEntity(InputStream.class)).hasContent("dossier1.txt");
        }

        try (AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq("Opi", processId));
            var acRegDetResponseAfterUpdate = adminExternalClient.findAccessionRegisterDetails(
                vitamContext,
                select.getFinalSelect()
            );

            AccessionRegisterDetailModel accessionRegisterDetail =
                ((RequestResponseOK<AccessionRegisterDetailModel>) acRegDetResponseAfterUpdate).getResults().get(0);

            assertThat(accessionRegisterDetail.getLegalStatus()).isEqualTo(LegalStatusType.PRIVATE_ARCHIVE.value());
            assertThat(accessionRegisterDetail.getComment().get(0)).isEqualTo(
                "Versement du service producteur : Cabinet de Michel Mercier"
            );
            assertThat(accessionRegisterDetail.getAcquisitionInformation()).isEqualTo("AcquisitionInformation");
        }

        //// Cette requette permet de récupérer des unités par transaction avec l'opi de vitam core
        // du coup le nombre des résultats obtenus doit être zéro.

        runner.startMetadataCollectServer();

        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                processId,
                new SelectMultiQuery().getFinalSelect()
            );

            assertEquals(0, unitsByTransaction.getResults().size());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_ingest_sip_with_complex_attachement() throws Exception {
        // Ingest holding schema SIP
        switchToVitamMetadataHack(runner);

        String holdingSchemeIngestProcessId = ingestToVitam(
            PropertiesUtils.getResourceAsStream("collect/complex_attachment_holding_scheme.zip"),
            Contexts.HOLDING_SCHEME
        );

        List<JsonNode> holdingSchemeUnits = selectVitamMetadataUnitsByOpi(holdingSchemeIngestProcessId);
        Map<String, String> holdingSchemeUnitIdByTitle = holdingSchemeUnits
            .stream()
            .collect(Collectors.toMap(unit -> unit.get("Title").asText(), unit -> unit.get("#id").asText()));

        switchToCollectMetadataHack(runner);

        // Prepare ZIP file
        File zipFile = PropertiesUtils.getResourceFile("collect/complex_attachement_with_update_operation.zip");

        File tmpUnzipFolder = tempFolder.newFolder(GUIDFactory.newGUID().getId());

        unzipFile(zipFile.getAbsolutePath(), tmpUnzipFolder.getAbsolutePath());

        Path metadataCsvFile = new File(tmpUnzipFolder, "metadata.jsonl").toPath();
        String newMetadataCsvFileContent = Files.readString(metadataCsvFile).replaceAll(
            "<GUID_HERE>",
            holdingSchemeUnitIdByTitle.get("Holding scheme - UpdateOperation by GUID")
        );
        Files.writeString(metadataCsvFile, newMetadataCsvFileContent);

        File finalZipFile = tempFolder.newFile();
        zipFolder(tmpUnzipFolder.toPath(), finalZipFile.getPath());

        // Create project & transaction with static & dynamic attachement configuration
        ProjectDto projectDto = initProjectData();
        projectDto.setUnitUp(holdingSchemeUnitIdByTitle.get("Holding scheme - Static Attachement"));
        projectDto.setUnitUps(
            List.of(
                new MetadataUnitUp(
                    holdingSchemeUnitIdByTitle.get("Holding scheme - Dynamic Attachement 1"),
                    "Tag",
                    "Tag1"
                ),
                new MetadataUnitUp(
                    holdingSchemeUnitIdByTitle.get("Holding scheme - Dynamic Attachement 2"),
                    "Tag",
                    "Tag2"
                )
            )
        );
        ProjectDto project = createProject(vitamContext, projectDto).orElseThrow();
        TransactionDto transaction = createTransaction(vitamContext, project.getId());

        // Upload ZIP
        uploadZipTransaction(vitamContext, transaction.getId(), new FileInputStream(finalZipFile), null);

        // Upload another ZIP with explicit X-Attachement-Id
        String explicitAttachmentIdForFolder1;
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
            selectMultiQuery.addQueries(QueryHelper.match("Title", "Folder1"));
            JsonNode folder1Unit =
                ((RequestResponseOK<JsonNode>) collectClient.getUnitsByTransaction(
                        vitamContext,
                        transaction.getId(),
                        selectMultiQuery.getFinalSelect()
                    )).getFirstResult();
            explicitAttachmentIdForFolder1 = folder1Unit.get("#id").asText();
        }

        uploadZipTransaction(
            vitamContext,
            transaction.getId(),
            "collect/complex_attachement_with_X_Attachement_Id_and_ObjectFiles.zip",
            explicitAttachmentIdForFolder1
        );

        // Close & send transaction to Vitam
        closeTransaction(vitamContext, transaction.getId());

        InputStream sip = generateSip(transaction.getId());

        switchToVitamMetadataHack(runner);

        String ingestProcessId = ingestToVitam(sip);

        // Check ingested data
        List<JsonNode> results = selectVitamMetadataUnitsByOpi(ingestProcessId);
        results.forEach(unit -> {
            if (unit.has("#object")) {
                ((ObjectNode) unit).put("#object", "OG ID -" + unit.get("Title").asText());
            }
        });

        Map<String, String> guidReplacement = new HashMap<>();
        for (String title : holdingSchemeUnitIdByTitle.keySet()) {
            guidReplacement.put(holdingSchemeUnitIdByTitle.get(title), title);
        }
        results.forEach(unit -> guidReplacement.put(unit.get("#id").asText(), unit.get("Title").asText()));
        guidReplacement.put(holdingSchemeIngestProcessId, "<OPI_HOLDING_SCHEME>");
        guidReplacement.put(ingestProcessId, "<OPI_COLLECT>");

        String json = JsonHandler.prettyPrint(results);
        for (Map.Entry<String, String> titleToIdEntry : guidReplacement.entrySet()) {
            json = json.replace(titleToIdEntry.getKey(), "ID - " + titleToIdEntry.getValue());
        }

        JsonNode expectedUnits = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("collect/complex_attachement_expects_units.json")
        );

        JsonNode actual = JsonHandler.getFromString(json);
        JsonAssert.assertJsonEquals(
            expectedUnits,
            actual,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.approximateCreationDate(),
                    "[*]." + VitamFieldsHelper.approximateUpdateDate(),
                    "[*]." + VitamFieldsHelper.implementationVersion()
                )
            )
        );
    }

    @Test
    @RunWithCustomExecutor
    public void shouldAutomaticallySendTransaction()
        throws InvalidParseOperationException, VitamClientException, IOException, InterruptedException, VitamApplicationServerException {
        runner.startMetadataCollectServer();

        IngestExternalClientFactory.getInstance()
            .setVitamClientType((VitamClientFactoryInterface.VitamClientType.MOCK));

        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();
            projectDto.setAutomaticIngest(true);

            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            assertThat(projectResponse.getStatus()).isEqualTo(200);

            ProjectDto projectDtoResult = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class
            );

            final TransactionDto transactiondto = initTransaction(projectDtoResult.getId());

            final RequestResponse<JsonNode> transactionResponse = collectClient.initTransaction(
                vitamContext,
                transactiondto,
                projectDtoResult.getId()
            );
            assertThat(transactionResponse.getStatus()).isEqualTo(200);

            final String idTransaction = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) transactionResponse).getFirstResult(),
                TransactionDto.class
            ).getId();
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/arbo_to_ingest.zip")) {
                RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    idTransaction,
                    inputStream,
                    null,
                    null
                );
                assertThat(response.getStatus()).isEqualTo(200);
            }
            collectClient.closeTransaction(vitamContext, idTransaction);
            retryAndWaitOperation(idTransaction, TransactionStatus.SENT);
        }
    }

    private void retryAndWaitOperation(String transactionId, TransactionStatus transactionStatus)
        throws InterruptedException, VitamClientException, InvalidParseOperationException {
        int maxRetries = 3;
        int attempts = 0;

        while (attempts < maxRetries) {
            // Effectuez l'opération
            TransactionStatus currentStatus = CollectTestHelper.getTransactionStatus(transactionId, vitamContext);

            // Vérifiez si le statut souhaité est atteint
            if (currentStatus.equals(transactionStatus)) {
                return;
            }

            // Attendez avant la prochaine tentative
            Thread.sleep(Duration.ofSeconds(5));

            attempts++;
        }

        // Si nous avons épuisé toutes les tentatives sans atteindre le statut souhaité
        throw new RuntimeException("Opération échouée après plusieurs tentatives");
    }

    @Test
    @RunWithCustomExecutor
    public void should_verify_sip_uses_seda_2_3() throws Exception {
        // Create a transaction
        String idTransaction;
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();

            ProjectDto projectDtoResult = createProject(vitamContext, projectDto).orElseThrow();
            TransactionDto transactiondto = createTransaction(vitamContext, projectDtoResult.getId());

            RequestResponse<JsonNode> transactionResponse = collectClient.initTransaction(
                vitamContext,
                transactiondto,
                projectDtoResult.getId()
            );
            assertThat(transactionResponse.getStatus()).isEqualTo(200);

            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult = JsonHandler.getFromJsonNode(
                requestResponseOK.getFirstResult(),
                TransactionDto.class
            );
            idTransaction = transactionDtoResult.getId();

            // Upload a ZIP file to the transaction
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/arbo_to_ingest.zip")) {
                RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDtoResult.getId(),
                    inputStream,
                    null,
                    null
                );
                assertThat(response.getStatus()).isEqualTo(200);
            }

            // Close the transaction
            collectClient.closeTransaction(vitamContext, transactionDtoResult.getId());
        }

        // Generate SIP from the transaction
        InputStream sipInputStream = generateSip(idTransaction);

        // Extract the manifest from the SIP and verify it uses SEDA 2.3
        File tempFolder = File.createTempFile("sip", ".zip", new File(VitamConfiguration.getVitamTmpFolder()));
        try (sipInputStream) {
            IOUtils.copy(sipInputStream, new FileOutputStream(tempFolder));
        }

        try (ZipFile zipFile = new ZipFile(tempFolder)) {
            ZipArchiveEntry manifest = zipFile.getEntry("manifest.xml");
            try (InputStream is = zipFile.getInputStream(manifest)) {
                String manifestContent = IOUtils.toString(is, StandardCharsets.UTF_8);

                // Verify the manifest uses SEDA 2.3
                String expectedNamespaceUri = SupportedSedaVersions.SEDA_2_3.getNamespaceURI();
                String expectedSchemaLocation =
                    expectedNamespaceUri + " " + SupportedSedaVersions.SEDA_2_3.getSedaValidatorXSD();

                assertTrue("Manifest should use SEDA 2.3 namespace", manifestContent.contains(expectedNamespaceUri));
                assertTrue(
                    "Manifest should use SEDA 2.3 schema location",
                    manifestContent.contains(expectedSchemaLocation)
                );
            }
        }
    }
}
