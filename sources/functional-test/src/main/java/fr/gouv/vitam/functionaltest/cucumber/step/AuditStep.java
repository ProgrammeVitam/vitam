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
package fr.gouv.vitam.functionaltest.cucumber.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response.Status;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

/**
 * AuditStep class
 */
public class AuditStep extends CommonStep {

    private static final String AUDIT_QUERY = "{auditActions:\"%s\",auditType:\"originatingagency\",objectId:\"%s\"}";

    private Status auditStatus;

    public AuditStep(World world) {
        super(world);
    }

    @When("^je lance un audit de cohérence$")
    public void evidenceAudit() throws VitamException {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        vitamContext.setAccessContract(world.getContractId());

        String query = world.getQuery();
        JsonNode queryString = JsonHandler.getFromString(query);

        RequestResponse<JsonNode> response = world.getAdminClient().evidenceAudit(vitamContext, queryString);

        assertThat(response.isOk()).isTrue();

        final String operationId = response.getHeaderString(X_REQUEST_ID);
        world.setOperationId(operationId);

        final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
        boolean processTimeout = vitamPoolingClient.wait(
            world.getTenantId(),
            operationId,
            ProcessState.COMPLETED,
            100,
            1_000L,
            TimeUnit.MILLISECONDS
        );

        if (!processTimeout) {
            fail("audit processing not finished. Timeout exceeded.");
        }

        assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
    }

    @When("^je lance un audit rectificatif sur l'operation (.*)")
    public void rectificationAudit(String id) throws VitamException {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        vitamContext.setAccessContract(world.getContractId());

        RequestResponse<JsonNode> response = world.getAdminClient().rectificationAudit(vitamContext, id);

        assertThat(response.isOk()).isTrue();

        final String operationId = response.getHeaderString(X_REQUEST_ID);
        world.setOperationId(operationId);

        final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
        boolean processTimeout = vitamPoolingClient.wait(
            world.getTenantId(),
            operationId,
            ProcessState.COMPLETED,
            100,
            1_000L,
            TimeUnit.MILLISECONDS
        );

        if (!processTimeout) {
            fail("dip processing not finished. Timeout exceeded.");
        }

        assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
    }

    @When("^je veux faire un audit sur (.*) des objets par service producteur \"([^\"]*)\"$")
    public void je_lance_l_audit_en_service_producteur(String action, String originatingAgnecy) throws Throwable {
        auditStatus = null;
        String auditActions = getAuditAction(action);
        String QUERY = String.format(AUDIT_QUERY, auditActions, originatingAgnecy);

        JsonNode auditOption = JsonHandler.getFromString(QUERY);
        VitamContext vitamContext = new VitamContext(world.getTenantId())
            .setAccessContract(world.getContractId())
            .setApplicationSessionId(world.getApplicationSessionId());

        RequestResponse<JsonNode> response = world.getAdminClient().launchAudit(vitamContext, auditOption);
        assertThat(response.isOk()).isTrue();
        auditStatus = Status.ACCEPTED;
    }

    @When("^je veux faire un audit sur (.*) des objets par tenant (\\d+)$")
    public void je_veux_faire_l_audit_des_objets_de_tenant(String action, int tenant) throws Throwable {
        auditStatus = null;
        String auditActions = getAuditAction(action);
        String QUERY = String.format(AUDIT_QUERY, auditActions, tenant);

        JsonNode auditOption = JsonHandler.getFromString(QUERY);
        VitamContext vitamContext = new VitamContext(world.getTenantId())
            .setAccessContract(world.getContractId())
            .setApplicationSessionId(world.getApplicationSessionId());

        RequestResponse<JsonNode> response = world.getAdminClient().launchAudit(vitamContext, auditOption);
        assertThat(response.isOk()).isTrue();
        auditStatus = Status.ACCEPTED;
    }

    @When("^je veux faire un audit sur (.*) des objets liés aux unités archivistiques de la requête$")
    public void je_veux_faire_l_audit_des_objets_par_requete(String action) throws Throwable {
        auditStatus = null;
        JsonNode query = JsonHandler.getFromString(world.getQuery());
        String auditActions = getAuditAction(action);

        ObjectNode auditOption = JsonHandler.createObjectNode();
        auditOption.put("auditActions", auditActions);
        auditOption.put("auditType", "dsl");
        auditOption.set("query", query);

        VitamContext vitamContext = new VitamContext(world.getTenantId())
            .setAccessContract(world.getContractId())
            .setApplicationSessionId(world.getApplicationSessionId());

        RequestResponse<JsonNode> response = world.getAdminClient().launchAudit(vitamContext, auditOption);
        assertThat(response.isOk()).isTrue();
        auditStatus = Status.ACCEPTED;
    }

    @Then("^le réultat de l'audit est succès$")
    public void audit_result_is_success() {
        assertThat(auditStatus.getStatusCode()).isEqualTo(202);
    }

    @Nonnull
    private String getAuditAction(String action) {
        String auditActions;
        switch (action) {
            case "l'existence":
                auditActions = "AUDIT_FILE_EXISTING";
                break;
            case "l'intégrité":
                auditActions = "AUDIT_FILE_INTEGRITY";
                break;
            default:
                throw new UnsupportedOperationException("Unknown action " + action);
        }
        return auditActions;
    }
}
