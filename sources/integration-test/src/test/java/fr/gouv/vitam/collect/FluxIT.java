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
import fr.gouv.vitam.collect.common.dto.BatchDto;
import fr.gouv.vitam.collect.common.dto.BatchStatusDto;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateResult;
import fr.gouv.vitam.collect.common.dto.BulkAtomicUpdateStatus;
import fr.gouv.vitam.collect.common.dto.MetadataUnitUp;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.collect.external.external.exception.CollectExternalClientInvalidRequestException;
import fr.gouv.vitam.collect.external.external.exception.CollectExternalClientNotFoundException;
import fr.gouv.vitam.collect.external.external.rest.CollectExternalMain;
import fr.gouv.vitam.collect.internal.CollectInternalMain;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.CollectInternalClientFactory;
import fr.gouv.vitam.collect.internal.core.helpers.MetadataHelper;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.workspace.api.model.FileParams;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import jakarta.ws.rs.core.Response;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.collect.CollectTestHelper.closeTransaction;
import static fr.gouv.vitam.collect.CollectTestHelper.createProject;
import static fr.gouv.vitam.collect.CollectTestHelper.createTransaction;
import static fr.gouv.vitam.collect.CollectTestHelper.initProjectData;
import static fr.gouv.vitam.collect.CollectTestHelper.updateUnitWithMetadataCsv;
import static fr.gouv.vitam.collect.CollectTestHelper.uploadZipTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class FluxIT extends VitamRuleRunner {

    private static final String GUID_ILE_DE_FRANCE = "aeaqaaaaaahb5rlnaat5yamglule7mqaaabq";
    private static final String UNIT_TITLE = "Paris";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        FluxIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            CollectInternalMain.class,
            CollectExternalMain.class
        )
    );

    private static final Integer TENANT_ID = 0;
    private static final String UNITS_UPDATED_BY_ZIP_PATH = "collect/units_with_description.json";
    private static final String UNITS_UPDATED_WITH_JSONL_BY_ZIP_PATH =
        "collect/units_updated_with_jsonl_with_description.json";
    private static final String UNITS_UPLOADED_WITH_JSONL = "collect/units_uploaded_with_jsonl_metadata.json";
    private static final String UNITS_UPDATED_WITH_JSONL_WITH_COMPLEX = "collect/units_updated_with_jsonl_complex.json";
    private static final String UPDATED_UNITS_WITH_DYNAMIC_ATTACHMENT =
        "collect/updated_units_with_dynamic_attachment.json";
    private static final String CREATED_UNIT_WITH_DYNAMIC_ATTACHMENT =
        "collect/created_unit_with_dynamic_attachment.json";
    private static final String ZIP_FILE = "collect/sampleStream.zip";
    private static final String ZIP_FILE_WITH_CSV_METADATA = "collect/sampleStreamWithCsvMetadata.zip";
    private static final String ZIP_FILE_FOR_ATTACHEMENT_WITH_CSV_METADATA =
        "collect/sampleAttachedStreamWithCsvMetadata.zip";
    private static final String ZIP_FILE_WITH_JSONL_METADATA = "collect/sampleStreamWithJsonlMetadata.zip";
    private static final String ZIP_FILE_WITH_INVALID_JSONL_METADATA =
        "collect/sampleStreamWithInvalidJsonlMetadata.zip";
    private static final String ZIP_FILE_WITH_WRONG_FILE_IN_JSONL_METADATA =
        "collect/sampleStreamWithWrongFileJsonlMetadata.zip";
    private static final String ZIP_FILE_WITH_BOTH_JSONL_AND_CSV_METADATA_FILES =
        "collect/sampleStreamWithJsonlAndCsvMetadataFiles.zip";
    private static final String METADATA_JSONL = "collect/metadata.jsonl";
    private static final String METADATA_JSONL_UPDATE_WITH_COMPLEX_SELECTORS =
        "collect/metadata_update_with_complex_selectors.jsonl";
    private static final String METADATA_JSONL_BAD_FORMAT = "collect/metadata_bad_format.jsonl";
    private static final String METADATA_JSONL_UNKNOWN_FILE = "collect/metadata_unknown_file.jsonl";
    private static final String FILE_ZIP_FILE = "collect/file.zip";
    private static final String UNITS_TO_UPDATE = "collect/updateMetadata/units.json";
    private static final String UNITS_UPDATED_BY_CSV_PATH = "collect/updateMetadata/units_updated.json";
    private static final String METADATA_FILE = "collect/updateMetadata/metadata.csv";
    private static final String METADATA_FILE_WITH_ID = "collect/updateMetadata/metadata_id.csv";
    private static final String ATTACHMENT_UNIT_ID = "aeeaaaaaaceevqftaammeamaqvje33aaaaaq";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private final VitamContext vitamContext = new VitamContext(TENANT_ID);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        runner.startMetadataCollectServer();
        runner.startWorkspaceCollectServer();
        runner.startWorkspaceServer();
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        runner.stopMetadataCollectServer(false);
        runner.stopWorkspaceCollectServer();
        runner.stopWorkspaceServer();
        runner.stopMetadataServer(true);
        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_zip_to_transaction() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE_WITH_CSV_METADATA)) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDto.getId(),
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDto.getId(),
                new SelectMultiQuery().getFinalSelect()
            );
            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(UNITS_UPDATED_BY_ZIP_PATH)
            );

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                expectedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate()
                    )
                )
            );

            // test download got
            String saintLazareLinkUnitId = getUnitIdByTitle(unitsByTransaction, "Saint-Lazare.link");
            assertUnitObjectGroupContent(
                collectClient,
                saintLazareLinkUnitId,
                "Link to 2_Front-Populaire/Porte-de-la-Chapelle/Marx-Dormoy/Saint-Lazare"
            );

            // Check folder with associated binary via ObjectFiles field
            JsonNode saintLazareUnit = getUnitByTitle(unitsByTransaction, "Saint-Lazare");
            String saintLazareUnitId = saintLazareUnit.get(VitamFieldsHelper.id()).asText();

            assertThat(saintLazareUnit.hasNonNull(VitamFieldsHelper.object())).isTrue();
            String saintLazareObjectGroupId = saintLazareUnit.get(VitamFieldsHelper.object()).asText();
            JsonNode saintLazareObjectGroup = getObjectGroupById(collectClient, saintLazareObjectGroupId);
            // FIXME: Should be "#unitups" instead of "_up"
            List<String> objectGroupParents = StreamSupport.stream(
                saintLazareObjectGroup.get("_up").spliterator(),
                false
            )
                .map(JsonNode::textValue)
                .toList();
            assertThat(objectGroupParents).containsExactly(saintLazareUnitId);
            assertUnitObjectGroupContent(collectClient, saintLazareUnitId, "Saint-Lazare.txt");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_zip_to_transaction_with_csv_metadata_and_complex_jslt_transformation() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDto = createProjectWithAttachement();
            projectDto.setTransformationRules(PropertiesUtils.getResourceAsString("collect/jslt_complex_rules.jslt"));
            ProjectDto projectDtoResult = createProject(vitamContext, projectDto).orElseThrow();
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());
            try (
                InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/zip_with_csv_complex_jslt.zip")
            ) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDto.getId(),
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDto.getId(),
                new SelectMultiQuery().getFinalSelect()
            );
            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile("collect/expected_units_after_jslt_complex_rules.json")
            );

            JsonAssert.assertJsonEquals(
                expectedUnits,
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate()
                    )
                )
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_zip_with_jsonl_metadata_to_transaction() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE_WITH_JSONL_METADATA)) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDto.getId(),
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDto.getId(),
                new SelectMultiQuery().getFinalSelect()
            );
            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(UNITS_UPLOADED_WITH_JSONL)
            );

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                expectedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate()
                    )
                )
            );

            // test download got
            String saintLazareLinkUnitId = getUnitIdByTitle(unitsByTransaction, "Saint-Lazare.link");
            assertUnitObjectGroupContent(
                collectClient,
                saintLazareLinkUnitId,
                "Link to 2_Front-Populaire/Porte-de-la-Chapelle/Marx-Dormoy/Saint-Lazare"
            );

            // Check folder with associated binary via ObjectFiles field
            JsonNode saintLazareUnit = getUnitByTitle(unitsByTransaction, "Saint-Lazare");
            String saintLazareUnitId = saintLazareUnit.get(VitamFieldsHelper.id()).asText();

            assertThat(saintLazareUnit.hasNonNull(VitamFieldsHelper.object())).isTrue();
            String saintLazareObjectGroupId = saintLazareUnit.get(VitamFieldsHelper.object()).asText();
            JsonNode saintLazareObjectGroup = getObjectGroupById(collectClient, saintLazareObjectGroupId);
            // FIXME: Should be "#unitups" instead of "_up"
            List<String> objectGroupParents = StreamSupport.stream(
                saintLazareObjectGroup.get("_up").spliterator(),
                false
            )
                .map(JsonNode::textValue)
                .toList();
            assertThat(objectGroupParents).containsExactly(saintLazareUnitId);
            assertUnitObjectGroupContent(collectClient, saintLazareUnitId, "Saint-Lazare.txt");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_zip_with_jsonl_metadata_and_jslt_transformation_to_transaction() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDto = initProjectDataWithAttachment();
            projectDto.setTransformationRules(
                """
                {
                  "Title": if (.Title != null) .Title + " - TRANSFORMED" else null,
                  *: .
                }
                """
            );
            ProjectDto projectDtoResult = createProject(vitamContext, projectDto).orElseThrow();
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE_WITH_JSONL_METADATA)) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDto.getId(),
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDto.getId(),
                new SelectMultiQuery().getFinalSelect()
            );
            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile("collect/units_with_jsonl_metadata_and_jslt_transformation.json")
            );

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                expectedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate()
                    )
                )
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_zip_with_jsonl_metadata_to_project() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE_WITH_JSONL_METADATA)) {
                final RequestResponse<String> response = collectClient.uploadZipToProject(
                    vitamContext,
                    projectDtoResult.getId(),
                    inputStream
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }
            // For now, we can't check unit metadata when uploading zip to project
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_upload_zip_to_transaction_with_wrong_file_in_jsonl_metadata() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());
            try (
                InputStream inputStream = PropertiesUtils.getResourceAsStream(
                    ZIP_FILE_WITH_WRONG_FILE_IN_JSONL_METADATA
                )
            ) {
                assertThatThrownBy(
                    () ->
                        collectClient.uploadZipToTransaction(
                            vitamContext,
                            transactionDto.getId(),
                            inputStream,
                            null,
                            null
                        )
                )
                    .isExactlyInstanceOf(CollectExternalClientInvalidRequestException.class)
                    .hasMessage(
                        "An unexpected error occurs when try to upload the ZIP: Invalid File or #uploadPath selector 'UnknownFile'. No such file or directory"
                    );
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_upload_zip_to_transaction_with_both_jsonl_and_csv_metadata_files() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());
            try (
                InputStream inputStream = PropertiesUtils.getResourceAsStream(
                    ZIP_FILE_WITH_BOTH_JSONL_AND_CSV_METADATA_FILES
                )
            ) {
                assertThatThrownBy(
                    () ->
                        collectClient.uploadZipToTransaction(
                            vitamContext,
                            transactionDto.getId(),
                            inputStream,
                            null,
                            null
                        )
                )
                    .isExactlyInstanceOf(CollectExternalClientInvalidRequestException.class)
                    .hasMessageContaining("Multiple metadata update files found.");
            }

            // Get the transaction and verify that it has a batch with PURGED status
            TransactionDto updatedTransaction = CollectTestHelper.getTransaction(vitamContext, transactionDto.getId());

            // Verify that the transaction has a batch
            List<BatchDto> batches = updatedTransaction.getBatches();
            assertThat(batches).isNotNull();
            assertThat(batches).isNotEmpty();

            // Verify that the batch status is PURGED
            batches.forEach(batch -> {
                assertThat(batch.getBatchStatus()).isEqualTo(BatchStatusDto.PURGED);
            });

            // Verify that no files were left in the workspace
            try (
                WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance(
                    WorkFlowExecutionContext.COLLECT
                ).getClient()
            ) {
                RequestResponse<Map<String, FileParams>> filesResponse = workspaceClient.getFilesWithParamsFromFolder(
                    transactionDto.getId(),
                    "Content"
                );

                if (filesResponse.isOk()) {
                    Map<String, FileParams> files =
                        ((RequestResponseOK<Map<String, FileParams>>) filesResponse).getFirstResult();
                    assertThat(files).isEmpty();
                }
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_upload_zip_with_invalid_jsonl_metadata_to_transaction() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE_WITH_INVALID_JSONL_METADATA)) {
                assertThatThrownBy(
                    () ->
                        collectClient.uploadZipToTransaction(
                            vitamContext,
                            transactionDto.getId(),
                            inputStream,
                            null,
                            null
                        )
                )
                    .isExactlyInstanceOf(CollectExternalClientInvalidRequestException.class)
                    .hasMessage(
                        "An unexpected error occurs when try to upload the ZIP: JSONL validation failed. 1 error:\n" +
                        "- Invalid unit metadata at index: 0. Empty metadata content"
                    );
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_windows_generated_zip_with_implicit_parent_entries_to_transaction_11756()
        throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());
            try (
                InputStream inputStream = PropertiesUtils.getResourceAsStream(
                    "collect/collect_windows_generated_zip_with_implicit_parent_entries_to_transaction_11756.zip"
                )
            ) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDto.getId(),
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDto.getId(),
                new SelectMultiQuery().addUsedProjection("#id", "Title").getFinalSelect()
            );

            assertThat(unitsByTransaction.getResults()).hasSize(6);
            assertThat(
                unitsByTransaction.getResults().stream().map(u -> u.get("Title").asText())
            ).containsExactlyInAnyOrder(
                MetadataHelper.STATIC_ATTACHMENT,
                "content",
                "AU1",
                "doc2.txt",
                "doc3.txt",
                "doc4.txt"
            );

            // test download got
            String unitId = getUnitIdByTitle(unitsByTransaction, "AU1");
            assertUnitObjectGroupContent(collectClient, unitId, "sdfgdsfgdsfgdfs");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_transaction_ko() throws Exception {
        //given
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            final TransactionDto transactiondto = CollectTestHelper.createTransaction(
                vitamContext,
                projectDtoResult.getId()
            );
            final RequestResponse<JsonNode> transactionResponse = collectClient.initTransaction(
                vitamContext,
                transactiondto,
                projectDtoResult.getId()
            );
            Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);
            final RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            final TransactionDto transactionDtoResult = JsonHandler.getFromJsonNode(
                requestResponseOK.getFirstResult(),
                TransactionDto.class
            );

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE)) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDtoResult.getId(),
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }

            final RequestResponseOK<JsonNode> unitsByTransactionbeforUploadTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDtoResult.getId(),
                new SelectMultiQuery().addUsedProjection("#id", "Title").getFinalSelect()
            );

            RequestResponse<Map<String, FileParams>> filesBeforeUpdate;
            try (
                WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance(
                    WorkFlowExecutionContext.COLLECT
                ).getClient()
            ) {
                filesBeforeUpdate = workspaceClient.getFilesWithParamsFromFolder(
                    transactionDtoResult.getId(),
                    "Content"
                );
            }

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/transaction_ko.zip")) {
                assertThatThrownBy(
                    () ->
                        collectClient.uploadZipToTransaction(
                            vitamContext,
                            transactionDtoResult.getId(),
                            inputStream,
                            null,
                            null
                        )
                ).isExactlyInstanceOf(CollectExternalClientInvalidRequestException.class);
            }

            RequestResponse<Map<String, FileParams>> filesAfterUpdate;
            try (
                WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance(
                    WorkFlowExecutionContext.COLLECT
                ).getClient()
            ) {
                filesAfterUpdate = workspaceClient.getFilesWithParamsFromFolder(
                    transactionDtoResult.getId(),
                    "Content"
                );
            }

            collectClient.closeTransaction(vitamContext, transactionDtoResult.getId());

            TransactionDto closedTransaction = CollectTestHelper.getTransaction(
                vitamContext,
                transactionDtoResult.getId()
            );
            // Transaction should be READY, or VALIDATED (if generation already proceeded)
            assertThat(closedTransaction.getStatus()).isIn(
                TransactionStatus.READY.name(),
                TransactionStatus.VALIDATED.name()
            );

            assertThatCode(
                () -> collectClient.ingest(vitamContext, transactionDtoResult.getId())
            ).doesNotThrowAnyException();
            // Transaction should be VALIDATED
            TransactionDto validTransaction = CollectTestHelper.getTransaction(
                vitamContext,
                transactionDtoResult.getId()
            );
            assertThat(validTransaction.getStatus()).isEqualTo(TransactionStatus.SENT.name());

            new SelectMultiQuery().addUsedProjection("#id", "Title", "#batchId").getFinalSelect();

            final RequestResponseOK<JsonNode> unitsByTransactionafterUploadTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDtoResult.getId(),
                new SelectMultiQuery().addUsedProjection("#id", "Title").getFinalSelect()
            );

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(unitsByTransactionbeforUploadTransaction.getResults()),
                unitsByTransactionafterUploadTransaction.getResults(),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
            );

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(filesBeforeUpdate.toJsonNode()),
                filesAfterUpdate.toJsonNode(),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_zip_to_project_ok() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();

            String automaticTransactionId;
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE_WITH_CSV_METADATA)) {
                final RequestResponse<String> response = collectClient.uploadZipToProject(
                    vitamContext,
                    projectDtoResult.getId(),
                    inputStream
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
                Assertions.assertThat(response).isInstanceOf(RequestResponseOK.class);
                automaticTransactionId = ((RequestResponseOK<String>) response).getFirstResult();
                assertThat(automaticTransactionId).isNotNull();
                assertThat(automaticTransactionId).isEqualTo("VIRTUAL_TX_" + projectDtoResult.getId());
            }

            // Temporary hack: We're using CollectInternalClient to query "virtual" transaction
            final RequestResponseOK<JsonNode> unitsByTransaction;
            try (CollectInternalClient collectInternalClient = CollectInternalClientFactory.getInstance().getClient()) {
                VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
                unitsByTransaction = collectInternalClient.getUnitsByTransaction(
                    automaticTransactionId,
                    new SelectMultiQuery().getFinalSelect()
                );
            }

            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(UNITS_UPDATED_BY_ZIP_PATH)
            );

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                expectedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate()
                    )
                )
            );

            // test download got
            String saintLazareLinkUnitId = getUnitIdByTitle(unitsByTransaction, "Saint-Lazare.link");
            assertUnitObjectGroupContent(
                collectClient,
                saintLazareLinkUnitId,
                "Link to 2_Front-Populaire/Porte-de-la-Chapelle/Marx-Dormoy/Saint-Lazare"
            );

            // test download folder with associated binary via ObjectFiles field
            String saintLazareUnitId = getUnitIdByTitle(unitsByTransaction, "Saint-Lazare");
            assertUnitObjectGroupContent(collectClient, saintLazareUnitId, "Saint-Lazare.txt");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_zip_to_project_ok_with_jslt() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDto = initProjectDataWithAttachment();
            projectDto.setTransformationRules(
                """
                {
                  "Title": .Title + " - TRANSFORMED",
                  *: .
                }
                """
            );
            ProjectDto projectDtoResult = createProject(vitamContext, projectDto).orElseThrow();

            String automaticTransactionId;
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE_WITH_CSV_METADATA)) {
                final RequestResponse<String> response = collectClient.uploadZipToProject(
                    vitamContext,
                    projectDtoResult.getId(),
                    inputStream
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
                Assertions.assertThat(response).isInstanceOf(RequestResponseOK.class);
                automaticTransactionId = ((RequestResponseOK<String>) response).getFirstResult();
                assertThat(automaticTransactionId).isNotNull();
                assertThat(automaticTransactionId).isEqualTo("VIRTUAL_TX_" + projectDtoResult.getId());
            }

            // Temporary hack: We're using CollectInternalClient to query "virtual" transaction
            final RequestResponseOK<JsonNode> unitsByTransaction;
            try (CollectInternalClient collectInternalClient = CollectInternalClientFactory.getInstance().getClient()) {
                VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
                unitsByTransaction = collectInternalClient.getUnitsByTransaction(
                    automaticTransactionId,
                    new SelectMultiQuery().getFinalSelect()
                );
            }

            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile("collect/units_with_csv_metadata_and_jslt_transformation.json")
            );
            JsonAssert.assertJsonEquals(
                expectedUnits,
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate()
                    )
                )
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_upload_zip_to_unknown_project() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE_WITH_CSV_METADATA)) {
                assertThatThrownBy(() -> collectClient.uploadZipToProject(vitamContext, "Unknown", inputStream))
                    .isExactlyInstanceOf(CollectExternalClientNotFoundException.class)
                    .hasMessage("Unable to find project Id or invalid status");
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_upload_zip_with_empty_binary_to_transaction_11756() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());
            try (
                InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/zipWithEmptyBinary_11756.zip")
            ) {
                assertThatThrownBy(
                    () ->
                        collectClient.uploadZipToTransaction(
                            vitamContext,
                            transactionDto.getId(),
                            inputStream,
                            null,
                            null
                        )
                )
                    .isExactlyInstanceOf(CollectExternalClientInvalidRequestException.class)
                    .hasMessage(
                        "An unexpected error occurs when try to upload the ZIP: Cannot upload empty file 'A/C.txt'"
                    );
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_zip_to_transaction_with_multi_rattachement() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();
            projectDto.setUnitUp(ATTACHMENT_UNIT_ID);
            List<MetadataUnitUp> metadataUnitUps = new ArrayList<>();
            metadataUnitUps.add(new MetadataUnitUp(GUID_ILE_DE_FRANCE, "Departement", "75"));
            metadataUnitUps.add(new MetadataUnitUp(GUID_ILE_DE_FRANCE, "Departement", "77"));
            metadataUnitUps.add(new MetadataUnitUp(GUID_ILE_DE_FRANCE, "Departement", "78"));
            metadataUnitUps.add(new MetadataUnitUp(GUID_ILE_DE_FRANCE, "Departement", "91"));
            metadataUnitUps.add(new MetadataUnitUp(GUID_ILE_DE_FRANCE, "Departement", "92"));
            metadataUnitUps.add(new MetadataUnitUp(GUID_ILE_DE_FRANCE, "Departement", "93"));
            metadataUnitUps.add(new MetadataUnitUp(GUID_ILE_DE_FRANCE, "Departement", "94"));
            metadataUnitUps.add(new MetadataUnitUp(GUID_ILE_DE_FRANCE, "Departement", "95"));
            projectDto.setUnitUps(metadataUnitUps);
            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

            ProjectDto projectDtoResult = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class
            );

            TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(FILE_ZIP_FILE)) {
                RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDto.getId(),
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }

            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDto.getId(),
                new SelectMultiQuery().getFinalSelect()
            );

            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(UPDATED_UNITS_WITH_DYNAMIC_ATTACHMENT)
            );

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                expectedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate()
                    )
                )
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_unit_with_multi_rattachement() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();
            List<MetadataUnitUp> metadataUnitUps = new ArrayList<>();
            metadataUnitUps.add(new MetadataUnitUp(GUID_ILE_DE_FRANCE, "Departement", "75"));
            projectDto.setUnitUps(metadataUnitUps);
            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

            ProjectDto projectDtoResult = JsonHandler.getFromJsonNode(
                ((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class
            );

            TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());

            ObjectNode unit = JsonHandler.createObjectNode();
            unit.put("Title", UNIT_TITLE);
            unit.put("DescriptionLevel", "Item");
            unit.put("Departement", 75);

            collectClient.uploadArchiveUnit(vitamContext, unit, transactionDto.getId());

            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDto.getId(),
                new SelectMultiQuery().getFinalSelect()
            );

            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(CREATED_UNIT_WITH_DYNAMIC_ATTACHMENT)
            );

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                expectedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.originatingAgencies(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate()
                    )
                )
            );

            int unitUpSize = unitsByTransaction
                .getResults()
                .stream()
                .filter(e -> e.get("Title").asText().equals(UNIT_TITLE))
                .map(e -> e.get(VitamFieldsHelper.unitups()))
                .map(JsonNode::size)
                .reduce(0, Integer::sum);
            assertEquals(1, unitUpSize);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_update_metadata_csv() throws Exception {
        ProjectDto projectDto = initProjectData();
        testUpdateMetadataCsv(projectDto);
    }

    @Test
    @RunWithCustomExecutor
    public void should_update_metadata_csv_with_id() throws Exception {
        ProjectDto projectDto = initProjectData();
        testUpdateMetadataCsvWithId(projectDto);
    }

    @Test
    @RunWithCustomExecutor
    public void should_update_metadata_csv_ignoring_jslt_transformation() throws Exception {
        ProjectDto projectDto = initProjectData();
        projectDto.setTransformationRules(
            """
            {
              "Title": "IGNORED",
              *: .
            }
            """
        );
        testUpdateMetadataCsv(projectDto);
    }

    private void testUpdateMetadataCsv(ProjectDto projectDto) throws Exception {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto project = createProject(vitamContext, projectDto).orElseThrow();
            final TransactionDto transaction = createTransaction(vitamContext, project.getId());

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(UNITS_TO_UPDATE)) {
                final List<Unit> units = JsonHandler.getFromInputStream(inputStream, List.class, Unit.class);
                for (Unit unit : units) {
                    unit.put(Unit.ID, GUIDFactory.newUnitGUID(TENANT_ID).toString());
                    unit.put(Unit.OPI, transaction.getId());
                }
                MetadataCollections.UNIT.<Unit>getCollection().insertMany(units);
                MetadataCollections.UNIT.getEsClient().insertFullDocuments(MetadataCollections.UNIT, TENANT_ID, units);
            }

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(METADATA_FILE)) {
                RequestResponse<JsonNode> response = client.updateUnitsWithCsvMetadata(
                    vitamContext,
                    transaction.getId(),
                    inputStream
                );
                Assert.assertTrue(response.isOk());
            }

            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) client.getUnitsByTransaction(
                vitamContext,
                transaction.getId(),
                new SelectMultiQuery().getFinalSelect()
            );

            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(UNITS_UPDATED_BY_CSV_PATH)
            );

            JsonAssert.assertJsonEquals(
                expectedUnits,
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate()
                    )
                )
            );
        }
    }

    private void testUpdateMetadataCsvWithId(ProjectDto projectDto) throws Exception {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto project = createProject(vitamContext, projectDto).orElseThrow();
            final TransactionDto transaction = createTransaction(vitamContext, project.getId());

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(UNITS_TO_UPDATE)) {
                final List<Unit> units = JsonHandler.getFromInputStream(inputStream, List.class, Unit.class);
                for (Unit unit : units) {
                    unit.put(Unit.OPI, transaction.getId());
                }
                MetadataCollections.UNIT.<Unit>getCollection().insertMany(units);
                MetadataCollections.UNIT.getEsClient().insertFullDocuments(MetadataCollections.UNIT, TENANT_ID, units);
            }

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(METADATA_FILE_WITH_ID)) {
                RequestResponse<JsonNode> response = client.updateUnitsWithCsvMetadata(
                    vitamContext,
                    transaction.getId(),
                    inputStream
                );
                Assert.assertTrue(response.isOk());
            }

            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) client.getUnitsByTransaction(
                vitamContext,
                transaction.getId(),
                new SelectMultiQuery().getFinalSelect()
            );

            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(UNITS_UPDATED_BY_CSV_PATH)
            );

            JsonAssert.assertJsonEquals(
                expectedUnits,
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate()
                    )
                )
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldUpdateTransactionFailWhenCsvLinesAreTooLong() throws Exception {
        final String unitUploadResourcePath = "collect/upload_au_collect.json";
        final String unitUpdateResourcePath = "collect/metadata.csv";
        final ProjectDto project = createProject(vitamContext).orElseThrow();
        final TransactionDto transaction = createTransaction(vitamContext, project.getId());

        assertThat(transaction).isNotNull();
        assertThat(transaction.getId()).isNotBlank();

        CollectTestHelper.uploadUnit(vitamContext, transaction.getId(), unitUploadResourcePath);

        final VitamClientException vitamClientException = assertThrows(
            VitamClientException.class,
            () -> updateUnitWithMetadataCsv(vitamContext, transaction.getId(), unitUpdateResourcePath)
        );

        assertThat(vitamClientException.getLocalizedMessage()).contains("Invalid input bytes length");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldUpdateTransactionFailWhenNotAllowedFileFormatHasMetadata() throws Exception {
        final String unitUploadResourcePath = "collect/upload_au_collect.json";
        final String unitUpdateResourcePath = "collect/transaction/unit/update/metadata.pdf";
        final ProjectDto project = createProject(vitamContext).orElseThrow();
        final TransactionDto transaction = createTransaction(vitamContext, project.getId());

        assertThat(transaction).isNotNull();
        assertThat(transaction.getId()).isNotBlank();

        CollectTestHelper.uploadUnit(vitamContext, transaction.getId(), unitUploadResourcePath);

        final VitamClientException vitamClientException = assertThrows(
            VitamClientException.class,
            () -> updateUnitWithMetadataCsv(vitamContext, transaction.getId(), unitUpdateResourcePath)
        );

        assertThat(vitamClientException.getLocalizedMessage()).contains("Invalid input bytes");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldUpdateTransactionFailWhenCsvContainsWrongFilePath() throws Exception {
        final String unitUpdateResourcePath = "collect/transaction/unit/update/metadata-with-wrong-file-path.csv";
        final String zipPath = "collect/transaction/unit/update/versement.zip";
        final ProjectDto project = createProject(vitamContext).orElseThrow();
        final TransactionDto transaction = createTransaction(vitamContext, project.getId());

        assertThat(transaction).isNotNull();
        assertThat(transaction.getId()).isNotBlank();

        uploadZipTransaction(vitamContext, transaction.getId(), zipPath, null);

        final VitamClientException vitamClientException = assertThrows(
            VitamClientException.class,
            () -> updateUnitWithMetadataCsv(vitamContext, transaction.getId(), unitUpdateResourcePath)
        );

        assertThat(vitamClientException.getLocalizedMessage()).contains(
            "Metadata update failed. Nb OK: 0, Nb KO: 1. Error messages:[No unit matches selection criteria]"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void shouldUpdateTransactionFailWhenCsvContainsObjectFiles() throws Exception {
        final String unitUpdateResourcePath = "collect/transaction/unit/update/metadata-update-with-objectfiles-ko.csv";
        final String zipPath = "collect/transaction/unit/update/versement.zip";
        final ProjectDto project = createProject(vitamContext).orElseThrow();
        final TransactionDto transaction = createTransaction(vitamContext, project.getId());

        assertThat(transaction).isNotNull();
        assertThat(transaction.getId()).isNotBlank();

        uploadZipTransaction(vitamContext, transaction.getId(), zipPath, null);

        final VitamClientException vitamClientException = assertThrows(
            VitamClientException.class,
            () -> updateUnitWithMetadataCsv(vitamContext, transaction.getId(), unitUpdateResourcePath)
        );

        assertThat(vitamClientException.getLocalizedMessage()).contains(
            """
            CSV validation failed. 1 error:
            - Invalid CSV record at line 2 (File="versement"): ObjectFiles field not supported for update operations"""
        );
    }

    @Test
    @RunWithCustomExecutor
    public void shouldUpdateTransactionFailWhenCsvContainsBadDateFormat() throws Exception {
        final String unitUpdateResourcePath = "collect/transaction/unit/update/metadata-with-bad-date-format.csv";
        final String zipPath = "collect/transaction/unit/update/versement.zip";
        final ProjectDto project = createProject(vitamContext).orElseThrow();
        final TransactionDto transaction = createTransaction(vitamContext, project.getId());

        assertThat(transaction).isNotNull();
        assertThat(transaction.getId()).isNotBlank();

        uploadZipTransaction(vitamContext, transaction.getId(), zipPath, null);

        final VitamClientException vitamClientException = assertThrows(
            VitamClientException.class,
            () -> updateUnitWithMetadataCsv(vitamContext, transaction.getId(), unitUpdateResourcePath)
        );

        assertThat(vitamClientException.getLocalizedMessage()).contains(
            "Metadata update failed. Nb OK: 1, Nb KO: 1. Error messages:[metadata contains fields declared in ontology with a wrong format"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void shouldUpdateTransactionFailWhenTransactionIsNotOpen() throws Exception {
        final String unitUpdateResourcePath = "collect/transaction/unit/update/metadata.csv";
        final String zipPath = "collect/transaction/unit/update/versement.zip";
        final ProjectDto project = createProject(vitamContext).orElseThrow();
        final TransactionDto transaction = createTransaction(vitamContext, project.getId());

        assertThat(transaction).isNotNull();
        assertThat(transaction.getId()).isNotBlank();

        uploadZipTransaction(vitamContext, transaction.getId(), zipPath, null);
        closeTransaction(vitamContext, transaction.getId());

        final VitamClientException vitamClientException = assertThrows(
            VitamClientException.class,
            () -> updateUnitWithMetadataCsv(vitamContext, transaction.getId(), unitUpdateResourcePath)
        );

        assertThat(vitamClientException.getLocalizedMessage()).contains("must be OPEN but was READY");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldUpdateMetadataWithJsonlMetadata() throws Exception {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            // Given
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            TransactionDto transaction = createTransaction(vitamContext, projectDtoResult.getId());

            uploadZipTransaction(vitamContext, transaction.getId(), ZIP_FILE, null);

            // When (first update with simple selectors)
            final RequestResponse<JsonNode> response;
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(METADATA_JSONL)) {
                response = client.updateUnitsWithJsonlMetadata(vitamContext, transaction.getId(), inputStream);
            }

            // Then
            Assertions.assertThat(response.isOk()).isTrue();
            Assertions.assertThat(response.getStatus()).isEqualTo(200);

            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) client.getUnitsByTransaction(
                vitamContext,
                transaction.getId(),
                new SelectMultiQuery().getFinalSelect()
            );
            final JsonNode expectedUnits = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(UNITS_UPDATED_WITH_JSONL_BY_ZIP_PATH)
            );

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(unitsByTransaction.getResults()),
                expectedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate(),
                        "[*]." + VitamFieldsHelper.version()
                    )
                )
            );

            // When (Second update with complex selectors)
            final RequestResponse<JsonNode> response2;
            try (
                InputStream inputStream = PropertiesUtils.getResourceAsStream(
                    METADATA_JSONL_UPDATE_WITH_COMPLEX_SELECTORS
                )
            ) {
                response2 = client.updateUnitsWithJsonlMetadata(vitamContext, transaction.getId(), inputStream);
            }

            // Then
            Assertions.assertThat(response2.isOk()).isTrue();
            Assertions.assertThat(response2.getStatus()).isEqualTo(200);

            final RequestResponseOK<JsonNode> unitsByTransaction2 = (RequestResponseOK<
                    JsonNode
                >) client.getUnitsByTransaction(
                vitamContext,
                transaction.getId(),
                new SelectMultiQuery().getFinalSelect()
            );
            final JsonNode expectedUnits2 = JsonHandler.getFromFile(
                PropertiesUtils.getResourceFile(UNITS_UPDATED_WITH_JSONL_WITH_COMPLEX)
            );

            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(unitsByTransaction2.getResults()),
                expectedUnits2,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of(
                        "[*]." + VitamFieldsHelper.id(),
                        "[*]." + VitamFieldsHelper.batchId(),
                        "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(),
                        "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate(),
                        "[*]." + VitamFieldsHelper.version()
                    )
                )
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldUpdateTransactionFailWhenJsonlContainsObjectFiles() throws Exception {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            final String unitUpdateResourcePath =
                "collect/transaction/unit/update/metadata-update-with-objectfiles-ko.jsonl";
            final String zipPath = "collect/transaction/unit/update/versement.zip";
            final ProjectDto project = createProject(vitamContext).orElseThrow();
            final TransactionDto transaction = createTransaction(vitamContext, project.getId());

            assertThat(transaction).isNotNull();
            assertThat(transaction.getId()).isNotBlank();

            uploadZipTransaction(vitamContext, transaction.getId(), zipPath, null);

            // When / Then
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(unitUpdateResourcePath)) {
                assertThatThrownBy(
                    () -> client.updateUnitsWithJsonlMetadata(vitamContext, transaction.getId(), inputStream)
                )
                    .isExactlyInstanceOf(CollectExternalClientInvalidRequestException.class)
                    .hasMessageContaining(
                        "Invalid unit metadata at index: 0. ObjectFiles field not allowed for update operations"
                    );
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldGetBadRequestWhenUpdateMetadataWithInvalidJsonlMetadata() throws Exception {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            // Given
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            TransactionDto transaction = createTransaction(vitamContext, projectDtoResult.getId());

            uploadZipTransaction(vitamContext, transaction.getId(), ZIP_FILE, null);

            // When
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(METADATA_JSONL_BAD_FORMAT)) {
                assertThatThrownBy(
                    () -> client.updateUnitsWithJsonlMetadata(vitamContext, transaction.getId(), inputStream)
                )
                    .isExactlyInstanceOf(CollectExternalClientInvalidRequestException.class)
                    .hasMessageContaining("Invalid unit metadata at index: 0. Empty metadata content");
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldGetBadRequestWhenUpdateMetadataWithJsonlMetadataUnknownFile() throws Exception {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            // Given
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            TransactionDto transaction = createTransaction(vitamContext, projectDtoResult.getId());

            uploadZipTransaction(vitamContext, transaction.getId(), ZIP_FILE, null);

            // When
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(METADATA_JSONL_UNKNOWN_FILE)) {
                assertThatThrownBy(
                    () -> client.updateUnitsWithJsonlMetadata(vitamContext, transaction.getId(), inputStream)
                )
                    .isExactlyInstanceOf(CollectExternalClientInvalidRequestException.class)
                    .hasMessageContaining(
                        "Metadata update failed. Nb OK: 0, Nb KO: 1. Error messages:[No unit matches selection criteria]"
                    );
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkAtomicUpdateTransactionUnits() throws Exception {
        // Given
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto project = createProject(vitamContext).orElseThrow();
            TransactionDto transaction = createTransaction(vitamContext, project.getId());

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/SimpleTreeZip.zip")) {
                RequestResponse<JsonNode> response = client.uploadZipToTransaction(
                    vitamContext,
                    transaction.getId(),
                    inputStream,
                    null,
                    null
                );
                assertThat(response.getStatus()).isEqualTo(200);
            }

            // When
            JsonNode updateQueriesJson = JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream("collect/BulkAtomicUpdateTransactionUnits.json")
            );

            RequestResponseOK<BulkAtomicUpdateResult> unitsByTransaction = client.bulkAtomicUpdateUnits(
                vitamContext,
                transaction.getId(),
                updateQueriesJson
            );

            // Then
            assertThat(unitsByTransaction.getResults()).hasSize(6);

            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
            selectMultiQuery.addQueries(QueryHelper.exists("#id"));
            selectMultiQuery.addUsedProjection("#id", "Title", "Description", "#opi");
            ObjectNode selectAllQuery = selectMultiQuery.getFinalSelect();

            RequestResponseOK<JsonNode> selectedUnits = (RequestResponseOK<JsonNode>) client.getUnitsByTransaction(
                vitamContext,
                transaction.getId(),
                selectAllQuery
            );
            Map<String, JsonNode> unitsByTitle = selectedUnits
                .getResults()
                .stream()
                .collect(Collectors.toMap(unit -> unit.get("Title").asText(), unit -> unit));

            assertThat(unitsByTitle).hasSize(4);
            assertThat(unitsByTitle.get("UnitA").get("Description").asText()).isEqualTo("Description A");
            assertThat(unitsByTitle.get("UnitB").get("Description").asText()).isEqualTo("Description B");
            assertThat(unitsByTitle.get("UnitC").get("Description").asText()).isEqualTo("Description C");
            assertThat(unitsByTitle.get("UnitD").get("#opi").asText()).isEqualTo(transaction.getId());

            assertThat(unitsByTransaction.getResults())
                .extracting(BulkAtomicUpdateResult::getUpdatedUnitId, BulkAtomicUpdateResult::getStatus)
                .containsExactly(
                    Tuple.tuple(unitsByTitle.get("UnitA").get("#id").asText(), BulkAtomicUpdateStatus.OK),
                    Tuple.tuple(unitsByTitle.get("UnitB").get("#id").asText(), BulkAtomicUpdateStatus.OK),
                    Tuple.tuple(unitsByTitle.get("UnitC").get("#id").asText(), BulkAtomicUpdateStatus.OK),
                    Tuple.tuple(null, BulkAtomicUpdateStatus.KO),
                    Tuple.tuple(null, BulkAtomicUpdateStatus.KO),
                    Tuple.tuple(null, BulkAtomicUpdateStatus.KO)
                );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkAtomicUpdateTransactionUnitsWithExceededThreshold() throws Exception {
        // Given
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto project = createProject(vitamContext).orElseThrow();
            TransactionDto transaction = createTransaction(vitamContext, project.getId());

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/SimpleTreeZip.zip")) {
                RequestResponse<JsonNode> response = client.uploadZipToTransaction(
                    vitamContext,
                    transaction.getId(),
                    inputStream,
                    null,
                    null
                );
                assertThat(response.getStatus()).isEqualTo(200);
            }

            // When
            JsonNode updateQueriesJson = JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream("collect/BulkAtomicUpdateTransactionUnitsThreshold.json")
            );

            assertThatThrownBy(() -> client.bulkAtomicUpdateUnits(vitamContext, transaction.getId(), updateQueriesJson))
                // Then
                .isInstanceOf(CollectExternalClientInvalidRequestException.class)
                .hasMessage("Too many update queries. Threshold exceeded.");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_zip_to_transaction_with_specific_attachement() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            //First collect ingest
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE_WITH_CSV_METADATA)) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDto.getId(),
                    inputStream,
                    null,
                    null
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }
            final RequestResponseOK<JsonNode> unitsByTransaction = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(
                vitamContext,
                transactionDto.getId(),
                new SelectMultiQuery().getFinalSelect()
            );

            assertThat(unitsByTransaction.getResults()).hasSize(13);

            // Get the id of an ingested unit in first collect ingest
            String attachementUnitId = getUnitIdByTitle(unitsByTransaction, "Saint-Denis-Basilique");

            try (
                InputStream inputStream = PropertiesUtils.getResourceAsStream(
                    ZIP_FILE_FOR_ATTACHEMENT_WITH_CSV_METADATA
                )
            ) {
                final RequestResponse<JsonNode> response = collectClient.uploadZipToTransaction(
                    vitamContext,
                    transactionDto.getId(),
                    inputStream,
                    null,
                    attachementUnitId
                );
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }

            SelectMultiQuery query = new SelectMultiQuery();
            query.addQueries(QueryHelper.eq(VitamFieldsHelper.allunitups(), attachementUnitId));

            final RequestResponseOK<JsonNode> unitsByTransactionAttachement = (RequestResponseOK<
                    JsonNode
                >) collectClient.getUnitsByTransaction(vitamContext, transactionDto.getId(), query.getFinalSelect());

            // Expect 3 child units from first upload + 6 from new upload
            assertThat(unitsByTransactionAttachement.getResults()).hasSize(6 + 3);

            //Check that all units contains attachement id
            for (JsonNode unitJsonNode : unitsByTransactionAttachement.getResults()) {
                Assert.assertTrue(
                    unitJsonNode.get(VitamFieldsHelper.allunitups()).toString().contains(attachementUnitId)
                );
            }
            Optional<JsonNode> unitWithObjectFiles = unitsByTransactionAttachement
                .getResults()
                .stream()
                .filter(unit -> unit.get("Title").asText().equals("Aubervillier"))
                .findFirst();
            assertThat(unitWithObjectFiles.isPresent()).isTrue();
            assertThat(unitWithObjectFiles.get().hasNonNull(VitamFieldsHelper.object())).isTrue();
            assertThat(unitWithObjectFiles.get().get("DescriptionLevel").asText()).isEqualTo("Item");
            String unitId = unitWithObjectFiles.get().get(VitamFieldsHelper.id()).asText();
            String objectGroupId = unitWithObjectFiles.get().get(VitamFieldsHelper.object()).asText();
            JsonNode objectGroup = getObjectGroupById(collectClient, objectGroupId);
            // FIXME: Should be "#unitups" instead of "_up"
            List<String> objectGroupParents = StreamSupport.stream(objectGroup.get("_up").spliterator(), false)
                .map(JsonNode::textValue)
                .toList();
            assertThat(objectGroupParents).containsExactly(unitId);
            assertUnitObjectGroupContent(collectClient, unitId, "Aubervillier");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_upload_zip_to_transaction_with_specific_attachement_fail_when_unit_not_exists()
        throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto projectDtoResult = createProjectWithAttachement();
            final TransactionDto transactionDto = createTransaction(vitamContext, projectDtoResult.getId());

            String attachementUnitId = "unknown_attachement_unit_id";

            try (
                InputStream inputStream = PropertiesUtils.getResourceAsStream(
                    ZIP_FILE_FOR_ATTACHEMENT_WITH_CSV_METADATA
                )
            ) {
                assertThatThrownBy(
                    () ->
                        collectClient.uploadZipToTransaction(
                            vitamContext,
                            transactionDto.getId(),
                            inputStream,
                            null,
                            attachementUnitId
                        )
                )
                    .isInstanceOf(CollectExternalClientInvalidRequestException.class)
                    .hasMessageContaining("No such unit with id 'unknown_attachement_unit_id' in the transaction");
            }
        }
    }

    private ProjectDto createProjectWithAttachement() {
        final ProjectDto projectDto = initProjectDataWithAttachment();
        return createProject(vitamContext, projectDto).orElseThrow();
    }

    private static ProjectDto initProjectDataWithAttachment() {
        final ProjectDto projectDto = initProjectData();
        projectDto.setUnitUp(ATTACHMENT_UNIT_ID);
        return projectDto;
    }

    private static String getUnitIdByTitle(RequestResponseOK<JsonNode> unitsByTransaction, String title) {
        return getUnitByTitle(unitsByTransaction, title).get(VitamFieldsHelper.id()).asText();
    }

    private static JsonNode getUnitByTitle(RequestResponseOK<JsonNode> unitsByTransaction, String title) {
        return unitsByTransaction
            .getResults()
            .stream()
            .filter(unit -> unit.has("Title") && unit.get("Title").asText().equals(title))
            .findFirst()
            .orElseThrow();
    }

    private JsonNode getObjectGroupById(CollectExternalClient collectClient, String saintLazareObjectGroupId)
        throws VitamClientException {
        JsonNode objectGroup =
            ((RequestResponseOK<JsonNode>) collectClient.getObjectById(
                    vitamContext,
                    saintLazareObjectGroupId
                )).getFirstResult();
        assertThat(objectGroup).isNotNull();
        return objectGroup;
    }

    private void assertUnitObjectGroupContent(CollectExternalClient collectClient, String unitId, String textContent)
        throws VitamClientException {
        Response response = collectClient.getObjectStreamByUnitId(
            vitamContext,
            unitId,
            DataObjectVersionType.BINARY_MASTER.getName(),
            1
        );

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(InputStream.class)).hasContent(textContent);
    }
}
