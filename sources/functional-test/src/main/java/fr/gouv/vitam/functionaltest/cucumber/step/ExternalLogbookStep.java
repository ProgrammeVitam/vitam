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
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;

import javax.ws.rs.core.Response.Status;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * External Logbook Step
 */
public class ExternalLogbookStep extends CommonStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ExternalLogbookStep.class);

    public ExternalLogbookStep(World world) {
        super(world);
    }

    private String fileName;

    /**
     * @return generic Model
     */
    public JsonNode getModel() {
        return model;
    }

    public void setModel(JsonNode model) {
        this.model = model;
    }

    /**
     * generic model result
     */
    private JsonNode model;

    /**
     * define a sip
     *
     * @param fileName name of a sip
     */
    @Given("^un fichier de logbook operation nommé (.*)$")
    public void a_sip_named(String fileName) {
        this.fileName = fileName;
    }

    @Then("^j'importe un journal d'opération correct$")
    public void createExternalLogbook() {
        callAdminExternal("CREATED");
    }

    @Then("^j'importe un journal d'opération incorrect$")
    public void createExternalLogbookKO() {
        callAdminExternal("BAD_REQUEST");
    }

    private void callAdminExternal(String expectedStatus) {
        Path logbookFile = Paths.get(world.getBaseDirectory(), fileName);

        try {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(world.getTenantId());

            String logbookString = JsonHandler.unprettyPrint(JsonHandler.getFromFile(logbookFile.toFile()));
            // String logbookString = FileUtil.readFile(logbookFile.toFile());

            logbookString = logbookString.replace("REPLACE_ME", operationGuid.getId());

            LogbookOperationParameters logbookOperationParams = JsonHandler.getFromString(
                logbookString,
                LogbookOperationParameters.class
            );

            RequestResponse response = world
                .getAdminClient()
                .createExternalOperation(new VitamContext(world.getTenantId()), logbookOperationParams);
            Status statusExpected = Status.CREATED;
            switch (expectedStatus) {
                case "CREATED":
                    statusExpected = Status.CREATED;
                    break;
                case "BAD_REQUEST":
                    statusExpected = Status.BAD_REQUEST;
            }
            assertThat(response.getHttpCode()).isEqualTo(statusExpected.getStatusCode());
            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            world.setOperationId(operationId);
        } catch (Exception e) {
            fail("should not produce this exception", e);
        }
    }
}
