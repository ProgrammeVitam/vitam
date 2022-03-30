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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.collect.external.client.CollectClient;
import fr.gouv.vitam.collect.external.client.CollectClientFactory;
import fr.gouv.vitam.collect.internal.CollectMain;
import fr.gouv.vitam.collect.internal.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.helpers.builders.TransactionDtoBuilder;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CollectIT extends VitamRuleRunner {

    private static String transactionGuuid;
    private static String unitGuuid;
    private static String usage = DataObjectVersionType.BINARY_MASTER.getName();
    private static Integer version = 1;
    private ObjectMapper mapper = new ObjectMapper();

    private static final Integer tenantId = 0;
    private static final String APPLICATION_SESSION_ID = "ApplicationSessionId";
    private static final String ACCESS_CONTRACT = "aName3";

    private static final String JSON_NODE_UNIT = "collect/upload_au_collect.json";
    private static final String JSON_NODE_OBJECT = "collect/upload_got_collect.json";
    private static final String BINARY_FILE = "collect/Plan-Barbusse.txt";

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(CollectIT.class, mongoRule.getMongoDatabase().getName(),
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
                AccessExternalMain.class,
                IngestExternalMain.class,
                CollectMain.class));

    private static CollectClient collectClient;
    private static AdminExternalClient adminExternalClient;
    private static AccessExternalClient accessExternalClient;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        collectClient = CollectClientFactory.getInstance().getClient();
        adminExternalClient = AdminExternalClientFactory.getInstance().getClient();
        accessExternalClient = AccessExternalClientFactory.getInstance().getClient();

        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }


    @RunWithCustomExecutor
    @Test
    public void test1_transaction_ok() throws Exception {

        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        TransactionDto transactionDto = new TransactionDtoBuilder()
            .withArchivalAgencyIdentifier("ArchivalAgencyIdentifier")
            .withTransferingAgencyIdentifier("TransferingAgencyIdentifier")
            .withOriginatingAgencyIdentifier("FRAN_NP_009913")
            .withArchivalProfile("ArchiveProfile")
            .withComment("Comments")
            .build();
        RequestResponse<JsonNode> response = collectClient.initTransaction(transactionDto);
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) response;
        TransactionDto transactionDtoResult =
            mapper.readValue(requestResponseOK.getFirstResult().toString(), TransactionDto.class);
        transactionGuuid = transactionDtoResult.getId();
    }

    @RunWithCustomExecutor
    @Test
    public void test2_upload_unit() throws Exception {
        JsonNode archiveUnitJson = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(JSON_NODE_UNIT));
        RequestResponseOK<JsonNode> response = collectClient.uploadArchiveUnit(transactionGuuid, archiveUnitJson);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getFirstResult()).isNotNull();
        assertThat(response.getFirstResult().get("#id")).isNotNull();
        unitGuuid = response.getFirstResult().get("#id").textValue();

    }

    @RunWithCustomExecutor
    @Test
    public void test3_upload_got() throws Exception {
        JsonNode gotJson = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(JSON_NODE_OBJECT));
        RequestResponseOK<JsonNode> response = collectClient.addObjectGroup(unitGuuid, usage, version, gotJson);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getFirstResult()).isNotNull();
        assertThat(response.getFirstResult().get("id")).isNotNull();
    }

    @RunWithCustomExecutor
    @Test
    public void test4_upload_binary() throws Exception {
        try (InputStream inputStream =
            PropertiesUtils.getResourceAsStream(BINARY_FILE)) {
            Response response = collectClient.addBinary(unitGuuid, usage, version, inputStream);
            Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void test5_close_transaction() throws Exception {
        Response response = collectClient.closeTransaction(transactionGuuid);
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
    }

    @RunWithCustomExecutor
    @Test
    public void test5_ingest() throws Exception {
        RequestResponseOK<JsonNode> response = collectClient.ingest(transactionGuuid);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getFirstResult()).isNotNull();
        assertThat(response.getFirstResult().get("id")).isNotNull();
        String operationId = response.getFirstResult().get("id").textValue();

    }

}
