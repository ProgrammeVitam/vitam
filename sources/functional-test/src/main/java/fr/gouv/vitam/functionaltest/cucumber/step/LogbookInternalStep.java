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
import cucumber.api.java.en.When;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.model.TenantLogbookOperationTraceabilityResult;
import fr.gouv.vitam.logbook.common.parameters.Contexts;

import java.util.Collections;
import java.util.concurrent.CompletionException;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class LogbookInternalStep extends CommonStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookInternalStep.class);

    public LogbookInternalStep(World world) {
        super(world);
    }

    /**
     * call vitam to generate a secured logbook
     */
    @When("^je génère un journal des opérations sécurisé")
    public void generate_secured_logbook() {
        runInVitamThread(() -> {
            try {
                VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
                VitamThreadUtils.getVitamSession().setContractId(world.getContractId());
                RequestResponseOK<TenantLogbookOperationTraceabilityResult> response = world
                    .getLogbookOperationsClient()
                    .traceability(Collections.singletonList(world.getTenantId()));

                VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());

                String operationId;
                if (response.getResults().isEmpty() || response.getResults().get(0).getOperationId() == null) {
                    LOGGER.info("no need to run traceability");
                    RequestResponse<JsonNode> logbookResponse = world
                        .getLogbookOperationsClient()
                        .getLastOperationByType("STP_OP_SECURISATION");
                    if (!logbookResponse.isOk()) {
                        fail("no traceability operation found");
                        return;
                    }
                    RequestResponseOK<JsonNode> logbook = (RequestResponseOK<JsonNode>) logbookResponse;
                    if (logbook.getFirstResult() == null) {
                        fail("no traceability operation found");
                        return;
                    }

                    operationId = logbook.getFirstResult().get(VitamFieldsHelper.id()).asText();
                } else {
                    operationId = response.getResults().get(0).getOperationId();
                }

                world.setOperationId(operationId);
                assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * call vitam to generate a secured logbook
     */
    @When("^je génère un journal des cycles de vie des unités archivistiques sécurisé")
    public void generate_secured_lfc_unit() {
        runInVitamThread(() -> {
            VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());
            String operationId;

            RequestResponseOK<String> response = world.getLogbookOperationsClient().traceabilityLfcUnit();

            if (response.getResults().isEmpty()) {
                LOGGER.info("no need to run traceability");
                RequestResponse<JsonNode> logbookResponse = world
                    .getLogbookOperationsClient()
                    .getLastOperationByType(Contexts.UNIT_LFC_TRACEABILITY.getEventType());
                if (!logbookResponse.isOk()) {
                    fail("no traceability operation found");
                    return;
                }
                RequestResponseOK<JsonNode> logbook = (RequestResponseOK<JsonNode>) logbookResponse;
                if (logbook.getFirstResult() == null) {
                    fail("no traceability operation found");
                    return;
                }

                operationId = logbook.getFirstResult().get(VitamFieldsHelper.id()).asText();
            } else {
                checkTraceabilityLfcResponseOKOrWarn(response);

                operationId = response.getResults().get(0);
            }
            world.setOperationId(operationId);
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
        });
    }

    /**
     * call vitam to generate a secured logbook
     */
    @When("^je génère un journal des cycles de vie des groupes d'objets sécurisé")
    public void generate_secured_lfc_objectgroup() {
        runInVitamThread(() -> {
            String operationId;
            VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());

            RequestResponseOK<String> response = world.getLogbookOperationsClient().traceabilityLfcObjectGroup();

            if (response.getResults().isEmpty()) {
                LOGGER.info("no need to run traceability");
                RequestResponse<JsonNode> logbookResponse = world
                    .getLogbookOperationsClient()
                    .getLastOperationByType(Contexts.OBJECTGROUP_LFC_TRACEABILITY.getEventType());
                if (!logbookResponse.isOk()) {
                    fail("no traceability operation found");
                    return;
                }
                RequestResponseOK<JsonNode> logbook = (RequestResponseOK<JsonNode>) logbookResponse;
                if (logbook.getFirstResult() == null) {
                    fail("no traceability operation found");
                    return;
                }

                operationId = logbook.getFirstResult().get(VitamFieldsHelper.id()).asText();
            } else {
                checkTraceabilityLfcResponseOKOrWarn(response);

                operationId = response.getResults().get(0);
            }
            world.setOperationId(operationId);
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
        });
    }

    private void checkTraceabilityLfcResponseOKOrWarn(RequestResponseOK<String> requestResponseOK)
        throws VitamException {
        assertThat(requestResponseOK.isOk()).isTrue();

        final String traceabilityOperationId = requestResponseOK.getHeaderString(GlobalDataRest.X_REQUEST_ID);

        checkOperationStatus(traceabilityOperationId, StatusCode.OK, StatusCode.WARNING);
    }
}
