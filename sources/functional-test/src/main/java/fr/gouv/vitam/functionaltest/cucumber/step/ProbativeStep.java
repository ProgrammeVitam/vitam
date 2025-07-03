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
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import io.cucumber.java.en.Then;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

public class ProbativeStep extends CommonStep {

    private String lastProbativeValueOperationId;

    public ProbativeStep(World world) {
        super(world);
    }

    @Then("^Je lance un rélevé de valeur probante avec l'usage suivant (.*)")
    public void probativeValue(String usage) throws Exception {
        this.probativeValue(usage, false);
    }

    @Then(
        "^Je lance un rélevé de valeur probante étendu aux éléments de preuves de signature électronique avec l'usage suivant (.*)"
    )
    public void probativeValueIncludeDetachedSigningInformation(String usage) throws Exception {
        this.probativeValue(usage, true);
    }

    private void probativeValue(String usage, boolean includeDetachedSigningInformation) throws Exception {
        JsonNode query = JsonHandler.getFromString(world.getQuery());
        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(
            query,
            usage,
            "1",
            includeDetachedSigningInformation
        );

        RequestResponse response = world
            .getAdminClient()
            .exportProbativeValue(
                new VitamContext(world.getTenantId())
                    .setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                probativeValueRequest
            );

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
            fail("Probative value processing not finished. Timeout exceeded.");
        }

        assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
        this.lastProbativeValueOperationId = operationId;
    }

    @Then("^le périmètre effectif du rapport de valeur probante contient les unités ayant pour titres$")
    public void probativeValueIncludeDetachedSigningInformation(List<String> expectedUnitTitles) throws Exception {
        VitamContext vitamContext = new VitamContext(world.getTenantId())
            .setAccessContract(world.getContractId())
            .setApplicationSessionId(world.getApplicationSessionId());

        JsonNode reportContent;
        try (
            Response response = world
                .getAdminClient()
                .downloadRulesReport(vitamContext, this.lastProbativeValueOperationId);
            InputStream is = response.readEntity(InputStream.class)
        ) {
            reportContent = JsonHandler.getFromInputStream(is);
        }

        List<String> unitIds = new ArrayList<>();
        for (JsonNode reportEntry : reportContent.get("reportEntries")) {
            for (JsonNode unitIdNode : reportEntry.get("unitIds")) {
                unitIds.add(unitIdNode.asText());
            }
        }

        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
        selectMultiQuery.addRoots(unitIds.toArray(String[]::new));
        selectMultiQuery.addUsedProjection("Title");
        RequestResponseOK<JsonNode> selectedUnits = (RequestResponseOK<JsonNode>) world
            .getAccessClient()
            .selectUnits(vitamContext, selectMultiQuery.getFinalSelect());

        List<String> foundUnitTitles = selectedUnits
            .getResults()
            .stream()
            .map(unit -> unit.get("Title").asText())
            .collect(Collectors.toList());

        assertThat(foundUnitTitles).hasSameSizeAs(unitIds);
        assertThat(foundUnitTitles).containsExactlyInAnyOrderElementsOf(expectedUnitTitles);
    }
}
