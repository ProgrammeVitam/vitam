/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

package fr.gouv.vitam.audit.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.audit.AuditReferentialOptions;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.Assert.fail;

public class ReferentialAuditIT extends VitamRuleRunner {

    private static final String PROFILE_FILE = "integration-ingest-internal/OK_profil.json";
    private static final String AUP_FILE = "integration-ingest-internal/archive-unit-profile.json";

    private static final Integer TENANT_ID = 0;

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(ReferentialAuditIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(
                AdminManagementMain.class,
                WorkspaceMain.class,
                StorageMain.class,
                DefaultOfferMain.class
            ));
    
    @Test
    @RunWithCustomExecutor
    public void should_run_audit_on_profile() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // import profil
        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            JsonNode profilesJsonNode =
                JsonHandler.getFromFile(PropertiesUtils.getResourceFile(PROFILE_FILE));
            List<ProfileModel> listProfiles = JsonHandler.getFromJsonNode(profilesJsonNode, new TypeReference<>() {
            });
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
            adminManagementClient.createProfiles(listProfiles);
        } catch (FileNotFoundException | ReferentialException | InvalidParseOperationException e) {
            fail("Error while creating Profile : " + e.getMessage());
        }
        // launch audit
        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
            adminManagementClient
                .launchReferentialAudit(new AuditReferentialOptions(FunctionalAdminCollections.PROFILE.name()));
        } catch (AdminManagementClientServerException e) {
            fail(String.format("Error on running audit on admin collection %s",
                FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE.name()));
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_run_audit_on_AUP() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            JsonNode profilesJsonNode =
                JsonHandler.getFromFile(PropertiesUtils.getResourceFile(AUP_FILE));
            List<ArchiveUnitProfileModel> archiveUnitProfiles =
                JsonHandler.getFromJsonNode(profilesJsonNode, new TypeReference<>() {
                });
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
            adminManagementClient.createArchiveUnitProfiles(archiveUnitProfiles);
        } catch (FileNotFoundException | ReferentialException | InvalidParseOperationException e) {
            fail("Error while creating AUP : " + e.getMessage());
        }
        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
            adminManagementClient.launchReferentialAudit(
                new AuditReferentialOptions(FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE.name()));
        } catch (AdminManagementClientServerException e) {
            fail(String.format("Error on running audit on admin collection %s",
                FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE.name()));
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }
}
