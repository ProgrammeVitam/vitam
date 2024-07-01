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
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;

import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

/**
 * BulkAtomicUpdateStep class
 */
public class BulkAtomicUpdateStep extends CommonStep {

    public BulkAtomicUpdateStep(World world) {
        super(world);
    }

    @When("^je lance la mise à jour unitaire de masse des units$")
    public void bulkAtomicUpdate() throws VitamException {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        vitamContext.setAccessContract(world.getContractId());

        String query = world.getQuery();
        JsonNode queryString = JsonHandler.getFromString(query);
        final RequestResponse<JsonNode> requestResponse = world
            .getAccessClient()
            .bulkAtomicUpdateUnits(vitamContext, queryString);

        assertThat(requestResponse.isOk()).isTrue();

        final String operationId = requestResponse.getHeaderString(X_REQUEST_ID);
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
            fail("units update  processing not finished. Timeout exceeded.");
        }

        assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
    }
}
