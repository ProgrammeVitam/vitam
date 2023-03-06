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
import com.google.common.collect.Sets;
import fr.gouv.vitam.collect.common.dto.MetadataUnitUp;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.collect.external.external.rest.CollectExternalMain;
import fr.gouv.vitam.collect.internal.CollectInternalMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.collect.ProjectIT.initProjectData;
import static fr.gouv.vitam.collect.TransactionIT.initTransaction;

@Ignore
public class FluxIT extends VitamRuleRunner {

    private static final String GUID_ILE_DE_FRANCE = "aeaqaaaaaahb5rlnaat5yamglule7mqaaabq";

    @ClassRule public static VitamServerRunner runner =
        new VitamServerRunner(FluxIT.class, mongoRule.getMongoDatabase().getName(), ElasticsearchRule.getClusterName(),
            Sets.newHashSet(MetadataMain.class, AdminManagementMain.class, LogbookMain.class, WorkspaceMain.class,
                CollectInternalMain.class, CollectExternalMain.class));

    private static final Integer TENANT_ID = 0;
    private static final String UNITS_UPDATED_BY_ZIP_PATH = "collect/units_with_description.json";
    private static final String UPDATED_UNITS_WITH_DYNAMIC_ATTACHMENT =
        "collect/updated_units_with_dynamic_attachment.json";
    private static final String ZIP_FILE = "collect/sampleStream.zip";
    private static final String FILE_ZIP_FILE = "collect/file.zip";
    private static final String UNITS_TO_UPDATE = "collect/updateMetadata/units.json";
    private static final String UNITS_UPDATED_BY_CSV_PATH = "collect/updateMetadata/units_updated.json";
    private static final String METADATA_FILE = "collect/updateMetadata/metadata.csv";
    private final static String ATTACHEMENT_UNIT_ID = "aeeaaaaaaceevqftaammeamaqvje33aaaaaq";
    private final VitamContext vitamContext = new VitamContext(TENANT_ID);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        runner.startMetadataCollectServer();
        runner.startWorkspaceCollectServer();
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        runner.stopMetadataCollectServer(false);
        runner.stopWorkspaceCollectServer();
        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    public void should_upload_project_zip() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();
            projectDto.setUnitUp(ATTACHEMENT_UNIT_ID);


            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

            ProjectDto projectDtoResult =
                JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                    ProjectDto.class);

            TransactionDto transactiondto = initTransaction();

            RequestResponse<JsonNode> transactionResponse =
                collectClient.initTransaction(vitamContext, transactiondto, projectDtoResult.getId());
            Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult =
                JsonHandler.getFromJsonNode(requestResponseOK.getFirstResult(), TransactionDto.class);
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE)) {
                RequestResponse<JsonNode> response =
                    collectClient.uploadProjectZip(vitamContext, transactionDtoResult.getId(), inputStream);
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }


            final RequestResponseOK<JsonNode> unitsByTransaction =
                (RequestResponseOK<JsonNode>) collectClient.getUnitsByTransaction(vitamContext,
                    transactionDtoResult.getId(), new SelectMultiQuery().getFinalSelect());


            final JsonNode expectedUnits =
                JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UNITS_UPDATED_BY_ZIP_PATH));

            JsonAssert.assertJsonEquals(JsonHandler.toJsonNode(unitsByTransaction.getResults()), expectedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of("[*]." + VitamFieldsHelper.id(), "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(), "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate())));
        }
    }

    @Test
    public void should_upload_project_zip_with_multi_rattachement() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();
            projectDto.setUnitUp(ATTACHEMENT_UNIT_ID);
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

            ProjectDto projectDtoResult =
                JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                    ProjectDto.class);

            TransactionDto transactiondto = initTransaction();

            RequestResponse<JsonNode> transactionResponse =
                collectClient.initTransaction(vitamContext, transactiondto, projectDtoResult.getId());
            Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult =
                JsonHandler.getFromJsonNode(requestResponseOK.getFirstResult(), TransactionDto.class);
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(FILE_ZIP_FILE)) {
                RequestResponse<JsonNode> response =
                    collectClient.uploadProjectZip(vitamContext, transactionDtoResult.getId(), inputStream);
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }

            final RequestResponseOK<JsonNode> unitsByTransaction =
                (RequestResponseOK<JsonNode>) collectClient.getUnitsByTransaction(vitamContext,
                    transactionDtoResult.getId(), new SelectMultiQuery().getFinalSelect());


            final JsonNode expectedUnits =
                JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UPDATED_UNITS_WITH_DYNAMIC_ATTACHMENT));

            JsonAssert.assertJsonEquals(JsonHandler.toJsonNode(unitsByTransaction.getResults()), expectedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of("[*]." + VitamFieldsHelper.id(), "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(), "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate())));
        }
    }

    @Test
    public void should_update_metadata() throws Exception {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();

            final RequestResponse<JsonNode> projectResponse = client.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

            ProjectDto projectDtoResult =
                JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                    ProjectDto.class);

            TransactionDto transactiondto = initTransaction();

            RequestResponse<JsonNode> transactionResponse =
                client.initTransaction(vitamContext, transactiondto, projectDtoResult.getId());
            Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult =
                JsonHandler.getFromJsonNode(requestResponseOK.getFirstResult(), TransactionDto.class);

            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(UNITS_TO_UPDATE)) {
                final List<Unit> units = JsonHandler.getFromInputStream(inputStream, List.class, Unit.class);
                for (Unit unit : units) {
                    unit.put(Unit.OPI, transactionDtoResult.getId());
                }
                MetadataCollections.UNIT.<Unit>getCollection().insertMany(units);
                MetadataCollections.UNIT.getEsClient().insertFullDocuments(MetadataCollections.UNIT, TENANT_ID, units);
            }


            try (InputStream inputStream = PropertiesUtils.getResourceAsStream(METADATA_FILE)) {
                RequestResponse<JsonNode> response =
                    client.updateUnits(vitamContext, transactionDtoResult.getId(), inputStream);
                Assert.assertTrue(response.isOk());
            }

            final RequestResponseOK<JsonNode> unitsByTransaction =
                (RequestResponseOK<JsonNode>) client.getUnitsByTransaction(vitamContext, transactionDtoResult.getId(),
                    new SelectMultiQuery().getFinalSelect());

            final JsonNode expectedUnits =
                JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UNITS_UPDATED_BY_CSV_PATH));

            JsonAssert.assertJsonEquals(JsonHandler.toJsonNode(unitsByTransaction.getResults()), expectedUnits,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                    List.of("[*]." + VitamFieldsHelper.id(), "[*]." + VitamFieldsHelper.unitups(),
                        "[*]." + VitamFieldsHelper.object(), "[*]." + VitamFieldsHelper.allunitups(),
                        "[*]." + VitamFieldsHelper.initialOperation(),
                        "[*]." + VitamFieldsHelper.approximateCreationDate(),
                        "[*]." + VitamFieldsHelper.approximateUpdateDate())));
        }
    }
}