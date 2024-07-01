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

package fr.gouv.vitam.referential;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientBadRequestException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
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
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccessContractsIT extends VitamRuleRunner {

    private static final int TENANT_0 = 0;
    private static final String CONTRACT_RULE_ID = "contract_rule";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        RulesIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            AdminManagementMain.class,
            ProcessManagementMain.class,
            BatchReportMain.class
        )
    );

    @BeforeClass
    public static void beforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void afterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
    }

    @After
    public void tearDown() {
        runAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void should_desactivate_then_activate_access_contract() {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Update updateQuery = new Update();
            updateQuery.addActions(
                UpdateActionHelper.set("Status", "INACTIVE"),
                UpdateActionHelper.set("DeactivationDate", LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now())),
                UpdateActionHelper.unset("ActivationDate")
            );
            RequestResponse<AccessContractModel> updatedAccessContract = client.updateAccessContract(
                "aName",
                updateQuery.getFinalUpdate()
            );
            assertTrue(updatedAccessContract.isOk());

            RequestResponse<AccessContractModel> contract = client.findAccessContractsByID("aName");
            assertTrue(contract.isOk());
            List<JsonNode> results = RequestResponseOK.getFromJsonNode(contract.toJsonNode()).getResults();
            assertEquals(1, results.size());
            assertEquals(
                ActivationStatus.INACTIVE,
                JsonHandler.getFromJsonNode(results.get(0), AccessContractModel.class).getStatus()
            );

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
            updateQuery.reset();
            updateQuery.addActions(
                UpdateActionHelper.set("Status", "ACTIVE"),
                UpdateActionHelper.set("DeactivationDate", ""),
                UpdateActionHelper.set("ActivationDate", LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            );
            assertThatCode(() -> client.updateAccessContract("aName", updateQuery.getFinalUpdate())).isInstanceOf(
                AdminManagementClientBadRequestException.class
            );
        } catch (
            ReferentialNotFoundException
            | InvalidParseOperationException
            | AdminManagementClientServerException
            | InvalidCreateOperationException e
        ) {
            fail("Error retreiving access contract", e);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_import_access_contract_having_hold_rule_filter() {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse<AccessContractModel> contract = client.findAccessContractsByID(CONTRACT_RULE_ID);
            assertTrue(contract.isOk());
            List<JsonNode> results = RequestResponseOK.getFromJsonNode(contract.toJsonNode()).getResults();
            assertEquals(1, results.size());
            assertThat(
                JsonHandler.getFromJsonNode(results.get(0), AccessContractModel.class).getRuleCategoryToFilter()
            ).contains(RuleType.HoldRule);
        } catch (
            ReferentialNotFoundException | InvalidParseOperationException | AdminManagementClientServerException e
        ) {
            fail("Error retreiving access contract", e);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_update_access_contract_with_hold_rule_filter() {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Update updateMultiQuery = new Update();
            updateMultiQuery.setQuery(QueryHelper.in(AccessContractModel.TAG_NAME, "aName"));
            updateMultiQuery
                .getActions()
                .add(new SetAction(AccessContractModel.RULE_CATEGORY_TO_FILTER, List.of(RuleType.HoldRule.name())));
            client.updateAccessContract("aName", updateMultiQuery.getFinalUpdate());

            RequestResponse<AccessContractModel> contract = client.findAccessContractsByID("aName");
            assertTrue(contract.isOk());
            List<JsonNode> results = RequestResponseOK.getFromJsonNode(contract.toJsonNode()).getResults();
            assertEquals(1, results.size());
            assertThat(
                JsonHandler.getFromJsonNode(results.get(0), AccessContractModel.class).getRuleCategoryToFilter()
            ).contains(RuleType.HoldRule);
        } catch (
            ReferentialNotFoundException
            | InvalidParseOperationException
            | AdminManagementClientServerException
            | InvalidCreateOperationException e
        ) {
            fail("Error retreiving access contract", e);
        }
    }
}
